package org.eris.network;

public interface Receiver<T>
{
    void received(T msg);

    void exception(Throwable t);

    void closed();
}
