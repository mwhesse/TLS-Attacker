/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol.preparator;

import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.protocol.constants.NamedEllipticCurveParameters;
import de.rub.nds.protocol.crypto.CyclicGroup;
import de.rub.nds.protocol.crypto.ec.EllipticCurve;
import de.rub.nds.protocol.crypto.ec.EllipticCurveOverFp;
import de.rub.nds.protocol.crypto.ec.EllipticCurveSECP256R1;
import de.rub.nds.protocol.crypto.ec.Point;
import de.rub.nds.protocol.crypto.ec.PointFormatter;
import de.rub.nds.protocol.exception.PreparationException;
import de.rub.nds.tlsattacker.core.constants.ECPointFormat;
import de.rub.nds.tlsattacker.core.constants.EllipticCurveType;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.exceptions.CryptoException;
import de.rub.nds.tlsattacker.core.protocol.message.PWDServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.protocol.message.computations.PWDComputations;
import de.rub.nds.tlsattacker.core.workflow.chooser.Chooser;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PWDServerKeyExchangePreparator
        extends ServerKeyExchangePreparator<PWDServerKeyExchangeMessage> {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final PWDServerKeyExchangeMessage msg;

    public PWDServerKeyExchangePreparator(Chooser chooser, PWDServerKeyExchangeMessage msg) {
        super(chooser, msg);
        this.msg = msg;
    }

    @Override
    public void prepareHandshakeMessageContents() {
        LOGGER.debug("Preparing PWDServerKeyExchangeMessage");
        msg.prepareKeyExchangeComputations();
        prepareCurveType(msg);
        NamedGroup group = selectNamedGroup(msg);
        msg.setNamedGroup(group.getValue());
        prepareSalt(msg);
        prepareSaltLength(msg);

        try {
            preparePasswordElement(msg);
        } catch (CryptoException e) {
            throw new PreparationException("Failed to generate password element", e);
        }
        prepareScalarElement(msg);
    }

    protected void preparePasswordElement(PWDServerKeyExchangeMessage msg) throws CryptoException {
        NamedGroup namedGroup = selectNamedGroup(msg);
        CyclicGroup<?> group = namedGroup.getGroupParameters().getGroup();
        EllipticCurve curve;
        if (group instanceof EllipticCurve) {
            curve = (EllipticCurve) group;
        } else {
            LOGGER.warn("Selected group is not an EllipticCurve. Using SECP256R1");
            curve = new EllipticCurveSECP256R1();
        }
        Point passwordElement = PWDComputations.computePasswordElement(chooser, curve);
        msg.getKeyExchangeComputations().setPasswordElement(passwordElement);

        LOGGER.debug(
                "PasswordElement.x: {}",
                () -> ArrayConverter.bigIntegerToByteArray(passwordElement.getFieldX().getData()));
    }

    protected NamedGroup selectNamedGroup(PWDServerKeyExchangeMessage msg) {
        NamedGroup namedGroup;
        if (chooser.getConfig().isEnforceSettings()) {
            namedGroup = chooser.getConfig().getDefaultSelectedNamedGroup();
        } else {
            Set<NamedGroup> serverSet = new HashSet<>();
            Set<NamedGroup> clientSet = new HashSet<>();
            for (int i = 0; i < chooser.getClientSupportedNamedGroups().size(); i++) {
                NamedGroup tempNamedGroup = chooser.getClientSupportedNamedGroups().get(i);
                if (tempNamedGroup.isShortWeierstrass()) {
                    CyclicGroup<?> group = tempNamedGroup.getGroupParameters().getGroup();
                    EllipticCurve curve;
                    if (group instanceof EllipticCurve) {
                        curve = (EllipticCurve) group;
                    } else {
                        LOGGER.warn("Selected group is not an EllipticCurve. Using SECP256R1");
                        curve = new EllipticCurveSECP256R1();
                    }
                    if (curve instanceof EllipticCurveOverFp) {
                        clientSet.add(tempNamedGroup);
                    }
                }
            }
            for (int i = 0; i < chooser.getConfig().getDefaultServerNamedGroups().size(); i++) {
                NamedGroup tempNamedGroup =
                        chooser.getConfig().getDefaultServerNamedGroups().get(i);
                if (tempNamedGroup.isShortWeierstrass()) {
                    CyclicGroup<?> group =
                            chooser.getSelectedNamedGroup().getGroupParameters().getGroup();
                    EllipticCurve curve;
                    if (group instanceof EllipticCurve) {
                        curve = (EllipticCurve) group;
                    } else {
                        LOGGER.warn("Selected group is not an EllipticCurve. Using SECP256R1");
                        curve = new EllipticCurveSECP256R1();
                    }
                    if (curve instanceof EllipticCurveOverFp) {
                        serverSet.add(tempNamedGroup);
                    }
                }
            }
            serverSet.retainAll(clientSet);
            if (serverSet.isEmpty()) {
                LOGGER.warn("No common NamedGroup - falling back to default");
                namedGroup = chooser.getConfig().getDefaultSelectedNamedGroup();
            } else {
                if (serverSet.contains(chooser.getConfig().getDefaultSelectedNamedGroup())) {
                    namedGroup = chooser.getConfig().getDefaultSelectedNamedGroup();
                } else {
                    namedGroup = (NamedGroup) serverSet.toArray()[0];
                }
            }
        }
        return namedGroup;
    }

    protected void prepareSalt(PWDServerKeyExchangeMessage msg) {
        msg.setSalt(chooser.getConfig().getDefaultServerPWDSalt());
        LOGGER.debug("Salt: {}", msg.getSalt().getValue());
    }

    protected void prepareSaltLength(PWDServerKeyExchangeMessage msg) {
        msg.setSaltLength(msg.getSalt().getValue().length);
        LOGGER.debug("SaltLength: " + msg.getSaltLength().getValue());
    }

    protected void prepareCurveType(PWDServerKeyExchangeMessage msg) {
        msg.setCurveType(EllipticCurveType.NAMED_CURVE.getValue());
    }

    protected List<ECPointFormat> getPointFormatList() {
        List<ECPointFormat> sharedPointFormats =
                new ArrayList<>(chooser.getServerSupportedPointFormats());

        if (sharedPointFormats.isEmpty()) {
            LOGGER.warn(
                    "Don't know which point format to use for PWD. Check if pointFormats is set in config.");
            sharedPointFormats = chooser.getConfig().getDefaultServerSupportedPointFormats();
        }

        List<ECPointFormat> unsupportedFormats = new ArrayList<>();

        if (!chooser.getConfig().isEnforceSettings()) {
            List<ECPointFormat> clientPointFormats = chooser.getClientSupportedPointFormats();
            for (ECPointFormat f : sharedPointFormats) {
                if (!clientPointFormats.contains(f)) {
                    unsupportedFormats.add(f);
                }
            }
        }

        sharedPointFormats.removeAll(unsupportedFormats);
        if (sharedPointFormats.isEmpty()) {
            sharedPointFormats =
                    new ArrayList<>(chooser.getConfig().getDefaultServerSupportedPointFormats());
        }

        return sharedPointFormats;
    }

    protected void prepareScalarElement(PWDServerKeyExchangeMessage msg) {
        CyclicGroup<?> group = selectNamedGroup(msg).getGroupParameters().getGroup();
        EllipticCurve curve;
        if (group instanceof EllipticCurve) {
            curve = (EllipticCurve) group;
        } else {
            LOGGER.warn("Selected group is not an EllipticCurve. Using SECP256R1");
            curve = new EllipticCurveSECP256R1();
        }
        PWDComputations.PWDKeyMaterial keyMaterial =
                PWDComputations.generateKeyMaterial(
                        curve, msg.getKeyExchangeComputations().getPasswordElement(), chooser);

        msg.getKeyExchangeComputations().setPrivateKeyScalar(keyMaterial.privateKeyScalar);
        LOGGER.debug(
                "Private: {}",
                () -> ArrayConverter.bigIntegerToByteArray(keyMaterial.privateKeyScalar));

        prepareScalar(msg, keyMaterial.scalar);
        prepareScalarLength(msg);

        prepareElement(msg, keyMaterial.element);
        prepareElementLength(msg);
    }

    protected void prepareScalar(PWDServerKeyExchangeMessage msg, BigInteger scalar) {
        msg.setScalar(ArrayConverter.bigIntegerToByteArray(scalar));
        LOGGER.debug("Scalar: {}", () -> ArrayConverter.bigIntegerToByteArray(scalar));
    }

    protected void prepareScalarLength(PWDServerKeyExchangeMessage msg) {
        msg.setScalarLength(msg.getScalar().getValue().length);
        LOGGER.debug("ScalarLength: " + msg.getScalarLength());
    }

    protected void prepareElement(PWDServerKeyExchangeMessage msg, Point element) {
        byte[] serializedElement =
                PointFormatter.formatToByteArray(
                        (NamedEllipticCurveParameters)
                                chooser.getConfig()
                                        .getDefaultSelectedNamedGroup()
                                        .getGroupParameters(),
                        element,
                        chooser.getConfig().getDefaultSelectedPointFormat().getFormat());
        msg.setElement(serializedElement);
        LOGGER.debug("Element: {}", serializedElement);
    }

    protected void prepareElementLength(PWDServerKeyExchangeMessage msg) {
        msg.setElementLength(msg.getElement().getValue().length);
        LOGGER.debug("ElementLength: {}", msg.getElementLength());
    }
}
