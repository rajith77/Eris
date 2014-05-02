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
package org.eris.messaging.server.amqp.proton;

/**
 * Internal impl class to represent a protocol event.
 * This class is not exposed via the API.
 */
public class ProtocolEvent
{
    enum EventType
    {
        CONNECTION_OPENED,
        CONNECTION_CLOSED,
        SESSION_OPENED,
        SESSION_CLOSED,
        LINK_OPENED,
        LINK_CLOSED,
        LINK_FLOW,
        INCOMING_DELIVERY,
        DELIVERY_UPDATED
    };

    private EventType _type;

    private Object _ctx;

    ProtocolEvent(EventType type, Object ctx)
    {
        _type = type;
        _ctx = ctx;
    }

    public EventType getType()
    {
        return _type;
    }

    public Object getContext()
    {
        return _ctx;
    }
}