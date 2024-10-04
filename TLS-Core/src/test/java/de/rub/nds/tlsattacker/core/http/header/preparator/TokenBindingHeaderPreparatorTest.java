/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.http.header.preparator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.TokenBindingKeyParameters;
import de.rub.nds.tlsattacker.core.http.header.TokenBindingHeader;
import de.rub.nds.tlsattacker.core.layer.constant.StackConfiguration;
import de.rub.nds.tlsattacker.core.layer.context.HttpContext;
import de.rub.nds.tlsattacker.core.state.Context;
import de.rub.nds.tlsattacker.core.state.State;
import java.math.BigInteger;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TokenBindingHeaderPreparatorTest {

    private TokenBindingHeader header;
    private TokenBindingHeaderPreparator preparator;

    @BeforeEach
    public void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        Config config = new Config();
        config.setDefaultLayerConfiguration(StackConfiguration.HTTPS);
        Context outerContext = new State(config).getContext();
        HttpContext context = outerContext.getHttpContext();

        List<TokenBindingKeyParameters> keyParameters = new ArrayList<>();
        keyParameters.add(TokenBindingKeyParameters.ECDSAP256);
        context.getConfig().setDefaultTokenBindingKeyParameters(keyParameters);
        context.getConfig().setDefaultTokenBindingEcPrivateKey(BigInteger.valueOf(3));
        context.getConfig().setDefaultEcdsaNonce(BigInteger.ONE);
        header = new TokenBindingHeader();
        preparator = new TokenBindingHeaderPreparator(context, header);
    }

    /** Test of prepare method, of class TokenBindingHeaderPreparator. */
    @Test
    @Disabled
    public void testPrepare() {
        preparator.prepare();

        assertEquals(header.getHeaderName().getValue(), "Sec-Token-Binding");
        assertEquals(
                "AIkAAgBBQF7L5NGmMwpEyPfvlR1L8WXmxrch762phftBZhvG5_1shzRkDEmY_343SwbOGmSi7NgqsDY4T7g9mnmxJ6J9UDIAQBiMGdH7awDozrs8wPI2pfRAqtPX2vx3LTNCmY-9ngpdWHu9GFxflmo9jN0yKR1IxnVNtU-85XOUEwjlYaPYUJMAAA",
                header.getHeaderValue().getValue());
    }
}
