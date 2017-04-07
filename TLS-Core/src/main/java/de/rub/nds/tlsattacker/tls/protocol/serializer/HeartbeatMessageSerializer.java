/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.tls.protocol.serializer;

import de.rub.nds.tlsattacker.tls.constants.HeartbeatByteLength;
import de.rub.nds.tlsattacker.tls.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.tls.protocol.message.HeartbeatMessage;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Robert Merget - robert.merget@rub.de
 */
public class HeartbeatMessageSerializer extends ProtocolMessageSerializer<HeartbeatMessage> {

    private static final Logger LOGGER = LogManager.getLogger("SERIALIZER");

    private final HeartbeatMessage msg;

    /**
     * Constructor for the HeartbeatMessageSerializer
     *
     * @param message
     *            Message that should be serialized
     * @param version
     *            Version of the Protocol
     */
    public HeartbeatMessageSerializer(HeartbeatMessage message, ProtocolVersion version) {
        super(message, version);
        this.msg = message;
    }

    @Override
    public byte[] serializeProtocolMessageContent() {
        writeHeartbeatMessageType(msg);
        writePayloadLength(msg);
        writePayload(msg);
        writePadding(msg);
        return getAlreadySerialized();
    }

    /**
     * Writes the HeartbeatMessageType of the HeartbeatMessage into the final
     * byte[]
     */
    private void writeHeartbeatMessageType(HeartbeatMessage msg) {
        appendByte(msg.getHeartbeatMessageType().getValue());
        LOGGER.debug("HeartbeatMessageType: " + msg.getHeartbeatMessageType().getValue());
    }

    /**
     * Writes the PayloadLength of the HeartbeatMessage into the final byte[]
     */
    private void writePayloadLength(HeartbeatMessage msg) {
        appendInt(msg.getPayloadLength().getValue(), HeartbeatByteLength.PAYLOAD_LENGTH);
        LOGGER.debug("PayloadLength: " + msg.getPayloadLength().getValue());
    }

    /**
     * Writes the Payload of the HeartbeatMessage into the final byte[]
     */
    private void writePayload(HeartbeatMessage msg) {
        appendBytes(msg.getPayload().getValue());
        LOGGER.debug("Payload: " + Arrays.toString(msg.getPayload().getValue()));
    }

    /**
     * Writes the Padding of the HeartbeatMessage into the final byte[]
     */
    private void writePadding(HeartbeatMessage msg) {
        appendBytes(msg.getPadding().getValue());
        LOGGER.debug("Padding: " + Arrays.toString(msg.getPadding().getValue()));
    }

}
