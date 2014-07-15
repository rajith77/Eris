package org.eris.network;

public interface Server<T>
{
    void start() throws TransportException;
    
    void shutdown();
    
    void setNetworkConnectionListener(NetworkConnectionListener<T> l);
}
