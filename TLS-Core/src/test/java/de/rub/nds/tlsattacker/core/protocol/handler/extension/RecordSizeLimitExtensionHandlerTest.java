/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.handler.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.connection.InboundConnection;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.exceptions.AdjustmentException;
import de.rub.nds.tlsattacker.core.protocol.message.extension.RecordSizeLimitExtensionMessage;
import de.rub.nds.tlsattacker.core.state.Context;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.transport.ConnectionEndType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class RecordSizeLimitExtensionHandlerTest
        extends AbstractExtensionMessageHandlerTest<
                RecordSizeLimitExtensionMessage, RecordSizeLimitExtensionHandler> {

    RecordSizeLimitExtensionHandlerTest() {
        super(
                RecordSizeLimitExtensionMessage::new,
                RecordSizeLimitExtensionHandler::new,
                () -> {
                    Config config = new Config();
                    config.setDefaultRunningMode(RunningModeType.SERVER);
                    return new Context(new State(config), new InboundConnection()).getTlsContext();
                });
    }

    /** Test of adjustContext method, of class RecordSizeLimitExtensionHandler. */
    @Test
    @Override
    public void testadjustTLSExtensionContext() {
        tlsContext.setTalkingConnectionEndType(ConnectionEndType.CLIENT);

        RecordSizeLimitExtensionMessage msg = new RecordSizeLimitExtensionMessage();
        msg.setRecordSizeLimit(new byte[] {(byte) 0x05, (byte) 0x39});
        assertNull(tlsContext.getOutboundRecordSizeLimit());
        handler.adjustTLSExtensionContext(msg);
        assertEquals(1337, (int) tlsContext.getOutboundRecordSizeLimit());
    }

    @Test
    public void testadjustTLSExtensionContextInvalidSize() {
        RecordSizeLimitExtensionMessage msg = new RecordSizeLimitExtensionMessage();
        msg.setRecordSizeLimit(new byte[] {(byte) 0x05, (byte) 0x39, (byte) 0x00});
        assertNull(tlsContext.getOutboundRecordSizeLimit());
        assertThrows(AdjustmentException.class, () -> handler.adjustTLSExtensionContext(msg));
    }

    @Test
    @Disabled("To be fixed")
    public void testadjustTLSExtensionContextSizeTooSmall() {
        RecordSizeLimitExtensionMessage msg = new RecordSizeLimitExtensionMessage();
        msg.setRecordSizeLimit(new byte[] {(byte) 0x00, (byte) 0x2A});
        assertNull(tlsContext.getOutboundRecordSizeLimit());
        handler.adjustContext(msg);
        assertNull(tlsContext.getOutboundRecordSizeLimit());
    }
}
