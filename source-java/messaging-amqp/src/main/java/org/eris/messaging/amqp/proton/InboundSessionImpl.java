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

import java.util.UUID;

import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.engine.Session;
import org.eris.messaging.server.InboundSession;
import org.eris.messaging.server.ReasonCode;

public class InboundSessionImpl implements InboundSession
{
    private final InboundConnectionImpl _conn;

    private final Session _ssn;

    private final String _id;

    InboundSessionImpl(Session ssn, InboundConnectionImpl conn)
    {
        _conn = conn;
        _ssn = ssn;
        _id = UUID.randomUUID().toString();
    }

    @Override
    public String getName()
    {
        return _id;
    }

    @Override
    public void accept()
    {
        _ssn.open();
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
        _ssn.close();
        _conn.write();
    }
}