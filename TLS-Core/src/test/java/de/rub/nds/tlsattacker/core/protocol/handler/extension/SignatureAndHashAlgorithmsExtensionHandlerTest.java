/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.handler.extension;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.protocol.constants.HashAlgorithm;
import de.rub.nds.protocol.constants.SignatureAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.protocol.message.extension.SignatureAndHashAlgorithmsExtensionMessage;
import org.junit.jupiter.api.Test;

public class SignatureAndHashAlgorithmsExtensionHandlerTest
        extends AbstractExtensionMessageHandlerTest<
                SignatureAndHashAlgorithmsExtensionMessage,
                SignatureAndHashAlgorithmsExtensionHandler> {

    public SignatureAndHashAlgorithmsExtensionHandlerTest() {
        super(
                SignatureAndHashAlgorithmsExtensionMessage::new,
                SignatureAndHashAlgorithmsExtensionHandler::new);
    }

    /** Test of adjustContext method, of class SignatureAndHashAlgorithmsExtensionHandler. */
    @Test
    @Override
    public void testadjustTLSExtensionContext() {
        SignatureAndHashAlgorithmsExtensionMessage msg =
                new SignatureAndHashAlgorithmsExtensionMessage();
        byte[] algoBytes =
                DataConverter.concatenate(
                        SignatureAndHashAlgorithm.DSA_SHA1.getByteValue(),
                        SignatureAndHashAlgorithm.RSA_SHA512.getByteValue());
        msg.setSignatureAndHashAlgorithms(algoBytes);
        tlsContext.setServerSupportedSignatureAndHashAlgorithms(
                SignatureAndHashAlgorithm.RSA_SHA512);
        handler.adjustTLSExtensionContext(msg);
        assertEquals(2, tlsContext.getClientSupportedSignatureAndHashAlgorithms().size());
        assertSame(
                HashAlgorithm.SHA1,
                tlsContext
                        .getClientSupportedSignatureAndHashAlgorithms()
                        .get(0)
                        .getHashAlgorithm());
        assertSame(
                SignatureAlgorithm.DSA,
                tlsContext
                        .getClientSupportedSignatureAndHashAlgorithms()
                        .get(0)
                        .getSignatureAlgorithm());
    }
}
