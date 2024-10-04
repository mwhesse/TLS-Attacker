/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.workflow;

import de.rub.nds.modifiablevariable.HoldsModifiableVariable;
import de.rub.nds.tlsattacker.core.connection.AliasedConnection;
import de.rub.nds.tlsattacker.core.connection.InboundConnection;
import de.rub.nds.tlsattacker.core.connection.OutboundConnection;
import de.rub.nds.tlsattacker.core.exceptions.ConfigurationException;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.core.workflow.action.MessageAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceivingAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendingAction;
import de.rub.nds.tlsattacker.core.workflow.action.StaticReceivingAction;
import de.rub.nds.tlsattacker.core.workflow.action.StaticSendingAction;
import de.rub.nds.tlsattacker.core.workflow.action.TlsAction;
import de.rub.nds.tlsattacker.core.workflow.action.executor.ActionOption;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** A wrapper class over a list of protocol expectedMessages. */
@XmlRootElement(name = "workflowTrace")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowTrace implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Copy a workflow trace.
     *
     * <p>TODO: This should be replaced by a better copy method. Using serialization is slow and
     * needs some additional "tweaks", i.e. we have to manually restore important fields marked as
     * XmlTransient. This problem arises because the classes are configured for nice JAXB output,
     * and not for copying/storing full objects.
     *
     * @param orig the original WorkflowTrace object to copy
     * @return a copy of the original WorkflowTrace
     */
    public static WorkflowTrace copy(WorkflowTrace orig) {
        WorkflowTrace copy = null;

        List<TlsAction> origActions = orig.getTlsActions();

        try {
            String origTraceStr = WorkflowTraceSerializer.write(orig);
            InputStream is =
                    new ByteArrayInputStream(origTraceStr.getBytes(StandardCharsets.UTF_8.name()));
            copy = WorkflowTraceSerializer.insecureRead(is);
        } catch (JAXBException | IOException | XMLStreamException ex) {
            throw new ConfigurationException("Could not copy workflow trace: ", ex);
        }

        List<TlsAction> copiedActions = copy.getTlsActions();
        for (int i = 0; i < origActions.size(); i++) {
            copiedActions
                    .get(i)
                    .setSingleConnectionWorkflow(origActions.get(i).isSingleConnectionWorkflow());
        }

        return copy;
    }

    @XmlElements(
            value = {
                @XmlElement(type = AliasedConnection.class, name = "AliasedConnection"),
                @XmlElement(type = InboundConnection.class, name = "InboundConnection"),
                @XmlElement(type = OutboundConnection.class, name = "OutboundConnection")
            })
    private List<AliasedConnection> connections = new ArrayList<>();

    @HoldsModifiableVariable
    @XmlAnyElement(lax = true)
    private List<TlsAction> tlsActions = new ArrayList<>();

    private String name = null;
    private String description = null;

    // A dirty flag used to determine if the WorkflowTrace is well defined or
    // not.
    @XmlTransient private boolean dirty = true;

    public WorkflowTrace() {
        this.tlsActions = new LinkedList<>();
    }

    public WorkflowTrace(List<AliasedConnection> cons) {
        this.connections = cons;
    }

    public void reset() {
        for (TlsAction action : getTlsActions()) {
            action.reset();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TlsAction> getTlsActions() {
        return tlsActions;
    }

    public void addTlsAction(TlsAction action) {
        dirty = true;
        tlsActions.add(action);
    }

    public void addTlsAction(int position, TlsAction action) {
        dirty = true;
        tlsActions.add(position, action);
    }

    public void addTlsActions(TlsAction... actions) {
        addTlsActions(Arrays.asList(actions));
    }

    public void addTlsActions(List<TlsAction> actions) {
        for (TlsAction action : actions) {
            addTlsAction(action);
        }
    }

    public TlsAction removeTlsAction(int index) {
        dirty = true;
        return tlsActions.remove(index);
    }

    public void setTlsActions(List<TlsAction> tlsActions) {
        dirty = true;
        this.tlsActions = tlsActions;
    }

    public void setTlsActions(TlsAction... tlsActions) {
        setTlsActions(new ArrayList<>(Arrays.asList(tlsActions)));
    }

    public List<AliasedConnection> getConnections() {
        return connections;
    }

    /**
     * Set connections of the workflow trace. Use only if you know what you are doing. Unless you
     * are manually configuring workflow traces (say for MiTM or unit tests), there shouldn't be any
     * need to call this method.
     *
     * @param connections new connection to use with this workflow trace
     */
    public void setConnections(List<AliasedConnection> connections) {
        dirty = true;
        this.connections = connections;
    }

    /**
     * Add a connection to the workflow trace. Use only if you know what you are doing. Unless you
     * are manually configuring workflow traces (say for MiTM or unit tests), there shouldn't be any
     * need to call this method.
     *
     * @param connection new connection to add to the workflow trace
     */
    public void addConnection(AliasedConnection connection) {
        dirty = true;
        this.connections.add(connection);
    }

    public List<MessageAction> getMessageActions() {
        List<MessageAction> messageActions = new LinkedList<>();
        for (TlsAction action : tlsActions) {
            if (action instanceof MessageAction) {
                messageActions.add((MessageAction) action);
            }
        }
        return messageActions;
    }

    public List<ReceivingAction> getReceivingActions() {
        List<ReceivingAction> receiveActions = new LinkedList<>();
        for (TlsAction action : tlsActions) {
            if (action instanceof ReceivingAction) {
                receiveActions.add((ReceivingAction) action);
            }
        }
        return receiveActions;
    }

    public List<StaticReceivingAction> getStaticConfiguredReceivingActions() {
        List<StaticReceivingAction> staticConfiguredReceivingActions = new LinkedList<>();
        for (TlsAction action : tlsActions) {
            if (action instanceof StaticReceivingAction) {
                staticConfiguredReceivingActions.add((StaticReceivingAction) action);
            }
        }
        return staticConfiguredReceivingActions;
    }

    public List<SendingAction> getSendingActions() {
        List<SendingAction> sendingActions = new LinkedList<>();
        for (TlsAction action : tlsActions) {
            if (action instanceof SendingAction) {
                sendingActions.add((SendingAction) action);
            }
        }
        return sendingActions;
    }

    public List<StaticSendingAction> getStaticConfiguredSendingActions() {
        List<StaticSendingAction> staticConfiguredSendingActions = new LinkedList<>();
        for (TlsAction action : tlsActions) {
            if (action instanceof StaticSendingAction) {
                staticConfiguredSendingActions.add((StaticSendingAction) action);
            }
        }
        return staticConfiguredSendingActions;
    }

    /**
     * Get the last TlsAction of the workflow trace.
     *
     * @return the last TlsAction of the workflow trace. Null if no actions are defined
     */
    public TlsAction getLastAction() {
        int size = tlsActions.size();
        if (size != 0) {
            return tlsActions.get(size - 1);
        }
        return null;
    }

    /**
     * Get the last MessageAction of the workflow trace.
     *
     * @return the last MessageAction of the workflow trace. Null if no message actions are defined
     */
    public MessageAction getLastMessageAction() {
        for (int i = tlsActions.size() - 1; i >= 0; i--) {
            if (tlsActions.get(i) instanceof MessageAction) {
                return (MessageAction) (tlsActions.get(i));
            }
        }
        return null;
    }

    /**
     * Get the last SendingAction of the workflow trace.
     *
     * @return the last SendingAction of the workflow trace. Null if no sending actions are defined
     */
    public SendingAction getLastSendingAction() {
        for (int i = tlsActions.size() - 1; i >= 0; i--) {
            if (tlsActions.get(i) instanceof SendingAction) {
                return (SendingAction) (tlsActions.get(i));
            }
        }
        return null;
    }

    /**
     * Get the last ReceivingActionAction of the workflow trace.
     *
     * @return the last ReceivingActionAction of the workflow trace. Null if no receiving actions
     *     are defined
     */
    public ReceivingAction getLastReceivingAction() {
        for (int i = tlsActions.size() - 1; i >= 0; i--) {
            if (tlsActions.get(i) instanceof ReceivingAction) {
                return (ReceivingAction) (tlsActions.get(i));
            }
        }
        return null;
    }

    /**
     * Get the first MessageAction of the workflow trace.
     *
     * @return the first MessageAction of the workflow trace. Null if no message actions are defined
     */
    public MessageAction getFirstMessageAction() {
        for (int i = 0; i < tlsActions.size(); i++) {
            if (tlsActions.get(i) instanceof MessageAction) {
                return (MessageAction) (tlsActions.get(i));
            }
        }
        return null;
    }

    /**
     * Get the first SendingAction of the workflow trace.
     *
     * @return the first SendingAction of the workflow trace. Null if no sending actions are defined
     */
    public SendingAction getFirstSendingAction() {
        for (int i = 0; i < tlsActions.size(); i++) {
            if (tlsActions.get(i) instanceof SendingAction) {
                return (SendingAction) (tlsActions.get(i));
            }
        }
        return null;
    }

    /**
     * Get the first ReceivingActionAction of the workflow trace.
     *
     * @return the first ReceivingActionAction of the workflow trace. Null if no receiving actions
     *     are defined
     */
    public ReceivingAction getFirstReceivingAction() {
        for (int i = 0; i < tlsActions.size(); i++) {
            if (tlsActions.get(i) instanceof ReceivingAction) {
                return (ReceivingAction) (tlsActions.get(i));
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Trace Actions:");
        for (TlsAction action : tlsActions) {
            sb.append("\n");
            sb.append(action.toString());
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(this.tlsActions);
        hash = 23 * hash + Objects.hashCode(this.name);
        hash = 23 * hash + Objects.hashCode(this.description);
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
        final WorkflowTrace other = (WorkflowTrace) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.description, other.description)) {
            return false;
        }
        return Objects.equals(this.tlsActions, other.tlsActions);
    }

    public boolean executedAsPlanned() {
        for (TlsAction action : tlsActions) {
            if (!action.executedAsPlanned()
                    && (action.getActionOptions() == null
                            || !action.getActionOptions().contains(ActionOption.MAY_FAIL))) {
                LOGGER.debug("Action {} did not execute as planned", action.toCompactString());
                return false;
            } else {
                LOGGER.debug("Action {} executed as planned", action.toCompactString());
            }
        }
        return true;
    }

    public boolean allActionsExecuted() {
        for (TlsAction action : tlsActions) {
            if (!action.isExecuted()) {
                return false;
            }
        }
        return true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public <T extends TlsAction> T getFirstAction(Class<T> actionCls) {
        List<TlsAction> actions = this.getTlsActions();
        for (TlsAction action : actions) {
            if (action.getClass().equals(actionCls)) {
                return actionCls.cast(action);
            }
        }
        return null;
    }

    public <T extends ProtocolMessage> T getFirstReceivedMessage(Class<T> msgClass) {
        List<ProtocolMessage> messageList = WorkflowTraceResultUtil.getAllReceivedMessages(this);
        messageList =
                messageList.stream()
                        .filter(i -> msgClass.isAssignableFrom(i.getClass()))
                        .collect(Collectors.toList());

        if (messageList.isEmpty()) {
            return null;
        } else {
            return (T) messageList.get(0);
        }
    }

    public <T extends ProtocolMessage> T getLastReceivedMessage(Class<T> msgClass) {
        List<ProtocolMessage> messageList = WorkflowTraceResultUtil.getAllReceivedMessages(this);
        messageList =
                messageList.stream()
                        .filter(i -> msgClass.isAssignableFrom(i.getClass()))
                        .collect(Collectors.toList());

        if (messageList.isEmpty()) {
            return null;
        } else {
            return (T) messageList.get(messageList.size() - 1);
        }
    }

    public <T extends ProtocolMessage> T getFirstSentMessage(Class<T> msgClass) {
        List<ProtocolMessage> messageList = WorkflowTraceResultUtil.getAllSentMessages(this);
        messageList =
                messageList.stream()
                        .filter(i -> msgClass.isAssignableFrom(i.getClass()))
                        .collect(Collectors.toList());

        if (messageList.isEmpty()) {
            return null;
        } else {
            return (T) messageList.get(0);
        }
    }

    public <T extends ProtocolMessage> T getLastSentMessage(Class<T> msgClass) {
        List<ProtocolMessage> messageList = WorkflowTraceResultUtil.getAllSentMessages(this);
        messageList =
                messageList.stream()
                        .filter(i -> msgClass.isAssignableFrom(i.getClass()))
                        .collect(Collectors.toList());

        if (messageList.isEmpty()) {
            return null;
        } else {
            return (T) messageList.get(messageList.size() - 1);
        }
    }

    public List<MessageAction> getMessageActionsWithUnreadBytes() {
        return WorkflowTraceResultUtil.getMessageActionsWithUnreadBytes(this);
    }

    public boolean hasUnreadByte() {
        return WorkflowTraceResultUtil.hasUnreadBytes(this);
    }

    public static SendingAction getLastSendingAction(WorkflowTrace trace) {
        List<SendingAction> sendingActions = trace.getSendingActions();
        return sendingActions.get(sendingActions.size() - 1);
    }
}
