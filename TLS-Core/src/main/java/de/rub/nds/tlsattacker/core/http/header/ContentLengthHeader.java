/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.http.header;

import de.rub.nds.modifiablevariable.ModifiableVariableFactory;
import de.rub.nds.modifiablevariable.integer.ModifiableInteger;
import de.rub.nds.tlsattacker.core.http.header.preparator.ContentLengthHeaderPreparator;
import de.rub.nds.tlsattacker.core.http.header.serializer.HttpHeaderSerializer;
import de.rub.nds.tlsattacker.core.layer.context.HttpContext;
import de.rub.nds.tlsattacker.core.layer.data.Handler;
import de.rub.nds.tlsattacker.core.layer.data.Parser;
import de.rub.nds.tlsattacker.core.layer.data.Serializer;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.InputStream;

public class ContentLengthHeader extends HttpHeader {

    private ModifiableInteger length;

    @XmlTransient private int configLength;

    public ContentLengthHeader() {}

    @Override
    public ContentLengthHeaderPreparator getPreparator(HttpContext httpContext) {
        return new ContentLengthHeaderPreparator(httpContext, this);
    }

    public ModifiableInteger getLength() {
        return length;
    }

    public void setLength(ModifiableInteger length) {
        this.length = length;
    }

    public void setLength(int length) {
        this.length = ModifiableVariableFactory.safelySetValue(this.length, length);
    }

    public int getConfigLength() {
        return configLength;
    }

    public void setConfigLength(int configLength) {
        this.configLength = configLength;
    }

    @Override
    public Parser<?> getParser(HttpContext context, InputStream stream) {
        return null; // TODO Parser is not used
    }

    @Override
    public Serializer<?> getSerializer(HttpContext context) {
        return new HttpHeaderSerializer(this);
    }

    @Override
    public Handler<?> getHandler(HttpContext context) {
        return null;
    }
}
