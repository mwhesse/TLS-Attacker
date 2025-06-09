/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.message.extension;

import de.rub.nds.modifiablevariable.HoldsModifiableVariable;
import de.rub.nds.modifiablevariable.ModifiableVariableFactory;
import de.rub.nds.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.modifiablevariable.integer.ModifiableInteger;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.protocol.handler.extension.AlpnExtensionHandler;
import de.rub.nds.tlsattacker.core.protocol.message.extension.alpn.AlpnEntry;
import de.rub.nds.tlsattacker.core.protocol.parser.extension.AlpnExtensionParser;
import de.rub.nds.tlsattacker.core.protocol.preparator.extension.AlpnExtensionPreparator;
import de.rub.nds.tlsattacker.core.protocol.serializer.extension.AlpnExtensionSerializer;
import de.rub.nds.tlsattacker.core.state.Context;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.util.List;

/** This extension is defined in RFC7301 */
@XmlRootElement(name = "AlpnExtension")
public class AlpnExtensionMessage extends ExtensionMessage {

    @ModifiableVariableProperty private ModifiableInteger proposedAlpnProtocolsLength;
    @ModifiableVariableProperty private ModifiableByteArray proposedAlpnProtocols;

    @HoldsModifiableVariable private List<AlpnEntry> alpnEntryList;

    public AlpnExtensionMessage() {
        super(ExtensionType.ALPN);
    }

    public List<AlpnEntry> getAlpnEntryList() {
        return alpnEntryList;
    }

    public void setAlpnEntryList(List<AlpnEntry> alpnEntryList) {
        this.alpnEntryList = alpnEntryList;
    }

    public ModifiableInteger getProposedAlpnProtocolsLength() {
        return proposedAlpnProtocolsLength;
    }

    public void setProposedAlpnProtocolsLength(ModifiableInteger proposedAlpnProtocolsLength) {
        this.proposedAlpnProtocolsLength = proposedAlpnProtocolsLength;
    }

    public void setProposedAlpnProtocolsLength(int proposedAlpnProtocolsLength) {
        this.proposedAlpnProtocolsLength =
                ModifiableVariableFactory.safelySetValue(
                        this.proposedAlpnProtocolsLength, proposedAlpnProtocolsLength);
    }

    public ModifiableByteArray getProposedAlpnProtocols() {
        return proposedAlpnProtocols;
    }

    public void setProposedAlpnProtocols(ModifiableByteArray proposedAlpnProtocols) {
        this.proposedAlpnProtocols = proposedAlpnProtocols;
    }

    public void setProposedAlpnProtocols(byte[] proposedAlpnProtocols) {
        this.proposedAlpnProtocols =
                ModifiableVariableFactory.safelySetValue(
                        this.proposedAlpnProtocols, proposedAlpnProtocols);
    }

    @Override
    public AlpnExtensionParser getParser(Context context, InputStream stream) {
        return new AlpnExtensionParser(stream, context.getTlsContext());
    }

    @Override
    public AlpnExtensionPreparator getPreparator(Context context) {
        return new AlpnExtensionPreparator(context.getChooser(), this);
    }

    @Override
    public AlpnExtensionSerializer getSerializer(Context context) {
        return new AlpnExtensionSerializer(this);
    }

    @Override
    public AlpnExtensionHandler getHandler(Context context) {
        return new AlpnExtensionHandler(context.getTlsContext());
    }
}
