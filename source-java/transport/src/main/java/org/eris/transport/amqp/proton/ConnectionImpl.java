package org.eris.transport.amqp.proton;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.Transport;
import org.eris.logging.Logger;
import org.eris.messaging.Tracker.TrackerState;
import org.eris.util.ConditionManager;
import org.eris.util.ConditionManagerTimeoutException;

public class ConnectionImpl implements org.eris.transport.Receiver<ByteBuffer>, org.eris.messaging.Connection
{
	private static final Logger _logger = Logger.get(ConnectionImpl.class);

	enum State {UNINITIALIZED, ACTIVE, DETACHED, CLOSED};

	private org.eris.messaging.ConnectionSettings _settings;
	private org.eris.transport.NetworkConnection<ByteBuffer> _networkConnection;
	private org.eris.transport.Sender<ByteBuffer> _sender;
	private Transport _transport = Proton.transport();
	private Connection _connection;
	private State _state = State.UNINITIALIZED;

	private final Map<Session, SessionImpl> _sessionMap = new ConcurrentHashMap<Session, SessionImpl>();

	private ConditionManager _connectionReady = new ConditionManager(false);

	public ConnectionImpl(String url)
	{

	}

	public ConnectionImpl(String host, int port)
	{

	}

	public ConnectionImpl(org.eris.messaging.ConnectionSettings settings)
	{
		_settings = settings;
	}

	@Override
	public void connect() throws org.eris.messaging.TransportException, org.eris.messaging.ConnectionException, org.eris.messaging.TimeoutException
	{
		try
		{
			// hard code for now
			_networkConnection = new org.eris.transport.io.IoNetworkConnection(_settings);
			_networkConnection.setReceiver(this);
			_networkConnection.connect();
		}
		catch (org.eris.transport.TransportException e)
		{
			throw new org.eris.messaging.TransportException("Exception occurred while making tcp connection to peer", e);
		}
		_sender = _networkConnection.getSender();

		_connection = Proton.connection();
		_connection.setContainer(UUID.randomUUID().toString());
		_connection.setHostname(_settings.getHost());
		_transport.bind(_connection);

		doSasl(_transport.sasl());
		_connection.open();
		write();
		try
		{
			_connectionReady.waitUntilTrue(getDefaultTimeout());
		}
		catch (ConditionManagerTimeoutException e)
		{
			throw new org.eris.messaging.TimeoutException("Timeout while waiting for connection to be ready",e);
		}
	}

	@Override
	public org.eris.messaging.Session createSession() throws org.eris.messaging.TransportException, org.eris.messaging.ConnectionException, org.eris.messaging.TimeoutException
	{
		Session ssn = _connection.session();
		ssn.open();
		SessionImpl session = new SessionImpl(this,ssn);
		_sessionMap.put(ssn, session);
		write();
		session.waitUntilActive(getDefaultTimeout());
		return session;
	}

	// Needs to expand to handle other mechs
	void doSasl(Sasl sasl)
	{
		if (sasl != null)
		{
			sasl.client();
			sasl.setMechanisms(new String[]{"ANONYMOUS"});
		}
	}

	void write() throws org.eris.messaging.TransportException
	{
		try
		{
			while (_transport.pending() > 0)
			{
				ByteBuffer data = _transport.getOutputBuffer();
				_sender.send(data);
				_transport.outputConsumed();
			}
		}
		catch (org.eris.transport.TransportException e)
		{
			_logger.error(e, "Error while writing to ouput stream");
			throw new org.eris.messaging.TransportException("Error while writing to ouput stream",e);
		}
	}

	@Override
	public void received(ByteBuffer data)
	{
		while (data.hasRemaining())
		{
			ByteBuffer buf = _transport.getInputBuffer();
			int maxAllowed = Math.min(data.remaining(), buf.remaining());
			ByteBuffer temp = data.duplicate();
			temp.limit(data.position() + maxAllowed);
			buf.put(temp);
			_transport.processInput();
			data.position(data.position() + maxAllowed);
		}
		if (_state == State.UNINITIALIZED)
		{
			if (_connection.getRemoteState() == EndpointState.ACTIVE)
			{
				_state = State.ACTIVE;
				_connectionReady.setValueAndNotify(true);
			}
		}
		else
		{
			processSessions();
			processLinks();
			processDeliveries();
		}
	}

	@Override
	public void exception(Throwable t)
	{

	}

	@Override
	public void closed()
	{
		// TODO Auto-generated method stub
	}

	public void processDeliveries()
	{
		Delivery delivery = _connection.getWorkHead();
		while (delivery != null)
		{
			// TODO this can be optimized?
			if (delivery.isUpdated() && delivery.getLink() instanceof Sender)
			{                
				if (delivery.getRemoteState() != null)
				{
					delivery.disposition(delivery.getRemoteState());
					TrackerImpl tracker = (TrackerImpl)delivery.getContext();
					if (delivery.getRemoteState() instanceof Accepted)
					{
						tracker.setState(TrackerState.ACCEPTED);
					}
					else if (delivery.getRemoteState() instanceof Rejected)
					{
						tracker.setState(TrackerState.REJECTED);
					}
					else if (delivery.getRemoteState() instanceof Released)
					{
						tracker.setState(TrackerState.RELEASED);
					}
					if (delivery.getLink().getRemoteReceiverSettleMode() == ReceiverSettleMode.SECOND)
					{
						if (tracker.isTerminalState())
						{
							delivery.settle();
							tracker.markSettled();
						}
					}
				}
				if (delivery.remotelySettled())
				{
					TrackerImpl tracker = (TrackerImpl)delivery.getContext();
					delivery.settle();
					tracker.markSettled();
				}
			}
			delivery = delivery.getWorkNext();
		}
	}

	void processSessions()
	{		
		Session ssn = _connection.sessionHead(EndpointStateHelper.ACTIVE, EndpointStateHelper.ACTIVE);
		while (ssn != null)
		{
			SessionImpl ssnImpl = _sessionMap.get(ssn);
			ssnImpl.markSessionReady();
			ssn = ssn.next(EndpointStateHelper.ACTIVE, EndpointStateHelper.ACTIVE);
		}

		ssn = _connection.sessionHead(EndpointStateHelper.ANY, EndpointStateHelper.CLOSED);
		while (ssn != null)
		{
			ssn.close();
			_sessionMap.remove(ssn);
			ssn = ssn.next(EndpointStateHelper.ANY, EndpointStateHelper.CLOSED);
		}
	}

	void processLinks()
	{		
		Link link = _connection.linkHead(EndpointStateHelper.ACTIVE, EndpointStateHelper.ACTIVE);
		while (link != null)
		{
			_sessionMap.get(link.getSession()).markLinkReady(link);
			link = link.next(EndpointStateHelper.ACTIVE, EndpointStateHelper.ACTIVE);
		}

		link = _connection.linkHead(EndpointStateHelper.ANY, EndpointStateHelper.CLOSED);
		while (link != null)
		{
			link.close();
			_sessionMap.get(link.getSession()).linkClosed(link);
			link = link.next(EndpointStateHelper.ANY, EndpointStateHelper.CLOSED);
		}
	}

	void closeSession(Session ssn) throws org.eris.messaging.TransportException
	{
		ssn.close();
		write();
	}

	long getDefaultTimeout()
	{
		return _settings.getConnectTimeout();
	}
	
	public static void main(String[] args) throws Exception
	{
		ConnectionImpl con = new ConnectionImpl(new org.eris.messaging.ConnectionSettings());
		con.connect();
	}
}