/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.quic.packet;

import de.rub.nds.modifiablevariable.ModifiableVariableFactory;
import de.rub.nds.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.tlsattacker.core.layer.data.Preparator;
import de.rub.nds.tlsattacker.core.layer.data.Serializer;
import de.rub.nds.tlsattacker.core.quic.constants.QuicPacketType;
import de.rub.nds.tlsattacker.core.quic.constants.QuicRetryConstants;
import de.rub.nds.tlsattacker.core.quic.constants.QuicVersion;
import de.rub.nds.tlsattacker.core.quic.handler.packet.RetryPacketHandler;
import de.rub.nds.tlsattacker.core.quic.parser.packet.RetryPacketParser;
import de.rub.nds.tlsattacker.core.quic.preparator.packet.RetryPacketPreparator;
import de.rub.nds.tlsattacker.core.quic.serializer.packet.RetryPacketSerializer;
import de.rub.nds.tlsattacker.core.state.Context;
import de.rub.nds.tlsattacker.core.state.quic.QuicContext;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.Arrays;

/**
 * A Retry packet carries an address validation token created by the server. It is used by a server
 * that wishes to perform a retry.
 */
@XmlRootElement
public class RetryPacket extends LongHeaderPacket {

    private static final Logger LOGGER = LogManager.getLogger();

    @ModifiableVariableProperty protected ModifiableByteArray retryToken;

    @ModifiableVariableProperty protected ModifiableByteArray retryIntegrityTag;

    public RetryPacket() {
        super(QuicPacketType.RETRY_PACKET);
        // TODO: Constant fixed, but not sure whether we should set this here
        this.setUnprotectedFlags((byte) 0xf0);
    }

    public RetryPacket(byte flags) {
        super(QuicPacketType.RETRY_PACKET);
        this.setProtectedFlags(flags);
        // We do not have any header protection in Retry packets
        this.setUnprotectedFlags(flags);
        protectedHeaderHelper.write(flags);
    }

    /**
     * Verifies the correctness of the Integrity Tag within this Retry Packet to determine
     * processing
     *
     * @param context Current QUIC Context
     * @return Whether the Retry Packet's Integrity is confirmed
     */
    public boolean verifyRetryIntegrityTag(QuicContext context) {
        // For construction of QUIC Retry Packet Integrity Pseudo Packet, see 5.8, RFC 9001
        byte[] pseudoPacket =
                ByteBuffer.allocate(
                                1 /* ODCID length field */
                                        + context.getFirstDestinationConnectionId().length
                                        + 1 /* Flags Byte */
                                        + 4 /* Version Field */
                                        + 1 /* DCID length field */
                                        + getDestinationConnectionIdLength().getValue()
                                        + 1 /* SCID length field */
                                        + getSourceConnectionIdLength().getValue()
                                        + retryToken.getValue().length)
                        .put((byte) (context.getFirstDestinationConnectionId().length & 0xff))
                        .put(context.getFirstDestinationConnectionId())
                        .put((byte) (getUnprotectedFlags().getValue()))
                        .put(context.getQuicVersion().getByteValue())
                        .put(getDestinationConnectionIdLength().getValue())
                        .put(getDestinationConnectionId().getValue())
                        .put(getSourceConnectionIdLength().getValue())
                        .put(getSourceConnectionId().getValue())
                        .put(retryToken.getValue())
                        .array();
        LOGGER.trace("Build Integrity Check Pseudo Packet {}", pseudoPacket);

        byte[] computedTag;
        try {
            // Secret Key is fixed value from 5.8, RFC 9001 (or 3.3.3, RFC 9369 for QUICv2)
            SecretKey secretKey =
                    new SecretKeySpec(
                            context.getQuicVersion() == QuicVersion.VERSION_1
                                    ? QuicRetryConstants.getQuic1RetryIntegrityTagKey()
                                    : QuicRetryConstants.getQuic2RetryIntegrityTagKey(),
                            "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            // IV is fixed value from 5.8, RFC 9001 (or 3.3.3, RFC 9369 for QUICv2)
            GCMParameterSpec gcmParameterSpec =
                    new GCMParameterSpec(
                            128,
                            context.getQuicVersion() == QuicVersion.VERSION_1
                                    ? QuicRetryConstants.getQuic1RetryIntegrityTagIv()
                                    : QuicRetryConstants.getQuic2RetryIntegrityTagIv());
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            cipher.updateAAD(pseudoPacket);
            computedTag = cipher.doFinal();
        } catch (AEADBadTagException e) {
            LOGGER.debug("Retry Tag is invalid!");
            return false;
        } catch (NoSuchAlgorithmException
                | NoSuchPaddingException
                | InvalidKeyException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException
                | BadPaddingException e) {
            LOGGER.error("Error initializing Ciphers to verify Retry Integrity Tag!");
            LOGGER.trace(e);
            return false;
        }
        if (computedTag.length == 0) {
            LOGGER.error(
                    "Attempted to compute Retry Integrity Tag for verification but result is empty!");
            return false;
        }
        boolean tagsEqual = Arrays.areEqual(getRetryIntegrityTag().getValue(), computedTag);
        LOGGER.debug("Retry Integrity Tag is valid? {}", tagsEqual);
        return tagsEqual;
    }

    @Override
    public RetryPacketHandler getHandler(Context context) {
        return new RetryPacketHandler(context.getQuicContext());
    }

    @Override
    public Serializer<RetryPacket> getSerializer(Context context) {
        return new RetryPacketSerializer(this);
    }

    @Override
    public Preparator<RetryPacket> getPreparator(Context context) {
        return new RetryPacketPreparator(context.getChooser(), this);
    }

    @Override
    public RetryPacketParser getParser(Context context, InputStream stream) {
        return new RetryPacketParser(stream, context.getQuicContext());
    }

    public ModifiableByteArray getRetryToken() {
        return retryToken;
    }

    public void setRetryToken(byte[] retryToken) {
        this.retryToken = ModifiableVariableFactory.safelySetValue(this.retryToken, retryToken);
    }

    public void setRetryToken(ModifiableByteArray retryToken) {
        this.retryToken = retryToken;
    }

    public ModifiableByteArray getRetryIntegrityTag() {
        return retryIntegrityTag;
    }

    public void setRetryIntegrityTag(byte[] retryIntegrityTag) {
        this.retryIntegrityTag =
                ModifiableVariableFactory.safelySetValue(this.retryIntegrityTag, retryIntegrityTag);
    }

    public void setRetryIntegrityTag(ModifiableByteArray retryIntegrityTag) {
        this.retryIntegrityTag = retryIntegrityTag;
    }
}
