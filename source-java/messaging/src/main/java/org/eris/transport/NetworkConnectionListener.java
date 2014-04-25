package org.eris.transport;

public interface NetworkConnectionListener<T>
{
    void connection(NetworkConnection<T> con);
}
