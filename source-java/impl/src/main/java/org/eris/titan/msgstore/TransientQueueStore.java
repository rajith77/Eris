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
package org.eris.titan.msgstore;

import java.util.concurrent.ArrayBlockingQueue;

import org.apache.qpid.proton.message.Message;
import org.eris.ErisMessage;
import org.eris.MessageStore;

public class TransientQueueStore implements MessageStore<Message>
{
    ArrayBlockingQueue<ErisMessage<Message>> _queue;

    public TransientQueueStore(int size)
    {
        _queue = new ArrayBlockingQueue<ErisMessage<Message>>(size);
    }

    @Override
    public void put(ErisMessage<Message> m)
    {
        try
        {
            _queue.put(m);
        }
        catch (InterruptedException e)
        {
            // TODO handle
        }
    }

    @Override
    public ErisMessage<Message> nextMessage(String... args)
    {
        return _queue.peek();
    }

    @Override
    public void remove(ErisMessage<Message> m)
    {
        _queue.remove(m);
    }

    public int available()
    {
        return _queue.size();
    }
}
