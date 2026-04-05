package pt.lsts.backseat;

import java.util.Date;

import pt.lsts.imc4j.msg.FollowRefState;

public class TimedFSM extends FSMController {

    protected Date deadline = new Date(System.currentTimeMillis() + 3600 * 1000);

    @Override
    public void update(FollowRefState fref) {
        if (deadline != null && deadline.getTime() < System.currentTimeMillis()) {
            onDeadline(fref);
        }
        else {
            super.update(fref);
        }
    }

    protected void onDeadline(FollowRefState fref) {
        print("Deadline reached, terminating.");
        sendViaIridium("ERROR: \"Deadline reached, stopped.\"", 60);
        sendViaSms("ERROR: \"Deadline reached, stopped.\"", 60);
        setPaused(true);
    }

    @Override
    protected void printFSMState() {
        String method = currentThread().getStackTrace()[2].getMethodName();
        if (deadline == null) {
            print("FSM State: " + method + " (past deadline)");
            return;
        }
        long timeMillis = deadline.getTime() - System.currentTimeMillis();
        print(String.format("FSM State: %s (%ds left, or %dmin left, or %fh left)", method,
                timeMillis / 1000, timeMillis / 60000, timeMillis / 3600000d));
    }

    protected void printException(Exception e) {
        printError("Exception: " + e.getMessage() + " - " + e);
        String trace = java.util.Arrays.toString(e.getStackTrace());
        printError("Trace: " + trace);
    }

    public void setDeadline(Date date) {
        this.deadline = date;
    }
}
