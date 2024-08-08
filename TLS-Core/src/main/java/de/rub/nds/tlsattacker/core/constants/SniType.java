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

/** Name Type for Server Name Indication */
public enum SniType {
    HOST_NAME((byte) 0);

    private byte value;

    private static final Map<Byte, SniType> MAP;

    private SniType(byte value) {
        this.value = value;
    }

    static {
        MAP = new HashMap<>();
        for (SniType cm : SniType.values()) {
            MAP.put(cm.value, cm);
        }
    }

    public static SniType getNameType(byte value) {
        return MAP.get(value);
    }

    public byte getValue() {
        return value;
    }

    public byte[] getArrayValue() {
        return new byte[] {value};
    }
}
