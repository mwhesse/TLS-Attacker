/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.constants;

import java.util.HashMap;
import java.util.Map;

/** RFC 4681 */
public enum UserMappingExtensionHintType {
    UPN_DOMAIN_HINT((byte) 0x40);

    private final byte value;
    private static final Map<Byte, UserMappingExtensionHintType> MAP;

    UserMappingExtensionHintType(byte value) {
        this.value = value;
    }

    static {
        MAP = new HashMap<>();
        for (UserMappingExtensionHintType c : values()) {
            MAP.put(c.value, c);
        }
    }

    public static UserMappingExtensionHintType getExtensionType(byte value) {
        return MAP.get(value);
    }

    public byte getValue() {
        return value;
    }
}
