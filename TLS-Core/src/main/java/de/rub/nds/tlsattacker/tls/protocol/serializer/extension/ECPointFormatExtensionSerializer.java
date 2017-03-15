/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.tls.protocol.serializer.extension;

import de.rub.nds.tlsattacker.tls.constants.ExtensionByteLength;
import de.rub.nds.tlsattacker.tls.protocol.parser.extension.*;
import de.rub.nds.tlsattacker.tls.protocol.extension.ECPointFormatExtensionMessage;
import de.rub.nds.tlsattacker.tls.protocol.extension.ExtensionMessage;

/**
 *
 * @author Robert Merget - robert.merget@rub.de
 */
public class ECPointFormatExtensionSerializer extends ExtensionSerializer<ECPointFormatExtensionMessage>{

    private final ECPointFormatExtensionMessage message;
             
    public ECPointFormatExtensionSerializer(ECPointFormatExtensionMessage message) {
        super(message);
        this.message = message;
    }

    @Override
    public byte[] serializeExtensionContent() {
        appendInt(message.getPointFormatsLength().getValue(), ExtensionByteLength.EC_POINT_FORMATS_LENGTH);
        appendBytes(message.getPointFormats().getValue());
        return getAlreadySerialized();
    }
}
