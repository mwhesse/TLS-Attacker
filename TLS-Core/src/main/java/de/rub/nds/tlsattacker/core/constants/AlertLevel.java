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

/** Alert level */
public enum AlertLevel {
    UNDEFINED((byte) 0),
    WARNING((byte) 1),
    FATAL((byte) 2);

    private byte value;

    private static final Map<Byte, AlertLevel> MAP;

    AlertLevel(byte value) {
        this.value = value;
    }

    static {
        MAP = new HashMap<>();
        for (AlertLevel cm : values()) {
            MAP.put(cm.value, cm);
        }
    }

    public static AlertLevel getAlertLevel(byte value) {
        AlertLevel level = MAP.get(value);
        if (level == null) {
            level = UNDEFINED;
        }
        return level;
    }

    public byte getValue() {
        return value;
    }

    public byte[] getArrayValue() {
        return new byte[] {value};
    }

    @Override
    public String toString() {
        return "AlertLevel{" + "value=" + this.name() + '}';
    }
}
