/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.preparator;

import de.rub.nds.protocol.util.SilentByteArrayOutputStream;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.layer.data.Preparator;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessagePreparator;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.HandshakeMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.EncryptedServerNameIndicationExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.ExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.KeyShareExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.PreSharedKeyExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.preparator.extension.EncryptedServerNameIndicationExtensionPreparator;
import de.rub.nds.tlsattacker.core.protocol.preparator.extension.PreSharedKeyExtensionPreparator;
import de.rub.nds.tlsattacker.core.protocol.serializer.HandshakeMessageSerializer;
import de.rub.nds.tlsattacker.core.workflow.chooser.Chooser;
import de.rub.nds.tlsattacker.transport.ConnectionEndType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @param <T> The HandshakeMessage that should be prepared
 */
public abstract class HandshakeMessagePreparator<T extends HandshakeMessage>
        extends ProtocolMessagePreparator<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    public HandshakeMessagePreparator(Chooser chooser, T message) {
        super(chooser, message);
    }

    protected void prepareMessageLength(int length) {
        message.setLength(length);
        LOGGER.debug("Length: {}", message.getLength().getValue());
    }

    private void prepareMessageType(HandshakeMessageType type) {
        message.setType(type.getValue());
        LOGGER.debug("Type: {}", message.getType().getValue());
    }

    private void prepareMessageContent(byte[] content) {
        message.setMessageContent(content);
        LOGGER.debug("Handshake message content: {}", message.getMessageContent().getValue());
    }

    @Override
    protected void prepareProtocolMessageContents() {
        prepareHandshakeMessageContents();
        prepareEncapsulatingFields();
    }

    public void prepareEncapsulatingFields() {
        HandshakeMessageSerializer<?> serializer = message.getSerializer(chooser.getContext());
        byte[] content = serializer.serializeHandshakeMessageContent();
        prepareMessageContent(content);
        prepareMessageLength(message.getMessageContent().getValue().length);
        prepareMessageType(message.getHandshakeMessageType());
    }

    public void autoSelectExtensions(
            Config tlsConfig,
            Set<ExtensionType> proposedExtensions,
            Set<ExtensionType> forbiddenExtensions,
            ExtensionType... exceptions) {
        setExtensionsBasedOnProposals(
                message.createConfiguredExtensions(tlsConfig),
                proposedExtensions,
                forbiddenExtensions,
                exceptions);
        LOGGER.debug(
                "Automatically selected extensions for message {}: {}",
                message.getHandshakeMessageType().name(),
                message.getExtensions().stream()
                        .map(ExtensionMessage::getExtensionTypeConstant)
                        .map(ExtensionType::name)
                        .collect(Collectors.joining(",")));
    }

    /**
     * @param configuredExtensions List of extensions to be added based on config
     * @param clientProposedExtensions List of types proposed by the client
     * @param forbiddenExtensions List of types that must not be added even if proposed by the
     *     client (i.e EC point format for RSA key exchange)
     * @param exceptions Extensions to be added even if the client did not propose them (i.e cookie
     *     extension)
     */
    public final void setExtensionsBasedOnProposals(
            List<ExtensionMessage> configuredExtensions,
            Set<ExtensionType> clientProposedExtensions,
            Set<ExtensionType> forbiddenExtensions,
            ExtensionType... exceptions) {
        message.setExtensions(new LinkedList<>());
        List<ExtensionType> listedExceptions = Arrays.asList(exceptions);
        configuredExtensions.stream()
                .filter(
                        configuredExtension ->
                                (!forbiddenExtensions.contains(
                                                configuredExtension.getExtensionTypeConstant())
                                        && (clientProposedExtensions.contains(
                                                        configuredExtension
                                                                .getExtensionTypeConstant())
                                                || listedExceptions.contains(
                                                        configuredExtension
                                                                .getExtensionTypeConstant()))))
                .forEach(message::addExtension);
    }

    protected abstract void prepareHandshakeMessageContents();

    protected void prepareExtensions() {
        SilentByteArrayOutputStream stream = new SilentByteArrayOutputStream();
        if (message.getExtensions() != null) {
            for (ExtensionMessage extensionMessage : message.getExtensions()) {
                if (extensionMessage instanceof KeyShareExtensionMessage
                        && message instanceof ServerHelloMessage) {
                    ServerHelloMessage serverHello = (ServerHelloMessage) message;
                    KeyShareExtensionMessage ksExt = (KeyShareExtensionMessage) extensionMessage;
                    if (serverHello.setRetryRequestModeInKeyShare()) {
                        ksExt.setRetryRequestMode(true);
                    }
                }
                extensionMessage.getPreparator(chooser.getContext()).prepare();
                stream.write(extensionMessage.getExtensionBytes().getValue());
            }
        }
        message.setExtensionBytes(stream.toByteArray());
        LOGGER.debug("ExtensionBytes: {}", message.getExtensionBytes().getValue());
    }

    protected void afterPrepareExtensions() {
        SilentByteArrayOutputStream stream = new SilentByteArrayOutputStream();
        if (message.getExtensions() != null) {
            for (ExtensionMessage extensionMessage : message.getExtensions()) {
                Preparator preparator = extensionMessage.getPreparator(chooser.getContext());
                if (extensionMessage instanceof PreSharedKeyExtensionMessage
                        && message instanceof ClientHelloMessage
                        && chooser.getConnectionEndType() == ConnectionEndType.CLIENT) {
                    ((PreSharedKeyExtensionPreparator) preparator)
                            .setClientHello((ClientHelloMessage) message);
                    preparator.afterPrepare();
                } else if (extensionMessage instanceof EncryptedServerNameIndicationExtensionMessage
                        && message instanceof ClientHelloMessage
                        && chooser.getConnectionEndType() == ConnectionEndType.CLIENT) {
                    ClientHelloMessage clientHelloMessage = (ClientHelloMessage) message;
                    ((EncryptedServerNameIndicationExtensionPreparator) preparator)
                            .setClientHelloMessage(clientHelloMessage);
                    preparator.afterPrepare();
                }
                if (extensionMessage.getExtensionBytes() != null
                        && extensionMessage.getExtensionBytes().getValue() != null) {
                    stream.write(extensionMessage.getExtensionBytes().getValue());
                } else {
                    LOGGER.debug(
                            "If we are in a SSLv2 or SSLv3 Connection we do not add extensions, as SSL did not contain extensions");
                    LOGGER.debug("If however, the extensions are prepared, we will add them");
                }
            }
        }
        message.setExtensionBytes(stream.toByteArray());
        prepareEncapsulatingFields();
        LOGGER.debug("ExtensionBytes: {}", message.getExtensionBytes().getValue());
    }

    protected void prepareExtensionLength() {
        message.setExtensionsLength(message.getExtensionBytes().getValue().length);
        LOGGER.debug("ExtensionLength: {}", message.getExtensionsLength().getValue());
    }
}
