/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.handler;

import de.rub.nds.protocol.constants.NamedEllipticCurveParameters;
import de.rub.nds.protocol.crypto.ec.Point;
import de.rub.nds.protocol.crypto.ec.PointFormatter;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.layer.context.TlsContext;
import de.rub.nds.tlsattacker.core.protocol.message.ECDHEServerKeyExchangeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ECDHEServerKeyExchangeHandler<KeyExchangeMessage extends ECDHEServerKeyExchangeMessage>
        extends ServerKeyExchangeHandler<KeyExchangeMessage> {

    private static final Logger LOGGER = LogManager.getLogger();

    public ECDHEServerKeyExchangeHandler(TlsContext tlsContext) {
        super(tlsContext);
    }

    @Override
    public void adjustContext(KeyExchangeMessage message) {
        adjustECParameter(message);
        adjustSelectedSignatureAndHashAlgorithm(message);

        if (message.getKeyExchangeComputations() != null) {
            tlsContext.setServerEphemeralEcPrivateKey(
                    message.getKeyExchangeComputations().getPrivateKey().getValue());
        }
    }

    protected void adjustECParameter(ECDHEServerKeyExchangeMessage message) {
        NamedGroup group = NamedGroup.getNamedGroup(message.getNamedGroup().getValue());
        if (group != null) {
            LOGGER.debug("Adjusting selected named group: " + group.name());
            tlsContext.setSelectedGroup(group);

            LOGGER.debug("Adjusting EC Point");
            Point publicKeyPoint =
                    PointFormatter.formatFromByteArray(
                            (NamedEllipticCurveParameters) group.getGroupParameters(),
                            message.getPublicKey().getValue());
            tlsContext.setServerEphemeralEcPublicKey(publicKeyPoint);
        } else {
            LOGGER.warn("Could not adjust server public key, named group is unknown.");
        }
    }
}
