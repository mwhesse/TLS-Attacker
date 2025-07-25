/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.crypto;

import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.protocol.constants.MacAlgorithm;
import de.rub.nds.protocol.exception.CryptoException;
import de.rub.nds.tlsattacker.core.workflow.chooser.Chooser;
import de.rub.nds.tlsattacker.transport.ConnectionEndType;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Arrays;

/** SSLUtils is a class with static methods that are supposed to calculate SSL-specific data. */
public class SSLUtils {

    private static final MessageFormat ILLEGAL_MAC_ALGORITHM =
            new MessageFormat(
                    "{0}, is not a valid MacAlgorithm for SSLv3, only MD5 and SHA-1 are available.");

    private static final byte[] MD5_PAD1 =
            DataConverter.hexStringToByteArray(StringUtils.repeat("36", 48));
    private static final byte[] MD5_PAD2 =
            DataConverter.hexStringToByteArray(StringUtils.repeat("5c", 48));
    private static final byte[] SHA_PAD1 =
            DataConverter.hexStringToByteArray(StringUtils.repeat("36", 40));
    private static final byte[] SHA_PAD2 =
            DataConverter.hexStringToByteArray(StringUtils.repeat("5c", 40));

    /**
     * Constants for masterSecret and keyBlock generation like 'A', 'BB', 'CC', as stated in
     * RFC-6101. See also {@link org.bouncycastle.tls.TlsUtils} Version 1.58
     */
    private static final byte[][] SSL3_CONST = genSSL3Const();

    /**
     * This method is borrowed from package-protected method
     * org.bouncycastle.tls.TlsUtils#genSSL3Const() Version 1.58
     *
     * @return the generated SSL3 consts
     */
    private static byte[][] genSSL3Const() {
        int n = 10;
        byte[][] arr = new byte[n][];
        for (int i = 0; i < n; i++) {
            byte[] b = new byte[i + 1];
            Arrays.fill(b, (byte) ('A' + i));
            arr[i] = b;
        }
        return arr;
    }

    /**
     * This method is borrowed from package-protected method
     * org.bouncycastle.tls.TlsUtils#calculateMasterSecret_SSL(byte[], byte[]) Version 1.58
     *
     * @param preMasterSecret the premastersecret
     * @param random The random bytes to use
     * @return master_secret
     */
    public static byte[] calculateMasterSecretSSL3(byte[] preMasterSecret, byte[] random) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            final MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            int md5Size = md5.getDigestLength();
            byte[] shaTmp = new byte[sha1.getDigestLength()];

            byte[] rval = new byte[md5Size * 3];
            int pos = 0;

            for (int i = 0; i < 3; ++i) {
                byte[] ssl3Const = SSL3_CONST[i];

                sha1.update(ssl3Const, 0, ssl3Const.length);
                sha1.update(preMasterSecret, 0, preMasterSecret.length);
                sha1.update(random, 0, random.length);
                sha1.digest(shaTmp, 0, shaTmp.length);

                md5.update(preMasterSecret, 0, preMasterSecret.length);
                md5.update(shaTmp, 0, shaTmp.length);
                md5.digest(rval, pos, md5.getDigestLength());

                pos += md5Size;
            }

            return rval;
        } catch (NoSuchAlgorithmException | DigestException e) {
            throw new CryptoException(
                    "Either MD5 or SHA-1 algorithm is not provided by the Execution-Environment, check your providers.",
                    e);
        }
    }

    /**
     * This method is borrowed from package-protected method
     * org.bouncycastle.tls.TlsUtils#calculateKeyBlock_SSL(byte[], byte[], int) Version 1.58
     *
     * @param masterSecret The master secret
     * @param random The Randombytes
     * @param size The size
     * @return masterSecret
     */
    public static byte[] calculateKeyBlockSSL3(byte[] masterSecret, byte[] random, int size) {
        try {

            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            int md5Size = md5.getDigestLength();
            byte[] shaTmp = new byte[sha1.getDigestLength()];
            byte[] tmp = new byte[size + md5Size];

            int i = 0;
            int pos = 0;
            while (pos < size) {
                if (SSL3_CONST.length <= i) {
                    // This should not happen with a normal random value
                    i = 0;
                }
                byte[] ssl3Const = SSL3_CONST[i];

                sha1.update(ssl3Const, 0, ssl3Const.length);
                sha1.update(masterSecret, 0, masterSecret.length);
                sha1.update(random, 0, random.length);
                sha1.digest(shaTmp, 0, shaTmp.length);

                md5.update(masterSecret, 0, masterSecret.length);
                md5.update(shaTmp, 0, shaTmp.length);
                md5.digest(tmp, pos, tmp.length - pos);

                pos += md5Size;
                ++i;
            }

            return Arrays.copyOfRange(tmp, 0, size);
        } catch (NoSuchAlgorithmException | DigestException e) {
            throw new CryptoException(
                    "Either MD5 or SHA-1 algorithm is not provided by the Execution-Environment, check your providers.",
                    e);
        }
    }

    /**
     * @param chooser The Chooser to use
     * @return 0x53525652 if ConnectionEndType.SERVER, 0x434C4E54 else. See RFC-6101: 5.6.9.
     *     Finished: enum { client(0x434C4E54), server(0x53525652) } Sender;
     */
    public static byte[] getSenderConstant(Chooser chooser) {
        return getSenderConstant(chooser.getConnectionEndType());
    }

    /**
     * @param connectionEndType The ConnectionEndType
     * @return 0x53525652 if ConnectionEndType.SERVER, 0x434C4E54 else. See RFC-6101: 5.6.9.
     *     Finished: enum { client(0x434C4E54), server(0x53525652) } Sender;
     */
    public static byte[] getSenderConstant(ConnectionEndType connectionEndType) {
        if (null == connectionEndType) {
            throw new IllegalArgumentException(
                    "The ConnectionEnd should be either of Type Client or Server but it is null");
        } else {
            switch (connectionEndType) {
                case SERVER:
                    return SSLUtils.Sender.SERVER.getValue();
                case CLIENT:
                    return SSLUtils.Sender.CLIENT.getValue();
                default:
                    throw new IllegalArgumentException(
                            "The ConnectionEnd should be either of Type Client or Server but it is "
                                    + connectionEndType);
            }
        }
    }

    /**
     * From RFC-6101:
     *
     * <p>pad_1: The character 0x36 repeated 48 times for MD5 or 40 times for SHA.
     *
     * @param macAlgorithm The macAlgorithm to use
     * @return the pad_1
     */
    public static byte[] getPad1(MacAlgorithm macAlgorithm) {
        if (null == macAlgorithm) {
            throw new IllegalArgumentException("MAC Algorithm must not be null");
        } else {
            switch (macAlgorithm) {
                case SSLMAC_MD5:
                    return MD5_PAD1.clone();
                case SSLMAC_SHA1:
                    return SHA_PAD1.clone();
                default:
                    throw new CryptoException(
                            ILLEGAL_MAC_ALGORITHM.format(macAlgorithm.getJavaName()));
            }
        }
    }

    /**
     * From RFC-6101: pad_2: The character 0x5c repeated 48 times for MD5 or 40 times for SHA.
     *
     * @param macAlgorithm The mac algorithm to use
     * @return pad_2
     */
    public static byte[] getPad2(MacAlgorithm macAlgorithm) {
        if (null == macAlgorithm) {
            throw new IllegalArgumentException("MAC Algorithm must not be null");
        } else {
            switch (macAlgorithm) {
                case SSLMAC_MD5:
                    return MD5_PAD2.clone();
                case SSLMAC_SHA1:
                    return SHA_PAD2.clone();
                default:
                    throw new CryptoException(
                            ILLEGAL_MAC_ALGORITHM.format(macAlgorithm.getJavaName()));
            }
        }
    }

    private static String getHashAlgorithm(MacAlgorithm macAlgorithm) {
        if (null == macAlgorithm) {
            throw new IllegalArgumentException("MAC Algorithm must not be null");
        } else {
            switch (macAlgorithm) {
                case SSLMAC_MD5:
                    return "MD5";
                case SSLMAC_SHA1:
                    return "SHA-1";
                default:
                    throw new CryptoException(
                            ILLEGAL_MAC_ALGORITHM.format(macAlgorithm.getJavaName()));
            }
        }
    }

    /**
     * From RFC-6101
     *
     * <p>The MAC is generated as:
     *
     * <p>hash(MAC_write_secret + pad_2 + hash(MAC_write_secret + pad_1 + seq_num +
     * SSLCompressed.type + SSLCompressed.length + SSLCompressed.fragment));
     *
     * @param input is the input for the chosen hashAlgorithm, which is (seq_num +
     *     SSLCompressed.type + SSLCompressed.length + SSLCompressed.fragment) from the fully
     *     defined hashFunction in the description.
     * @param macWriteSecret is MAC_write_secret from the defined hashFunction.
     * @param macAlgorithm should resolve to either MD5 or SHA-1
     * @return full calculated MAC-Bytes
     */
    public static byte[] calculateSSLMac(
            byte[] input, byte[] macWriteSecret, MacAlgorithm macAlgorithm) {
        final byte[] pad1 = SSLUtils.getPad1(macAlgorithm);
        final byte[] pad2 = SSLUtils.getPad2(macAlgorithm);
        try {
            final String hashName = getHashAlgorithm(macAlgorithm);
            final MessageDigest hashFunction = MessageDigest.getInstance(hashName);
            final byte[] innerInput = DataConverter.concatenate(macWriteSecret, pad1, input);
            final byte[] innerHash = hashFunction.digest(innerInput);
            final byte[] outerInput = DataConverter.concatenate(macWriteSecret, pad2, innerHash);
            final byte[] outerHash = hashFunction.digest(outerInput);
            return outerHash;
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(ILLEGAL_MAC_ALGORITHM.format(macAlgorithm.getJavaName()));
        }
    }

    /**
     * From RFC-6101: 5.6.8. Certificate Verify This message is used to provide explicit
     * verification of a client certificate. ... struct { Signature signature; } CertificateVerify;
     * CertificateVerify.signature.md5_hash MD5(master_secret + pad_2 + MD5(handshake_messages +
     * master_secret + pad_1)); Certificate.signature.sha_hash SHA(master_secret + pad_2 +
     * SHA(handshake_messages + master_secret + pad_1));
     *
     * @param handshakeMessages handshake_messages
     * @param masterSecret master_secret
     * @return CertificateVerify.signature
     */
    public static byte[] calculateSSLCertificateVerifySignature(
            byte[] handshakeMessages, byte[] masterSecret) {
        return calculateSSLMd5SHASignature(handshakeMessages, masterSecret);
    }

    /**
     * From RFC-6101: 5.6.9. Finished A finished message is always sent immediately after a change
     * cipher spec ... enum { client(0x434C4E54), server(0x53525652) } Sender; struct { opaque
     * md5_hash[16]; opaque sha_hash[20]; } Finished; md5_hash: MD5(master_secret + pad2 +
     * MD5(handshake_messages + Sender + master_secret + pad1)); sha_hash: SHA(master_secret + pad2
     * + SHA(handshake_messages + Sender + master_secret + pad1));
     *
     * @param handshakeMessages handshake_messages
     * @param masterSecret master_secret
     * @param connectionEndType Sender
     * @return Finished
     */
    public static byte[] calculateFinishedData(
            byte[] handshakeMessages, byte[] masterSecret, ConnectionEndType connectionEndType) {
        final byte[] input =
                DataConverter.concatenate(handshakeMessages, getSenderConstant(connectionEndType));
        return calculateSSLMd5SHASignature(input, masterSecret);
    }

    /**
     * Calculates the concatenation of a nested MD5 and a nested SHA-1 checksum like specified in
     * RFC-6101 for CertificateVerify- and Finished-Messages.
     *
     * @param input The input
     * @param masterSecret the master secret
     * @return the calculated ssl md5 sha signature
     */
    private static byte[] calculateSSLMd5SHASignature(byte[] input, byte[] masterSecret) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final MessageDigest sha = MessageDigest.getInstance("SHA-1");
            final byte[] innerMD5Content = DataConverter.concatenate(input, masterSecret, MD5_PAD1);
            final byte[] innerSHAContent = DataConverter.concatenate(input, masterSecret, SHA_PAD1);
            final byte[] innerMD5 = md5.digest(innerMD5Content);
            final byte[] innerSHA = sha.digest(innerSHAContent);
            final byte[] outerMD5Content =
                    DataConverter.concatenate(masterSecret, MD5_PAD2, innerMD5);
            final byte[] outerSHAContent =
                    DataConverter.concatenate(masterSecret, SHA_PAD2, innerSHA);
            final byte[] outerMD5 = md5.digest(outerMD5Content);
            final byte[] outerSHA = sha.digest(outerSHAContent);
            return DataConverter.concatenate(outerMD5, outerSHA);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(
                    "Either MD5 or SHA-1 algorithm is not provided by the Execution-Environment, check your providers.",
                    e);
        }
    }

    private SSLUtils() {}

    /**
     * From RFC-6101:
     *
     * <p>5.6.9.
     *
     * <p>Finished A finished message is always sent immediately after a change cipher spec message
     * to verify that the key exchange and authentication processes were successful. The finished
     * message is the first protected with the just-negotiated algorithms, keys, and secrets. No
     * acknowledgment of the finished message is required; parties may begin sending encrypted data
     * immediately after sending the finished message. Recipients of finished messages must verify
     * that the contents are correct.
     *
     * <p>enum { client(0x434C4E54), server(0x53525652) } Sender;
     */
    private static enum Sender {
        CLIENT("434C4E54"),
        SERVER("53525652");

        Sender(String hex) {
            value = DataConverter.hexStringToByteArray(hex);
        }

        private final byte[] value;

        public byte[] getValue() {
            return value.clone();
        }
    }
}
