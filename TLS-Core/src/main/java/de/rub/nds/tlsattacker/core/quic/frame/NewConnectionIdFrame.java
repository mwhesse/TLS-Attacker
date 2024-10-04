/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.quic.frame;

import de.rub.nds.modifiablevariable.ModifiableVariableFactory;
import de.rub.nds.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.modifiablevariable.integer.ModifiableInteger;
import de.rub.nds.modifiablevariable.longint.ModifiableLong;
import de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType;
import de.rub.nds.tlsattacker.core.quic.handler.frame.NewConnectionIdFrameHandler;
import de.rub.nds.tlsattacker.core.quic.parser.frame.NewConnectionIdFrameParser;
import de.rub.nds.tlsattacker.core.quic.preparator.frame.NewConnectionIdFramePreparator;
import de.rub.nds.tlsattacker.core.quic.serializer.frame.NewConnectionIdFrameSerializer;
import de.rub.nds.tlsattacker.core.state.quic.QuicContext;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;

/**
 * An endpoint sends a NEW_CONNECTION_ID frame (type=0x18) to provide its peer with alternative
 * connection IDs that can be used to break linkability when migrating connections.
 */
@XmlRootElement
public class NewConnectionIdFrame extends QuicFrame {

    @ModifiableVariableProperty protected ModifiableLong sequenceNumber;

    @ModifiableVariableProperty protected ModifiableLong retirePriorTo;

    @ModifiableVariableProperty protected ModifiableInteger connectionIdLength;

    @ModifiableVariableProperty protected ModifiableByteArray connectionId;

    @ModifiableVariableProperty protected ModifiableByteArray statelessResetToken;

    private long sequenceNumberConfig;
    private long retirePriorToConfig;
    private int lengthConfig;
    private byte[] connectionIdConfig;
    private byte[] statelessResetTokenConfig;

    public static final int STATELESS_RESET_TOKEN_LENGTH = 16;

    public static final int CONNECTION_ID_LENGTH_FIELD_LENGTH = 1;

    public NewConnectionIdFrame() {
        super(QuicFrameType.NEW_CONNECTION_ID_FRAME);
    }

    @Override
    public NewConnectionIdFrameHandler getHandler(QuicContext context) {
        return new NewConnectionIdFrameHandler(context);
    }

    @Override
    public NewConnectionIdFrameSerializer getSerializer(QuicContext context) {
        return new NewConnectionIdFrameSerializer(this);
    }

    @Override
    public NewConnectionIdFramePreparator getPreparator(QuicContext context) {
        return new NewConnectionIdFramePreparator(context.getChooser(), this);
    }

    @Override
    public NewConnectionIdFrameParser getParser(QuicContext context, InputStream stream) {
        return new NewConnectionIdFrameParser(stream);
    }

    public ModifiableLong getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber =
                ModifiableVariableFactory.safelySetValue(this.sequenceNumber, sequenceNumber);
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.setSequenceNumber((long) sequenceNumber);
    }

    public ModifiableLong getRetirePriorTo() {
        return retirePriorTo;
    }

    public void setRetirePriorTo(long retirePriorTo) {
        this.retirePriorTo =
                ModifiableVariableFactory.safelySetValue(this.retirePriorTo, retirePriorTo);
    }

    public void setRetirePriorTo(int retirePriorTo) {
        this.setRetirePriorTo((long) retirePriorTo);
    }

    public ModifiableInteger getConnectionIdLength() {
        return connectionIdLength;
    }

    public void setConnectionIdLength(int length) {
        this.connectionIdLength =
                ModifiableVariableFactory.safelySetValue(this.connectionIdLength, length);
    }

    public ModifiableByteArray getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(byte[] connectionId) {
        this.connectionId =
                ModifiableVariableFactory.safelySetValue(this.connectionId, connectionId);
    }

    public ModifiableByteArray getStatelessResetToken() {
        return statelessResetToken;
    }

    public void setStatelessResetToken(byte[] statelessResetToken) {
        this.statelessResetToken =
                ModifiableVariableFactory.safelySetValue(
                        this.statelessResetToken, statelessResetToken);
    }

    public long getSequenceNumberConfig() {
        return sequenceNumberConfig;
    }

    public void setSequenceNumberConfig(long sequenceNumberConfig) {
        this.sequenceNumberConfig = sequenceNumberConfig;
    }

    public long getRetirePriorToConfig() {
        return retirePriorToConfig;
    }

    public void setRetirePriorToConfig(long retirePriorToConfig) {
        this.retirePriorToConfig = retirePriorToConfig;
    }

    public int getLengthConfig() {
        return lengthConfig;
    }

    public void setLengthConfig(int lengthConfig) {
        this.lengthConfig = lengthConfig;
    }

    public byte[] getConnectionIdConfig() {
        return connectionIdConfig;
    }

    public void setConnectionIdConfig(byte[] connectionIdConfig) {
        this.connectionIdConfig = connectionIdConfig;
    }

    public byte[] getStatelessResetTokenConfig() {
        return statelessResetTokenConfig;
    }

    public void setStatelessResetTokenConfig(byte[] statelessResetTokenConfig) {
        this.statelessResetTokenConfig = statelessResetTokenConfig;
    }
}
