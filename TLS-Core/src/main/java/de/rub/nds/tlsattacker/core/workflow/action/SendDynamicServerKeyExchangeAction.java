/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.workflow.action;

import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.layer.LayerConfiguration;
import de.rub.nds.tlsattacker.core.layer.SpecificSendLayerConfiguration;
import de.rub.nds.tlsattacker.core.layer.constant.ImplementedLayers;
import de.rub.nds.tlsattacker.core.layer.context.TlsContext;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.container.ActionHelperUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "SendDynamicServerKeyExchange")
public class SendDynamicServerKeyExchangeAction extends CommonSendAction {

    public SendDynamicServerKeyExchangeAction() {
        super();
    }

    public SendDynamicServerKeyExchangeAction(String connectionAlias) {
        super(connectionAlias);
    }

    @Override
    public String toString() {
        StringBuilder sb;
        if (isExecuted()) {
            sb = new StringBuilder("Send Action:\n");
        } else {
            sb = new StringBuilder("Send Action: (not executed)\n");
        }
        sb.append("\tMessages:");
        if (getSentMessages() != null) {
            for (ProtocolMessage message : getSentMessages()) {
                sb.append(message.toCompactString());
                sb.append(", ");
            }
            sb.append("\n");
        } else {
            sb.append("null (no messages set)");
        }
        return sb.toString();
    }

    @Override
    public String toCompactString() {
        StringBuilder sb = new StringBuilder(super.toCompactString());
        if ((getSentMessages() != null) && (!getSentMessages().isEmpty())) {
            sb.append(" (");
            for (ProtocolMessage message : getSentMessages()) {
                sb.append(message.toCompactString());
                sb.append(",");
            }
            sb.deleteCharAt(sb.lastIndexOf(",")).append(")");
        } else {
            sb.append(" (no messages set)");
        }
        return sb.toString();
    }

    @Override
    protected List<LayerConfiguration<?>> createLayerConfiguration(State state) {
        TlsContext tlsContext = state.getTlsContext(getConnectionAlias());
        CipherSuite selectedCipherSuite = tlsContext.getChooser().getSelectedCipherSuite();
        ServerKeyExchangeMessage serverKeyExchangeMessage =
                new WorkflowConfigurationFactory(tlsContext.getConfig())
                        .createServerKeyExchangeMessage(
                                selectedCipherSuite.getKeyExchangeAlgorithm());
        if (serverKeyExchangeMessage != null) {
            List<LayerConfiguration<?>> configurationList = new LinkedList<>();
            configurationList.add(
                    new SpecificSendLayerConfiguration<>(
                            ImplementedLayers.MESSAGE, serverKeyExchangeMessage));
            return ActionHelperUtil.sortAndAddOptions(
                    tlsContext.getLayerStack(), true, getActionOptions(), configurationList);

        } else {
            return null;
        }
    }
}
