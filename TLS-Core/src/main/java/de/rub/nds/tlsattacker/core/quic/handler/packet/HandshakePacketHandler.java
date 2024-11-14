/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.quic.handler.packet;

import de.rub.nds.tlsattacker.core.exceptions.CryptoException;
import de.rub.nds.tlsattacker.core.quic.packet.HandshakePacket;
import de.rub.nds.tlsattacker.core.quic.packet.QuicPacketCryptoComputations;
import de.rub.nds.tlsattacker.core.state.quic.QuicContext;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HandshakePacketHandler extends LongHeaderPacketHandler<HandshakePacket> {

    private static final Logger LOGGER = LogManager.getLogger();

    public HandshakePacketHandler(QuicContext quicContext) {
        super(quicContext);
    }

    @Override
    public void adjustContext(HandshakePacket object) {
        // update quic keys
        try {
            if (!quicContext.isHandshakeSecretsInitialized()) {
                QuicPacketCryptoComputations.calculateHandshakeSecrets(quicContext.getContext());
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | CryptoException e) {
            LOGGER.error("Could not calculate handshake secrets", e);
        }
    }
}
