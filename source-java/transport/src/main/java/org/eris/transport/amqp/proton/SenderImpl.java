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

import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.message.Message;
import org.eris.util.ConditionManager;
import org.eris.util.ConditionManagerTimeoutException;

public class SenderImpl implements org.eris.messaging.Sender 
{
	private SessionImpl _ssn;
	private Sender _sender;

	private ConditionManager _senderReady = new ConditionManager(false);

	SenderImpl(SessionImpl ssn, Sender sender)
	{
		_ssn = ssn;
		_sender = sender;
	}

	// Need to handle buffer overflows
	public org.eris.messaging.Tracker send(org.eris.messaging.Message msg) throws org.eris.messaging.SenderException, org.eris.messaging.TransportException
	{
		if (_sender.getLocalState() == EndpointState.CLOSED || _sender.getRemoteState() == EndpointState.CLOSED)
		{
			throw new org.eris.messaging.SenderException("Sender closed");
		}

		if (msg instanceof MessageImpl)
		{
			byte[] tag = String.valueOf(_ssn.getNextDeliveryTag()).getBytes();
			Delivery delivery = _sender.delivery(tag);
			TrackerImpl tracker = new TrackerImpl();
			delivery.setContext(tracker);
			if (_sender.getSenderSettleMode() == SenderSettleMode.SETTLED)
			{
				delivery.settle();
				//tracker.setState();
				tracker.markSettled();
			}

			Message m = ((MessageImpl) msg).getProtocolMessage();
			byte[] buffer = new byte[1024];
			int encoded = m.encode(buffer, 0, buffer.length);
			_sender.send(buffer, 0, encoded);
			_ssn.write();
			return tracker;
		}
		else
		{
			throw new org.eris.messaging.SenderException("Unsupported message implementation");
		}
	}

	@Override
	public void offerCredits(int credits) throws org.eris.messaging.SenderException, org.eris.messaging.TransportException
	{
		checkPreConditions();
		_sender.offer(credits);
		_ssn.write();
	}

	@Override
	public int getUnsettled() throws org.eris.messaging.SenderException
	{
		checkPreConditions();
		return _sender.getUnsettled();
	}

	@Override
	public void close() throws org.eris.messaging.TransportException
	{
		_ssn.closeLink(_sender);
	}

	void markSenderReady()
	{
		_senderReady.setValueAndNotify(true);
	}

	void waitUntilActive(long timeout) throws org.eris.messaging.TimeoutException
	{
		try
		{
			_senderReady.waitUntilTrue(timeout);
		}
		catch (ConditionManagerTimeoutException e)
		{
			throw new org.eris.messaging.TimeoutException("Timeout waiting for session to be active");
		}
	}

	void checkPreConditions() throws org.eris.messaging.SenderException
	{
		if (!(_sender.getLocalState() == EndpointState.ACTIVE && _sender.getRemoteState() == EndpointState.ACTIVE))
		{
			throw new org.eris.messaging.SenderException("Sender is closed");
		}
	}
}