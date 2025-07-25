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

/** <a href="https://tools.ietf.org/html/rfc5077#section-4">RFC 5077 Section 4</a> */
public enum ClientAuthenticationType {
    ANONYMOUS((byte) 0x00),
    CERTIFICATE_BASED((byte) 0x01),
    PSK((byte) 0x02);

    private byte value;

    private static final Map<Byte, ClientAuthenticationType> MAP;

    ClientAuthenticationType(byte value) {
        this.value = value;
    }

    static {
        MAP = new HashMap<>();
        for (ClientAuthenticationType c : values()) {
            MAP.put(c.value, c);
        }
    }

    public static ClientAuthenticationType getClientAuthenticationType(byte value) {
        return MAP.get(value);
    }

    public byte getValue() {
        return value;
    }

    public byte[] getArrayValue() {
        return new byte[] {value};
    }
}
