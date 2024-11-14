/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.message.extension;

import de.rub.nds.modifiablevariable.ModifiableVariableFactory;
import de.rub.nds.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.protocol.handler.extension.GreaseExtensionHandler;
import de.rub.nds.tlsattacker.core.protocol.parser.extension.GreaseExtensionParser;
import de.rub.nds.tlsattacker.core.protocol.preparator.extension.GreaseExtensionPreparator;
import de.rub.nds.tlsattacker.core.protocol.serializer.extension.GreaseExtensionSerializer;
import de.rub.nds.tlsattacker.core.state.Context;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@XmlRootElement(name = "GreaseExtension")
public class GreaseExtensionMessage extends ExtensionMessage {

    private static final Logger LOGGER = LogManager.getLogger();

    @ModifiableVariableProperty private ModifiableByteArray randomData;

    private byte[] data;
    private ExtensionType type;

    public GreaseExtensionMessage() {
        super(ExtensionType.GREASE_00);
        this.type = ExtensionType.GREASE_00;
        data = new byte[0];
    }

    public GreaseExtensionMessage(ExtensionType type, byte[] data) {
        super(type);
        if (!type.isGrease()) {
            LOGGER.warn("GreaseExtension message inizialized with non Grease extension type");
        }
        this.data = data;
        this.type = type;
    }

    /**
     * Constructor that creates a grease message with a specified payload length
     *
     * @param type
     * @param length
     */
    public GreaseExtensionMessage(ExtensionType type, int length) {
        super(type);
        if (!type.isGrease()) {
            LOGGER.warn("GreaseExtension message inizialized with non Grease extension type");
        }
        byte[] b = new byte[length];
        this.data = b;
        this.type = type;
    }

    @Override
    public ExtensionType getExtensionTypeConstant() {
        return this.type;
    }

    public ModifiableByteArray getRandomData() {
        return randomData;
    }

    public void setRandomData(byte[] bytes) {
        this.randomData = ModifiableVariableFactory.safelySetValue(randomData, bytes);
    }

    public void setRandomData(ModifiableByteArray randomData) {
        this.randomData = randomData;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public ExtensionType getType() {
        return type;
    }

    public void setType(ExtensionType type) {
        if (!type.isGrease()) {
            LOGGER.warn("GreaseExtension message type was set to non Grease extension type");
        }
        this.type = type;
        extensionTypeConstant = type;
    }

    @Override
    public GreaseExtensionParser getParser(Context context, InputStream stream) {
        return new GreaseExtensionParser(stream, context.getTlsContext());
    }

    @Override
    public GreaseExtensionPreparator getPreparator(Context context) {
        return new GreaseExtensionPreparator(context.getChooser(), this);
    }

    @Override
    public GreaseExtensionSerializer getSerializer(Context context) {
        return new GreaseExtensionSerializer(this);
    }

    @Override
    public GreaseExtensionHandler getHandler(Context context) {
        return new GreaseExtensionHandler(context.getTlsContext());
    }
}
