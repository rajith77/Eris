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

import org.apache.qpid.proton.engine.Receiver;
import org.eris.messaging.server.InboundConnection;
import org.eris.messaging.server.InboundSession;
import org.eris.messaging.server.ReasonCode;
import org.eris.messaging.server.Subscriber;

public class SubscriberImpl implements Subscriber
{
    private final Receiver _receiver;
    private final InboundConnectionImpl _conn;
    private final String _address;
    
    SubscriberImpl(String address, Receiver receiver, InboundConnectionImpl conn)
    {
        _address = address;
        _receiver = receiver;
        _conn = conn;
    }

    @Override
    public String getAddress()
    {
        return _address;
    }

    @Override
    public void accept()
    {
        _receiver.open();
        _conn.write();
    }

    @Override
    public void reject(ReasonCode code, String desc)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void close()
    {
        // TODO Auto-generated method stub

    }

}
