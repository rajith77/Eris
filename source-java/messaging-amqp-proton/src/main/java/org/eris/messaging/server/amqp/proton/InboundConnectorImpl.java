package org.eris.messaging.server.amqp.proton;

import java.nio.ByteBuffer;

import org.eris.messaging.ConnectionSettings;
import org.eris.messaging.TransportException;
import org.eris.messaging.amqp.proton.ConnectionSettingsImpl;
import org.eris.messaging.server.InboundConnectionListener;
import org.eris.messaging.server.InboundConnector;
import org.eris.network.NetworkConnection;
import org.eris.network.NetworkConnectionListener;
import org.eris.network.Server;
import org.eris.network.io.ServerImpl;

public class InboundConnectorImpl implements InboundConnector, NetworkConnectionListener<ByteBuffer>
{
    private ConnectionSettings _settings;

    private Server<ByteBuffer> _server;

    private InboundConnectionListener _listener;

    InboundConnectorImpl(String host, int port)
    {
        this(new ConnectionSettingsImpl(host, port));
    }

    InboundConnectorImpl(ConnectionSettings settings)
    {
        _settings = settings;
    }

    @Override
    public void start() throws TransportException
    {
        if (_listener == null)
        {
            throw new TransportException("InboundConnectionListener needs to be set before starting the connector");
        }
        
        // HardCode for now
        _server = new ServerImpl(_settings);
        try
        {
            _server.start();
        }
        catch (org.eris.network.TransportException e)
        {
            throw new TransportException("Exception during Inbound Connector start", e);
        }
    }

    @Override
    public void setInboundConnectionListener(InboundConnectionListener l)
    {
        _listener = l;
    }

    @Override
    public void close() throws TransportException
    {
        _server.shutdown();
    }

    @Override
    public void connection(NetworkConnection<ByteBuffer> con)
    {
                
    }

}
