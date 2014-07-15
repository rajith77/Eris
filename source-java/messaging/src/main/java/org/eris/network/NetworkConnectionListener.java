package org.eris.network;

public interface NetworkConnectionListener<T>
{
    void connection(NetworkConnection<T> con);
}
