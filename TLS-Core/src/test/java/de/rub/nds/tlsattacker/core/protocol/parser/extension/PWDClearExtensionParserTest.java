/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.parser.extension;

import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.protocol.message.extension.PWDClearExtensionMessage;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;

public class PWDClearExtensionParserTest
        extends AbstractExtensionParserTest<PWDClearExtensionMessage, PWDClearExtensionParser> {

    public PWDClearExtensionParserTest() {
        super(
                PWDClearExtensionMessage.class,
                PWDClearExtensionParser::new,
                List.of(
                        Named.of(
                                "PWDClearExtensionMessage::getUsernameLength",
                                PWDClearExtensionMessage::getUsernameLength),
                        Named.of(
                                "PWDClearExtensionMessage::getUsername",
                                PWDClearExtensionMessage::getUsername)));
    }

    public static Stream<Arguments> provideTestVectors() {
        return Stream.of(
                Arguments.of(
                        DataConverter.hexStringToByteArray("001e00050466726564"),
                        List.of(),
                        ExtensionType.PWD_CLEAR,
                        5,
                        List.of(4, "fred")));
    }
}
