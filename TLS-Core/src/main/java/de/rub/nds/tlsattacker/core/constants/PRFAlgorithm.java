/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.constants;

import de.rub.nds.protocol.constants.MacAlgorithm;

public enum PRFAlgorithm {
    TLS_PRF_LEGACY(null),
    TLS_PRF_SHA256(MacAlgorithm.HMAC_SHA256),
    TLS_PRF_SHA384(MacAlgorithm.HMAC_SHA384),
    TLS_PRF_GOSTR3411(MacAlgorithm.HMAC_GOSTR3411),
    TLS_PRF_GOSTR3411_2012_256(MacAlgorithm.HMAC_GOSTR3411_2012_256);

    PRFAlgorithm(MacAlgorithm macAlgorithm) {
        this.macAlgorithm = macAlgorithm;
    }

    private final MacAlgorithm macAlgorithm;

    public MacAlgorithm getMacAlgorithm() {
        return macAlgorithm;
    }
}
