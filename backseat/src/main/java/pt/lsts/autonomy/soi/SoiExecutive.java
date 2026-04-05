package pt.lsts.autonomy.soi;

import pt.lsts.backseat.TimedFSM;
import pt.lsts.endurance.Plan;
import pt.lsts.endurance.Waypoint;
import pt.lsts.imc4j.annotations.Consume;
import pt.lsts.imc4j.annotations.Parameter;
import pt.lsts.imc4j.annotations.FieldChange;
import pt.lsts.imc4j.def.SpeedUnits;
import pt.lsts.imc4j.msg.DesiredSpeed;
import pt.lsts.imc4j.msg.EntityParameter;
import pt.lsts.imc4j.msg.EntityParameters;
import pt.lsts.imc4j.msg.EstimatedState;
import pt.lsts.imc4j.msg.FollowRefState;
import pt.lsts.imc4j.msg.FuelLevel;
import pt.lsts.imc4j.msg.GpsFix;
import pt.lsts.imc4j.msg.Message;
import pt.lsts.imc4j.msg.PlanControl;
import pt.lsts.imc4j.msg.PlanControlState;
import pt.lsts.imc4j.msg.ReportControl;
import pt.lsts.imc4j.msg.Salinity;
import pt.lsts.imc4j.msg.SoiCommand;
import pt.lsts.imc4j.msg.StateReport;
import pt.lsts.imc4j.msg.Temperature;
import pt.lsts.imc4j.msg.TextMessage;
import pt.lsts.imc4j.msg.TransmissionRequest;
import pt.lsts.imc4j.msg.VehicleMedium;
import pt.lsts.imc4j.msg.VerticalProfile;
import pt.lsts.imc4j.msg.VerticalProfile.PARAMETER;
import pt.lsts.imc4j.util.PojoConfig;
import pt.lsts.imc4j.util.TupleList;
import pt.lsts.imc4j.util.WGS84Utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

public class SoiExecutive extends TimedFSM {

    static class MessageRequest {
        Message msg;
        int ttl;
        int tries;
        int max_tries;

        public MessageRequest(Message m, int ttl, int max_tries) {
            msg = m;
            this.ttl = ttl;
            tries = 0;
            this.max_tries = max_tries;
        }
    }

    private static final double TWO_PI_RADS = Math.PI * 2.0;

    // Maximum Iridium Packet size - TODO: Check if not 340 or 250
    private static final int MAX_IR_SIZE = 320;
    private static final String SOI_PLAN_ID = "soi_plan";
    private static final int ANGLE_DIFF_DEGS = 5;
    protected static File CONFIG_FILE = null;
    final private ArrayList<String> txtMessages = new ArrayList<>();
    final private ArrayList<MessageRequest> imcMessages = new ArrayList<>();
    // Transmissions that MUST be sent!
    final private LinkedHashMap<Integer, MessageRequest> pendingTransmissions = new LinkedHashMap<>();
    private DataProfiler<Temperature> tempProfiler;
    private DataProfiler<Salinity> salProfiler;

    @Parameter(description = "Nominal Speed")
    public double speed = 1;
    @Parameter(description = "Maximum Depth")
    public double maxDepth = 10;
    @Parameter(description = "Minimum Depth")
    public double minDepth = 0.0;
    @Parameter(description = "Maximum Speed")
    public double maxSpeed = 1.5;
    @Parameter(description = "Minimum Speed")
    public double minSpeed = 0.7;
    @Parameter(description = "DUNE Host Address")
    public String hAddr = "127.0.0.1";
    @Parameter(description = "DUNE Host Port (TCP)")
    public int hPort = 6006;
    @Parameter(description = "Minutes before termination")
    public int timeout = 30;
    @Parameter(description = "Maximum time without reporting position")
    public int minsOff = 15;
    @Parameter(description = "Maximum time without GPS")
    public int minsUnder = 3;
    @Parameter(description = "Seconds to idle at each vertex")
    public int wptSecs = 60;
    @Parameter(description = "Maximum seconds to wait at each vertex")
    public int wptMaxWait = 90;
    @Parameter(description = "Distance to consider waypoint was reached")
    public double wptDst = 50.0;
    @Parameter(description = "Cyclic execution")
    public boolean cycle = false;
    @Parameter(description = "Speed up before descending")
    public int descRpm = 1300;
    @Parameter(description = "Upload temperature profiles")
    public boolean upTemp = false;
    @Parameter(description = "Upload salinity profiles")
    public boolean upSal = true;
    @Parameter(description = "Align with destination waypoint before going underwater")
    public boolean align = true;
    @Parameter(description = "Split transects based on maximum offline time")
    public boolean split = false;
    @Parameter(description = "Use vertical profile as the data profiler")
    public boolean useVP = true;
    @Parameter(description = "Sample to send for meters covered")
    public double space_resolution = 360.0;
    @Parameter(description = "Depth tolerance to consider as reached")
    public double depthTolerance = 0.3;

    private Plan plan = new Plan("idle");
    private int secs_no_comms = 0;
    private double last_report_ts;
    private int count_secs = 0;
    private int secs_underwater = 0;
    private int wpt_index = 0;

    private double distanceTraveled = 0;
    private double[] lastPosition = null;
    // Starting position of plan.
    private double[] startPos = null;
    // seconds at the surface
    private long secs_surface = 0;

    private GpsFix valid = null;

    // FSM state references for identity-based comparison
    protected final FSMState endOfDeadlineState = this::endOfDeadline;
    protected final FSMState idleAtSurfaceState = this::idleAtSurface;
    protected final FSMState idleState = this::idle;
    protected final FSMState execState = this::exec;
    protected final FSMState descendState = this::descend;
    protected final FSMState ascendState = this::ascend;
    protected final FSMState keepSurfaceState = this::keepSurface;
    protected final FSMState alignState = this::align;
    protected final FSMState diveState = this::dive;
    protected final FSMState communicateState = this::communicate;
    protected final FSMState surfaceWaypointState = this::surfaceWaypoint;
    protected final FSMState onWaypointState = this::onWaypoint;
    protected final FSMState startWaitingState = this::start_waiting;
    protected final FSMState waitState = this::wait;
    protected final FSMState sendPositionState = this::sendPosition;
    protected final FSMState criticalErrorState = this::criticalError;
    protected final FSMState reportErrorsState = this::reportErrors;
    protected final FSMState getGPSState = this::getGPS;

    /**
     * Class constructor
     */
    public SoiExecutive() {
        setPlanName(SOI_PLAN_ID);
        deadline = null;
        state = idleAtSurfaceState;
    }

    @Override
    protected void onDeadline() {
        // Go to surface
        double[] deadlinePosition = getPosition();
        setLocation(deadlinePosition[0], deadlinePosition[1]);
        setDepth(0);

        print("Deadline reached. Surfacing to communicate.");
        txtMessages.add("INFO: Deadline reached.");

        // Scale number of profile samples by distance traveled (1 sample per 100m, min 2, max 20)
        int numSamples = Math.max(2, Math.min(10, (int) (distanceTraveled / space_resolution)));
        print("Distance traveled: " + (int) distanceTraveled + "m, sending " + numSamples + " profile samples.");
        if (upSal) {
            ArrayList<Message> vps = salProfiler.getProfile(PARAMETER.PROF_SALINITY, numSamples);
            vps.forEach(vps_ -> vps_.src = remoteSrc);
            queueMessages(vps, 60, 5);
        }
        if (upTemp) {
            ArrayList<Message> vps = tempProfiler.getProfile(PARAMETER.PROF_TEMPERATURE, numSamples);
            vps.forEach(vps_ -> vps_.src = remoteSrc);
            queueMessages(vps, 60, 5);
        }

        SoiCommand cmd = new SoiCommand();
        cmd.command = SoiCommand.COMMAND.SOICMD_GET_PLAN;
        cmd.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
        cmd.plan = null;
        cmd.src = remoteSrc;
        cmd.dst = 0xFFFF;
        cmd.info = "Deadline Reached: " + numSamples + " samples.";
        queueMessage(cmd, 60, 5);

        plan = null;
        wpt_index = 0;
        state = endOfDeadlineState;
        deadline = null;
        super.update(get(FollowRefState.class));
    }

    @FieldChange(field = "useVP")
    public void onProfilerChange() {

        tempProfiler = useVP ? new DepthBinnedProfiler<>() : new SubsamplingProfiler<>();
        salProfiler = useVP ? new DepthBinnedProfiler<>() : new SubsamplingProfiler<>();

        String text = String.format("Using %s profiler", useVP ? "Depth bin" : "Subsampling");
        print(text);
    }

    private static double normalizeAngleRads2Pi(double angle) {
        double ret = angle;
        ret = ret % TWO_PI_RADS;
        if (ret < 0.0) {
            ret += TWO_PI_RADS;
        }
        return ret;
    }

    /// Override to not allow finish to happen, but pause instead
    @Override
    public void end() {
        super.end();
        finished = false;
        setPaused(true);
    }

    /// Override to not allow finish to happen, but pause instead
    @Override
    public void startPlan(String id) {
        super.startPlan(id);
        finished = false;
        setPaused(true);
    }

    /**
     * In case the last plan failed, report the resulting error
     *
     * @param pControl A {@link PlanControl} message
     */
    @Consume
    public final void on(PlanControl pControl) {
        if (pControl.op == PlanControl.OP.PC_START && pControl.type == PlanControl.TYPE.PC_FAILURE) {
            if (pControl.plan_id.equals(getPlanName())) {
                // Error during execution!
                String err = "Detected error during execution: " + pControl.info;
                printError(err);
                txtMessages.add("ERROR: " + pControl.info);
                print("Ascending for report");
                state = criticalErrorState;
            }
        }

        if (pControl.op == PlanControl.OP.PC_STOP && pControl.type == PlanControl.TYPE.PC_SUCCESS) {
            state = idleAtSurfaceState;
        }
    }

    @Override
    public void setPaused(boolean paused) {
        boolean informPaused = !this.paused && paused;
        super.setPaused(paused);
        if (informPaused) {
            String txt = "INFO: Paused execution. Waiting for instructions.";
            txtMessages.add(txt);
            sendViaSms(txt, (int) Duration.ofMinutes(5).toMinutes());
            SoiCommand reply = new SoiCommand();
            reply.command = SoiCommand.COMMAND.SOICMD_STOP;
            reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
            reply.src = remoteSrc;
            reply.dst = 0xFFFF;
            reply.plan = null;
            reply.info = "Paused execution. Waiting for instructions.";
            queueMessage(reply, 30, 3);
        }
    }

    /**
     * React to incoming commands
     *
     * @param cmd The received command
     */
    @Consume
    public final void on(SoiCommand cmd) {
        if (cmd.type != SoiCommand.TYPE.SOITYPE_REQUEST) {
            return;
        }
        SoiCommand reply = new SoiCommand();
        reply.command = cmd.command;
        reply.type = SoiCommand.TYPE.SOITYPE_ERROR;
        reply.src = remoteSrc;
        reply.dst = cmd.src;
        reply.dst_ent = cmd.src_ent;

        FSMState nextState = null;

        switch (cmd.command) {

            case SOICMD_EXEC:
                print("CMD: Exec plan!");
                if (cmd.plan == null || cmd.plan.waypoints.isEmpty()) {
                    plan = null;
                    reply.type = SoiCommand.TYPE.SOITYPE_ERROR;
                    break;
                }

                plan = Plan.parse(cmd.plan);
                print("Received plan with settings: " + cmd.settings);
                parseSettings(cmd.settings, reply);
                if (paused) {
                    setPaused(false);
                }

                // Reset only if no current deadline available
                if (deadline == null) {
                    resetDeadline();
                }

                if (!plan.scheduledInTheFuture()) {
                    double[] pos = getPosition();
                    if (pos != null) {
                        plan.scheduleWaypoints(System.currentTimeMillis(), wptSecs, pos[0], pos[1], speed, split ?
                                minsOff * 60 : 0);
                    }
                    else {
                        plan.scheduleWaypoints(System.currentTimeMillis(), wptSecs, speed, split ? minsOff
                                                                                                   * 60 : 0);
                    }
                }

                // ignore waypoints in the past
                wpt_index = 0;
                Date now = new Date();
                startPos = getPosition();
                for (; wpt_index < plan.waypoints().size(); wpt_index++) {

                    Waypoint wp = plan.waypoints().get(wpt_index);
                    if (arrivedWaypoint(wp) || hasPassedWaypoint(wp)) {
                        print("Skipping waypoint " + wpt_index + " - already passed");
                        continue;
                    }

                    if (plan.waypoint(wpt_index).getArrivalTime().before(now)) {
                        print("Skipping waypoint " + wpt_index + " as it is in the past.");
                        continue;
                    }

                    break;
                }

                if (wpt_index != 0) {
                    reply.info = "Skipped to waypoint " + wpt_index;
                }

                print("Start executing this plan:");
                print("" + plan);
                print("Plan serialization size is " + reply.serialize().length);
                reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;

                count_secs = 0;
                nextState = communicateState;
                break;

            case SOICMD_GET_PARAMS:
                print("CMD: Get Params!");
                reply.settings = params();
                reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
                break;

            case SOICMD_SET_PARAMS:
                print("CMD: Set Params!");
                parseSettings(cmd.settings, reply);
                break;

            case SOICMD_STOP:
                print("CMD: Stop execution!");
                setPaused(true);
                reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
                if (plan != null && !plan.waypoints().isEmpty()) {
                    reply.info = "was executing " + wpt_index + " of " + plan.waypoints().size();
                }
                break;

            case SOICMD_GET_PLAN:
                print("CMD: Get plan!");
                if (plan != null) {
                    reply.plan = plan.asImc();
                }
                reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
                if (plan != null && !plan.waypoints().isEmpty()) {
                    reply.info = "is executing " + wpt_index + " of " + plan.waypoints().size();
                }
                break;

            case SOICMD_RESUME:
                print("CMD: Resume execution!");
//                resetDeadline();
                reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
                if (paused) {
                    setPaused(false);
                    reply.info = "was paused; ";
                    if (plan != null && !plan.waypoints().isEmpty()) {
                        reply.info += "going to " + wpt_index + " of " + plan.waypoints().size() + "; ";
                    }
                    //return; // removed this but added doChangeState=false to avoid changing state
                }
                else {
                    if (plan != null && !plan.waypoints().isEmpty()) {
                        reply.info = "is executing " + wpt_index + " of " + plan.waypoints().size() + "; ";
                    }
                }
                break;
            default:
                reply.info = "Unknown command";
                break;
        }

        print("Replying with " + reply);

        // If message is too large to send over Iridium, try to split its settings
        switch (reply.command) {
            case SOICMD_EXEC:
            case SOICMD_GET_PARAMS:
            case SOICMD_SET_PARAMS:
                if (reply.serialize().length > MAX_IR_SIZE) {
                    ArrayList<SoiCommand> cmds = splitSettings(reply);
                    if (cmds == null) {
                        return;
                    }
                    queueMessages(cmds, 60, 1);
                }
                else {
                    queueMessage(reply, 60, 2);
                }
                break;
            default:
                queueMessage(reply, 120, 1);
                break;
        }

        if (nextState != null) {
            state = nextState;
        }
    }

    @Consume
    public final void on(GpsFix fix) {

        if (!fix.validity.contains(GpsFix.VALIDITY.GFV_VALID_POS)) {
            return;
        }
        valid = fix;
    }

    @Override
    public boolean hasGps(double ageSeconds) {
        return (System.currentTimeMillis() / 1000.0 - valid.timestamp) < ageSeconds;
    }

    double calculateBearing(double[] start, double[] end) {
        double[] diff = WGS84Utilities.WGS84displacement(start[0], start[1], 0, end[0], end[1], 0);
        return Math.toDegrees(Math.atan2(diff[1], diff[0]));
    }

    private double getDesiredBearing() throws RuntimeException {
        Waypoint tgt = plan.waypoint(wpt_index);
        // Plan ended
        if (tgt == null) {
            throw new RuntimeException("Plan ended!");
        }

        double[] end_deg = new double[2];
        end_deg[0] = tgt.getLatitude();
        end_deg[1] = tgt.getLongitude();

        double[] start_deg;
        if (wpt_index == 0) {
            start_deg = startPos;
        }
        else {
            start_deg = new double[2];
            Waypoint s = plan.waypoint(wpt_index - 1);
            start_deg[0] = s.getLatitude();
            start_deg[1] = s.getLongitude();
        }

        return calculateBearing(start_deg, end_deg);
    }

    private void parseSettings(TupleList settings, SoiCommand reply) {
        TupleList oldSettings = params();
        try {
            for (String key : settings.keys()) {
                try {
                    PojoConfig.setProperty(this, key, settings.get(key));
                }
                catch (Exception e) {
                    printException(e);
                }
            }

            reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
            TupleList diffSettings = oldSettings.diff(params());

            reply.settings = diffSettings;

            if (diffSettings.keys().isEmpty()) {
                reply.info = "no changes";
            }
            else {
                reply.info = "parameters changed";
            }

            saveConfig(CONFIG_FILE);
            print("Config saved to " + CONFIG_FILE.getAbsolutePath());

        }
        catch (Exception e) {
            printException(e);
        }
    }

    private ArrayList<SoiCommand> splitSettings(SoiCommand cmd) {
        TupleList settings = cmd.settings;
        List<String> keys = new ArrayList<>(settings.keys());
        Collections.sort(keys);

        ArrayList<SoiCommand> cmds = new ArrayList<>();
        SoiCommand clone;
        try {
            clone = (SoiCommand) SoiCommand.deserialize(cmd.serialize());
        }
        catch (Exception e) {
            printException(e);
            return null;
        }
        clone.settings = new TupleList();

        for (String key : keys) {

            clone.settings.set(key, cmd.settings.get(key));

            if (clone.serialize().length > MAX_IR_SIZE) {
                clone.settings.remove(key);
                cmds.add(clone);
                try {
                    clone = (SoiCommand) SoiCommand.deserialize(cmd.serialize());
                }
                catch (Exception e) {
                    printException(e);
                    return null;
                }

                clone.settings = new TupleList();
                clone.settings.set(key, cmd.settings.get(key));
            }
        }

        if (!clone.settings.keys().isEmpty()) {
            cmds.add(clone);
        }

        return cmds;
    }

    private TupleList params() {
        TupleList settings = new TupleList();

        Class<?> clazz = getClass();

        do {
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Parameter p = f.getAnnotation(Parameter.class);
                if (p == null) {
                    continue;
                }
                String name = f.getName();
                try {
                    settings.set(name, f.get(this));
                }
                catch (Exception e) {
                    printException(e);
                }
            }
            clazz = clazz.getSuperclass();
        } while (!clazz.equals(Object.class));

        return settings;
    }

    @Consume
    public final void on(EntityParameters params) {
        if (params.name.equals(getClass().getSimpleName())) {
            for (EntityParameter param : params.params) {
                try {
                    PojoConfig.setValue(this, param.name, param.value);
                    print("Set " + param.name + " := " + param.value);

                    saveConfig(CONFIG_FILE);
                    print("Config saved to " + CONFIG_FILE.getAbsolutePath());

                }
                catch (Exception e) {
                    printException(e);
                }
            }
        }
    }

    @Consume
    public final void on(Temperature temp) {
        tempProfiler.setSample(get(EstimatedState.class), temp);
    }

    public boolean invalidSalinity(Salinity sal) {
        if (sal == null) {
            return true;
        }

        return sal.value < 0 || sal.value > 100;
    }

    @Consume
    public final void on(Salinity sal) {

        if (invalidSalinity(sal)) {
            return;
        }

        salProfiler.setSample(get(EstimatedState.class), sal);
    }

    @Consume
    public final void on(VehicleMedium medium) {
        secs_underwater = (medium.medium == VehicleMedium.MEDIUM.VM_UNDERWATER) ? secs_underwater + 1 : 0;
        secs_surface = (medium.medium == VehicleMedium.MEDIUM.VM_WATER) ? secs_surface + 1 : 0;
    }

    @Override
    protected void onTransmissionSuccess(TransmissionRequest treq) {
        pendingTransmissions.remove(treq.req_id);
    }

    @Override
    protected void onTransmissionFailed(TransmissionRequest treq) {
        MessageRequest m = pendingTransmissions.remove(treq.req_id);
        if (m == null) {
            return;
        }

        if (m.tries >= m.max_tries) {
            return;
        }

        List<Integer> ids = sendViaIridium(m.msg, m.ttl);
        m.tries++;
        for (Integer id : ids) {
            pendingTransmissions.put(id, m);
        }
    }

    public void trySend(Message msg) {
        try {
            send(msg);
        }
        catch (IOException e) {
            printException(e);
        }
    }

    public FSMState endOfDeadline(FollowRefState state) {
        printFSMState();

        if (!atSurface()) {
            setDepth(0);
            return endOfDeadlineState;
        }

        // Send all pending messages via Iridium and track their request IDs
        sendMessages();

        // Transition only when every profile transmission has been confirmed
        if (pendingTransmissions.isEmpty()) {
            print("All profile transmissions confirmed. Transitioning to wait.");
            return idleAtSurfaceState;
        }

        return endOfDeadlineState;
    }

    /**
     * Actively go to the surface to wait for a plan
     */
    public FSMState idleAtSurface(FollowRefState state) {
        printFSMState();
        double[] pos = getPosition();
        setLocation(pos[0], pos[1]);
        setDepth(0);
        setSpeed(speed, SpeedUnits.METERS_PS);
        return idleState;
    }

    /**
     * Wait for a plan to arrive
     */
    public FSMState idle(FollowRefState state) {
        printFSMState();

        if (atSurface()) {
            sendMessages();
        }

        FSMState newState = onIdle();
        return newState != null ? newState : idleState;
    }

    protected FSMState onIdle() {
        return null;
    }

    protected FSMState onSalinityProfile(VerticalProfile salinity) {
        return null;
    }

    protected FSMState onTemperatureProfile(VerticalProfile salinity) {
        return null;
    }

    private double distanceWaypoint(int idx) {
        return distanceWaypoint(plan.waypoint(idx));
    }

    private double distanceWaypoint(Waypoint wpt) {
        if (wpt == null) {
            return 0;
        }

        double[] pos = getPosition();
        return WGS84Utilities.distance(pos[0], pos[1],
                wpt.getLatitude(), wpt.getLongitude());
    }

    private boolean arrivedWaypoint(Waypoint wpt) {
        return distanceWaypoint(wpt) < wptDst;
    }

    private boolean hasPassedWaypoint(int idx) {
        return hasPassedWaypoint(plan.waypoint(idx));
    }

    private boolean hasPassedWaypoint(Waypoint tgt) {
        if (tgt == null) {
            return true;
        }

        double[] end_deg = new double[2];
        end_deg[0] = tgt.getLatitude();
        end_deg[1] = tgt.getLongitude();

        double[] start_deg = getPosition();
        double desiredBearing;
        try {
            desiredBearing = getDesiredBearing();
        }
        catch (Exception e) {
            // Plan Ended!
            return true;
        }

        double currBearing = calculateBearing(start_deg, end_deg);
        return Math.abs(currBearing - desiredBearing) > 90;
    }

    /**
     * Updates the communication timer and evaluates global transition guards.
     * <p>
     * This method checks for safety-critical conditions (communication timeouts) and mission progress (reaching a
     * waypoint) that trigger a state change regardless of the current FSM activity.
     *
     * @return The next {@link FSMState} to transition to, or {@code null} if no transition is required.
     */
    private FSMState checkTransitions(FollowRefState ref) {
        secs_no_comms++;
        updateDistanceTraveled();

        // Not communicated position for too long
        double now = System.currentTimeMillis() / 1000.0;
        double delta_secs = (now - last_report_ts);
        if ((delta_secs > minsOff * 60) && !isInState(sendPositionState)) {
            count_secs = 0;
            setDepth(0);
            print("Not send a valid position in " + delta_secs + " seconds");
            return sendPositionState;
        }

        // No GPS for too long and not waiting for a new GPS signal
        if (!hasGps(minsUnder * 60) && !isInState(getGPSState)) {
            print("No GPS for too long (" + secs_no_comms + ")");
            return getGPSState;
        }

        if (arrivedXY() && ref.state != FollowRefState.STATE.FR_HOVER) {
            print("Should be hovering by now!! forcing update ...");
            updateSpeed();
        }

        if (hasPassedWaypoint(wpt_index)) {
            print("Passed waypoint " + wpt_index);
            return onWaypointState;
        }

        return null;
    }

    private void updateDistanceTraveled() {
        double[] pos = getPosition();
        if (pos == null) {
            return;
        }
        if (lastPosition != null) {
            distanceTraveled += WGS84Utilities.distance(lastPosition[0], lastPosition[1], pos[0], pos[1]);
        }
        lastPosition = pos;
    }

    public FSMState startCyclePlan() {
        print("Starting over (cyclic)...");
        wpt_index = 0;
        plan.removeSchedule();
        double[] pos = getPosition();
        if (pos != null) {
            plan.scheduleWaypoints(System.currentTimeMillis(), wptSecs, pos[0], pos[1], speed, minsOff * 60);
        }
        else {
            plan.scheduleWaypoints(System.currentTimeMillis(), wptSecs, speed, minsOff * 60);
        }

        if (plan.getETA().after(deadline)) {
            int timeDiff = (int) ((plan.getETA().getTime() - deadline.getTime()) / 1000.0);
            String err =
                    "Cycled. Deadline would be reached " + timeDiff + " seconds before the end of the plan";
            printError(err);
            plan = null;
            txtMessages.add(err);
            return idleAtSurfaceState;
        }

        SoiCommand reply = new SoiCommand();
        reply.command = SoiCommand.COMMAND.SOICMD_GET_PLAN;
        reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
        reply.src = remoteSrc;
        reply.dst = 0xFFFF;
        reply.plan = plan.asImc();
        reply.info = "Restart cycled plan.";
        queueMessage(reply, 120, 1);

        return startWaitingState;
    }

    /**
     * Execute the next waypoint
     */
    public FSMState exec(FollowRefState state) {
        printFSMState();
        if (plan == null || plan.waypoints().isEmpty()) {
            return idleAtSurfaceState;
        }

        Waypoint wpt = plan.waypoint(wpt_index);
        if (wpt == null) {
            print("Finished executing plan.");
            if (cycle && plan != null) {
                return startCyclePlan();
            }

            plan = null;
            wpt_index = 0;
            return idleAtSurfaceState;
        }

        print("Executing wpt " + wpt_index);
        setLocation(wpt.getLatitude(), wpt.getLongitude());
        setSpeed();

        return alignState;
    }

    private boolean planEnded() {

        // No valid plan was running
        if (plan == null || plan.waypoints().isEmpty()) {
            return false;
        }

        Waypoint wpt = plan.waypoint(wpt_index);
        if (cycle) {
            return plan.getETA().after(deadline);
        }

        return wpt == null;
    }

    private boolean incompleteYoYo() {

        // TODO add time check within deadline!
        try {
            double dist = distanceWaypoint(wpt_index);

            // Rough estimate: horizontal distance for a full yo-yo cycle
            // descent vertical speed ~0.14 m/s, ascent vertical speed ~0.40 m/s
            // Add a 10% error to be conservative
            // TODO: Add parameters for these velocities
            // TODO: Add a conversion from requested speed to dive and ascend speed
            double yoyoDistance = speed * maxDepth * (1.0 / 0.14 + 1.0 / 0.40) * 1.1;
            if (dist < yoyoDistance) {
                print("Waypoint " + wpt_index + ": distance " + Math.round(dist)
                        + " m < yo-yo needs " + Math.round(yoyoDistance)
                        + " m. Staying at minDepth.");
                return true;
            }
        }
        catch (Exception e) {
            printException(e);
        }
        return false;
    }

    private void setAndInformEndOfPlan() {

        wpt_index = 0;
        plan = null;

        String txtDeadline = "INFO: Finished plan execution. Waiting instructions.";
        txtMessages.add(txtDeadline);
        SoiCommand reply = new SoiCommand();
        reply.command = SoiCommand.COMMAND.SOICMD_GET_PLAN;
        reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
        reply.src = remoteSrc;
        reply.dst = 0xFFFF;
        reply.plan = null;
        reply.info = "Finished plan execution. Waiting instructions.";
        queueMessage(reply, 60, 3);
    }

    /**
     * Go to maximum depth
     */
    public FSMState descend(FollowRefState ref) {
        printFSMState();
        setDepth(maxDepth);

        FSMState next = checkTransitions(ref);
        if (next != null) {
            return next;
        }

        try {
            double[] cur_pos = getPosition();
            double[] target_pos = new double[]{plan.waypoint(wpt_index).getLatitude(),
                    plan.waypoint(wpt_index).getLongitude()};

            double dist = WGS84Utilities.distance(cur_pos[0], cur_pos[1], target_pos[0], target_pos[1]);
            double min_dist = 4 * cur_pos[2];

            if (dist < min_dist) {
                print("!!!!!!!!!! Starting to ascend, getting close to destination.  " + Math.round(dist) + " < "
                        + Math.round(min_dist) + " (cur depth " + Math.round(cur_pos[2]) + ")");
                return ascendState;
            }
        }
        catch (Exception e) {
            printException(e);
        }

        if (arrivedZ()) {
            setSpeed();
            if (minDepth < maxDepth) {
                print("Now ascending.");
            }
            return ascendState;
        }
        else {
            return descendState;
        }
    }

    /**
     * Go to minimum depth
     */
    public FSMState ascend(FollowRefState ref) {
        printFSMState();
        setDepth(minDepth);

        FSMState next = checkTransitions(ref);
        if (next != null) {
            return next;
        }

        if (!arrivedDepth(minDepth, depthTolerance)) {
            return ascendState;
        }

        if (incompleteYoYo()) {
            setDepth(0);
            return keepSurfaceState;
        }

        if (isUnderwater()) {
            return descendState;
        }
        else if (align) {
            return alignState;
        }

        return diveState;
    }

    public FSMState keepSurface(FollowRefState ref) {
        printFSMState();
        setDepth(0);

        FSMState next = checkTransitions(ref);
        if (next != null) {
            return next;
        }

        if (atSurface()) {
            sendMessages();
        }

        return keepSurfaceState;
    }

    /**
     * Right before diving, align yaw with the target waypoint
     */
    public FSMState align(FollowRefState ref) {
        printFSMState();
        EstimatedState state = get(EstimatedState.class);
        double[] pos = WGS84Utilities.toLatLonDepth(state);

        FSMState next = checkTransitions(ref);
        if (next != null) {
            return next;
        }

        double[] dest = getDestinationDegs();
        // Difference between current and destination location
        double[] diff = WGS84Utilities.WGS84displacement(pos[0], pos[1], 0, dest[0], dest[1], 0);
        double des_ang = Math.toDegrees(Math.atan2(diff[1], diff[0]));
        double cur_ang = Math.toDegrees(state.psi);
        // Angle difference to destination (degrees)
        double ang_diff = Math.abs(des_ang - cur_ang);

        if (descRpm > 0) {
            setSpeed(descRpm, SpeedUnits.RPM);
        }

        // go underwater only if aligned with destination
        if (ang_diff < ANGLE_DIFF_DEGS) {
            if (incompleteYoYo()) {
                setDepth(0);
                setSpeed(speed, SpeedUnits.METERS_PS);
                return keepSurfaceState;
            }
            setDepth(maxDepth);
            return diveState;
        }
        else {
            setDepth(0);
            return alignState;
        }
    }

    /**
     * Go underwater at a fixed RPM speed
     */
    public FSMState dive(FollowRefState ref) {
        printFSMState();
        double[] pos = getPosition();

        FSMState next = checkTransitions(ref);
        if (next != null) {
            return next;
        }

        setDepth(maxDepth);
        if (descRpm > 0) {
            setSpeed(descRpm, SpeedUnits.RPM);
        }

        if (pos[2] < 2 && pos[2] < maxDepth) {
            return diveState;
        }
        else {
            setSpeed();
            return descendState;
        }
    }

    /**
     * Send a position report and any pending replies / errors
     */
    public FSMState communicate(FollowRefState ref) {
        printFSMState();

        if (planEnded()) {
            setAndInformEndOfPlan();
        }

        int min_wait = wptSecs;
        int max_wait = wptMaxWait;

        // Send "DUNE" report at communication start
        if (count_secs == 0) {

            EnumSet<ReportControl.COMM_INTERFACE> itfs = EnumSet.of(ReportControl.COMM_INTERFACE.CI_GSM);
            sendReport(itfs);
            queueMessage(createStateReport(), max_wait, 1);
            print("Will wait from " + min_wait + " to " + max_wait + " seconds to send " + txtMessages.size()
                    + " texts, " + imcMessages.size() + " imc messages.");
        }

        if (!atSurface()) {
            setDepth(0);
        }
        else {
            sendMessages();
        }

        if (count_secs >= max_wait) {
            print("Advancing to next waypoint as maximum time was reached.");
            return execState;
        }

        if (pendingTransmissions.isEmpty()) {
            print("Completed all transmissions. Advancing to next waypoint");
            return execState;
        }

        count_secs++;
        return communicateState;

    }

    public FSMState surfaceWaypoint(FollowRefState ref) {
        printFSMState();

        setDepth(0);
        if (!atSurface()) {
            return surfaceWaypointState;
        }

        wpt_index++;
        count_secs = 0;
        return communicateState;
    }

    /// Waypoint was reached!
    public FSMState onWaypoint(FollowRefState ref) {
        printFSMState();

        double[] pos = getPosition();
        setLocation(pos[0], pos[1]);
        setDepth(0);

        print("Surfacing at waypoint ...");
        return surfaceWaypointState;
    }

    /**
     * Request the vehicle to (actively) go at the surface
     */
    public FSMState start_waiting(FollowRefState ref) {
        printFSMState();
        double[] pos = getPosition();
        setLocation(pos[0], pos[1]);
        setDepth(0);
        setSpeed(speed, SpeedUnits.METERS_PS);

        print("Surfacing...");
        return waitState;
    }

    /**
     * Send a {@link StateReport} via Iridium when the vehicle has been offline for too long.
     * <p>
     * Entered from {@link #checkTransitions} when {@code secs_no_comms / 60 > minsOff}. Sends the report on the first
     * tick, then waits for DUNE to confirm the Iridium transmission was successful before resuming execution.
     */
    public FSMState sendPosition(FollowRefState ref) {
        printFSMState();
        setDepth(0);

        FSMState next = checkTransitions(ref);
        if (next != null) {
            return next;
        }

        if (!atSurface()) {
            count_secs = 0;
            return sendPositionState;
        }

        if (count_secs == 0) {
            queueMessage(createStateReport(), 60, 3);
            print("Position report queued. Waiting for transmission confirmation...");
        }

        count_secs++;
        sendMessages();

        if (pendingTransmissions.isEmpty()) {
            print("Position report transmitted successfully. Resuming execution...");
            secs_no_comms = 0;
            count_secs = 0;
            return execState;
        }

        return sendPositionState;
    }

    public FSMState criticalError(FollowRefState ref) {
        printFSMState();

        double[] pos = getPosition();
        // TODO Should StationKeep at current position!
        setLocation(pos[0], pos[1]);
        setDepth(0);
        count_secs = 0;
        return reportErrorsState;
    }

    protected void queueMessages(Collection<? extends Message> c, int ttl, int tries) {
        for (Message item : c) {
            queueMessage(item, ttl, tries);
        }
    }

    protected void queueMessage(Message m, int ttl, int tries) {

        if (ttl < 0) {
            ttl = Integer.MAX_VALUE;
        }

        imcMessages.add(new MessageRequest(m, ttl, tries));
    }

    protected void sendMessages() {
        for (String txt : txtMessages) {
            // TODO: Why was this 132?
            if (txt.length() > 132) {
                txt = txt.substring(0, 132);
            }
            sendViaSms(txt, 60);
            sendViaIridium(txt, 60);

            // Send message to DUNE LOG
            TextMessage tmsg = new TextMessage();
            tmsg.origin = "Soi-exec";
            tmsg.text = txt;
            trySend(tmsg);
        }
        txtMessages.clear();

        for (MessageRequest msg : imcMessages) {
            List<Integer> reqIds = sendViaIridium(msg.msg, msg.ttl);
            msg.tries++;
            for (Integer id : reqIds) {
                pendingTransmissions.put(id, msg);
            }
        }
        imcMessages.clear();
    }

    /**
     * Critical error recovery state: vehicle spent more than 10 seconds at the surface without a valid GPS fix in
     * {@link #getGPS}.
     * <p>
     * On the first tick, sends an error text message via SMS and Iridium and dispatches a {@link StateReport}. Then
     * waits for all Iridium transmissions to be confirmed
     * <b>and</b> for {@link #hasGps(double)} to return true before resuming execution.
     */
    public FSMState reportErrors(FollowRefState ref) {
        printFSMState();

        if (!atSurface()) {
            return reportErrorsState;
        }

        if (count_secs == 0) {
            queueMessage(createStateReport(), 60, 3);
        }

        sendMessages();
        count_secs++;

        boolean transmitted = pendingTransmissions.isEmpty();
        boolean gpsAcquired = hasGps(minsUnder * 60);

        if (transmitted && gpsAcquired) {
            print("Error reported and GPS fix acquired. Resuming execution...");
            count_secs = 0;
            return execState;
        }

        return reportErrorsState;
    }

    // Keep going to waypoint but go to surface to report Position!
    public FSMState getGPS(FollowRefState ref) {
        printFSMState();

        setDepth(0);
        FSMState next = checkTransitions(ref);
        if (next != null) {
            return next;
        }

        if (atSurface()) {
            sendMessages();
        }

        if (secs_surface > 10) {
            String errorMsg = "Error: 10 secs at Surface with no GPS!";
            print(errorMsg);
            txtMessages.add(errorMsg);
            return criticalErrorState;
        }

        if (hasGps(minsUnder * 60)) {
            print("Got new valid GPSFix! Resuming executing...");
            return execState;
        }

        return getGPSState;
    }

    /**
     * Actively go at the surface before communications
     */
    public FSMState wait(FollowRefState ref) {
        printFSMState();
        secs_no_comms++;

        // arrived at surface
        if (atSurface()) {
            print("Now at surface, starting communications.");
            double[] pos = getPosition();
            setLocation(pos[0], pos[1]);
            secs_no_comms = 0;
            count_secs = 0;
            return communicateState;
        }
        else {
            return waitState;
        }
    }

    public void updateSpeed() {
        // Hack if DUNE did not enter hover mode.
        // An update to the reference will force it to enter.
        DesiredSpeed curr = getSpeed();
        if (curr == null) {
            setSpeed(minSpeed, SpeedUnits.METERS_PS);
            return;
        }

        double delta = (Math.random() - 0.5) * 0.001;
        double new_val = Math.max(minSpeed, Math.min(maxSpeed, curr.value + delta));
        setSpeed(new_val, curr.speed_units);
    }

    /**
     * Set the desired speed based on current ETA and distance
     */
    public void setSpeed() {
        Waypoint wpt = plan.waypoint(wpt_index);
        double speed = this.speed;

        if (wpt == null) {
            speed = 0;
        }

        else if (wpt.getArrivalTime() != null) {
            double[] pos = getPosition();
            double dist = WGS84Utilities.distance(wpt.getLatitude(), wpt.getLongitude(), pos[0], pos[1]);
            double secs = (wpt.getArrivalTime().getTime() - System.currentTimeMillis()) / 1000.0;

            if (secs < 0) {
                speed = maxSpeed;
            }
            else {
                speed = Math.min(maxSpeed, dist / secs);
                speed = Math.max(minSpeed, speed);
            }
        }

        print("Setting speed according to ETA: " + speed + " m/s.");
        setSpeed(speed, SpeedUnits.METERS_PS);
    }

    /**
     * Reset watchdog based on a timeout parameter
     */
    private void resetDeadline() {
        distanceTraveled = 0;
        lastPosition = null;
        deadline = new Date(System.currentTimeMillis() + (long) timeout * 60 * 1000);
        String txtDeadline = "INFO: Execution will end by " + deadline;
        txtMessages.add(txtDeadline);
        setDeadline(deadline);
        print(txtDeadline);
    }

    /**
     * Generate a state report to be sent over Iridium
     *
     * @return A {@link StateReport} to be sent over Iridium
     */
    private StateReport createStateReport() {
        EstimatedState estate = get(EstimatedState.class);
        FuelLevel flevel = get(FuelLevel.class);
        PlanControlState pcs = get(PlanControlState.class);

        StateReport report = new StateReport();
        report.depth = estate == null || estate.depth == -1 ? 0xFFFF : (int) (estate.depth * 10);
        report.altitude = estate == null || estate.alt == -1 ? 0xFFFF : (int) (estate.alt * 10);
        report.speed = estate == null ? 0xFFFF : (int) (estate.u * 100);
        report.fuel = flevel == null ? -1 : (int) flevel.value;

        if (estate != null) {
            double[] loc = WGS84Utilities.toLatLonDepth(estate);
            report.latitude = (float) loc[0];
            report.longitude = (float) loc[1];
            double rads = estate.psi;
            rads = normalizeAngleRads2Pi(rads);
            report.heading = (int) ((rads / TWO_PI_RADS) * 65535);
        }

        report.exec_state = -20;
        if (pcs != null) {
            switch (pcs.state) {
                case PCS_EXECUTING:
                    report.exec_state = (int) pcs.plan_progress;
                    break;
                case PCS_READY:
                    report.exec_state = -10;
                    break;
                case PCS_INITIALIZING:
                    report.exec_state = -30;
                    break;
                case PCS_BLOCKED:
                    report.exec_state = -40;
                    break;
                default:
                    break;
            }
        }

        if (plan != null) {
            report.plan_checksum = plan.checksum();
        }

        report.stime = (int) (System.currentTimeMillis() / 1000);
        last_report_ts = report.stime;
        return report;
    }

    public void saveConfig(File destination) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(destination));
        writer.write("#SOI Executive settings\n\n");
        for (Field f : PojoConfig.loadFields(this)) {
            f.setAccessible(true);
            Parameter p = f.getAnnotation(Parameter.class);
            if (p != null) {
                writer.write("#" + p.description() + "\n");
                writer.write(f.getName() + "=" + f.get(this) + "\n\n");
            }
        }
        writer.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java -jar SoiExec.jar <FILE>");
            System.exit(1);
        }

        CONFIG_FILE = new File(args[0]);
        if (!CONFIG_FILE.exists()) {
            new SoiExecutive().saveConfig(CONFIG_FILE);
            System.out.println("Wrote default properties to " + CONFIG_FILE.getAbsolutePath());
            System.exit(0);
        }

        Properties props = new Properties();
        props.load(new FileInputStream(CONFIG_FILE));

        SoiExecutive tracker = PojoConfig.create(SoiExecutive.class, props);

        System.out.println("Executive started with settings:");
        for (Field f : tracker.getClass().getDeclaredFields()) {
            Parameter p = f.getAnnotation(Parameter.class);
            if (p != null) {
                System.out.println(f.getName() + "=" + f.get(tracker));
            }
        }
        System.out.println();

        tracker.connect(tracker.hAddr, tracker.hPort);
        tracker.join();
    }
}
