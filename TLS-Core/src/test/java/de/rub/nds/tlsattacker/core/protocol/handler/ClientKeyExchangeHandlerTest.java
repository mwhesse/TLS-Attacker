/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.handler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.protocol.exception.CryptoException;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.DHClientKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.record.cipher.cryptohelper.KeyDerivator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

public class ClientKeyExchangeHandlerTest
        extends AbstractProtocolMessageHandlerTest<
                DHClientKeyExchangeMessage,
                DHClientKeyExchangeHandler<DHClientKeyExchangeMessage>> {

    public ClientKeyExchangeHandlerTest() {
        super(DHClientKeyExchangeMessage::new, DHClientKeyExchangeHandler::new);
    }

    @Test
    @Override
    public void testadjustContext() {}

    /**
     * From RFC 6101: 6.1. Asymmetric Cryptographic Computations The asymmetric algorithms are used
     * in the handshake protocol to authenticate parties and to generate shared keys and secrets.
     * For Diffie-Hellman, RSA, and FORTEZZA, the same algorithm is used to convert the
     * pre_master_secret into the master_secret. The pre_master_secret should be deleted from memory
     * once the master_secret has been computed. master_secret = MD5(pre_master_secret + SHA('A' +
     * pre_master_secret + ClientHello.random + ServerHello.random)) + MD5(pre_master_secret +
     * SHA('BB' + pre_master_secret + ClientHello.random + ServerHello.random)) +
     * MD5(pre_master_secret + SHA('CCC' + pre_master_secret + ClientHello.random +
     * ServerHello.random)); ..... It is hard to read how the Constants have to be implemented. We
     * will use the ASCII values.
     */
    @Test
    public void testMasterSecretCalculationSSL3() throws NoSuchAlgorithmException, CryptoException {
        byte[] preMasterSecret = DataConverter.hexStringToByteArray(StringUtils.repeat("01", 48));
        byte[] serverRdm = DataConverter.hexStringToByteArray(StringUtils.repeat("02", 32));
        byte[] clientRdm = DataConverter.hexStringToByteArray(StringUtils.repeat("03", 32));

        DHClientKeyExchangeMessage message = new DHClientKeyExchangeMessage();
        message.prepareComputations();
        message.getComputations().setPremasterSecret(preMasterSecret);
        message.getComputations()
                .setClientServerRandom(DataConverter.concatenate(clientRdm, serverRdm));
        tlsContext.setServerRandom(serverRdm);
        tlsContext.setSelectedCipherSuite(CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA);
        tlsContext.setSelectedProtocolVersion(ProtocolVersion.SSL3);
        tlsContext.setPreMasterSecret(preMasterSecret);

        final MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");
        final MessageDigest sha = java.security.MessageDigest.getInstance("SHA-1");
        final byte[] shaDigest1 =
                sha.digest(
                        DataConverter.concatenate(
                                DataConverter.hexStringToByteArray("41"),
                                preMasterSecret,
                                clientRdm,
                                serverRdm));
        final byte[] shaDigest2 =
                sha.digest(
                        DataConverter.concatenate(
                                DataConverter.hexStringToByteArray("4242"),
                                preMasterSecret,
                                clientRdm,
                                serverRdm));
        final byte[] shaDigest3 =
                sha.digest(
                        DataConverter.concatenate(
                                DataConverter.hexStringToByteArray("434343"),
                                preMasterSecret,
                                clientRdm,
                                serverRdm));
        final byte[] md5Digest1 =
                md5.digest(DataConverter.concatenate(preMasterSecret, shaDigest1));
        final byte[] md5Digest2 =
                md5.digest(DataConverter.concatenate(preMasterSecret, shaDigest2));
        final byte[] md5Digest3 =
                md5.digest(DataConverter.concatenate(preMasterSecret, shaDigest3));
        byte[] expectedMasterSecret = DataConverter.concatenate(md5Digest1, md5Digest2, md5Digest3);

        byte[] calculatedMasterSecret =
                KeyDerivator.calculateMasterSecret(
                        tlsContext, message.getComputations().getClientServerRandom().getValue());

        assertArrayEquals(expectedMasterSecret, calculatedMasterSecret);
    }
}
