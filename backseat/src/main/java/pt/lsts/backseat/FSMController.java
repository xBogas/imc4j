package pt.lsts.backseat;

import pt.lsts.imc4j.msg.FollowRefState;

public class FSMController extends BackSeatDriver {
	
	private FSMState state = null;
    private FSMState pendingState = null;
    private boolean hasPending = false;

    /** Set the starting state. Call only from the constructor. */
    protected synchronized void setInitialState(FSMState s) {
        this.state = s;
    }

    /** Request a state transition. Safe from any thread. */
    protected synchronized void setState(FSMState next) {
        pendingState = next;
        hasPending = true;
    }

    protected synchronized boolean isInState(FSMState target) {
        return (hasPending ? pendingState : state) == target;
    }

    protected FSMState currentState() {
        return state;
    }

	@Override
	public void update(FollowRefState fref) {
        FSMState current;
        synchronized (this) {
            if (hasPending) {                 // promote anything queued
                state = pendingState;          // since the previous tick
                pendingState = null;
                hasPending = false;
            }
            current = state;
        }

		if (state == null) {
			end();
            return;
		}

        try {
            FSMState next = current.step(fref);
            synchronized (this) {
                if (hasPending) {   // state transition was requested during step()
                    state = pendingState;
                    pendingState = null;
                    hasPending = false;
                } else {
                    state = next;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            end();
        }
	}
	
    protected void printFSMState() {
    	String method = currentThread().getStackTrace()[2].getMethodName();
        print("FSM State: " + method);        
    }

	@FunctionalInterface
	public static interface FSMState {
		public FSMState step(FollowRefState refState);
	}
}
