/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.dtls;

import de.rub.nds.modifiablevariable.ModifiableVariableFactory;
import de.rub.nds.modifiablevariable.ModifiableVariableHolder;
import de.rub.nds.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.modifiablevariable.integer.ModifiableInteger;
import de.rub.nds.modifiablevariable.singlebyte.ModifiableByte;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.dtls.handler.DtlsHandshakeMessageFragmentHandler;
import de.rub.nds.tlsattacker.core.dtls.parser.DtlsHandshakeMessageFragmentParser;
import de.rub.nds.tlsattacker.core.dtls.preparator.DtlsHandshakeMessageFragmentPreparator;
import de.rub.nds.tlsattacker.core.dtls.serializer.DtlsHandshakeMessageFragmentSerializer;
import de.rub.nds.tlsattacker.core.layer.data.DataContainer;
import de.rub.nds.tlsattacker.core.state.Context;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

@XmlRootElement(name = "DtlsHandshakeMessageFragment")
public class DtlsHandshakeMessageFragment extends ModifiableVariableHolder
        implements DataContainer {

    @ModifiableVariableProperty private ModifiableInteger fragmentOffset = null;

    @ModifiableVariableProperty(type = ModifiableVariableProperty.Type.LENGTH)
    private ModifiableByte type = null;

    private ModifiableInteger length = null;

    private ModifiableInteger fragmentLength = null;

    private ModifiableInteger epoch = null;

    private ModifiableInteger messageSequence = null;

    private ModifiableByteArray fragmentContent = null;

    private ModifiableByteArray completeResultingMessage = null;

    private HandshakeMessageType handshakeMessageType = null;
    private byte[] fragmentContentConfig = new byte[0];
    private int messageSequenceConfig;
    private int offsetConfig;
    private int handshakeMessageLengthConfig;
    private HandshakeMessageType handshakeMessageTypeConfig;
    private int maxFragmentLengthConfig;

    public DtlsHandshakeMessageFragment(
            HandshakeMessageType handshakeMessageType,
            byte[] fragmentContentConfig,
            int messageSequenceConfig,
            int offsetConfig,
            int handshakeMessageLengthConfig) {
        this.handshakeMessageType = handshakeMessageType;
        this.handshakeMessageTypeConfig = handshakeMessageType;
        this.fragmentContentConfig = fragmentContentConfig;
        this.messageSequenceConfig = messageSequenceConfig;
        this.offsetConfig = offsetConfig;
        this.handshakeMessageLengthConfig = handshakeMessageLengthConfig;
    }

    public DtlsHandshakeMessageFragment() {
        this.handshakeMessageType = HandshakeMessageType.UNKNOWN;
    }

    public DtlsHandshakeMessageFragment(Config tlsConfig) {
        this.handshakeMessageType = HandshakeMessageType.UNKNOWN;
        this.maxFragmentLengthConfig = tlsConfig.getDtlsMaximumFragmentLength();
    }

    public DtlsHandshakeMessageFragment(int maxFragmentLengthConfig) {
        this.handshakeMessageType = HandshakeMessageType.UNKNOWN;
        this.maxFragmentLengthConfig = maxFragmentLengthConfig;
    }

    public DtlsHandshakeMessageFragment(HandshakeMessageType handshakeMessageType) {
        this.handshakeMessageType = handshakeMessageType;
    }

    @Override
    public DtlsHandshakeMessageFragmentHandler getHandler(Context context) {
        return new DtlsHandshakeMessageFragmentHandler();
    }

    @Override
    public DtlsHandshakeMessageFragmentParser getParser(Context context, InputStream stream) {
        return new DtlsHandshakeMessageFragmentParser(stream);
    }

    @Override
    public DtlsHandshakeMessageFragmentPreparator getPreparator(Context context) {
        return new DtlsHandshakeMessageFragmentPreparator(context.getChooser(), this);
    }

    @Override
    public DtlsHandshakeMessageFragmentSerializer getSerializer(Context context) {
        return new DtlsHandshakeMessageFragmentSerializer(this);
    }

    public ModifiableByteArray getCompleteResultingMessage() {
        return completeResultingMessage;
    }

    public void setCompleteResultingMessage(ModifiableByteArray completeResultingMessage) {
        this.completeResultingMessage = completeResultingMessage;
    }

    public void setCompleteResultingMessage(byte[] completeResultingMessage) {
        this.completeResultingMessage =
                ModifiableVariableFactory.safelySetValue(
                        this.completeResultingMessage, completeResultingMessage);
    }

    public ModifiableByte getType() {
        return type;
    }

    public void setType(ModifiableByte type) {
        this.type = type;
    }

    public void setType(byte type) {
        this.type = ModifiableVariableFactory.safelySetValue(this.type, type);
    }

    public ModifiableByteArray getFragmentContent() {
        return fragmentContent;
    }

    public void setFragmentContent(ModifiableByteArray fragmentContent) {
        this.fragmentContent = fragmentContent;
    }

    public void setFragmentContent(byte[] fragmentContent) {
        this.fragmentContent =
                ModifiableVariableFactory.safelySetValue(this.fragmentContent, fragmentContent);
    }

    public void setMessageSequence(int messageSequence) {
        this.messageSequence =
                ModifiableVariableFactory.safelySetValue(this.messageSequence, messageSequence);
    }

    public void setMessageSequence(ModifiableInteger messageSequence) {
        this.messageSequence = messageSequence;
    }

    public ModifiableInteger getMessageSequence() {
        return messageSequence;
    }

    public ModifiableInteger getLength() {
        return length;
    }

    public void setLength(ModifiableInteger length) {
        this.length = length;
    }

    public void setLength(int length) {
        this.length = ModifiableVariableFactory.safelySetValue(this.length, length);
    }

    public HandshakeMessageType getHandshakeMessageTypeConfig() {
        return handshakeMessageTypeConfig;
    }

    public void setHandshakeMessageTypeConfig(HandshakeMessageType handshakeMessageTypeConfig) {
        this.handshakeMessageTypeConfig = handshakeMessageTypeConfig;
    }

    public Integer getMaxFragmentLengthConfig() {
        return maxFragmentLengthConfig;
    }

    public void setMaxFragmentLengthConfig(int maxFragmentLengthConfig) {
        this.maxFragmentLengthConfig = maxFragmentLengthConfig;
    }

    public byte[] getFragmentContentConfig() {
        return fragmentContentConfig;
    }

    public void setFragmentContentConfig(byte[] fragmentContentConfig) {
        this.fragmentContentConfig = fragmentContentConfig;
    }

    public int getMessageSequenceConfig() {
        return messageSequenceConfig;
    }

    public void setMessageSequenceConfig(int messageSequenceConfig) {
        this.messageSequenceConfig = messageSequenceConfig;
    }

    public int getOffsetConfig() {
        return offsetConfig;
    }

    public void setOffsetConfig(int offsetConfig) {
        this.offsetConfig = offsetConfig;
    }

    public int getHandshakeMessageLengthConfig() {
        return handshakeMessageLengthConfig;
    }

    public void setHandshakeMessageLengthConfig(int handshakeMessageLengthConfig) {
        this.handshakeMessageLengthConfig = handshakeMessageLengthConfig;
    }

    public ModifiableInteger getFragmentOffset() {
        return fragmentOffset;
    }

    public ModifiableInteger getFragmentLength() {
        return fragmentLength;
    }

    public void setFragmentOffset(int fragmentOffset) {
        this.fragmentOffset =
                ModifiableVariableFactory.safelySetValue(this.fragmentOffset, fragmentOffset);
    }

    public void setFragmentOffset(ModifiableInteger fragmentOffset) {
        this.fragmentOffset = fragmentOffset;
    }

    public void setFragmentLength(int fragmentLength) {
        this.fragmentLength =
                ModifiableVariableFactory.safelySetValue(this.fragmentLength, fragmentLength);
    }

    public void setFragmentLength(ModifiableInteger fragmentLength) {
        this.fragmentLength = fragmentLength;
    }

    public ModifiableInteger getEpoch() {
        return epoch;
    }

    public void setEpoch(ModifiableInteger epoch) {
        this.epoch = epoch;
    }

    public void setEpoch(int epoch) {
        this.epoch = ModifiableVariableFactory.safelySetValue(this.epoch, epoch);
    }

    public HandshakeMessageType getHandshakeMessageType() {
        return handshakeMessageType;
    }

    public void setHandshakeMessageType(HandshakeMessageType handshakeMessageType) {
        this.handshakeMessageType = handshakeMessageType;
    }

    @Override
    public String toCompactString() {
        return this.getHandshakeMessageType().name().toUpperCase() + "_DTLS_FRAGMENT";
    }

    @Override
    public String toShortString() {
        return "DTLS_FRAG";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.fragmentOffset);
        hash = 67 * hash + Objects.hashCode(this.fragmentLength);
        hash = 67 * hash + Objects.hashCode(this.epoch);
        hash = 67 * hash + Arrays.hashCode(this.fragmentContentConfig);
        hash = 67 * hash + this.messageSequenceConfig;
        hash = 67 * hash + this.offsetConfig;
        hash = 67 * hash + this.handshakeMessageLengthConfig;
        hash = 67 * hash + Objects.hashCode(this.handshakeMessageTypeConfig);
        hash = 67 * hash + this.maxFragmentLengthConfig;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DtlsHandshakeMessageFragment other = (DtlsHandshakeMessageFragment) obj;
        if (this.messageSequenceConfig != other.messageSequenceConfig) {
            return false;
        }
        if (this.offsetConfig != other.offsetConfig) {
            return false;
        }
        if (this.handshakeMessageLengthConfig != other.handshakeMessageLengthConfig) {
            return false;
        }
        if (this.maxFragmentLengthConfig != other.maxFragmentLengthConfig) {
            return false;
        }
        if (!Objects.equals(this.fragmentOffset, other.fragmentOffset)) {
            return false;
        }
        if (!Objects.equals(this.fragmentLength, other.fragmentLength)) {
            return false;
        }
        if (!Objects.equals(this.epoch, other.epoch)) {
            return false;
        }
        if (!Arrays.equals(this.fragmentContentConfig, other.fragmentContentConfig)) {
            return false;
        }
        return this.handshakeMessageTypeConfig == other.handshakeMessageTypeConfig;
    }
}
