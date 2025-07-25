/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.preparator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.rub.nds.modifiablevariable.util.BadFixedRandom;
import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.modifiablevariable.util.RandomHelper;
import de.rub.nds.protocol.constants.MacAlgorithm;
import de.rub.nds.protocol.exception.CryptoException;
import de.rub.nds.tlsattacker.core.constants.CipherAlgorithm;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.CompressionMethod;
import de.rub.nds.tlsattacker.core.constants.HandshakeByteLength;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.NewSessionTicketMessage;
import de.rub.nds.tlsattacker.core.util.StaticTicketCrypto;
import de.rub.nds.tlsattacker.util.FixedTimeProvider;
import de.rub.nds.tlsattacker.util.TimeHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class NewSessionTicketPreparatorTest
        extends AbstractProtocolMessagePreparatorTest<
                NewSessionTicketMessage, NewSessionTicketPreparator> {

    public NewSessionTicketPreparatorTest() {
        super(NewSessionTicketMessage::new, NewSessionTicketPreparator::new);
    }

    @AfterEach
    public void cleanUp() {
        RandomHelper.setRandom(null);
        TimeHelper.setProvider(null);
    }

    /**
     * Test of prepareProtocolMessageContents method, of class NewSessionTicketPreparator.
     *
     * @throws de.rub.nds.protocol.exception.CryptoException
     */
    @Test
    @Override
    public void testPrepare() throws CryptoException {
        tlsContext.setSelectedProtocolVersion(ProtocolVersion.TLS12);
        tlsContext.setSelectedCipherSuite(CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256);
        tlsContext.setSelectedCompressionMethod(CompressionMethod.NULL);
        tlsContext.setMasterSecret(
                DataConverter.hexStringToByteArray(
                        "53657373696f6e5469636b65744d532b53657373696f6e5469636b65744d532b53657373696f6e5469636b65744d532b")); // SessionTicketMS+SessionTicketMS+SessionTicketMS+
        tlsContext.setClientAuthentication(false);
        TimeHelper.setProvider(new FixedTimeProvider(152113433000l)); // 0x09111119
        tlsContext.getConfig().setSessionTicketLifetimeHint(3600); // 3600 = 0xe10

        RandomHelper.setRandom(new BadFixedRandom((byte) 0x55));
        preparator.prepare();

        // Check ticketdata
        // Correct value was calculated by http://aes.online-domain-tools.com/
        assertArrayEquals(
                message.getTicket().getEncryptedState().getValue(),
                DataConverter.hexStringToByteArray(
                        "23403433756E7E6C0777047BECA5B4A1FC987804A39B420BE56DA996D6F9C233CC6C97FC2F5A3EE3A193A2ACE6F320E6AA3E98B66B4A3C51AA4056D7EF5898F8"));

        // Revert encryption to check the correct encryption
        // Correct value was assembled by hand because I found no testdata
        byte[] decrypted =
                StaticTicketCrypto.decrypt(
                        CipherAlgorithm.AES_128_CBC,
                        message.getTicket().getEncryptedState().getValue(),
                        tlsContext.getChooser().getConfig().getSessionTicketEncryptionKey(),
                        message.getTicket().getIV().getValue());
        assertArrayEquals(
                decrypted,
                DataConverter.hexStringToByteArray(
                        "0303009c0053657373696f6e5469636b65744d532b53657373696f6e5469636b65744d532b53657373696f6e5469636b65744d532b0009111119"));

        // Smaller Tests to be complete
        assertEquals(3600, (long) message.getTicketLifetimeHint().getValue());
        assertEquals(130, (int) message.getTicket().getIdentityLength().getValue());
        assertArrayEquals(
                message.getTicket().getIV().getValue(),
                DataConverter.hexStringToByteArray("55555555555555555555555555555555"));
        assertArrayEquals(
                message.getTicket().getKeyName().getValue(),
                DataConverter.hexStringToByteArray("544c532d41747461636b6572204b6579"));

        // Correct value was assembled by hand and calculated by
        // https://www.liavaag.org/English/SHA-Generator/HMAC/
        assertArrayEquals(
                message.getTicket().getMAC().getValue(),
                DataConverter.hexStringToByteArray(
                        "C12AC5FD8690B8E61F647F86630271F16C9A6281663014C2873EE4934A6C9C3B"));

        byte[] macinput =
                DataConverter.concatenate(
                        message.getTicket().getKeyName().getValue(),
                        message.getTicket().getIV().getValue());
        macinput =
                DataConverter.concatenate(
                        macinput,
                        DataConverter.intToBytes(
                                message.getTicket().getEncryptedState().getValue().length,
                                HandshakeByteLength.ENCRYPTED_STATE_LENGTH));
        macinput =
                DataConverter.concatenate(
                        macinput, message.getTicket().getEncryptedState().getValue());
        assertTrue(
                StaticTicketCrypto.verifyHMAC(
                        MacAlgorithm.HMAC_SHA256,
                        message.getTicket().getMAC().getValue(),
                        macinput,
                        tlsContext.getChooser().getConfig().getSessionTicketKeyHMAC()));
    }
}
