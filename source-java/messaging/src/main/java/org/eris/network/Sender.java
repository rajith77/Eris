package org.eris.network;

public interface Sender<T>
{
    void send(T msg) throws NetworkException;

    void flush() throws NetworkException;

    void close() throws NetworkException;
}
