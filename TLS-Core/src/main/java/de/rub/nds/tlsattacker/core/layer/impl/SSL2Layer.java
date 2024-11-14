/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.layer.impl;

import de.rub.nds.protocol.exception.EndOfStreamException;
import de.rub.nds.tlsattacker.core.constants.SSL2MessageType;
import de.rub.nds.tlsattacker.core.constants.SSL2TotalHeaderLengths;
import de.rub.nds.tlsattacker.core.constants.ssl.SSL2ByteLength;
import de.rub.nds.tlsattacker.core.exceptions.TimeoutException;
import de.rub.nds.tlsattacker.core.layer.LayerConfiguration;
import de.rub.nds.tlsattacker.core.layer.LayerProcessingResult;
import de.rub.nds.tlsattacker.core.layer.ProtocolLayer;
import de.rub.nds.tlsattacker.core.layer.constant.ImplementedLayers;
import de.rub.nds.tlsattacker.core.layer.hints.LayerProcessingHint;
import de.rub.nds.tlsattacker.core.layer.hints.RecordLayerHint;
import de.rub.nds.tlsattacker.core.layer.stream.HintedInputStream;
import de.rub.nds.tlsattacker.core.protocol.handler.SSL2MessageHandler;
import de.rub.nds.tlsattacker.core.protocol.message.SSL2ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.SSL2ClientMasterKeyMessage;
import de.rub.nds.tlsattacker.core.protocol.message.SSL2Message;
import de.rub.nds.tlsattacker.core.protocol.message.SSL2ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.SSL2ServerVerifyMessage;
import de.rub.nds.tlsattacker.core.protocol.message.UnknownSSL2Message;
import de.rub.nds.tlsattacker.core.protocol.preparator.SSL2MessagePreparator;
import de.rub.nds.tlsattacker.core.protocol.serializer.SSL2MessageSerializer;
import de.rub.nds.tlsattacker.core.state.Context;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSL2Layer extends ProtocolLayer<LayerProcessingHint, SSL2Message> {

    private static final Logger LOGGER = LogManager.getLogger();
    private Context context;

    public SSL2Layer(Context context) {
        super(ImplementedLayers.SSL2);
        this.context = context;
    }

    @Override
    public LayerProcessingResult<SSL2Message> sendConfiguration() throws IOException {
        LayerConfiguration<SSL2Message> configuration = getLayerConfiguration();
        if (configuration != null
                && configuration.getContainerList() != null
                && !configuration.getContainerList().isEmpty()) {
            for (SSL2Message ssl2message : getUnprocessedConfiguredContainers()) {
                SSL2MessagePreparator preparator = ssl2message.getPreparator(context);
                preparator.prepare();
                preparator.afterPrepare();
                SSL2MessageHandler handler = ssl2message.getHandler(context);
                handler.adjustContext(ssl2message);
                SSL2MessageSerializer serializer = ssl2message.getSerializer(context);
                byte[] serializedMessage = serializer.serialize();
                ssl2message.setCompleteResultingMessage(serializedMessage);
                handler.adjustContextAfterSerialize(ssl2message);
                handler.updateDigest(ssl2message, true);
                getLowerLayer()
                        .sendData(
                                new RecordLayerHint(ssl2message.getProtocolMessageType()),
                                serializedMessage);
                addProducedContainer(ssl2message);
            }
        }
        return getLayerResult();
    }

    @Override
    public LayerProcessingResult<SSL2Message> sendData(
            LayerProcessingHint hint, byte[] additionalData) throws IOException {
        return sendConfiguration();
    }

    @Override
    public LayerProcessingResult<SSL2Message> receiveData() {
        try {
            int messageLength = 0;
            byte paddingLength = 0;
            byte[] totalHeader;
            HintedInputStream dataStream = null;
            SSL2MessageType messageType = SSL2MessageType.SSL_UNKNOWN;
            try {
                dataStream = getLowerLayer().getDataStream();
                if (dataStream.available() == 0) {
                    LOGGER.debug("Reached end of stream, cannot parse more messages");
                    return getLayerResult();
                }

                totalHeader = dataStream.readNBytes(SSL2ByteLength.LENGTH);
                if (SSL2TotalHeaderLengths.isNoPaddingHeader(totalHeader[0])) {
                    messageLength = resolveUnpaddedMessageLength(totalHeader);
                    paddingLength = 0x00;
                } else {
                    if (SSL2TotalHeaderLengths.isNoPaddingHeader(totalHeader[0])) {
                        messageLength = resolveUnpaddedMessageLength(totalHeader);
                        paddingLength = 0x00;
                    } else {
                        messageLength = resolvePaddedMessageLength(totalHeader);
                        paddingLength = dataStream.readByte();
                    }
                    messageType = SSL2MessageType.getMessageType(dataStream.readByte());
                }
            } catch (IOException e) {
                LOGGER.warn(
                        "Failed to parse SSL2 message header, parsing as unknown SSL2 message", e);
                messageType = SSL2MessageType.SSL_UNKNOWN;
            }

            SSL2Message message = null;
            switch (messageType) {
                case SSL_CLIENT_HELLO:
                    message = new SSL2ClientHelloMessage();
                    break;
                case SSL_CLIENT_MASTER_KEY:
                    message = new SSL2ClientMasterKeyMessage();
                    break;
                case SSL_SERVER_VERIFY:
                    message = new SSL2ServerVerifyMessage();
                    break;
                case SSL_SERVER_HELLO:
                    message = new SSL2ServerHelloMessage();
                    break;
                default:
                    message = new UnknownSSL2Message();
            }
            message.setType((byte) messageType.getType());
            message.setMessageLength(messageLength);
            message.setPaddingLength((int) paddingLength);
            readDataContainer(message, context);
        } catch (TimeoutException ex) {
            LOGGER.debug("Received a timeout");
        } catch (EndOfStreamException ex) {
            LOGGER.debug("Reached end of stream, cannot parse more messages", ex);
        }

        return getLayerResult();
    }

    private static int resolvePaddedMessageLength(final byte[] totalHeaderLength) {
        return (totalHeaderLength[0] & SSL2TotalHeaderLengths.ALL_BUT_TWO_BIT.getValue()) << 8
                | totalHeaderLength[1] & 0xff;
    }

    private static int resolveUnpaddedMessageLength(final byte[] totalHeaderLength) {
        return (totalHeaderLength[0] & SSL2TotalHeaderLengths.ALL_BUT_ONE_BIT.getValue()) << 8
                | totalHeaderLength[1] & 0xff;
    }

    @Override
    public void receiveMoreDataForHint(LayerProcessingHint hint) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
