package org.eris.messaging.server.amqp.proton;

import java.nio.ByteBuffer;

import org.eris.logging.Logger;
import org.eris.messaging.server.InboundConnection;
import org.eris.transport.NetworkConnection;
import org.eris.transport.Receiver;
import org.eris.transport.TransportException;

public class InboundConnectionImpl implements InboundConnection, Receiver<ByteBuffer>
{
    private static final Logger _logger = Logger.get(InboundConnectionImpl.class);

    private NetworkConnection<ByteBuffer> _networkConnection;

    private org.eris.transport.Sender<ByteBuffer> _sender;

    InboundConnectionImpl(NetworkConnection<ByteBuffer> networkCon)
    {
        _networkConnection = networkCon;
        _networkConnection.setReceiver(this);
        try
        {
            _networkConnection.start();
        }
        catch (TransportException e)
        {
            _logger.error(e, "Exception creating InboundConnection object. Dropping network connection");
            try
            {
                _networkConnection.close();
            }
            catch (TransportException ex)
            {
                //ignore.
            }
        }
        _sender = _networkConnection.getSender();
    }

    @Override
    public void accept()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void reject()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void redirect()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setEventListener()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void close()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void received(ByteBuffer msg)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void exception(Throwable t)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void closed()
    {
        // TODO Auto-generated method stub
    }
}
