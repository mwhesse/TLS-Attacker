/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.message.computations;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.protocol.constants.NamedEllipticCurveParameters;
import de.rub.nds.protocol.crypto.ec.EllipticCurve;
import de.rub.nds.protocol.crypto.ec.Point;
import de.rub.nds.protocol.exception.CryptoException;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.connection.InboundConnection;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.layer.context.TlsContext;
import de.rub.nds.tlsattacker.core.state.Context;
import de.rub.nds.tlsattacker.core.state.State;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

public class PWDComputationsTest {

    @Test
    public void testComputePasswordElement() throws CryptoException {
        TlsContext context =
                new Context(new State(new Config()), new InboundConnection()).getTlsContext();
        context.setSelectedCipherSuite(CipherSuite.TLS_ECCPWD_WITH_AES_128_GCM_SHA256);
        context.setSelectedProtocolVersion(ProtocolVersion.TLS12);
        context.setClientRandom(
                DataConverter.hexStringToByteArray(
                        "528fbf52175de2c869845fdbfa8344f7d732712ebfa679d8643cd31a880e043d"));
        context.setServerRandom(
                DataConverter.hexStringToByteArray(
                        "528fbf524378a1b13b8d2cbd247090721369f8bfa3ceeb3cfcd85cbfcdd58eaa"));
        context.setClientPWDUsername("fred");
        context.getConfig().setDefaultPWDPassword("barney");
        EllipticCurve curve =
                ((NamedEllipticCurveParameters) NamedGroup.BRAINPOOLP256R1.getGroupParameters())
                        .getGroup();
        Point passwordElement = PWDComputations.computePasswordElement(context.getChooser(), curve);
        BigInteger expectedX =
                new BigInteger(
                        "686B0D3FC49894DD621EC04F925E029B2B1528EDEDCA46007254281E9A6EDC", 16);
        assertArrayEquals(
                DataConverter.bigIntegerToByteArray(expectedX),
                DataConverter.bigIntegerToByteArray(passwordElement.getFieldX().getData()));

        context.setSelectedProtocolVersion(ProtocolVersion.TLS13);
        passwordElement = PWDComputations.computePasswordElement(context.getChooser(), curve);
        expectedX =
                new BigInteger(
                        "0BA387CE8123BEA05A4327520F5A2A66B038F2024F239F330038DA0A2744F79B", 16);
        assertArrayEquals(
                DataConverter.bigIntegerToByteArray(expectedX),
                DataConverter.bigIntegerToByteArray(passwordElement.getFieldX().getData()));
    }
}
