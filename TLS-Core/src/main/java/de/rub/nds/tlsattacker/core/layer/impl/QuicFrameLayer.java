/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.layer.impl;

import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.CONNECTION_CLOSE_QUIC_FRAME;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.CRYPTO_FRAME;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.PATH_CHALLENGE_FRAME;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.PATH_RESPONSE_FRAME;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.PING_FRAME;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.STREAM_FRAME;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.STREAM_FRAME_FIN;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.STREAM_FRAME_LEN;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.STREAM_FRAME_LEN_FIN;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.STREAM_FRAME_OFF;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.STREAM_FRAME_OFF_FIN;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.STREAM_FRAME_OFF_LEN;
import static de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType.STREAM_FRAME_OFF_LEN_FIN;

import de.rub.nds.protocol.exception.EndOfStreamException;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.exceptions.TimeoutException;
import de.rub.nds.tlsattacker.core.layer.AcknowledgingProtocolLayer;
import de.rub.nds.tlsattacker.core.layer.LayerConfiguration;
import de.rub.nds.tlsattacker.core.layer.LayerProcessingResult;
import de.rub.nds.tlsattacker.core.layer.constant.ImplementedLayers;
import de.rub.nds.tlsattacker.core.layer.hints.LayerProcessingHint;
import de.rub.nds.tlsattacker.core.layer.hints.QuicFrameLayerHint;
import de.rub.nds.tlsattacker.core.layer.hints.QuicPacketLayerHint;
import de.rub.nds.tlsattacker.core.layer.hints.RecordLayerHint;
import de.rub.nds.tlsattacker.core.layer.stream.HintedLayerInputStream;
import de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType;
import de.rub.nds.tlsattacker.core.quic.constants.QuicPacketType;
import de.rub.nds.tlsattacker.core.quic.frame.AckFrame;
import de.rub.nds.tlsattacker.core.quic.frame.ConnectionCloseFrame;
import de.rub.nds.tlsattacker.core.quic.frame.CryptoFrame;
import de.rub.nds.tlsattacker.core.quic.frame.HandshakeDoneFrame;
import de.rub.nds.tlsattacker.core.quic.frame.NewConnectionIdFrame;
import de.rub.nds.tlsattacker.core.quic.frame.NewTokenFrame;
import de.rub.nds.tlsattacker.core.quic.frame.PaddingFrame;
import de.rub.nds.tlsattacker.core.quic.frame.PathChallengeFrame;
import de.rub.nds.tlsattacker.core.quic.frame.PathResponseFrame;
import de.rub.nds.tlsattacker.core.quic.frame.PingFrame;
import de.rub.nds.tlsattacker.core.quic.frame.QuicFrame;
import de.rub.nds.tlsattacker.core.quic.frame.StreamFrame;
import de.rub.nds.tlsattacker.core.state.quic.QuicContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The QuicFrameLayer handles QUIC frames. The encapsulation into QUIC packets happens in the {@link
 * QuicPacketLayer}.
 */
public class QuicFrameLayer extends AcknowledgingProtocolLayer<QuicFrameLayerHint, QuicFrame> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final QuicContext context;
    private final int MAX_FRAME_SIZE;
    private final int DEFAULT_STREAM_ID = 2;
    private final int MIN_FRAME_SIZE = 32;

    private long initialPhaseExpectedCryptoFrameOffset = 0;
    private long handshakePhaseExpectedCryptoFrameOffset = 0;
    private long applicationPhaseExpectedCryptoFrameOffset = 0;

    private List<CryptoFrame> cryptoFrameBuffer = new ArrayList<>();

    public QuicFrameLayer(QuicContext context) {
        super(ImplementedLayers.QUICFRAME);
        this.context = context;
        this.MAX_FRAME_SIZE = context.getConfig().getQuicMaximumFrameSize();
    }

    /**
     * Sends the given frames of this layer using the lower layer.
     *
     * @return LayerProcessingResult A result object storing information about sending the data
     * @throws IOException When the data cannot be sent
     */
    @Override
    public LayerProcessingResult sendConfiguration() throws IOException {
        LayerConfiguration<QuicFrame> configuration = getLayerConfiguration();
        if (configuration != null && configuration.getContainerList() != null) {
            for (QuicFrame frame : configuration.getContainerList()) {
                byte[] bytes = writeFrame(frame);
                QuicPacketLayerHint hint = getHintForFrame();
                addProducedContainer(frame);
                getLowerLayer().sendData(hint, bytes);
            }
        }
        return getLayerResult();
    }

    /**
     * Sends data from an upper layer using the lower layer. Puts the given bytes into frames and
     * sends those.
     *
     * @param hint Hint for the layer
     * @param data The data to send
     * @return LayerProcessingResult A result object containing information about the sent packets
     * @throws IOException When the data cannot be sent
     */
    @Override
    public LayerProcessingResult sendData(QuicFrameLayerHint hint, byte[] data) throws IOException {
        if (hint != null && hint.getMessageType() != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            QuicPacketLayerHint packetLayerHint;
            switch (hint.getMessageType()) {
                case HANDSHAKE:
                    if (hint.isFirstMessage()) {
                        packetLayerHint = new QuicPacketLayerHint(QuicPacketType.INITIAL_PACKET);
                    } else {
                        packetLayerHint = new QuicPacketLayerHint(QuicPacketType.HANDSHAKE_PACKET);
                    }
                    List<QuicFrame> givenFrames = getUnprocessedConfiguredContainers();
                    if (getLayerConfiguration().getContainerList() != null
                            && givenFrames.size() > 0) {
                        givenFrames =
                                givenFrames.stream()
                                        .filter(
                                                frame ->
                                                        QuicFrameType.getFrameType(
                                                                        frame.getFrameType()
                                                                                .getValue())
                                                                == QuicFrameType.CRYPTO_FRAME)
                                        .collect(Collectors.toList());
                        int offset = 0;
                        for (QuicFrame frame : givenFrames) {
                            int toCopy =
                                    ((CryptoFrame) frame).getMaxFrameLengthConfig() != 0
                                            ? ((CryptoFrame) frame).getMaxFrameLengthConfig()
                                            : MAX_FRAME_SIZE;
                            byte[] payload = Arrays.copyOfRange(data, offset, offset + toCopy);
                            ((CryptoFrame) frame).setCryptoDataConfig(payload);
                            ((CryptoFrame) frame).setOffsetConfig(offset);
                            ((CryptoFrame) frame).setLengthConfig(payload.length);
                            stream = new ByteArrayOutputStream();
                            stream.writeBytes(writeFrame(frame));
                            addProducedContainer(frame);
                            // TODO: Add option to pass everything together to the next layer
                            getLowerLayer().sendData(packetLayerHint, stream.toByteArray());

                            offset += toCopy;
                            if (offset >= data.length) {
                                break;
                            }
                        }
                        // Not enough crypto frames
                        for (; offset < data.length; offset += MAX_FRAME_SIZE) {
                            byte[] payload =
                                    Arrays.copyOfRange(
                                            data,
                                            offset,
                                            Math.min(offset + MAX_FRAME_SIZE, data.length));
                            CryptoFrame frame = new CryptoFrame(payload, offset, payload.length);
                            stream = new ByteArrayOutputStream();
                            stream.writeBytes(writeFrame(frame));
                            addProducedContainer(frame);
                            // TODO: Add option to pass everything together to the next layer
                            getLowerLayer().sendData(packetLayerHint, stream.toByteArray());
                        }
                    } else {
                        // produce enough crypto frames
                        for (int offset = 0; offset < data.length; offset += MAX_FRAME_SIZE) {
                            byte[] payload =
                                    Arrays.copyOfRange(
                                            data,
                                            offset,
                                            Math.min(offset + MAX_FRAME_SIZE, data.length));
                            CryptoFrame frame = new CryptoFrame(payload, offset, payload.length);
                            stream = new ByteArrayOutputStream();
                            stream.writeBytes(writeFrame(frame));
                            addProducedContainer(frame);
                            // TODO: Add option to pass everything together to the next layer
                            getLowerLayer().sendData(packetLayerHint, stream.toByteArray());
                        }
                    }
                    break;
                case APPLICATION_DATA:
                    // TODO: Use existing STREAM frames from the configuration first
                    // prepare hint
                    if (context.isApplicationSecretsInitialized()) {
                        packetLayerHint = new QuicPacketLayerHint(QuicPacketType.ONE_RTT_PACKET);
                    } else {
                        packetLayerHint = new QuicPacketLayerHint(QuicPacketType.ZERO_RTT_PACKET);
                    }
                    // prepare bytes
                    StreamFrame frame = new StreamFrame(data, DEFAULT_STREAM_ID);
                    stream.writeBytes(writeFrame(frame));
                    addProducedContainer(frame);
                    if (data.length < MIN_FRAME_SIZE) {
                        PaddingFrame paddingFrame = new PaddingFrame(MIN_FRAME_SIZE - data.length);
                        stream.writeBytes(writeFrame(paddingFrame));
                        addProducedContainer(paddingFrame);
                    }
                    getLowerLayer().sendData(packetLayerHint, stream.toByteArray());
                    break;
            }
        } else {
            throw new UnsupportedOperationException(
                    "No QuicFrameLayerHint passed - Not supported yet.");
        }
        return getLayerResult();
    }

    /**
     * Receives data from the lower layer.
     *
     * @return LayerProcessingResult A result object containing information about the received data.
     */
    @Override
    public LayerProcessingResult receiveData() {
        try {
            InputStream dataStream;
            do {
                try {
                    dataStream = getLowerLayer().getDataStream();
                    readFrames(dataStream);
                } catch (IOException ex) {
                    LOGGER.warn("The lower layer did not produce a data stream: ", ex);
                    return getLayerResult();
                }
            } while (shouldContinueProcessing());
        } catch (TimeoutException ex) {
            LOGGER.debug("Received a timeout");
            LOGGER.trace(ex);
        } catch (EndOfStreamException ex) {
            LOGGER.debug("Reached end of stream, cannot parse more messages");
            LOGGER.trace(ex);
        }
        return getLayerResult();
    }

    /**
     * Receive more data for the upper layer using the lower layer.
     *
     * @param hint This hint from the calling layer specifies which data its wants to read.
     * @throws IOException When no data can be read
     */
    @Override
    public void receiveMoreDataForHint(LayerProcessingHint hint) throws IOException {
        try {
            InputStream dataStream;
            try {
                dataStream = getLowerLayer().getDataStream();
                // For now, we ignore the hint
                readFrames(dataStream);
            } catch (IOException ex) {
                LOGGER.warn("The lower layer did not produce a data stream: ", ex);
            }
        } catch (TimeoutException ex) {
            LOGGER.debug("Received a timeout");
            LOGGER.trace(ex);
        } catch (EndOfStreamException ex) {
            LOGGER.debug("Reached end of stream, cannot parse more messages");
            LOGGER.trace(ex);
        }
    }

    /** Reads all frames in one QUIC packet and add to frame buffer. */
    private void readFrames(InputStream dataStream) throws IOException {
        PushbackInputStream inputStream = new PushbackInputStream(dataStream);
        RecordLayerHint recordLayerHint = null;
        boolean isAckEliciting = false;

        while (inputStream.available() > 0) {
            int firstByte = inputStream.read();
            QuicFrameType frameType = QuicFrameType.getFrameType((byte) firstByte);
            switch (frameType) {
                case ACK_FRAME:
                    readDataContainer(new AckFrame(false), context, inputStream);
                    break;
                case ACK_FRAME_WITH_ECN:
                    readDataContainer(new AckFrame(true), context, inputStream);
                    break;
                case CONNECTION_CLOSE_QUIC_FRAME:
                    readDataContainer(new ConnectionCloseFrame(true), context, inputStream);
                    break;
                case CONNECTION_CLOSE_APPLICATION_FRAME:
                    readDataContainer(new ConnectionCloseFrame(false), context, inputStream);
                    break;
                case CRYPTO_FRAME:
                    recordLayerHint = new RecordLayerHint(ProtocolMessageType.HANDSHAKE);
                    CryptoFrame frame = new CryptoFrame();
                    readDataContainer(frame, context, inputStream);
                    cryptoFrameBuffer.add(frame);
                    isAckEliciting = true;
                    break;
                case HANDSHAKE_DONE_FRAME:
                    readDataContainer(new HandshakeDoneFrame(), context, inputStream);
                    isAckEliciting = true;
                    break;
                case NEW_CONNECTION_ID_FRAME:
                    readDataContainer(new NewConnectionIdFrame(), context, inputStream);
                    isAckEliciting = true;
                    break;
                case NEW_TOKEN_FRAME:
                    readDataContainer(new NewTokenFrame(), context, inputStream);
                    isAckEliciting = true;
                    break;
                case PADDING_FRAME:
                    readDataContainer(new PaddingFrame(), context, inputStream);
                    break;
                case PATH_CHALLENGE_FRAME:
                    readDataContainer(new PathChallengeFrame(), context, inputStream);
                    isAckEliciting = true;
                    break;
                case PATH_RESPONSE_FRAME:
                    readDataContainer(new PathResponseFrame(), context, inputStream);
                    isAckEliciting = true;
                    break;
                case PING_FRAME:
                    readDataContainer(new PingFrame(), context, inputStream);
                    isAckEliciting = true;
                    break;
                case STREAM_FRAME:
                case STREAM_FRAME_OFF_LEN_FIN:
                case STREAM_FRAME_OFF_LEN:
                case STREAM_FRAME_LEN_FIN:
                case STREAM_FRAME_OFF_FIN:
                case STREAM_FRAME_FIN:
                case STREAM_FRAME_LEN:
                case STREAM_FRAME_OFF:
                    readDataContainer(new StreamFrame(frameType), context, inputStream);
                    isAckEliciting = true;
                    break;
                default:
                    LOGGER.error("Undefined QUIC frame type");
                    break;
            }
        }

        // reorder cryptoFrames according to offset and check if they are consecutive and can be
        // passed to the upper layer without gaps
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (!cryptoFrameBuffer.isEmpty()) {
            cryptoFrameBuffer.sort(Comparator.comparingLong(frame -> frame.getOffset().getValue()));
            cryptoFrameBuffer = cryptoFrameBuffer.stream().distinct().collect(Collectors.toList());
            if (isCryptoBufferConsecutive()) {
                for (CryptoFrame frame : cryptoFrameBuffer) {
                    outputStream.write(frame.getCryptoData().getValue());
                }
                CryptoFrame lastFrame = cryptoFrameBuffer.get(cryptoFrameBuffer.size() - 1);
                long nextExpectedCryptoOffset =
                        lastFrame.getOffset().getValue() + lastFrame.getLength().getValue();
                if (!context.isHandshakeSecretsInitialized()) {
                    initialPhaseExpectedCryptoFrameOffset = nextExpectedCryptoOffset;
                } else if (!context.isApplicationSecretsInitialized()) {
                    handshakePhaseExpectedCryptoFrameOffset = nextExpectedCryptoOffset;
                } else {
                    applicationPhaseExpectedCryptoFrameOffset = nextExpectedCryptoOffset;
                }
                cryptoFrameBuffer.clear();
            }
        }

        if (isAckEliciting) {
            sendAck(null);
        } else {
            if (!context.getReceivedPackets().isEmpty()) {
                context.getReceivedPackets().removeLast();
            }
        }

        if (currentInputStream == null) {
            currentInputStream = new HintedLayerInputStream(recordLayerHint, this);
            currentInputStream.extendStream(outputStream.toByteArray());
        } else {
            currentInputStream.setHint(recordLayerHint);
            currentInputStream.extendStream(outputStream.toByteArray());
        }

        outputStream.flush();
    }

    private boolean isCryptoBufferConsecutive() {
        long lastSeenCryptoOffset;
        if (!context.isHandshakeSecretsInitialized()) {
            lastSeenCryptoOffset = initialPhaseExpectedCryptoFrameOffset;
        } else if (!context.isApplicationSecretsInitialized()) {
            lastSeenCryptoOffset = handshakePhaseExpectedCryptoFrameOffset;
        } else {
            lastSeenCryptoOffset = applicationPhaseExpectedCryptoFrameOffset;
        }
        if (cryptoFrameBuffer.get(0).getOffset().getValue() != lastSeenCryptoOffset) {
            LOGGER.warn(
                    "Missing CryptoFrames in buffer: {}, lastSeenCryptoOffset={}",
                    cryptoBufferToString(),
                    lastSeenCryptoOffset);
            return false;
        }
        for (int i = 1; i < cryptoFrameBuffer.size(); i++) {
            if (cryptoFrameBuffer.get(i).getOffset().getValue()
                    != cryptoFrameBuffer.get(i - 1).getOffset().getValue()
                            + cryptoFrameBuffer.get(i - 1).getLength().getValue()) {
                LOGGER.warn(
                        "Missing CryptoFrames in buffer: {}, lastSeenCryptoOffset={}",
                        cryptoBufferToString(),
                        lastSeenCryptoOffset);
                return false;
            }
        }
        return true;
    }

    private String cryptoBufferToString() {
        return cryptoFrameBuffer.stream()
                .map(
                        cryptoFrame ->
                                "o: "
                                        + cryptoFrame.getOffset().getValue()
                                        + ", l: "
                                        + cryptoFrame.getLength().getValue())
                .collect(Collectors.joining(" | "));
    }

    private byte[] writeFrame(QuicFrame frame) {
        frame.getPreparator(context).prepare();
        return frame.getSerializer(context).serialize();
    }

    private QuicPacketLayerHint getHintForFrame() {
        if (context.isInitialSecretsInitialized() && !context.isHandshakeSecretsInitialized()) {
            return new QuicPacketLayerHint(QuicPacketType.INITIAL_PACKET);
        } else if (context.isHandshakeSecretsInitialized()
                && !context.isApplicationSecretsInitialized()) {
            return new QuicPacketLayerHint(QuicPacketType.HANDSHAKE_PACKET);
        } else if (context.isApplicationSecretsInitialized()) {
            return new QuicPacketLayerHint(QuicPacketType.ONE_RTT_PACKET);
        }
        return null;
    }

    @Override
    public void sendAck(byte[] data) {
        AckFrame frame = new AckFrame(false);
        if (context.getReceivedPackets().getLast() == QuicPacketType.INITIAL_PACKET) {
            frame.setLargestAcknowledged(context.getReceivedInitialPacketNumbers().getLast());
            LOGGER.debug(
                    "Send Ack for Initial Packet #{}", frame.getLargestAcknowledged().getValue());
        } else if (context.getReceivedPackets().getLast() == QuicPacketType.HANDSHAKE_PACKET) {
            frame.setLargestAcknowledged(context.getReceivedHandshakePacketNumbers().getLast());
            LOGGER.debug(
                    "Send Ack for Handshake Packet #{}", frame.getLargestAcknowledged().getValue());
        } else if (context.getReceivedPackets().getLast() == QuicPacketType.ONE_RTT_PACKET) {
            frame.setLargestAcknowledged(context.getReceivedOneRTTPacketNumbers().getLast());
            LOGGER.debug("Send Ack for 1RTT Packet #{}", frame.getLargestAcknowledged().getValue());
        }

        frame.setAckDelay(1);
        frame.setAckRangeCount(0);
        frame.setFirstACKRange(0);
        ((AcknowledgingProtocolLayer) getLowerLayer()).sendAck(writeFrame(frame));
    }

    /**
     * Clears the frame buffer and reset the variables. This function is typically used when
     * resetting the connection.
     */
    public void clearCryptoFrameBuffer() {
        cryptoFrameBuffer.clear();
        initialPhaseExpectedCryptoFrameOffset = 0;
        handshakePhaseExpectedCryptoFrameOffset = 0;
        applicationPhaseExpectedCryptoFrameOffset = 0;
    }
}
