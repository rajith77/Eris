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
package org.eris.titan;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;
import org.eris.ErisMessage;
import org.eris.titan.util.Functions;

/**
 * Base class for all Nodes that makes up the Eris infrastructure.
 * 
 */
public class ErisNode
{
    /** The identity of the node **/
    private final String _id;

    /** The ip of the node **/
    private final String _host;

    /** The port the node is listening on **/
    private final int _port;

    /** The url used for creating the Messenger Impl **/
    private final String _address;

    private AtomicBoolean _alive = new AtomicBoolean();

    private Messenger _messenger = Functions.createMessenger();

    public ErisNode(String id, String host, int port)
    {
        _id = id;
        _host = host;
        _port = port;
        _address = Functions.createMessengerURLToListen(host, port, id);
    }

    public static ErisNode createNode(String[] args) throws Exception
    {
        if (args.length < 3)
        {
            throw new Exception(
                    "Insufficient arguments. Requires ID, Host and Port to create a node");
        }
        String id = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        return new ErisNode(id, host, port);
    }

    protected int available()
    {
        return Integer.MAX_VALUE;
    }

    protected void handleMessage(ErisMessage<Message> m)
    {

    }

    public void start() throws Exception
    {
        _alive.set(true);
        _messenger.start();
        _messenger.subscribe(_address);

        while (_alive.get())
        {
            _messenger.recv(available());
            while (_messenger.incoming() > 0)
            {
                handleMessage(new ErisMessageImpl(_messenger.get()));
            }
        }
    }

    public void stop() throws Exception
    {
        _alive.set(false);
        _messenger.stop();
    }

    public static void main(String[] args) throws Exception
    {
        ErisNode node = ErisNode.createNode(args);
        node.start();
    }
}
