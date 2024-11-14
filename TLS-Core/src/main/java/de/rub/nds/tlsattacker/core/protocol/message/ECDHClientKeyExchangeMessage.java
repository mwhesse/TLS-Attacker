/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.message;

import de.rub.nds.modifiablevariable.HoldsModifiableVariable;
import de.rub.nds.modifiablevariable.ModifiableVariableHolder;
import de.rub.nds.tlsattacker.core.protocol.handler.ECDHClientKeyExchangeHandler;
import de.rub.nds.tlsattacker.core.protocol.message.computations.ECDHClientComputations;
import de.rub.nds.tlsattacker.core.protocol.parser.ECDHClientKeyExchangeParser;
import de.rub.nds.tlsattacker.core.protocol.preparator.ECDHClientKeyExchangePreparator;
import de.rub.nds.tlsattacker.core.protocol.serializer.ECDHClientKeyExchangeSerializer;
import de.rub.nds.tlsattacker.core.state.Context;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.util.List;

@XmlRootElement(name = "ECDHClientKeyExchange")
public class ECDHClientKeyExchangeMessage extends ClientKeyExchangeMessage {

    @HoldsModifiableVariable protected ECDHClientComputations computations;

    public ECDHClientKeyExchangeMessage() {
        super();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ECDHClientKeyExchangeMessage:");
        return sb.toString();
    }

    @Override
    public ECDHClientComputations getComputations() {
        return computations;
    }

    @Override
    public ECDHClientKeyExchangeHandler<? extends ECDHClientKeyExchangeMessage> getHandler(
            Context context) {
        return new ECDHClientKeyExchangeHandler<>(context.getTlsContext());
    }

    @Override
    public ECDHClientKeyExchangeParser<? extends ECDHClientKeyExchangeMessage> getParser(
            Context context, InputStream stream) {
        return new ECDHClientKeyExchangeParser<>(stream, context.getTlsContext());
    }

    @Override
    public ECDHClientKeyExchangePreparator<? extends ECDHClientKeyExchangeMessage> getPreparator(
            Context context) {
        return new ECDHClientKeyExchangePreparator<>(context.getChooser(), this);
    }

    @Override
    public ECDHClientKeyExchangeSerializer<? extends ECDHClientKeyExchangeMessage> getSerializer(
            Context context) {
        return new ECDHClientKeyExchangeSerializer<>(this);
    }

    @Override
    public String toCompactString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ECDH_CLIENT_KEY_EXCHANGE");
        if (isRetransmission()) {
            sb.append(" (ret.)");
        }
        return sb.toString();
    }

    @Override
    public String toShortString() {
        return "ECDH_CKE";
    }

    @Override
    public void prepareComputations() {
        if (computations == null) {
            computations = new ECDHClientComputations();
        }
    }

    @Override
    public List<ModifiableVariableHolder> getAllModifiableVariableHolders() {
        List<ModifiableVariableHolder> holders = super.getAllModifiableVariableHolders();
        if (computations != null) {
            holders.add(computations);
        }
        return holders;
    }
}
