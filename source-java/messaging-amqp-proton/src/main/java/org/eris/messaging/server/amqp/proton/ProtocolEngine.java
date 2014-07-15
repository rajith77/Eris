package org.eris.messaging.server.amqp.proton;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.UnsignedShort;
import org.apache.qpid.proton.amqp.transport.Attach;
import org.apache.qpid.proton.amqp.transport.Begin;
import org.apache.qpid.proton.amqp.transport.Close;
import org.apache.qpid.proton.amqp.transport.Detach;
import org.apache.qpid.proton.amqp.transport.Disposition;
import org.apache.qpid.proton.amqp.transport.End;
import org.apache.qpid.proton.amqp.transport.Flow;
import org.apache.qpid.proton.amqp.transport.FrameBody;
import org.apache.qpid.proton.amqp.transport.Open;
import org.apache.qpid.proton.amqp.transport.Transfer;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.impl.FrameHandler;
import org.apache.qpid.proton.framing.TransportFrame;
import org.eris.messaging.server.InboundConnection;

/**
 * One instance per Connection.  
 */
public class ProtocolEngine 
{
    void encode(ByteBuffer out, int channel, Object frameBody, ByteBuffer payload)
    {
        
    }
}