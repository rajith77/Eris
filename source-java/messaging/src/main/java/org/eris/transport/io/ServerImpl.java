package org.eris.transport.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eris.logging.Logger;
import org.eris.messaging.ConnectionSettings;
import org.eris.threading.Threading;
import org.eris.transport.NetworkConnection;
import org.eris.transport.NetworkConnectionListener;
import org.eris.transport.Server;
import org.eris.transport.TransportException;

public class ServerImpl implements Server<ByteBuffer>, Runnable
{
    private static final Logger _logger = Logger.get(ServerImpl.class);

    private final AtomicBoolean _closed = new AtomicBoolean(false);

    private ConnectionSettings _settings;

    private ServerSocket _serverSocket;

    private Thread _acceptThread;

    private NetworkConnectionListener<ByteBuffer> _listener;

    public ServerImpl(ConnectionSettings settings)
    {
        _settings = settings;
    }

    @Override
    public void start() throws TransportException
    {
        try
        {
            _serverSocket = new ServerSocket();
            _serverSocket.bind(new InetSocketAddress(_settings.getHost(), _settings.getPort()));
            _serverSocket.setReuseAddress(true);
        }
        catch (IOException e)
        {
            throw new TransportException(String.format("Error setting up socket for %s : %s", _settings.getHost(),
                    _settings.getPort()), e);
        }

        try
        {
            _acceptThread = Threading.getThreadFactory().createThread(this);
        }
        catch (Exception e)
        {
            shutdown();
            throw new TransportException("Error creating accept thread. Server shutting down", e);
        }
        _acceptThread.start();
    }

    @Override
    public void run()
    {
        try
        {
            while (!_closed.get())
            {
                Socket socket = null;
                try
                {
                    socket = _serverSocket.accept();

                    NetworkConnection<ByteBuffer> connection = new IoNetworkConnection(_settings, socket);
                    _listener.connection(connection);
                }
                catch (IOException e)
                {
                    _logger.error(e, "IO Error when accepting connection on %s", _serverSocket.getInetAddress());
                    shutdown();
                }
                catch (TransportException e)
                {
                    _logger.error("Transport error when accepting connection on %s", _serverSocket.getInetAddress());
                    shutdown();
                }
                catch (RuntimeException e)
                {
                    _logger.error("Runtime error when accepting connection on %s", _serverSocket.getInetAddress());
                    shutdown();
                }
            }
        }
        finally
        {
            _logger.warn("Acceptor thread exiting, no new connections will be accepted on address %s",
                    _serverSocket.getInetAddress());
        }
    }

    @Override
    public void shutdown()
    {
        if (!_closed.get())
        {
            _closed.set(true);
            try
            {
                if (!_serverSocket.isClosed())
                {
                    _serverSocket.close();
                }
            }
            catch (IOException e)
            {
                _logger.warn(e, "Error during shutdown for %s", _serverSocket.getInetAddress());
            }
        }
    }

    @Override
    public void setNetworkConnectionListener(NetworkConnectionListener<ByteBuffer> l)
    {
        _listener = l;
    }
}