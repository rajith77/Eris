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
package org.eris.titan.util;

import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.MessageFactory;
import org.apache.qpid.proton.message.impl.MessageFactoryImpl;
import org.apache.qpid.proton.messenger.Messenger;
import org.apache.qpid.proton.messenger.impl.MessengerImpl;

public class Functions
{
    final static MessageFactory _msgFactory = new MessageFactoryImpl();

    public static String createMessengerURLToConnect(String host, int port,
            String dest)
    {
        return "amqp://" + host + ":" + port + "/" + dest;
    }

    public static String createMessengerURLToListen(String host, int port,
            String dest)
    {
        return "amqp://~" + host + ":" + port + "/" + dest;
    }

    public static String cleanPath(String path)
    {
        // remove leading '/'
        if (path != null && path.length() > 0 && path.charAt(0) == '/')
        {
            return path.substring(1);
        }
        else
        {
            return path;
        }
    }

    public static Messenger createMessenger()
    {
        return new MessengerImpl();
    }

    public static Message createMessage()
    {
        return _msgFactory.createMessage();
    }
}
