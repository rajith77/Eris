package org.eris.messaging.server.amqp.proton;

import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.eris.messaging.server.InboundSession;

public class InboundSessionImpl implements InboundSession
{
    private InboundConnectionImpl _conn;
    
    private int localChannel = -1;

    private int remoteChannel = -1;

    private UnsignedInteger nextOutgoingId = UnsignedInteger.ONE;

    private UnsignedInteger nextIncomingId = null;

    InboundSessionImpl(InboundConnectionImpl conn)
    {
        _conn = conn;
    }
    
    public int getLocalChannel()
    {
        return localChannel;
    }

    public void setLocalChannel(int localChannel)
    {
        this.localChannel = localChannel;
    }

    public int getRemoteChannel()
    {
        return remoteChannel;
    }

    public void setRemoteChannel(int remoteChannel)
    {
        this.remoteChannel = remoteChannel;
    }

    public UnsignedInteger getNextOutgoingId()
    {
        return nextOutgoingId;
    }

    public void setNextOutgoingId(UnsignedInteger nextOutgoingId)
    {
        this.nextOutgoingId = nextOutgoingId;
    }

    public UnsignedInteger getNextIncomingId()
    {
        return nextIncomingId;
    }

    public void setNextIncomingId(UnsignedInteger nextIncomingId)
    {
        this.nextIncomingId = nextIncomingId;
    }

    @Override
    public String getName()
    {
        return null;
    }
}
