/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.constants;

public enum KeyExchangeAlgorithm {
    NULL,
    DHE_DSS,
    DHE_RSA,
    DHE_PSK,
    DH_ANON,
    RSA,
    RSA_EXPORT,
    RSA_PSK,
    DH_DSS,
    DH_RSA,
    KRB5,
    SRP_SHA_DSS,
    SRP_SHA_RSA,
    SRP_SHA,
    PSK,
    ECDH_RSA,
    ECDH_ANON,
    ECDH_ECDSA,
    ECDHE_ECDSA,
    ECDHE_RSA,
    ECDHE_PSK,
    VKO_GOST94,
    VKO_GOST01,
    VKO_GOST12,
    FORTEZZA_KEA,
    ECMQV_ECDSA,
    ECMQV_ECNRA,
    ECDH_ECNRA,
    CECPQ1_ECDSA,
    ECCPWD,
    CECPQ1_RSA,
    GOSTR341112_256;

    public boolean isKeyExchangeRsa() {
        switch (this) {
            case RSA:
            case RSA_EXPORT:
                return true;
            default:
                return false;
        }
    }

    public boolean isKeyExchangeDh() {
        switch (this) {
            case DHE_DSS:
            case DHE_PSK:
            case DHE_RSA:
            case DH_ANON:
            case DH_DSS:
            case DH_RSA:
                return true;
            default:
                return false;
        }
    }

    public boolean isKeyExchangeStaticDh() {
        switch (this) {
            case DH_DSS:
            case DH_RSA:
                return true;
            default:
                return false;
        }
    }

    public boolean isSrp() {
        switch (this) {
            case SRP_SHA_DSS:
            case SRP_SHA_RSA:
            case SRP_SHA:
                return true;
            default:
                return false;
        }
    }

    public boolean isKeyExchangeDhe() {
        switch (this) {
            case DHE_DSS:
            case DHE_PSK:
            case DHE_RSA:
            case DH_ANON: // This is also ephemeral(!)
                return true;
            default:
                return false;
        }
    }

    public boolean isKeyExchangeEcdhe() {
        switch (this) {
            case ECDHE_ECDSA:
            case ECDHE_PSK:
            case ECDHE_RSA:
            case ECDH_ANON:
                return true;
            default:
                return false;
        }
    }

    public boolean isKeyExchangeEcdh() {
        switch (this) {
            case ECDHE_ECDSA:
            case ECDHE_PSK:
            case ECDHE_RSA:
            case ECDH_ANON:
            case ECDH_ECDSA:
            case ECDH_ECNRA:
            case ECDH_RSA:
                return true;
            default:
                return false;
        }
    }

    public boolean isKeyExchangeStaticEcdh() {
        switch (this) {
            case ECDH_ECDSA:
            case ECDH_ECNRA:
            case ECDH_RSA:
                return true;
            default:
                return false;
        }
    }

    public boolean isKeyExchangeEphemeralEcdh() {
        switch (this) {
            case ECDHE_ECDSA:
            case ECDHE_PSK:
            case ECDHE_RSA:
            case ECDH_ANON: // This is also ephemeral(!)
                return true;
            default:
                return false;
        }
    }

    public boolean isKeyExchangeEphemeral() {
        switch (this) {
            case ECDHE_ECDSA:
            case ECDHE_PSK:
            case ECDHE_RSA:
            case ECDH_ANON: // This is also ephemeral(!)
            case DHE_DSS:
            case DHE_PSK:
            case DHE_RSA:
            case DH_ANON: // This is also ephemeral(!)
                return true;
            default:
                return false;
        }
    }

    public boolean isEC() {
        switch (this) {
            case ECDH_RSA:
            case ECDH_ANON:
            case ECDH_ECDSA:
            case ECDHE_ECDSA:
            case ECDHE_RSA:
            case ECDHE_PSK:
            case ECDH_ECNRA:
            case ECMQV_ECDSA:
            case ECMQV_ECNRA:
            case CECPQ1_ECDSA:
                return true;
            default:
                return false;
        }
    }

    public boolean isAnon() {
        switch (this) {
            case DH_ANON:
            case ECDH_ANON:
                return true;
            default:
                return false;
        }
    }

    public boolean isPsk() {
        switch (this) {
            case PSK:
            case RSA_PSK:
            case DHE_PSK:
            case ECDHE_PSK:
                return true;
            default:
                return false;
        }
    }

    public boolean isExport() {
        return this.name().contains("EXPORT");
    }

    public boolean requiresCertificate() {
        switch (this) {
            case RSA:
            case RSA_EXPORT:
            case DHE_DSS:
            case DHE_RSA:
            case DH_DSS:
            case DH_RSA:
            case ECDHE_ECDSA:
            case ECDHE_RSA:
            case ECDH_RSA:
            case ECDH_ECDSA:
            case ECDH_ECNRA:
            case ECMQV_ECDSA:
            case ECMQV_ECNRA:
            case CECPQ1_ECDSA:
            case VKO_GOST01:
            case VKO_GOST12:
            case SRP_SHA_DSS:
            case SRP_SHA_RSA:
                return true;
            case FORTEZZA_KEA: // I dont know if this is correct actually
            case KRB5:
            case PSK:
            case RSA_PSK:
            case ECDHE_PSK:
            case DHE_PSK:
            case ECCPWD:
            case SRP_SHA:
            case DH_ANON:
            case ECDH_ANON:
            case NULL:
                return false;
            default:
                throw new UnsupportedOperationException(
                        this.name()
                                + " not defined yet! Please ask the developers to add this KEX algorithm");
        }
    }

    public boolean isDss() {
        switch (this) {
            case DHE_DSS:
            case DH_DSS:
            case SRP_SHA_DSS:
                return true;
            default:
                return false;
        }
    }

    public boolean isGost() {
        switch (this) {
            case VKO_GOST94:
            case VKO_GOST01:
            case VKO_GOST12:
            case GOSTR341112_256:
                return true;
            default:
                return false;
        }
    }

    public boolean isEcdsa() {
        switch (this) {
            case ECDHE_ECDSA:
            case ECDH_ECDSA:
            case ECMQV_ECDSA:
            case CECPQ1_ECDSA:
                return true;
            default:
                return false;
        }
    }
}
