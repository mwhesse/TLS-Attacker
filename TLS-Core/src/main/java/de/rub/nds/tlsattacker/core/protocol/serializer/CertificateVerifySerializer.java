/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.serializer;

import de.rub.nds.tlsattacker.core.constants.HandshakeByteLength;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.CertificateVerifyMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CertificateVerifySerializer
        extends HandshakeMessageSerializer<CertificateVerifyMessage> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final CertificateVerifyMessage msg;

    private ProtocolVersion version;

    /**
     * Constructor for the CertificateVerifyMessageSerializer
     *
     * @param message Message that should be serialized
     * @param version Version of the Protocol
     */
    public CertificateVerifySerializer(CertificateVerifyMessage message, ProtocolVersion version) {
        super(message);
        this.version = version;
        this.msg = message;
    }

    @Override
    public byte[] serializeHandshakeMessageContent() {
        LOGGER.debug("Serializing CertificateVerifyMessage");
        if (version == ProtocolVersion.TLS12
                || version == ProtocolVersion.DTLS12
                || version.is13()) {
            writeSignatureHashAlgorithm(msg);
        }
        writeSignatureLength(msg);
        writeSignature(msg);
        return getAlreadySerialized();
    }

    /** Writes the SignatureHashAlgorithm of the CertificateVerifyMessage into the final byte[] */
    private void writeSignatureHashAlgorithm(CertificateVerifyMessage msg) {
        appendBytes(msg.getSignatureHashAlgorithm().getValue());
        LOGGER.debug("SignatureHashAlgorithms: {}", msg.getSignatureHashAlgorithm().getValue());
    }

    /** Writes the SignatureLength of the CertificateVerifyMessage into the final byte[] */
    private void writeSignatureLength(CertificateVerifyMessage msg) {
        appendInt(msg.getSignatureLength().getValue(), HandshakeByteLength.SIGNATURE_LENGTH);
        LOGGER.debug("SignatureLength: {}", msg.getSignatureLength().getValue());
    }

    /** Writes the Signature of the CertificateVerifyMessage into the final byte[] */
    private void writeSignature(CertificateVerifyMessage msg) {
        appendBytes(msg.getSignature().getValue());
        LOGGER.debug("Signature: {}", msg.getSignature().getValue());
    }
}
