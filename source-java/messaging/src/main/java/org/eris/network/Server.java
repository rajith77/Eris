package org.eris.transport;

public interface Server<T>
{
    void start() throws TransportException;
    
    void shutdown();
    
    void setNetworkConnectionListener(NetworkConnectionListener<T> l);
}
