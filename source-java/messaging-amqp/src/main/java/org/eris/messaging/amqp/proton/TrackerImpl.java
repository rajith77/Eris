/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.eris.messaging.amqp.proton;

import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.eris.messaging.TrackerState;
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
        _state = TrackerState.UNKNOWN;
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
