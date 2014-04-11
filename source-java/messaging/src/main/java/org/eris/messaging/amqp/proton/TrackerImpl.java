package org.eris.messaging.amqp.proton;

import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.eris.messaging.Tracker.TrackerState;
import org.eris.util.ConditionManager;

public class TrackerImpl implements org.eris.messaging.Tracker
{
    private final SessionImpl _ssn;
    private TrackerState _state = null;
    private ConditionManager _pending = new ConditionManager(true);
    private boolean _settled = false;

    TrackerImpl(SessionImpl ssn)
    {
        _ssn = ssn;
        _state = TrackerState.PENDING;
    }

    void setState(TrackerState state)
    {
        _state = state;
    }

    void markSettled()
    {
        _settled = true;
        _pending.setValueAndNotify(false);
    }

    void update(DeliveryState state)
    {
        if (state instanceof Accepted)
        {
            setState(TrackerState.ACCEPTED);
        }
        else if (state instanceof Rejected)
        {
            setState(TrackerState.REJECTED);
        }
        else if (state instanceof Released)
        {
            setState(TrackerState.RELEASED);
        }
    }
    
    boolean isTerminalState()
    {
        switch (_state)
        {
        case ACCEPTED:
        case REJECTED:
        case RELEASED:
        case FAILED:
            return true;
        default:
            return false;
        }
    }

    SessionImpl getSession()
    {
        return _ssn;
    }

    @Override
    public TrackerState getState()
    {
        return _state;
    }

    @Override
    public void awaitSettlement()
    {
        _pending.waitUntilFalse();
    }

    @Override
    public boolean isSettled()
    {
        return _settled;
    }
}
