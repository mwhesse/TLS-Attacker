/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.protocol.parser;

import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.tlsattacker.core.constants.HandshakeByteLength;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.NamedCurve;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.PSKECDHEServerKeyExchangeMessage;
import static de.rub.nds.tlsattacker.core.protocol.parser.Parser.LOGGER;

/**
 *
 * @author Florian Linsner - florian.linsner@rub.de
 */
public class PSKECDHEServerKeyExchangeParser extends ServerKeyExchangeParser<PSKECDHEServerKeyExchangeMessage> {

    private final ProtocolVersion version;

    /**
     * Constructor for the Parser class
     *
     * @param pointer
     *            Position in the array where the ServerKeyExchangeParser is
     *            supposed to start parsing
     * @param array
     *            The byte[] which the ServerKeyExchangeParser is supposed to
     *            parse
     * @param version
     *            Version of the Protocol
     */
    public PSKECDHEServerKeyExchangeParser(int pointer, byte[] array, ProtocolVersion version) {
        super(pointer, array, HandshakeMessageType.SERVER_KEY_EXCHANGE, version);
        this.version = version;
    }

    @Override
    protected void parseHandshakeMessageContent(PSKECDHEServerKeyExchangeMessage msg) {
        LOGGER.debug("Parsing PSKECDHEServerKeyExchangeMessage");
        parsePskIdentityHintLength(msg);
        parsePskIdentityHint(msg);
        parseCurveType(msg);
        parseNamedCurve(msg);
        parseSerializedPublicKeyLength(msg);
        parseSerializedPublicKey(msg);
    }

    @Override
    protected PSKECDHEServerKeyExchangeMessage createHandshakeMessage() {
        return new PSKECDHEServerKeyExchangeMessage();
    }

    private void parsePskIdentityHintLength(PSKECDHEServerKeyExchangeMessage msg) {
        msg.setIdentityHintLength(parseByteArrayField(HandshakeByteLength.PSK_IDENTITY_LENGTH));
        LOGGER.debug("SerializedPSK-IdentityLength: " + msg.getIdentityHintLength().getValue());
    }

    /**
     * Reads the next bytes as the PSKIdentityHint and writes them in the
     * message
     *
     * @param msg
     *            Message to write in
     */
    private void parsePskIdentityHint(PSKECDHEServerKeyExchangeMessage msg) {
        msg.setIdentityHint(parseByteArrayField(ArrayConverter.bytesToInt(msg.getIdentityHintLength().getValue())));
        LOGGER.debug("SerializedPSK-Identity: " + ArrayConverter.bytesToHexString(msg.getIdentityHint().getValue()));
    }

    /**
     * Reads the next bytes as the CurveType and writes them in the message
     *
     * @param msg
     *            Message to write in
     */
    private void parseCurveType(PSKECDHEServerKeyExchangeMessage msg) {
        msg.setCurveType(parseByteField(HandshakeByteLength.ELLIPTIC_CURVE));
        LOGGER.debug("CurveType: " + msg.getCurveType().getValue());
    }

    /**
     * Reads the next bytes as the Curve and writes them in the message
     *
     * @param msg
     *            Message to write in
     */
    private void parseNamedCurve(PSKECDHEServerKeyExchangeMessage msg) {
        msg.setNamedCurve(parseByteArrayField(NamedCurve.LENGTH));
        LOGGER.debug("NamedCurve: " + ArrayConverter.bytesToHexString(msg.getNamedCurve().getValue()));
    }

    /**
     * Reads the next bytes as the SerializedPublicKeyLength and writes them in
     * the message
     *
     * @param msg
     *            Message to write in
     */
    private void parseSerializedPublicKeyLength(PSKECDHEServerKeyExchangeMessage msg) {
        msg.setPublicKeyLength(parseIntField(HandshakeByteLength.ECDHE_PARAM_LENGTH));
        LOGGER.debug("SerializedPublicKeyLength: " + msg.getPublicKeyLength().getValue());
    }

    /**
     * Reads the next bytes as the SerializedPublicKey and writes them in the
     * message
     *
     * @param msg
     *            Message to write in
     */
    private void parseSerializedPublicKey(PSKECDHEServerKeyExchangeMessage msg) {
        msg.setPublicKey(parseByteArrayField(msg.getPublicKeyLength().getValue()));
        LOGGER.debug("SerializedPublicKey: " + ArrayConverter.bytesToHexString(msg.getPublicKey().getValue()));
    }
}
