/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.quic.serializer.frame;

import de.rub.nds.tlsattacker.core.quic.VariableLengthIntegerEncoding;
import de.rub.nds.tlsattacker.core.quic.frame.NewTokenFrame;

public class NewTokenFrameSerializer extends QuicFrameSerializer<NewTokenFrame> {

    public NewTokenFrameSerializer(NewTokenFrame frame) {
        super(frame);
    }

    @Override
    protected byte[] serializeBytes() {
        writeFrameType();
        writeTokenLength();
        writeToken();
        return getAlreadySerialized();
    }

    private void writeTokenLength() {
        appendBytes(
                VariableLengthIntegerEncoding.encodeVariableLengthInteger(
                        frame.getTokenLength().getValue()));
    }

    private void writeToken() {
        appendBytes(frame.getToken().getValue());
    }
}
