/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.quic.packet;

import de.rub.nds.modifiablevariable.ModifiableVariableHolder;
import de.rub.nds.tlsattacker.core.constants.AlgorithmResolver;
import de.rub.nds.tlsattacker.core.constants.HKDFAlgorithm;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.crypto.HKDFunction;
import de.rub.nds.tlsattacker.core.exceptions.CryptoException;
import de.rub.nds.tlsattacker.core.quic.constants.QuicVersion;
import de.rub.nds.tlsattacker.core.state.quic.QuicContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QuicPacketCryptoComputations extends ModifiableVariableHolder {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int INITIAL_KEY_LENGTH = 16;
    private static final int IV_LENGTH = 12;

    public QuicPacketCryptoComputations() {}

    public static byte[] generateHeaderProtectionMask(
            Cipher cipher, byte[] headerProtectionKey, byte[] sample) throws CryptoException {
        try {
            byte[] mask;
            SecretKeySpec keySpec = new SecretKeySpec(headerProtectionKey, cipher.getAlgorithm());
            if (cipher.getAlgorithm().equals("ChaCha20")) {
                // 5.4.4. ChaCha20-Based Header Protection
                //  header_protection(hp_key, sample):
                //    counter = sample[0..3]
                //    nonce = sample[4..15]
                //    mask = ChaCha20(hp_key, counter, nonce, {0,0,0,0,0})

                ByteBuffer wrapped =
                        ByteBuffer.wrap(Arrays.copyOfRange(sample, 0, 4)); // big-endian by default
                wrapped.order(ByteOrder.LITTLE_ENDIAN);
                int counter = wrapped.getInt();
                byte[] nonce = Arrays.copyOfRange(sample, 4, 16);
                ChaCha20ParameterSpec param = new ChaCha20ParameterSpec(nonce, counter);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, param);
                mask = cipher.doFinal(new byte[] {0, 0, 0, 0, 0});
            } else {
                // 5.4.3. AES-Based Header Protection
                //  header_protection(hp_key, sample):
                //    mask = AES-ECB(hp_key, sample)

                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                mask = cipher.doFinal(sample);
            }
            return mask;

        } catch (BadPaddingException
                | IllegalBlockSizeException
                | InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            throw new CryptoException("Could not generate header protection mask", e);
        }
    }

    public static byte[] generateInitialClientHeaderProtectionMask(
            QuicContext context, byte[] sample) throws CryptoException {
        return generateHeaderProtectionMask(
                context.getInitalHeaderProtectionCipher(),
                context.getInitialClientHeaderProtectionKey(),
                sample);
    }

    public static byte[] generateInitialServerHeaderProtectionMask(
            QuicContext context, byte[] sample) throws CryptoException {
        return generateHeaderProtectionMask(
                context.getInitalHeaderProtectionCipher(),
                context.getInitialServerHeaderProtectionKey(),
                sample);
    }

    public static byte[] generateHandshakeClientHeaderProtectionMask(
            QuicContext context, byte[] sample) throws CryptoException {
        return generateHeaderProtectionMask(
                context.getHeaderProtectionCipher(),
                context.getHandshakeClientHeaderProtectionKey(),
                sample);
    }

    public static byte[] generateHandshakeServerHeaderProtectionMask(
            QuicContext context, byte[] sample) throws CryptoException {
        return generateHeaderProtectionMask(
                context.getHeaderProtectionCipher(),
                context.getHandshakeServerHeaderProtectionKey(),
                sample);
    }

    public static byte[] generateApplicationClientHeaderProtectionMask(
            QuicContext context, byte[] sample) throws CryptoException {
        return generateHeaderProtectionMask(
                context.getHeaderProtectionCipher(),
                context.getApplicationClientHeaderProtectionKey(),
                sample);
    }

    public static byte[] generateApplicationServerHeaderProtectionMask(
            QuicContext context, byte[] sample) throws CryptoException {
        return generateHeaderProtectionMask(
                context.getHeaderProtectionCipher(),
                context.getApplicationServerHeaderProtectionKey(),
                sample);
    }

    public static byte[] generateZeroRTTClientHeaderProtectionMask(
            QuicContext context, byte[] sample) throws CryptoException {
        return generateHeaderProtectionMask(
                context.getZeroRTTHeaderProtectionCipher(),
                context.getZeroRTTClientHeaderProtectionKey(),
                sample);
    }

    public static byte[] generateZeroRTTServerHeaderProtectionMask(
            QuicContext context, byte[] sample) throws CryptoException {
        return generateHeaderProtectionMask(
                context.getZeroRTTHeaderProtectionCipher(),
                context.getZeroRTTServerHeaderProtectionKey(),
                sample);
    }

    /**
     * Calculates all initial client and server secrets including key, IV, and the key for header
     * protection.
     *
     * @param context
     * @throws CryptoException
     * @throws NoSuchAlgorithmException
     */
    public static void calculateInitialSecrets(QuicContext context)
            throws CryptoException, NoSuchAlgorithmException {
        LOGGER.debug("Initialize Quic Initial Secrets");
        HKDFAlgorithm hkdfAlgorithm = context.getInitialHKDFAlgorithm();
        Mac mac = Mac.getInstance(hkdfAlgorithm.getMacAlgorithm().getJavaName());

        context.setInitialSecret(
                HKDFunction.extract(
                        hkdfAlgorithm,
                        context.getInitialSalt(),
                        context.getFirstDestinationConnectionId()));

        QuicVersion version = context.getQuicVersion();

        if (version == QuicVersion.NEGOTIATION_VERSION) {
            // There are no initial secrets, version negotiation packets are unencrypted
            throw new UnsupportedOperationException(
                    "Version Negotiation Packets do not have initial secrets. They are not encrypted.");
        }

        // client
        context.setInitialClientSecret(
                HKDFunction.expandLabel(
                        hkdfAlgorithm,
                        context.getInitialSecret(),
                        HKDFunction.CLIENT_IN,
                        new byte[0],
                        mac.getMacLength(),
                        ProtocolVersion.TLS13));
        context.setInitialClientKey(
                deriveKeyFromSecret(
                        version,
                        hkdfAlgorithm,
                        context.getInitialClientSecret(),
                        INITIAL_KEY_LENGTH));
        context.setInitialClientIv(
                deriveIvFromSecret(version, hkdfAlgorithm, context.getInitialClientSecret()));
        context.setInitialClientHeaderProtectionKey(
                deriveHeaderProtectionKeyFromSecret(
                        version,
                        hkdfAlgorithm,
                        context.getInitialClientSecret(),
                        INITIAL_KEY_LENGTH));

        // server
        context.setInitialServerSecret(
                HKDFunction.expandLabel(
                        hkdfAlgorithm,
                        context.getInitialSecret(),
                        HKDFunction.SERVER_IN,
                        new byte[0],
                        mac.getMacLength(),
                        ProtocolVersion.TLS13));
        context.setInitialServerKey(
                deriveKeyFromSecret(
                        version,
                        hkdfAlgorithm,
                        context.getInitialServerSecret(),
                        INITIAL_KEY_LENGTH));
        context.setInitialServerIv(
                deriveIvFromSecret(version, hkdfAlgorithm, context.getInitialServerSecret()));
        context.setInitialServerHeaderProtectionKey(
                deriveHeaderProtectionKeyFromSecret(
                        version,
                        hkdfAlgorithm,
                        context.getInitialServerSecret(),
                        INITIAL_KEY_LENGTH));

        context.setInitialSecretsInitialized(true);
    }

    /**
     * Calculates all handshake client and server secrets including key, IV, and the key for header
     * protection.
     *
     * @param context
     * @throws CryptoException
     * @throws NoSuchAlgorithmException
     */
    public static void calculateHandshakeSecrets(QuicContext context)
            throws NoSuchPaddingException, NoSuchAlgorithmException, CryptoException {
        LOGGER.debug("Initialize Quic Handshake Secrets");
        context.setAeadCipher(
                Cipher.getInstance(
                        AlgorithmResolver.getCipher(
                                        context.getContext()
                                                .getTlsContext()
                                                .getSelectedCipherSuite())
                                .getJavaName()));
        // TODO: maybe move to handler
        switch (context.getContext().getTlsContext().getSelectedCipherSuite()) {
            case TLS_AES_128_CCM_SHA256:
            case TLS_AES_128_GCM_SHA256:
            case TLS_AES_256_GCM_SHA384:
                context.setHeaderProtectionCipher(Cipher.getInstance("AES/ECB/NoPadding"));
                break;
            case TLS_CHACHA20_POLY1305_SHA256:
                context.setHeaderProtectionCipher(Cipher.getInstance("ChaCha20"));
                break;
        }

        context.setHkdfAlgorithm(
                AlgorithmResolver.getHKDFAlgorithm(
                        context.getContext().getTlsContext().getSelectedCipherSuite()));

        HKDFAlgorithm hkdfAlgorithm = context.getHkdfAlgorithm();

        int keyLength = 16;

        switch (context.getContext().getTlsContext().getSelectedCipherSuite()) {
            case TLS_AES_128_CCM_SHA256:
            case TLS_AES_128_GCM_SHA256:
                keyLength = 16;
                break;
            case TLS_AES_256_GCM_SHA384:
            case TLS_CHACHA20_POLY1305_SHA256:
                keyLength = 32;
                break;
        }

        QuicVersion version = context.getQuicVersion();

        // client
        context.setHandshakeClientSecret(
                context.getContext().getTlsContext().getClientHandshakeTrafficSecret());
        context.setHandshakeClientKey(
                deriveKeyFromSecret(
                        version, hkdfAlgorithm, context.getHandshakeClientSecret(), keyLength));
        context.setHandshakeClientIv(
                deriveIvFromSecret(version, hkdfAlgorithm, context.getHandshakeClientSecret()));
        context.setHandshakeClientHeaderProtectionKey(
                deriveHeaderProtectionKeyFromSecret(
                        version, hkdfAlgorithm, context.getHandshakeClientSecret(), keyLength));

        // server
        context.setHandshakeServerSecret(
                context.getContext().getTlsContext().getServerHandshakeTrafficSecret());
        context.setHandshakeServerKey(
                deriveKeyFromSecret(
                        version, hkdfAlgorithm, context.getHandshakeServerSecret(), keyLength));
        context.setHandshakeServerIv(
                deriveIvFromSecret(version, hkdfAlgorithm, context.getHandshakeServerSecret()));
        context.setHandshakeServerHeaderProtectionKey(
                deriveHeaderProtectionKeyFromSecret(
                        version, hkdfAlgorithm, context.getHandshakeServerSecret(), keyLength));

        context.setHandshakeSecretsInitialized(true);
    }

    /**
     * Calculates all application client and server secrets including key, IV, and the key for
     * header protection.
     *
     * @param context
     * @throws CryptoException
     * @throws NoSuchAlgorithmException
     */
    public static void calculateApplicationSecrets(QuicContext context)
            throws NoSuchPaddingException, NoSuchAlgorithmException, CryptoException {
        LOGGER.debug("Initialize Quic Application Secrets");
        HKDFAlgorithm hkdfAlgorithm = context.getHkdfAlgorithm();

        int keyLength = 16;

        switch (context.getContext().getTlsContext().getSelectedCipherSuite()) {
            case TLS_AES_128_CCM_SHA256:
            case TLS_AES_128_GCM_SHA256:
                keyLength = 16;
                break;
            case TLS_AES_256_GCM_SHA384:
            case TLS_CHACHA20_POLY1305_SHA256:
                keyLength = 32;
                break;
        }

        QuicVersion version = context.getQuicVersion();

        // client
        context.setApplicationClientSecret(
                context.getContext().getTlsContext().getClientApplicationTrafficSecret());
        context.setApplicationClientKey(
                deriveKeyFromSecret(
                        version, hkdfAlgorithm, context.getApplicationClientSecret(), keyLength));
        context.setApplicationClientIv(
                deriveIvFromSecret(version, hkdfAlgorithm, context.getApplicationClientSecret()));
        context.setApplicationClientHeaderProtectionKey(
                deriveHeaderProtectionKeyFromSecret(
                        version, hkdfAlgorithm, context.getApplicationClientSecret(), keyLength));

        // server
        context.setApplicationServerSecret(
                context.getContext().getTlsContext().getServerApplicationTrafficSecret());
        context.setApplicationServerKey(
                deriveKeyFromSecret(
                        version, hkdfAlgorithm, context.getApplicationServerSecret(), keyLength));
        context.setApplicationServerIv(
                deriveIvFromSecret(version, hkdfAlgorithm, context.getApplicationServerSecret()));
        context.setApplicationServerHeaderProtectionKey(
                deriveHeaderProtectionKeyFromSecret(
                        version, hkdfAlgorithm, context.getApplicationServerSecret(), keyLength));

        context.setApplicationSecretsInitialized(true);
    }

    public static void calculate0RTTSecrets(QuicContext context)
            throws CryptoException, NoSuchPaddingException, NoSuchAlgorithmException {
        LOGGER.debug("Initialize Quic 0-RTT Secrets");
        context.setZeroRTTCipherSuite(
                context.getContext().getTlsContext().getEarlyDataCipherSuite());
        context.setZeroRTTAeadCipher(
                Cipher.getInstance(
                        AlgorithmResolver.getCipher(
                                        context.getContext()
                                                .getTlsContext()
                                                .getEarlyDataCipherSuite())
                                .getJavaName()));
        // TODO: maybe move to handler
        switch (context.getZeroRTTCipherSuite()) {
            case TLS_AES_128_CCM_SHA256:
            case TLS_AES_128_GCM_SHA256:
            case TLS_AES_256_GCM_SHA384:
                context.setZeroRTTHeaderProtectionCipher(Cipher.getInstance("AES/ECB/NoPadding"));
                break;
            case TLS_CHACHA20_POLY1305_SHA256:
                context.setZeroRTTHeaderProtectionCipher(Cipher.getInstance("ChaCha20"));
                break;
        }

        context.setZeroRTTHKDFAlgorithm(
                AlgorithmResolver.getHKDFAlgorithm(context.getZeroRTTCipherSuite()));

        HKDFAlgorithm hkdfAlgorithm = context.getZeroRTTHKDFAlgorithm();

        int keyLength = 16;

        switch (context.getZeroRTTCipherSuite()) {
            case TLS_AES_128_CCM_SHA256:
            case TLS_AES_128_GCM_SHA256:
                keyLength = 16;
                break;
            case TLS_AES_256_GCM_SHA384:
            case TLS_CHACHA20_POLY1305_SHA256:
                keyLength = 32;
                break;
        }

        QuicVersion version = context.getQuicVersion();

        // client
        context.setZeroRTTClientSecret(
                context.getContext().getTlsContext().getClientEarlyTrafficSecret());
        context.setZeroRTTClientKey(
                deriveKeyFromSecret(
                        version, hkdfAlgorithm, context.getZeroRTTClientSecret(), keyLength));
        context.setZeroRTTClientIv(
                deriveIvFromSecret(version, hkdfAlgorithm, context.getZeroRTTClientSecret()));
        context.setZeroRTTClientHeaderProtectionKey(
                deriveHeaderProtectionKeyFromSecret(
                        version, hkdfAlgorithm, context.getZeroRTTClientSecret(), keyLength));

        // server
        context.setZeroRTTServerSecret(
                context.getContext().getTlsContext().getClientEarlyTrafficSecret());
        context.setZeroRTTServerKey(
                deriveKeyFromSecret(
                        version, hkdfAlgorithm, context.getZeroRTTServerSecret(), keyLength));
        context.setZeroRTTServerIv(
                deriveIvFromSecret(version, hkdfAlgorithm, context.getZeroRTTServerSecret()));
        context.setZeroRTTServerHeaderProtectionKey(
                deriveHeaderProtectionKeyFromSecret(
                        version, hkdfAlgorithm, context.getZeroRTTServerSecret(), keyLength));

        context.setZeroRTTSecretsInitialized(true);
    }

    private static byte[] deriveKeyFromSecret(
            QuicVersion version, HKDFAlgorithm hkdfAlgorithm, byte[] secret, int keyLength)
            throws CryptoException {
        return HKDFunction.expandLabel(
                hkdfAlgorithm,
                secret,
                version.getKeyLabel(),
                new byte[0],
                keyLength,
                ProtocolVersion.TLS13);
    }

    private static byte[] deriveIvFromSecret(
            QuicVersion version, HKDFAlgorithm hkdfAlgorithm, byte[] secret)
            throws CryptoException {
        return HKDFunction.expandLabel(
                hkdfAlgorithm,
                secret,
                version.getIvLabel(),
                new byte[0],
                IV_LENGTH,
                ProtocolVersion.TLS13);
    }

    private static byte[] deriveHeaderProtectionKeyFromSecret(
            QuicVersion version, HKDFAlgorithm hkdfAlgorithm, byte[] secret, int keyLength)
            throws CryptoException {
        return HKDFunction.expandLabel(
                hkdfAlgorithm,
                secret,
                version.getHeaderProtectionLabel(),
                new byte[0],
                keyLength,
                ProtocolVersion.TLS13);
    }
}
