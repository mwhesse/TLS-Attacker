/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.parser;

import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.DHEServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.protocol.message.PskDheServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerKeyExchangeMessage;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;

public class PskDheServerKeyExchangeParserTest
        extends AbstractHandshakeMessageParserTest<
                PskDheServerKeyExchangeMessage, PskDheServerKeyExchangeParser> {

    public PskDheServerKeyExchangeParserTest() {
        super(
                PskDheServerKeyExchangeMessage.class,
                PskDheServerKeyExchangeParser::new,
                List.of(
                        Named.of(
                                "PskDheServerKeyExchangeMessage::getIdentityHintLength",
                                PskDheServerKeyExchangeMessage::getIdentityHintLength),
                        Named.of(
                                "PskDheServerKeyExchangeMessage::getIdentityHint",
                                PskDheServerKeyExchangeMessage::getIdentityHint),
                        Named.of(
                                "DHEServerKeyExchangeMessage::getModulusLength",
                                DHEServerKeyExchangeMessage::getModulusLength),
                        Named.of(
                                "DHEServerKeyExchangeMessage::getModulus",
                                DHEServerKeyExchangeMessage::getModulus),
                        Named.of(
                                "DHEServerKeyExchangeMessage::getGeneratorLength",
                                DHEServerKeyExchangeMessage::getGeneratorLength),
                        Named.of(
                                "DHEServerKeyExchangeMessage::getGenerator",
                                DHEServerKeyExchangeMessage::getGenerator),
                        Named.of(
                                "ServerKeyExchangeMessage::getPublicKeyLength",
                                ServerKeyExchangeMessage::getPublicKeyLength),
                        Named.of(
                                "ServerKeyExchangeMessage::getPublicKey",
                                ServerKeyExchangeMessage::getPublicKey),
                        Named.of(
                                "ServerKeyExchangeMessage::getSignatureAndHashAlgorithm",
                                ServerKeyExchangeMessage::getSignatureAndHashAlgorithm),
                        Named.of(
                                "ServerKeyExchangeMessage::getSignatureLength",
                                ServerKeyExchangeMessage::getSignatureLength),
                        Named.of(
                                "ServerKeyExchangeMessage::getSignature",
                                ServerKeyExchangeMessage::getSignature)));
    }

    public static Stream<Arguments> provideTestVectors() {
        return Stream.of(
                Arguments.of(
                        ProtocolVersion.TLS12,
                        DataConverter.hexStringToByteArray(
                                "0c0001880001aa0080b10b8f96a080e01dde92de5eae5d54ec52c99fbcfb06a3c69a6a9dca52d23b616073e28675a23d189838ef1e2ee652c013ecb4aea906112324975c3cd49b83bfaccbdd7d90c4bd7098488e9c219a73724effd6fae5644738faa31a4ff55bccc0a151af5f0dc8b4bd45bf37df365c1a65e68cfda76d4da708df1fb2bc2e4a43710080a4d1cbd5c3fd34126765a442efb99905f8104dd258ac507fd6406cff14266d31266fea1e5c41564b777e690f5504f213160217b4b01b886a5e91547f9e2749f4d7fbd7d3b9a92ee1909d0d2263f80a76a6a24c087a091f531dbf0a0169b6a28ad662a4d18e73afa32d779d5918d08bc8858f4dcef97c2a24855e6eeb22b3b2e5008070dd13c4bca8c96983bdf065ce9517eb44114a4cf4cdbc55b3bfdabde8510faa38142139409378b90e3ceba61167056fc8b0ee088132183e48b986ed468eeaaf435c9dea3a5d9c01b63a3aae176971a2e1142674675dedca8a8e91093cd42246b4fa37893d7e66534a59461935274955e5dc623c9897a6c4a8501f37427e079d"),
                        Arrays.asList(
                                HandshakeMessageType.SERVER_KEY_EXCHANGE.getValue(),
                                392,
                                1,
                                DataConverter.hexStringToByteArray("aa"),
                                128,
                                DataConverter.hexStringToByteArray(
                                        "b10b8f96a080e01dde92de5eae5d54ec52c99fbcfb06a3c69a6a9dca52d23b616073e28675a23d189838ef1e2ee652c013ecb4aea906112324975c3cd49b83bfaccbdd7d90c4bd7098488e9c219a73724effd6fae5644738faa31a4ff55bccc0a151af5f0dc8b4bd45bf37df365c1a65e68cfda76d4da708df1fb2bc2e4a4371"),
                                128,
                                DataConverter.hexStringToByteArray(
                                        "a4d1cbd5c3fd34126765a442efb99905f8104dd258ac507fd6406cff14266d31266fea1e5c41564b777e690f5504f213160217b4b01b886a5e91547f9e2749f4d7fbd7d3b9a92ee1909d0d2263f80a76a6a24c087a091f531dbf0a0169b6a28ad662a4d18e73afa32d779d5918d08bc8858f4dcef97c2a24855e6eeb22b3b2e5"),
                                128,
                                DataConverter.hexStringToByteArray(
                                        "70dd13c4bca8c96983bdf065ce9517eb44114a4cf4cdbc55b3bfdabde8510faa38142139409378b90e3ceba61167056fc8b0ee088132183e48b986ed468eeaaf435c9dea3a5d9c01b63a3aae176971a2e1142674675dedca8a8e91093cd42246b4fa37893d7e66534a59461935274955e5dc623c9897a6c4a8501f37427e079d"),
                                null,
                                null,
                                null)));
    }
}
