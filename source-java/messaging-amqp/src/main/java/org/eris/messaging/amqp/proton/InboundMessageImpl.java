package org.eris.messaging.amqp.proton;

import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.message.Message;

public class InboundMessageImpl extends MessageImpl
{
    private Delivery _delivery;

    public InboundMessageImpl(Delivery d,Message msg)
    {
        super(msg);
        _delivery = d;
    }

    Delivery getDelivery()
    {
        return _delivery;
    }
}