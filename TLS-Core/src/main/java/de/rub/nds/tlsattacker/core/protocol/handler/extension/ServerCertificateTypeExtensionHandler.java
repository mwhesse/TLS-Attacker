/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.protocol.handler.extension;

import de.rub.nds.tlsattacker.core.constants.CertificateType;
import de.rub.nds.tlsattacker.core.protocol.message.extension.ServerCertificateTypeExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.parser.extension.ServerCertificateTypeExtensionParser;
import de.rub.nds.tlsattacker.core.protocol.preparator.extension.ServerCertificateTypeExtensionPreparator;
import de.rub.nds.tlsattacker.core.protocol.serializer.extension.ServerCertificateTypeExtensionSerializer;
import de.rub.nds.tlsattacker.core.state.TlsContext;

public class ServerCertificateTypeExtensionHandler extends ExtensionHandler<ServerCertificateTypeExtensionMessage> {

    public ServerCertificateTypeExtensionHandler(TlsContext context) {
        super(context);
    }

    @Override
    public ServerCertificateTypeExtensionParser getParser(byte[] message, int pointer) {
        return new ServerCertificateTypeExtensionParser(pointer, message);
    }

    @Override
    public ServerCertificateTypeExtensionPreparator getPreparator(ServerCertificateTypeExtensionMessage message) {
        return new ServerCertificateTypeExtensionPreparator(context.getChooser(), message, getSerializer(message));
    }

    @Override
    public ServerCertificateTypeExtensionSerializer getSerializer(ServerCertificateTypeExtensionMessage message) {
        return new ServerCertificateTypeExtensionSerializer(message);
    }

    @Override
    public void adjustTLSExtensionContext(ServerCertificateTypeExtensionMessage message) {
        context.setServerCertificateTypeDesiredTypes(CertificateType.getCertificateTypesAsList(message
                .getCertificateTypes().getValue()));
    }

}
