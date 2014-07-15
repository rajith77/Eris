package org.eris.transport;

public interface Receiver<T>
{
    void received(T msg);

    void exception(Throwable t);

    void closed();
}
