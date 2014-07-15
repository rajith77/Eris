package org.eris.network;

public interface Server<T>
{
    void start() throws NetworkException;
    
    void shutdown();
    
    void setNetworkConnectionListener(NetworkConnectionListener<T> l);
}
