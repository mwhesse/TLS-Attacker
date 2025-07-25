/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.serializer.extension;

import de.rub.nds.tlsattacker.core.constants.ExtensionByteLength;
import de.rub.nds.tlsattacker.core.protocol.message.extension.PreSharedKeyExtensionMessage;
import de.rub.nds.tlsattacker.transport.ConnectionEndType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** RFC draft-ietf-tls-tls13-21 */
public class PreSharedKeyExtensionSerializer
        extends ExtensionSerializer<PreSharedKeyExtensionMessage> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final PreSharedKeyExtensionMessage msg;

    public PreSharedKeyExtensionSerializer(
            PreSharedKeyExtensionMessage message, ConnectionEndType connectionType) {
        super(message);
        msg = message;
    }

    @Override
    public byte[] serializeExtensionContent() {
        LOGGER.debug("Serializing PreSharedKeyExtensionMessage");
        if (msg.getSelectedIdentity() == null || msg.getSelectedIdentity().getValue() == null) {
            appendInt(
                    msg.getIdentityListLength().getValue(),
                    ExtensionByteLength.PSK_IDENTITY_LIST_LENGTH);
            LOGGER.debug(
                    "PreSharedKeyIdentityListLength: " + msg.getIdentityListLength().getValue());
            writeIdentities();

            appendInt(
                    msg.getBinderListLength().getValue(),
                    ExtensionByteLength.PSK_BINDER_LIST_LENGTH);
            LOGGER.debug("PreSharedKeyBinderListLength: {}", msg.getBinderListLength().getValue());
            writeBinders();
        } else {
            writeSelectedIdentity();
        }

        return getAlreadySerialized();
    }

    public void writeIdentities() {
        appendBytes(msg.getIdentityListBytes().getValue());
    }

    public void writeBinders() {
        appendBytes(msg.getBinderListBytes().getValue());
    }

    public void writeSelectedIdentity() {
        appendInt(
                msg.getSelectedIdentity().getValue(),
                ExtensionByteLength.PSK_SELECTED_IDENTITY_LENGTH);
    }
}
