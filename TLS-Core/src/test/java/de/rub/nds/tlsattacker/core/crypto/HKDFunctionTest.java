/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.protocol.exception.CryptoException;
import de.rub.nds.tlsattacker.core.constants.DigestAlgorithm;
import de.rub.nds.tlsattacker.core.constants.HKDFAlgorithm;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import org.junit.jupiter.api.Test;

public class HKDFunctionTest {

    /**
     * Test of extract and expand method, of class HKDFunction. Test cases from: <a
     * href="https://tools.ietf.org/html/rfc5869#appendix-A">RFC 5869 Appendix A</a>
     *
     * @throws CryptoException
     */
    @Test
    public void testExtractAndExpand() throws CryptoException {
        HKDFAlgorithm hkdfAlgorithm = HKDFAlgorithm.TLS_HKDF_SHA256;
        byte[] ikm =
                DataConverter.hexStringToByteArray("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
        byte[] salt = DataConverter.hexStringToByteArray("000102030405060708090a0b0c");
        byte[] info = DataConverter.hexStringToByteArray("f0f1f2f3f4f5f6f7f8f9");
        int outLen = 42;
        byte[] prk = HKDFunction.extract(hkdfAlgorithm, salt, ikm);
        byte[] okm = HKDFunction.expand(hkdfAlgorithm, prk, info, outLen);
        byte[] prkCorrect =
                DataConverter.hexStringToByteArray(
                        "077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5");
        byte[] okmCorrect =
                DataConverter.hexStringToByteArray(
                        "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865");
        assertArrayEquals(prk, prkCorrect);
        assertArrayEquals(okm, okmCorrect);

        ikm =
                DataConverter.hexStringToByteArray(
                        "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f");
        salt =
                DataConverter.hexStringToByteArray(
                        "606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeaf");
        info =
                DataConverter.hexStringToByteArray(
                        "b0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff");
        outLen = 82;
        prk = HKDFunction.extract(hkdfAlgorithm, salt, ikm);
        okm = HKDFunction.expand(hkdfAlgorithm, prk, info, outLen);
        prkCorrect =
                DataConverter.hexStringToByteArray(
                        "06a6b88c5853361a06104c9ceb35b45cef760014904671014a193f40c15fc244");
        okmCorrect =
                DataConverter.hexStringToByteArray(
                        "b11e398dc80327a1c8e7f78c596a49344f012eda2d4efad8a050cc4c19afa97c59045a99cac7827271cb41c65e590e09da3275600c2f09b8367793a9aca3db71cc30c58179ec3e87c14c01d5c1f3434f1d87");
        assertArrayEquals(prk, prkCorrect);
        assertArrayEquals(okm, okmCorrect);

        ikm = DataConverter.hexStringToByteArray("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
        salt = new byte[0];
        info = new byte[0];
        outLen = 42;
        prk = HKDFunction.extract(hkdfAlgorithm, salt, ikm);
        okm = HKDFunction.expand(hkdfAlgorithm, prk, info, outLen);
        prkCorrect =
                DataConverter.hexStringToByteArray(
                        "19ef24a32c717b167f33a91d6f648bdf96596776afdb6377ac434c1c293ccb04");
        okmCorrect =
                DataConverter.hexStringToByteArray(
                        "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8");
        assertArrayEquals(prk, prkCorrect);
        assertArrayEquals(okm, okmCorrect);
    }

    /**
     * Test of extract method, of class HKDFunction
     *
     * @throws de.rub.nds.protocol.exception.CryptoException
     */
    @Test
    public void testExtractNoSalt() throws CryptoException {
        HKDFAlgorithm hkdfAlgorithm = HKDFAlgorithm.TLS_HKDF_SHA256;
        byte[] salt = new byte[0];
        byte[] ikm =
                DataConverter.hexStringToByteArray(
                        "0000000000000000000000000000000000000000000000000000000000000000");

        byte[] result = HKDFunction.extract(hkdfAlgorithm, salt, ikm);
        byte[] resultCorrect =
                DataConverter.hexStringToByteArray(
                        "33ad0a1c607ec03b09e6cd9893680ce210adf300aa1f2660e1b22e10f170f92a");
        assertArrayEquals(result, resultCorrect);
    }

    /**
     * Test of extract method, of class HKDFunction
     *
     * @throws de.rub.nds.protocol.exception.CryptoException
     */
    @Test
    public void testExtractWithSalt() throws CryptoException {
        HKDFAlgorithm hkdfAlgorithm = HKDFAlgorithm.TLS_HKDF_SHA256;
        byte[] salt =
                DataConverter.hexStringToByteArray(
                        "33ad0a1c607ec03b09e6cd9893680ce210adf300aa1f2660e1b22e10f170f92a");
        byte[] ikm =
                DataConverter.hexStringToByteArray(
                        "c08acc73ba101d7fea86d223de32d9fc4948e145493680594b83b0a109f83649");

        byte[] result = HKDFunction.extract(hkdfAlgorithm, salt, ikm);
        byte[] resultCorrect =
                DataConverter.hexStringToByteArray(
                        "31168cad69862a80c6f6bfd42897d0fe23c406a12e652a8d3ae4217694f49844");
        assertArrayEquals(result, resultCorrect);
    }

    /**
     * Test of deriveSecret method, of class HKDFunction in TLS 1.3
     *
     * @throws de.rub.nds.protocol.exception.CryptoException
     */
    @Test
    public void testDeriveSecretTls13() throws CryptoException {
        HKDFAlgorithm hkdfAlgorithm = HKDFAlgorithm.TLS_HKDF_SHA256;
        String hashAlgorithm = DigestAlgorithm.SHA256.getJavaName();
        byte[] prk =
                DataConverter.hexStringToByteArray(
                        "33AD0A1C607EC03B09E6CD9893680CE210ADF300AA1F2660E1B22E10F170F92A");
        byte[] toHash = DataConverter.hexStringToByteArray("");
        String labelIn = HKDFunction.DERIVED;

        byte[] result =
                HKDFunction.deriveSecret(
                        hkdfAlgorithm, hashAlgorithm, prk, labelIn, toHash, ProtocolVersion.TLS13);
        byte[] resultCorrect =
                DataConverter.hexStringToByteArray(
                        "6F2615A108C702C5678F54FC9DBAB69716C076189C48250CEBEAC3576C3611BA");
        assertArrayEquals(result, resultCorrect);
    }

    /**
     * Test of deriveSecret method, of class HKDFunction in DTLS 1.3
     *
     * @throws de.rub.nds.protocol.exception.CryptoException
     */
    @Test
    public void testDeriveSecretDtls13() throws CryptoException {
        HKDFAlgorithm hkdfAlgorithm = HKDFAlgorithm.TLS_HKDF_SHA256;
        String hashAlgorithm = DigestAlgorithm.SHA256.getJavaName();
        byte[] prk =
                DataConverter.hexStringToByteArray(
                        "33AD0A1C607EC03B09E6CD9893680CE210ADF300AA1F2660E1B22E10F170F92A");
        byte[] toHash = DataConverter.hexStringToByteArray("");
        String labelIn = HKDFunction.DERIVED;

        byte[] result =
                HKDFunction.deriveSecret(
                        hkdfAlgorithm, hashAlgorithm, prk, labelIn, toHash, ProtocolVersion.DTLS13);
        byte[] resultCorrect =
                DataConverter.hexStringToByteArray(
                        "B17BCE9451EE8BC6E8AEBC0AA9E98295677A4A6A91F78440833146E465F2FF95");
        assertArrayEquals(result, resultCorrect);
    }

    /**
     * Test of expandLabel method, of class HKDFunction for TLS 1.3
     *
     * @throws de.rub.nds.protocol.exception.CryptoException
     */
    @Test
    public void testExpandLabelTls13() throws CryptoException {
        HKDFAlgorithm hkdfAlgorithm = HKDFAlgorithm.TLS_HKDF_SHA256;
        byte[] prk =
                DataConverter.hexStringToByteArray(
                        "E056D47C7DB9C04BBECE6AC9525163DE72B7D25B6B0899366F8FA741A5C01709");
        byte[] hashValue = DataConverter.hexStringToByteArray("");
        String labelIn = HKDFunction.KEY;
        int outLen = 16;

        byte[] result =
                HKDFunction.expandLabel(
                        hkdfAlgorithm, prk, labelIn, hashValue, outLen, ProtocolVersion.TLS13);
        byte[] resultCorrect =
                DataConverter.hexStringToByteArray("04C5DA6EC39FC1653E085FA83E51C6AF");
        assertArrayEquals(result, resultCorrect);
    }

    /**
     * Test of expandLabel method, of class HKDFunction for DTLS 1.3
     *
     * @throws de.rub.nds.protocol.exception.CryptoException
     */
    @Test
    public void testExpandLabelDtls13() throws CryptoException {
        HKDFAlgorithm hkdfAlgorithm = HKDFAlgorithm.TLS_HKDF_SHA256;
        byte[] prk =
                DataConverter.hexStringToByteArray(
                        "E056D47C7DB9C04BBECE6AC9525163DE72B7D25B6B0899366F8FA741A5C01709");
        byte[] hashValue = DataConverter.hexStringToByteArray("");
        String labelIn = HKDFunction.KEY;
        int outLen = 16;

        byte[] result =
                HKDFunction.expandLabel(
                        hkdfAlgorithm, prk, labelIn, hashValue, outLen, ProtocolVersion.DTLS13);
        byte[] resultCorrect =
                DataConverter.hexStringToByteArray("EDAE449DA1ABB0929AA80268FA0D4E25");
        assertArrayEquals(result, resultCorrect);
    }

    @Test
    public void testExtractHandshake() throws CryptoException {
        byte[] expand =
                HKDFunction.extract(
                        HKDFAlgorithm.TLS_HKDF_SHA256,
                        DataConverter.hexStringToByteArray(
                                "6f2615a108c702c5678f54fc9dbab69716c076189c48250cebeac3576c3611ba"),
                        DataConverter.hexStringToByteArray(
                                "8151d1464c1b55533623b9c2246a6a0e6e7e185063e14afdaff0b6e1c61a8642"));
        assertArrayEquals(
                DataConverter.hexStringToByteArray(
                        "5b4f965df03c682c46e6ee86c311636615a1d2bbb24345c25205953c879e8d06"),
                expand);
    }

    @Test
    public void testExtractEarly() throws CryptoException {
        byte[] expand =
                HKDFunction.extract(
                        HKDFAlgorithm.TLS_HKDF_SHA256,
                        DataConverter.hexStringToByteArray(""),
                        DataConverter.hexStringToByteArray(
                                "0000000000000000000000000000000000000000000000000000000000000000"));
        assertArrayEquals(
                DataConverter.hexStringToByteArray(
                        "33ad0a1c607ec03b09e6cd9893680ce210adf300aa1f2660e1b22e10f170f92a"),
                expand);
    }

    @Test
    public void testExpand() throws CryptoException {
        byte[] expand =
                HKDFunction.expand(
                        HKDFAlgorithm.TLS_HKDF_SHA256,
                        DataConverter.hexStringToByteArray(
                                "3b7a839c239ef2bf0b7305a0e0c4e5a8c6c69330a753b308f5e3a83aa2ef6979"),
                        DataConverter.hexStringToByteArray("001009746c733133206b657900"),
                        16);
        assertArrayEquals(
                DataConverter.hexStringToByteArray("c66cb1aec519df44c91e10995511ac8b"), expand);
    }
}
