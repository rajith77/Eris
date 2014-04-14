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
package org.eris.messaging;

/**
 * 
 * Provides an entry point for using the messaging library.
 * It provides several methods for obtaining a connection which can then
 * be used to create the appropriate constructs to send and receive messages.
 * 
 * It also acts as a factory for Message objects @see #message().
 * 
 * <h3>Connection URL Syntax</h3>
 *
 *  The URL has the following form:
 *
 *    [ amqp[s]:// ] [user[:password]@] domain]
 *
 *  Where domain can be one of:
 *
 *    host | host:port | ip | ip:port | name
 *
 *  The following are valid examples of addresses:
 *
 *   - example.org
 *   - example.org:1234
 *   - amqp://example.org
 *   - amqps://example.org
 *   - amqps://fred:trustno1@example.org
 *   - 127.0.0.1:1234
 *   - amqps://127.0.0.1:1234
 *   
 */
import org.eris.messaging.amqp.proton.MessagingImpl;

public class Messaging
{
    private Messaging() {}
    /**
     * Provides a concrete instance of the Message object that can be used for sending.
     * @see Message
     */
    public static Message message()
    {
        return MessagingImpl.message();
    }

    /**
     * Constructs a Connection object with the given URL
     * This does not establish the underlying physical connection. 
     * The application needs to call connect() in order to establish the physical connection to the peer.
     * @see Connection#connect()
     */
    public static Connection connection(String url)
    {
        return MessagingImpl.connection(url);
    }

    /**
     * Constructs a Connection object with the given host and port
     * This does not establish the underlying physical connection. 
     * The application needs to call connect() in order to establish the physical connection to the peer.
     * @see Connection#connect()
     */
    public static Connection connection(String host, int port)
    {
        return MessagingImpl.connection(host, port);
    }

    /**
     * Constructs a Connection object with the given ConnectionSettings.
     * @see ConnectionSettings
     * This does not establish the underlying physical connection. 
     * The application needs to call connect() in order to establish the physical connection to the peer.
     * @see Connection#connect()
     */
    public static Connection connection(ConnectionSettings settings)
    {
        return MessagingImpl.connection(settings);
    }
}