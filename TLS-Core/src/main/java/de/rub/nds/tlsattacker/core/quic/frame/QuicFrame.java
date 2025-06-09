/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.quic.frame;

import de.rub.nds.modifiablevariable.ModifiableVariableFactory;
import de.rub.nds.modifiablevariable.ModifiableVariableHolder;
import de.rub.nds.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.modifiablevariable.singlebyte.ModifiableByte;
import de.rub.nds.tlsattacker.core.layer.data.DataContainer;
import de.rub.nds.tlsattacker.core.quic.constants.QuicFrameType;
import de.rub.nds.tlsattacker.core.quic.handler.frame.QuicFrameHandler;
import de.rub.nds.tlsattacker.core.quic.parser.frame.QuicFrameParser;
import de.rub.nds.tlsattacker.core.quic.preparator.frame.QuicFramePreparator;
import de.rub.nds.tlsattacker.core.quic.serializer.frame.QuicFrameSerializer;
import de.rub.nds.tlsattacker.core.state.Context;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;

@XmlRootElement
public abstract class QuicFrame extends ModifiableVariableHolder implements DataContainer {

    protected boolean ackEliciting = true;

    @ModifiableVariableProperty private ModifiableByte frameType;

    public QuicFrame() {}

    public QuicFrame(QuicFrameType quicFrameType) {
        setFrameType(quicFrameType.getValue());
    }

    public void setFrameType(QuicFrameType frameType) {
        this.frameType =
                ModifiableVariableFactory.safelySetValue(this.frameType, frameType.getValue());
    }

    public void setFrameType(ModifiableByte frameType) {
        this.frameType = frameType;
    }

    public void setFrameType(byte frameType) {
        this.frameType = ModifiableVariableFactory.safelySetValue(this.frameType, frameType);
    }

    public ModifiableByte getFrameType() {
        return this.frameType;
    }

    public boolean isAckEliciting() {
        return ackEliciting;
    }

    @Override
    public String toCompactString() {
        return QuicFrameType.getFrameType(frameType.getValue()).getName();
    }

    @Override
    public abstract QuicFrameHandler getHandler(Context context);

    @Override
    public abstract QuicFrameSerializer getSerializer(Context context);

    @Override
    public abstract QuicFramePreparator getPreparator(Context context);

    @Override
    public abstract QuicFrameParser getParser(Context context, InputStream stream);
}
