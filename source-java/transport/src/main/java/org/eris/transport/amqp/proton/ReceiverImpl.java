package org.eris.transport.amqp.proton;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.message.Message;

public class ReceiverImpl implements org.eris.messaging.Receiver
{
    private String _address;
    private SessionImpl _ssn;
    private Receiver _receiver;
    private int _capacity = Integer.getInteger("eris.consumer.capacity", 10);

    private LinkedBlockingQueue<org.eris.messaging.Message> _queue;

    ReceiverImpl(String address, SessionImpl ssn, Receiver receiver)
    {
        _address = address;
        _ssn = ssn;
        _receiver = receiver;
        _queue = new LinkedBlockingQueue<org.eris.messaging.Message>();
    }

    @Override
    public String getAddress()
    {
        return _address;
    }

    @Override
    public int getCapacity()
    {
        return _capacity;
    }

    @Override
    public int getAvailable()
    {
        return _queue.size();
    }

    @Override
    public int getUnsettled()
    {
        return 0;
    }

    @Override
    public org.eris.messaging.Message get() throws org.eris.messaging.ReceiverException
    {
        checkClosed();
        return _queue.poll();
    }

    @Override
    public org.eris.messaging.Message get(long timeout) throws org.eris.messaging.TransportException, org.eris.messaging.ReceiverException, org.eris.messaging.TimeoutException
    {
        checkClosed();
        try
        {
            return _queue.poll(timeout, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            checkClosed();
        }
        // assuming interrupted exception is only thrown when the sender/session is closed.
        throw new org.eris.messaging.TimeoutException("Timeout waiting for message to be available");
    }

    @Override
    public void setCapacity(int credits) throws org.eris.messaging.TransportException, org.eris.messaging.ReceiverException
    {
        _receiver.flow(credits);
        _ssn.write();
    }
    
    void checkClosed() throws org.eris.messaging.ReceiverException
    {
        if (_receiver.getLocalState() != EndpointState.ACTIVE)
        {
            throw new org.eris.messaging.ReceiverException("Receiver is closed");
        }
    }

    void enqueue(MessageImpl msg)
    {
        try
        {
            _queue.put(msg);
        }
        catch (InterruptedException e)
        {
            // ignore?
        }
    }

    SessionImpl getSession()
    {
        return _ssn;
    }
}
