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
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.eris.messaging.ReceiverMode;
import org.eris.messaging.SenderMode;
import org.eris.util.ConditionManager;
import org.eris.util.ConditionManagerTimeoutException;

public class SessionImpl implements org.eris.messaging.Session
{
	private ConnectionImpl _conn;
	private Session _session;
	private AtomicLong _deliveryTag = new AtomicLong(0);
	private final Map<Sender, SenderImpl> _senders = new ConcurrentHashMap<Sender, SenderImpl>(2);
	private final Map<Sender, ReceiverImpl> _receivers = new ConcurrentHashMap<Sender, ReceiverImpl>(2);

	private ConditionManager _sessionReady = new ConditionManager(false);

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

		SenderImpl senderImpl = new SenderImpl(this,sender);
		_senders.put(sender, senderImpl);
		_conn.write();
		senderImpl.waitUntilActive(_conn.getDefaultTimeout());
		return senderImpl;
	}

	@Override
	public org.eris.messaging.Receiver createReceiver(String address, ReceiverMode mode) throws org.eris.messaging.TransportException, org.eris.messaging.SessionException, org.eris.messaging.TimeoutException
	{
		checkPreConditions();
		return null;
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

	void markSessionReady()
	{
		_sessionReady.setValueAndNotify(true);
	}

	void waitUntilActive(long timeout) throws org.eris.messaging.TimeoutException
	{
		try
		{
			_sessionReady.waitUntilTrue(timeout);
		}
		catch (ConditionManagerTimeoutException e)
		{
			throw new org.eris.messaging.TimeoutException("Timeout waiting for session to be active");
		}
	}

	void markLinkReady(Link link)
	{
		if (link instanceof Sender)
		{
			SenderImpl sender = _senders.get(link);
			sender.markSenderReady();
		}
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

	void checkPreConditions() throws org.eris.messaging.SessionException
	{
		if (!(_session.getLocalState() == EndpointState.ACTIVE && _session.getRemoteState() == EndpointState.ACTIVE))
		{
			throw new org.eris.messaging.SessionException("Session is closed");
		}
	}
}