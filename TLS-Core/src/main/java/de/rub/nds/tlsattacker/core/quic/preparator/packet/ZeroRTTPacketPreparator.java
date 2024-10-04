/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.quic.preparator.packet;

import de.rub.nds.tlsattacker.core.quic.constants.QuicPacketType;
import de.rub.nds.tlsattacker.core.quic.packet.ZeroRTTPacket;
import de.rub.nds.tlsattacker.core.workflow.chooser.Chooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZeroRTTPacketPreparator extends LongHeaderPacketPreparator<ZeroRTTPacket> {

    private static final Logger LOGGER = LogManager.getLogger();

    public ZeroRTTPacketPreparator(Chooser chooser, ZeroRTTPacket packet) {
        super(chooser, packet);
    }

    @Override
    public void prepare() {
        LOGGER.debug("Preparing 0-RTT Packet");
        prepareUnprotectedFlags();
        prepareUnprotectedPacketNumber();
        prepareLongHeaderPacket();
    }

    private void prepareUnprotectedPacketNumber() {
        // packet numbers for 0-RTT packets use the same space as 1-RTT protected packets
        if (packet.getUnprotectedPacketNumber() == null) {
            packet.setUnprotectedPacketNumber(context.getOneRTTPacketPacketNumber());
            context.setOneRTTPacketPacketNumber(context.getOneRTTPacketPacketNumber() + 1);
        }
        LOGGER.debug(
                "Unprotected Packet Number: {}", packet.getUnprotectedPacketNumber().getValue());
    }

    private void prepareUnprotectedFlags() {
        packet.setUnprotectedFlags(
                QuicPacketType.ZERO_RTT_PACKET.getHeader(context.getQuicVersion()));
        LOGGER.debug("Unprotected Flags: {}", packet.getUnprotectedFlags().getValue());
    }
}
