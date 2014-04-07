package org.eris.transport;

public interface Sender<T>
{
    void send(T msg) throws TransportException;

    void flush() throws TransportException;

    void close() throws TransportException;
}
