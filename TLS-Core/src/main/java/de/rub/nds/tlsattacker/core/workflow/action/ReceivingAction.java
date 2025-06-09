/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.workflow.action;

import de.rub.nds.tlsattacker.core.dtls.DtlsHandshakeMessageFragment;
import de.rub.nds.tlsattacker.core.http.HttpMessage;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.core.protocol.message.SSL2Message;
import de.rub.nds.tlsattacker.core.quic.frame.QuicFrame;
import de.rub.nds.tlsattacker.core.quic.packet.QuicPacket;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.tcp.TcpStreamContainer;
import de.rub.nds.tlsattacker.core.udp.UdpDataPacket;
import java.util.List;
import java.util.Set;

public interface ReceivingAction {

    List<ProtocolMessage> getReceivedMessages();

    List<SSL2Message> getReceivedSSL2Messages();

    List<Record> getReceivedRecords();

    List<DtlsHandshakeMessageFragment> getReceivedFragments();

    List<HttpMessage> getReceivedHttpMessages();

    List<QuicFrame> getReceivedQuicFrames();

    List<QuicPacket> getReceivedQuicPackets();

    List<TcpStreamContainer> getReceivedTcpStreamContainers();

    List<UdpDataPacket> getReceivedUdpDataPackets();

    public abstract Set<String> getAllReceivingAliases();
}
