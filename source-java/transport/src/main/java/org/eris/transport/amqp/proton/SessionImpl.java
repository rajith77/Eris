package org.eris.transport.amqp.proton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;

public class SessionImpl implements org.eris.messaging.Session
{
	private ConnectionImpl _conn;
	private Session _session;
	private AtomicLong _deliveryTag = new AtomicLong(0);
	private final Map<Sender, SenderImpl> _senders = new ConcurrentHashMap<Sender, SenderImpl>(2);
	private final Map<Sender, ReceiverImpl> _receivers = new ConcurrentHashMap<Sender, ReceiverImpl>(2);

	SessionImpl(ConnectionImpl conn, Session ssn)
	{
		_conn = conn;
		_session = ssn;
	}

	@Override
	public org.eris.messaging.Sender createSender(String address)
	{
		Sender sender = _session.sender(address);
		Target target = new Target();
		target.setAddress(address);
		sender.setTarget(target);
		Source source = new Source();
		source.setAddress(address);
		sender.setSource(source);
		sender.open();

		SenderImpl protonSender = new SenderImpl(this,sender);
		_senders.put(sender, protonSender);
		return protonSender;
	}

	@Override
	public org.eris.messaging.Receiver createReceiver(String address)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close()
	{
		_session.close();
	}

	long getNextDeliveryTag()
	{
		return _deliveryTag.incrementAndGet();
	}

	ConnectionImpl getConnection()
	{
		return _conn;
	}
}