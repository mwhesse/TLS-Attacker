/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.message;

import de.rub.nds.modifiablevariable.ModifiableVariableFactory;
import de.rub.nds.modifiablevariable.ModifiableVariableHolder;
import de.rub.nds.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.modifiablevariable.integer.ModifiableInteger;
import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.tlsattacker.core.protocol.handler.PWDClientKeyExchangeHandler;
import de.rub.nds.tlsattacker.core.protocol.message.computations.PWDComputations;
import de.rub.nds.tlsattacker.core.protocol.parser.PWDClientKeyExchangeParser;
import de.rub.nds.tlsattacker.core.protocol.preparator.PWDClientKeyExchangePreparator;
import de.rub.nds.tlsattacker.core.protocol.serializer.PWDClientKeyExchangeSerializer;
import de.rub.nds.tlsattacker.core.state.Context;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.util.List;

@XmlRootElement(name = "PWDClientKeyExchange")
public class PWDClientKeyExchangeMessage extends ClientKeyExchangeMessage {

    @ModifiableVariableProperty(purpose = ModifiableVariableProperty.Purpose.LENGTH)
    private ModifiableInteger elementLength;

    @ModifiableVariableProperty private ModifiableByteArray element;

    @ModifiableVariableProperty(purpose = ModifiableVariableProperty.Purpose.LENGTH)
    private ModifiableInteger scalarLength;

    @ModifiableVariableProperty private ModifiableByteArray scalar;

    protected PWDComputations computations;

    public PWDClientKeyExchangeMessage() {
        super();
    }

    @Override
    public PWDComputations getComputations() {
        return computations;
    }

    @Override
    public void prepareComputations() {
        if (getComputations() == null) {
            computations = new PWDComputations();
        }
    }

    @Override
    public PWDClientKeyExchangeHandler getHandler(Context context) {
        return new PWDClientKeyExchangeHandler(context.getTlsContext());
    }

    @Override
    public PWDClientKeyExchangeParser getParser(Context context, InputStream stream) {
        return new PWDClientKeyExchangeParser(stream, context.getTlsContext());
    }

    @Override
    public PWDClientKeyExchangePreparator getPreparator(Context context) {
        return new PWDClientKeyExchangePreparator(context.getChooser(), this);
    }

    @Override
    public PWDClientKeyExchangeSerializer getSerializer(Context context) {
        return new PWDClientKeyExchangeSerializer(this);
    }

    public ModifiableInteger getElementLength() {
        return elementLength;
    }

    public void setElementLength(ModifiableInteger elementLength) {
        this.elementLength = elementLength;
    }

    public void setElementLength(int elementLength) {
        this.elementLength =
                ModifiableVariableFactory.safelySetValue(this.elementLength, elementLength);
    }

    public ModifiableByteArray getElement() {
        return element;
    }

    public void setElement(ModifiableByteArray element) {
        this.element = element;
    }

    public void setElement(byte[] element) {
        this.element = ModifiableVariableFactory.safelySetValue(this.element, element);
    }

    public ModifiableInteger getScalarLength() {
        return scalarLength;
    }

    public void setScalarLength(ModifiableInteger scalarLength) {
        this.scalarLength = scalarLength;
    }

    public void setScalarLength(int scalarLength) {
        this.scalarLength =
                ModifiableVariableFactory.safelySetValue(this.scalarLength, scalarLength);
    }

    public ModifiableByteArray getScalar() {
        return scalar;
    }

    public void setScalar(ModifiableByteArray scalar) {
        this.scalar = scalar;
    }

    public void setScalar(byte[] scalar) {
        this.scalar = ModifiableVariableFactory.safelySetValue(this.scalar, scalar);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PWDClientKeyExchangeMessage:");
        sb.append("\n  Element: ");
        if (getElement() != null && getElement().getValue() != null) {
            sb.append(DataConverter.bytesToHexString(getElement().getValue()));
        } else {
            sb.append("null");
        }
        sb.append("\n  Scalar: ");
        if (getScalar() != null && getScalar().getValue() != null) {
            sb.append(DataConverter.bytesToHexString(getScalar().getValue()));
        } else {
            sb.append("null");
        }

        return sb.toString();
    }

    @Override
    public String toCompactString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PWD_CLIENT_KEY_EXCHANGE");
        if (isRetransmission()) {
            sb.append(" (ret.)");
        }
        return sb.toString();
    }

    @Override
    public List<ModifiableVariableHolder> getAllModifiableVariableHolders() {
        List<ModifiableVariableHolder> allModifiableVariableHolders =
                super.getAllModifiableVariableHolders();
        if (computations != null) {
            allModifiableVariableHolders.add(computations);
        }
        return allModifiableVariableHolders;
    }

    @Override
    public String toShortString() {
        return "PWD_CKE";
    }
}
