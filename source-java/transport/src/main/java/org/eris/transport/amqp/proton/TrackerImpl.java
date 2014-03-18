package org.eris.transport.amqp.proton;

import org.eris.util.ConditionManager;

public class TrackerImpl implements org.eris.messaging.Tracker
{
	private TrackerState _state = null;
	private ConditionManager _inProgress = new ConditionManager(true);

	TrackerImpl()
	{
		_state = TrackerState.IN_PROGRESS;
	}

	void setState(TrackerState state)
	{
		_state = state;
		_inProgress.setValueAndNotify(false);
	}
	
	@Override
	public TrackerState getState()
	{
		return _state;
	}

	@Override
	public void awaitCompletion()
	{
		_inProgress.waitUntilFalse();
	}
}
