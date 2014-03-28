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
package org.eris.transport.amqp.proton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Session;
import org.eris.messaging.ReceiverMode;
import org.eris.messaging.SenderMode;

public class SessionImpl implements org.eris.messaging.Session
{
	private ConnectionImpl _conn;
	private Session _session;
	private AtomicLong _deliveryTag = new AtomicLong(0);
	private final Map<Sender, SenderImpl> _senders = new ConcurrentHashMap<Sender, SenderImpl>(2);
	private final Map<Receiver, ReceiverImpl> _receivers = new ConcurrentHashMap<Receiver, ReceiverImpl>(2);

	SessionImpl(ConnectionImpl conn, Session ssn)
	{
		_conn = conn;
		_session = ssn;
	}

	@Override
	public org.eris.messaging.Sender createSender(String address, SenderMode mode) throws org.eris.messaging.TransportException, org.eris.messaging.SessionException, org.eris.messaging.TimeoutException
	{
		checkPreConditions();
		Sender sender = _session.sender(address);
		Target target = new Target();
		target.setAddress(address);
		sender.setTarget(target);
		Source source = new Source();
		source.setAddress(address);
		sender.setSource(source);
		sender.setSenderSettleMode(mode == SenderMode.AT_MOST_ONCE ? SenderSettleMode.SETTLED : SenderSettleMode.UNSETTLED);
		sender.open();

		SenderImpl senderImpl = new SenderImpl(address,this,sender);
		_senders.put(sender, senderImpl);
		sender.setContext(senderImpl);
		_conn.write();
		return senderImpl;
	}

	@Override
	public org.eris.messaging.Receiver createReceiver(String address, ReceiverMode mode) throws org.eris.messaging.TransportException, org.eris.messaging.SessionException, org.eris.messaging.TimeoutException
	{
		checkPreConditions();
		Receiver receiver = _session.receiver(address);
		Source source = new Source();
        source.setAddress(address);
        receiver.setSource(source);
        Target target = new Target();
        target.setAddress(address);
        receiver.setTarget(target);
        switch (mode)
        {
        case AT_MOST_ONCE:
        	receiver.setReceiverSettleMode(ReceiverSettleMode.FIRST);
        	receiver.setSenderSettleMode(SenderSettleMode.SETTLED);
        	break;
        case AT_LEAST_ONCE:
        	receiver.setReceiverSettleMode(ReceiverSettleMode.FIRST);
        	receiver.setSenderSettleMode(SenderSettleMode.UNSETTLED);
        	break;
        case EXACTLY_ONCE:
        	receiver.setReceiverSettleMode(ReceiverSettleMode.SECOND);
        	receiver.setSenderSettleMode(SenderSettleMode.UNSETTLED);
        	break;
        }        
		receiver.open();
		
		ReceiverImpl receiverImpl = new ReceiverImpl(address,this,receiver);
		_receivers.put(receiver, receiverImpl);
		receiver.setContext(receiverImpl);
		_conn.write();
		return receiverImpl;
	}

	@Override
	public void close() throws org.eris.messaging.TransportException
	{
		_conn.closeSession(_session);
	}

	long getNextDeliveryTag()
	{
		return _deliveryTag.incrementAndGet();
	}

	ConnectionImpl getConnection()
	{
		return _conn;
	}

	void closeLink(Link link) throws org.eris.messaging.TransportException
	{
		link.close();
		_conn.write();
	}

	void linkClosed(Link link)
	{
		if (link instanceof Sender)
		{
			_senders.remove(link);
		}
	}

	void write() throws org.eris.messaging.TransportException
	{
		_conn.write();
	}

	void checkPreConditions() throws org.eris.messaging.SessionException
	{
		if (_session.getLocalState() != EndpointState.ACTIVE)
		{
			throw new org.eris.messaging.SessionException("Session is closed");
		}
	}

	
}