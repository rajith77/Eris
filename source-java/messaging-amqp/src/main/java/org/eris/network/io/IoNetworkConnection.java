package org.eris.network.io;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.eris.messaging.ConnectionSettings;
import org.eris.network.NetworkConnection;
import org.eris.network.Receiver;
import org.eris.network.Sender;
import org.eris.network.NetworkException;

public class IoNetworkConnection implements NetworkConnection<ByteBuffer>
{
    private ConnectionSettings _settings;
    private Socket _socket;
    private Receiver<ByteBuffer> _delegate;
    private IoReceiver _receiver;
    private IoSender _sender;
    
    public IoNetworkConnection(ConnectionSettings settings) throws NetworkException
    {
        this(settings, new Socket());
    }

    public IoNetworkConnection(ConnectionSettings settings, Socket socket) throws NetworkException
    {
        _settings = settings;
        _socket = socket;
        try
        {
            _socket.setReuseAddress(true);
            _socket.setTcpNoDelay(_settings.isTcpNodelay());
            _socket.setSendBufferSize(_settings.getWriteBufferSize());
            _socket.setReceiveBufferSize(_settings.getReadBufferSize());

        }
        catch (SocketException e)
        {
            throw new NetworkException("Error setting up socket", e);
        }
    }
    
    @Override
    public void start() throws NetworkException
    {
        if (_delegate == null)
        {
            throw new NetworkException("A receiver needs to be set (using setReceiver) before connecting");
        }
        if (!_socket.isConnected())
        {
            try
            {
                InetAddress address = InetAddress.getByName(_settings.getHost());
                _socket.connect(new InetSocketAddress(address, _settings.getPort()), (int)_settings.getConnectTimeout());
            }
            catch (UnknownHostException e)
            {
                throw new NetworkException("Error connecting to given host", e);
            }
            catch (IOException e)
            {
                throw new NetworkException("IO error when connecting to peer", e);
            }
        }
        _receiver = new IoReceiver(_socket, _delegate, _settings.getReadBufferSize(), _settings.getIdleTimeout());
        _sender = new IoSender(_socket, _settings.getWriteBufferSize(), _settings.getIdleTimeout());
    }

    @Override
    public void setReceiver(Receiver<ByteBuffer> receiver)
    {
        _delegate = receiver;
    }

    @Override
    public Sender<ByteBuffer> getSender()
    {
        return _sender;
    }

    @Override
    public void close() throws NetworkException
    {
        _sender.close();
        _receiver.close();
    }
}