package org.eris.transport.amqp.proton;

import org.eris.util.ConditionManager;

public class TrackerImpl implements org.eris.messaging.Tracker
{
	private TrackerState _state = null;
	private ConditionManager _pending = new ConditionManager(true);

	TrackerImpl()
	{
		_state = TrackerState.PENDING;
	}

	void setState(TrackerState state)
	{
		_state = state;
		_pending.setValueAndNotify(false);
	}
	
	@Override
	public TrackerState getState()
	{
		return _state;
	}

	@Override
	public void awaitCompletion()
	{
		_pending.waitUntilFalse();
	}
}
