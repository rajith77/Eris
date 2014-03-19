package org.eris.transport.amqp.proton;

import org.eris.util.ConditionManager;

public class TrackerImpl implements org.eris.messaging.Tracker
{
	private TrackerState _state = null;
	private ConditionManager _pending = new ConditionManager(true);
	private boolean _settled = false;

	TrackerImpl()
	{
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
