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
import de.rub.nds.modifiablevariable.ModifiableVariableFactory;
import de.rub.nds.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.modifiablevariable.integer.ModifiableInteger;
import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.tlsattacker.core.constants.ConnectionIdUsage;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.protocol.handler.NewConnectionIdHandler;
import de.rub.nds.tlsattacker.core.protocol.message.connectionid.ConnectionId;
import de.rub.nds.tlsattacker.core.protocol.parser.NewConnectionIdParser;
import de.rub.nds.tlsattacker.core.protocol.preparator.NewConnectionIdPreparator;
import de.rub.nds.tlsattacker.core.protocol.serializer.NewConnectionIdSerializer;
import de.rub.nds.tlsattacker.core.state.Context;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "NewConnectionId")
public class NewConnectionIdMessage extends HandshakeMessage {

    @ModifiableVariableProperty private ConnectionIdUsage usage;

    @ModifiableVariableProperty(purpose = ModifiableVariableProperty.Purpose.LENGTH)
    private ModifiableInteger connectionIdsLength;

    @HoldsModifiableVariable private List<ConnectionId> connectionIds;

    public NewConnectionIdMessage() {
        super(HandshakeMessageType.NEW_CONNECTION_ID);
    }

    @Override
    public NewConnectionIdParser getParser(Context context, InputStream stream) {
        return new NewConnectionIdParser(stream, context.getTlsContext());
    }

    @Override
    public NewConnectionIdPreparator getPreparator(Context context) {
        return new NewConnectionIdPreparator(context.getChooser(), this);
    }

    @Override
    public NewConnectionIdSerializer getSerializer(Context context) {
        return new NewConnectionIdSerializer(this);
    }

    @Override
    public NewConnectionIdHandler getHandler(Context context) {
        return new NewConnectionIdHandler(context.getTlsContext());
    }

    public ConnectionIdUsage getUsage() {
        return usage;
    }

    public void setUsage(ConnectionIdUsage usage) {
        this.usage = usage;
    }

    public ModifiableInteger getConnectionIdsLength() {
        return connectionIdsLength;
    }

    public void setConnectionIdsLength(Integer connectionIdsLength) {
        this.connectionIdsLength =
                ModifiableVariableFactory.safelySetValue(
                        this.connectionIdsLength, connectionIdsLength);
    }

    public List<ConnectionId> getConnectionIds() {
        return connectionIds;
    }

    public void setConnectionIdsLength(ModifiableInteger connectionIdsLength) {
        this.connectionIdsLength = connectionIdsLength;
    }

    public void setConnectionIds(List<ConnectionId> connectionIds) {
        this.connectionIds = connectionIds;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.usage);
        hash = 79 * hash + Objects.hashCode(this.connectionIdsLength);
        hash = 79 * hash + Objects.hashCode(this.connectionIds);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NewConnectionIdMessage other = (NewConnectionIdMessage) obj;
        if (this.usage != other.usage) {
            return false;
        }
        if (!Objects.equals(this.connectionIdsLength, other.connectionIdsLength)) {
            return false;
        }
        return Objects.equals(this.connectionIds, other.connectionIds);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NewConnectionId:");
        sb.append("\n  Usage: ");
        if (usage != null) {
            sb.append(usage);
        } else {
            sb.append("null");
        }
        sb.append("\n  ConnectionIds: ");
        if (connectionIds != null && !connectionIds.isEmpty()) {
            sb.append(
                    DataConverter.bytesToHexString(
                            connectionIds.get(0).getConnectionId().getValue()));
            for (int i = 1; i < connectionIds.size(); i++) {
                sb.append(", ");
                sb.append(
                        DataConverter.bytesToHexString(
                                connectionIds.get(i).getConnectionId().getValue()));
            }
        } else {
            sb.append("null");
        }
        return sb.toString();
    }

    @Override
    public String toShortString() {
        return "NCID";
    }
}
