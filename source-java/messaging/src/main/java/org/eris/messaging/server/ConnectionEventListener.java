package org.eris.messaging.server;


public interface ConnectionEventListener
{
    void sessionCreated(InboundSession ssn);

    void sessionClosed(InboundSession ssn);

    void linkCreated(Link link);

    void linkClosed(Link link);

    void linkCredit(Link link, int credits);

    void incomingDelivery(Delivery d);

    void deliveryUpdated(Delivery d);
}
