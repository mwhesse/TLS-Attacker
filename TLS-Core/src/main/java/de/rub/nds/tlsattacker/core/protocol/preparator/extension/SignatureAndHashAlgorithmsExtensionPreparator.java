/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.preparator.extension;

import de.rub.nds.protocol.util.SilentByteArrayOutputStream;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.protocol.message.extension.SignatureAndHashAlgorithmsExtensionMessage;
import de.rub.nds.tlsattacker.core.workflow.chooser.Chooser;
import de.rub.nds.tlsattacker.transport.ConnectionEndType;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SignatureAndHashAlgorithmsExtensionPreparator
        extends ExtensionPreparator<SignatureAndHashAlgorithmsExtensionMessage> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final SignatureAndHashAlgorithmsExtensionMessage msg;

    public SignatureAndHashAlgorithmsExtensionPreparator(
            Chooser chooser, SignatureAndHashAlgorithmsExtensionMessage message) {
        super(chooser, message);
        this.msg = message;
    }

    @Override
    public void prepareExtensionContent() {
        LOGGER.debug("Preparing SignatureAndHashAlgorithmsExtensionMessage");
        prepareSignatureAndHashAlgorithms(msg);
        prepareSignatureAndHashAlgorithmsLength(msg);
    }

    private void prepareSignatureAndHashAlgorithms(SignatureAndHashAlgorithmsExtensionMessage msg) {
        msg.setSignatureAndHashAlgorithms(createSignatureAndHashAlgorithmsArray());
        LOGGER.debug(
                "SignatureAndHashAlgorithms: {}", msg.getSignatureAndHashAlgorithms().getValue());
    }

    private byte[] createSignatureAndHashAlgorithmsArray() {
        SilentByteArrayOutputStream stream = new SilentByteArrayOutputStream();
        List<SignatureAndHashAlgorithm> signatureAndHashAlgorithmList;
        if (chooser.getContext().getTalkingConnectionEndType() == ConnectionEndType.SERVER) {
            signatureAndHashAlgorithmList =
                    chooser.getConfig().getDefaultServerSupportedSignatureAndHashAlgorithms();
        } else {
            signatureAndHashAlgorithmList =
                    chooser.getConfig().getDefaultClientSupportedSignatureAndHashAlgorithms();
        }

        for (SignatureAndHashAlgorithm algo : signatureAndHashAlgorithmList) {
            stream.write(algo.getByteValue());
        }
        return stream.toByteArray();
    }

    private void prepareSignatureAndHashAlgorithmsLength(
            SignatureAndHashAlgorithmsExtensionMessage msg) {
        msg.setSignatureAndHashAlgorithmsLength(
                msg.getSignatureAndHashAlgorithms().getValue().length);
        LOGGER.debug(
                "SignatureAndHashAlgorithmsLength: "
                        + msg.getSignatureAndHashAlgorithmsLength().getValue());
    }
}
