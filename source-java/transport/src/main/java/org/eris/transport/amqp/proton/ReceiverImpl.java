package org.eris.transport.amqp.proton;

import org.apache.qpid.proton.engine.Receiver;

public class ReceiverImpl implements org.eris.messaging.Receiver
{
    private String _address;
    private SessionImpl _ssn;
    private Receiver _receiver;
    private int _capacity;
    
    ReceiverImpl(String address, SessionImpl ssn, Receiver receiver)
    {
        _address = address;
        _ssn = ssn;
        _receiver = receiver;
    }


    @Override
    public String getAddress()
    {
        return _address;
    }

    @Override
    public int getCapacity()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getAvailable()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getUnsettled()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public org.eris.messaging.Message get() throws org.eris.messaging.ReceiverException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public org.eris.messaging.Message get(long timeout) throws org.eris.messaging.TransportException, org.eris.messaging.ReceiverException, org.eris.messaging.TimeoutException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public org.eris.messaging.Message fetch() throws org.eris.messaging.TransportException, org.eris.messaging.ReceiverException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public org.eris.messaging.Message fetch(long timeout) throws org.eris.messaging.TransportException, org.eris.messaging.ReceiverException, org.eris.messaging.TimeoutException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCapacity(int credits) throws org.eris.messaging.TransportException, org.eris.messaging.ReceiverException
    {
        _receiver.flow(credits);
    }

}
