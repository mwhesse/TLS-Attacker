/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.layer.context;

import de.rub.nds.modifiablevariable.util.BadRandom;
import de.rub.nds.modifiablevariable.util.DataConverter;
import de.rub.nds.protocol.crypto.ec.Point;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.config.delegate.CertificateDelegate;
import de.rub.nds.tlsattacker.core.constants.AuthzDataFormat;
import de.rub.nds.tlsattacker.core.constants.CertificateStatusRequestType;
import de.rub.nds.tlsattacker.core.constants.CertificateType;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ClientCertificateType;
import de.rub.nds.tlsattacker.core.constants.CompressionMethod;
import de.rub.nds.tlsattacker.core.constants.ECPointFormat;
import de.rub.nds.tlsattacker.core.constants.EsniDnsKeyRecordVersion;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.constants.GOSTCurve;
import de.rub.nds.tlsattacker.core.constants.HeartbeatMode;
import de.rub.nds.tlsattacker.core.constants.MaxFragmentLength;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.PRFAlgorithm;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.constants.PskKeyExchangeMode;
import de.rub.nds.tlsattacker.core.constants.SSL2CipherSuite;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SrtpProtectionProfile;
import de.rub.nds.tlsattacker.core.constants.Tls13KeySetType;
import de.rub.nds.tlsattacker.core.constants.TokenBindingKeyParameters;
import de.rub.nds.tlsattacker.core.constants.TokenBindingVersion;
import de.rub.nds.tlsattacker.core.constants.UserMappingExtensionHintType;
import de.rub.nds.tlsattacker.core.crypto.MessageDigestCollector;
import de.rub.nds.tlsattacker.core.dtls.DtlsHandshakeMessageFragment;
import de.rub.nds.tlsattacker.core.layer.impl.DtlsFragmentLayer;
import de.rub.nds.tlsattacker.core.layer.impl.RecordLayer;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.EncryptedClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ack.RecordNumber;
import de.rub.nds.tlsattacker.core.protocol.message.extension.EchConfig;
import de.rub.nds.tlsattacker.core.protocol.message.extension.cachedinfo.CachedObject;
import de.rub.nds.tlsattacker.core.protocol.message.extension.keyshare.KeyShareEntry;
import de.rub.nds.tlsattacker.core.protocol.message.extension.keyshare.KeyShareStoreEntry;
import de.rub.nds.tlsattacker.core.protocol.message.extension.psk.PskSet;
import de.rub.nds.tlsattacker.core.protocol.message.extension.sni.SNIEntry;
import de.rub.nds.tlsattacker.core.protocol.message.extension.statusrequestv2.RequestItemV2;
import de.rub.nds.tlsattacker.core.protocol.message.extension.trustedauthority.TrustedAuthority;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.record.cipher.RecordNullCipher;
import de.rub.nds.tlsattacker.core.record.cipher.cryptohelper.KeySet;
import de.rub.nds.tlsattacker.core.state.Context;
import de.rub.nds.tlsattacker.core.state.Keylogfile;
import de.rub.nds.tlsattacker.core.state.session.IdSession;
import de.rub.nds.tlsattacker.core.state.session.Session;
import de.rub.nds.tlsattacker.core.state.session.TicketSession;
import de.rub.nds.tlsattacker.core.workflow.chooser.Chooser;
import de.rub.nds.tlsattacker.transport.ConnectionEndType;
import de.rub.nds.x509attacker.config.X509CertificateConfig;
import de.rub.nds.x509attacker.context.X509Context;
import de.rub.nds.x509attacker.x509.X509CertificateChain;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Holds all runtime variables of the TLSLayer. */
@XmlAccessorType(XmlAccessType.FIELD)
public class TlsContext extends LayerContext {

    private List<Session> sessionList;

    private Keylogfile keylogfile;

    /** Shared key established during the handshake. */
    private byte[] handshakeSecret;

    private byte[] clientHandshakeTrafficSecret;

    private byte[] serverHandshakeTrafficSecret;

    /** shared key established during the handshake */
    private byte[] clientApplicationTrafficSecret;

    /** shared key established during the handshake */
    private byte[] serverApplicationTrafficSecret;

    /** Early traffic secret used to encrypt early data. */
    private byte[] clientEarlyTrafficSecret;

    /** Handshake traffic secret in case it needs to be precalculated during early data * */
    private KeySet keySetHandshake;

    /** CipherSuite used for early data. */
    private CipherSuite earlyDataCipherSuite;

    /** EarlySecret used to derive EarlyTrafficSecret and more. */
    private byte[] earlySecret;

    /** The known TLS 1.3 PSK-Sets. */
    private List<PskSet> pskSets;

    /** The selected Pre Shared key. */
    private byte[] psk;

    /** The selected earlyData PSK. */
    private byte[] earlyDataPsk;

    /** Identity of the PSK used for earlyData. */
    private byte[] earlyDataPSKIdentity;

    /** Identity of the PSK used for earlyData. */
    private int selectedIdentityIndex;

    /** The Client's chosen Kex-Modes. */
    private List<PskKeyExchangeMode> clientPskKeyExchangeModes;

    /** Maximum number of bytes to transmit as early-data. */
    private Integer maxEarlyDataSize;

    /** Master secret established during the handshake. */
    private byte[] masterSecret;

    /** Cleartext portion of the master secret for SSLv2 export ciphers. */
    private byte[] clearKey;

    /** Premaster secret established during the handshake. */
    private byte[] preMasterSecret;

    /** Master secret established during the handshake. */
    private byte[] resumptionMasterSecret;

    /** Client Extended Random used in Extended Random Extension */
    private byte[] clientExtendedRandom;

    /** Server Extended Random used in Extended Random Extension */
    private byte[] serverExtendedRandom;

    /** Client random, including unix time. */
    private byte[] clientRandom;

    /** Server random, including unix time. */
    private byte[] serverRandom;

    /** Selected cipher suite. */
    private CipherSuite selectedCipherSuite = null;

    /*
     * (Preferred) cipher suite for SSLv2.
     */
    private SSL2CipherSuite ssl2CipherSuite = null;

    /** Selected compression algorithm. */
    private CompressionMethod selectedCompressionMethod;

    /** Server session ID. */
    private byte[] serverSessionId;

    /** Client session ID. */
    private byte[] clientSessionId;

    /**
     * Initialization vector for SSLv2 with block ciphers. Unlike for SSLv3 and TLS, this is
     * explicitly transmitted in the handshake and cannot be derived from other data.
     */
    private byte[] ssl2Iv;

    /** Server certificate parsed from the server certificate message. */
    private X509CertificateChain serverCertificateChain;

    /** Client certificate parsed from the client certificate message. */
    private X509CertificateChain clientCertificateChain;

    /** Collects messages for computation of the Finished and CertificateVerify hashes */
    private MessageDigestCollector digest;

    private byte[] dtlsCookie;

    private byte[] extensionCookie;

    private ProtocolVersion selectedProtocolVersion;

    private ProtocolVersion highestClientProtocolVersion;

    private List<CipherSuite> clientSupportedCipherSuites;

    private List<CompressionMethod> clientSupportedCompressions;

    private List<SignatureAndHashAlgorithm> serverSupportedSignatureAndHashAlgorithms;

    private List<SignatureAndHashAlgorithm> clientSupportedSignatureAndHashAlgorithms;

    private List<SignatureAndHashAlgorithm> clientSupportedCertificateSignAlgorithms;

    private List<SignatureAndHashAlgorithm> serverSupportedCertificateSignAlgorithms;

    private HeartbeatMode heartbeatMode;

    private boolean cachedInfoExtensionClientState;

    private List<CachedObject> cachedInfoExtensionObjects;

    private List<RequestItemV2> statusRequestV2RequestList;

    private CertificateType selectedClientCertificateType;

    private CertificateType selectedServerCertificateType;

    /** These are the padding bytes as used in the padding extension. */
    private byte[] paddingExtensionBytes;

    /** The renegotiation info of the RenegotiationInfo extension. */
    private byte[] renegotiationInfo;

    /** The requestContext from the CertificateRequest message in TLS 1.3. */
    private byte[] certificateRequestContext;

    /** Timestamp of the SignedCertificateTimestamp extension. */
    private byte[] signedCertificateTimestamp;

    /** This is the request type of the CertificateStatusRequest extension */
    private CertificateStatusRequestType certificateStatusRequestExtensionRequestType;

    /** This is the responder ID list of the CertificateStatusRequest extension */
    private byte[] certificateStatusRequestExtensionResponderIDList;

    /** This is the request extension of the CertificateStatusRequest extension */
    private byte[] certificateStatusRequestExtensionRequestExtension;

    /** This is the user identifier of the SRP extension */
    private byte[] secureRemotePasswordExtensionIdentifier;

    /** This is the master key identifier of the SRTP extension */
    private byte[] secureRealTimeProtocolMasterKeyIdentifier;

    /** These are the protection profiles supported by the client */
    private List<SrtpProtectionProfile> clientSupportedSrtpProtectionProfiles;

    private SrtpProtectionProfile selectedSrtpProtectionProfile;

    /** User mapping extension hint type */
    private UserMappingExtensionHintType userMappingExtensionHintType;

    /** Client authz extension data format list */
    private List<AuthzDataFormat> clientAuthzDataFormatList;

    /** Server authz extension data format list */
    private List<AuthzDataFormat> serverAuthzDataFormatList;

    /** This is only the ephemeral generator */
    private BigInteger serverEphemeralDhGenerator;

    /** This is only the ephemeral modulus */
    private BigInteger serverEphemeralDhModulus;

    /** This is only the ephemeral private key */
    private BigInteger serverEphemeralDhPrivateKey;

    /** This is only the ephemeral public key */
    private BigInteger serverEphemeralDhPublicKey;

    /** This is only the 'ephemeral' private key */
    private BigInteger clientEphemeralDhPrivateKey;

    /** This is only the ephemeral modulus */
    private BigInteger clientEphemeralDhPublicKey;

    private BigInteger srpModulus;

    private BigInteger srpGenerator;

    private BigInteger serverSRPPublicKey;

    private BigInteger serverSRPPrivateKey;

    private BigInteger clientSRPPublicKey;

    private BigInteger clientSRPPrivateKey;

    private byte[] srpServerSalt;

    private byte[] srpPassword;

    private byte[] srpIdentity;

    private byte[] pskKey;

    private byte[] pskIdentity;

    private byte[] pskIdentityHint;

    private NamedGroup selectedGroup;

    /** This is only the 'ephemeral' key */
    private Point clientEphemeralEcPublicKey;

    /** This is only the 'ephemeral' key */
    private Point serverEphemeralEcPublicKey;

    /** This is only the 'ephemeral' key */
    private BigInteger serverEphemeralEcPrivateKey;

    /** This is only the 'ephemeral' key */
    private BigInteger clientEphemeralEcPrivateKey;

    /** This is only the 'ephemeral' key for RSA export */
    private BigInteger serverEphemeralRsaExportModulus;

    /** This is only the 'ephemeral' key for RSA export */
    private BigInteger serverEphemeralRsaExportPublicKey;

    /** This is only the 'ephemeral' key for RSA export */
    private BigInteger serverEphemeralRsaExportPrivateKey;

    private List<NamedGroup> clientNamedGroupsList;

    private List<NamedGroup> serverNamedGroupsList;

    private List<ECPointFormat> clientPointFormatsList;

    private List<ECPointFormat> serverPointFormatsList;

    private boolean receivedFatalAlert = false;

    private boolean receivedMessageWithWrongTls13KeyType = false;

    private List<ClientCertificateType> clientCertificateTypes;

    private byte[] distinguishedNames;

    private ProtocolVersion lastRecordVersion;

    private List<SNIEntry> clientSNIEntryList;

    private List<KeyShareStoreEntry> clientKeyShareStoreEntryList;

    private KeyShareStoreEntry serverKeyShareStoreEntry;

    private GOSTCurve selectedGostCurve;

    /** the currently used type of keySet by the client */
    private Tls13KeySetType activeClientKeySetType = Tls13KeySetType.NONE;

    /** the currently used type of keySet by the server */
    private Tls13KeySetType activeServerKeySetType = Tls13KeySetType.NONE;

    private Set<Integer> dtlsReceivedHandshakeMessageSequences;

    private Set<Integer> dtlsReceivedChangeCipherSpecEpochs;

    /** supported protocol versions */
    private List<ProtocolVersion> clientSupportedProtocolVersions;

    private TokenBindingVersion tokenBindingVersion;

    private List<TokenBindingKeyParameters> tokenBindingKeyParameters;

    /** Whether Token Binding negotiation completed successful or not. */
    private boolean tokenBindingNegotiatedSuccessfully = false;

    private List<String> proposedAlpnProtocols;

    private String selectedAlpnProtocol;

    private List<CertificateType> certificateTypeClientDesiredTypes;

    private List<CertificateType> serverCertificateTypeDesiredTypes;

    private List<CertificateType> clientCertificateTypeDesiredTypes;

    private List<TrustedAuthority> trustedCaIndicationExtensionCas;

    private SignatureAndHashAlgorithm selectedSignatureAndHashAlgorithm;

    private PRFAlgorithm prfAlgorithm;

    private ProtocolVersion highestProtocolVersion;

    private Boolean clientAuthentication;

    private String clientPWDUsername;

    private byte[] serverPWDSalt;

    /** Password Element for TLS_ECCPWD */
    private Point pwdPasswordElement;

    private BigInteger clientPWDPrivate;

    private BigInteger serverPWDPrivate;

    private BigInteger serverPWDScalar;

    private Point serverPWDElement;

    /**
     * Last application message data received/send by this context. This is especially useful for
     * forwarding application messages via ForwardAction.
     */
    private byte[] lastHandledApplicationMessageData;

    private byte[] lastClientVerifyData;

    private byte[] lastServerVerifyData;

    private byte[] lastClientHello;

    private Random random;

    private LinkedList<ProtocolMessage> messageBuffer;

    private LinkedList<Record> recordBuffer;

    private LinkedList<DtlsHandshakeMessageFragment> fragmentBuffer;

    /** Contains the TLS extensions proposed by the client. */
    private final EnumSet<ExtensionType> proposedExtensionSet = EnumSet.noneOf(ExtensionType.class);

    /** Contains the TLS extensions proposed by the server. */
    private final EnumSet<ExtensionType> negotiatedExtensionSet =
            EnumSet.noneOf(ExtensionType.class);

    /**
     * The "secure_renegotiation" flag of the Renegotiation Indication Extension as defined in
     * RFC5746. Indicates whether secure renegotiation is in use for the connection. Note that this
     * flag reflects a connection "state" and differs from
     * isProposedTlsExtensions*(ExtensionType.RENEGOTIATION_INFO). The latter merely says that the
     * extension was send by client or server.
     */
    private boolean secureRenegotiation = false;

    /**
     * Whether to use the extended master secret or not. This flag is set if the EMS extension was
     * send by both peers. Note that this flag reflects a connection "state" and differs from
     * isProposedTlsExtensions*(ExtensionType. EXTENDED_MASTER_SECRET). The latter merely says that
     * the extension was sent by client or server.
     */
    private boolean useExtendedMasterSecret;

    private boolean receivedTransportHandlerException = false;

    /** Experimental flag for forensics and reparsing */
    private boolean reversePrepareAfterParse = false;

    /** An EchConfig object that holds information regarding the server's ECH capabilities */
    private EchConfig echConfig;

    /** innerClientHello if present */
    private ClientHelloMessage innerClientHello;

    /** Outer clientHello needs to be saved for server handling of ECH */
    private EncryptedClientHelloMessage outerClientHello;

    /** Indicates whether the server supports ECH */
    private boolean supportsECH;

    /** KeyShare object for ECH holding clients public key etc. */
    private KeyShareEntry echClientKeyShareEntry;

    /** KeyShare object for ECH holding clients public key etc. */
    private KeyShareEntry echServerKeyShareEntry;

    /** Nonce sent by the Client in the EncryptedServerNameIndication extension */
    private byte[] esniClientNonce;

    /** Nonce sent by the Server in the EncryptedServerNameIndication extension */
    private byte[] esniServerNonce;

    /** Contains the keyRecord for the EncryptedServerNameIndication extension */
    private byte[] esniRecordBytes;

    private EsniDnsKeyRecordVersion esniRecordVersion;

    private byte[] esniRecordChecksum;

    private byte[] publicName;

    private List<KeyShareStoreEntry> esniServerKeyShareEntries;

    private List<CipherSuite> esniServerCipherSuites;

    private Integer esniPaddedLength;

    private Long esniNotBefore;

    private Long esniNotAfter;

    /**
     * Both methods of limiting record size as defined in RFC 3546 (MaximumFragmentLength extension)
     * and RFC 8449 (RecordSizeLimit extension)
     */
    private MaxFragmentLength maxFragmentLength;

    private Integer outboundRecordSizeLimit;
    private Integer inboundRecordSizeLimit;

    private Integer peerReceiveLimit;

    private List<byte[]> writeConnectionIds = new ArrayList<>();

    private Integer writeConnectionIdIndex;

    private List<byte[]> readConnectionIDs = new ArrayList<>();

    private Integer readConnectionIdIndex;

    private Integer numberOfRequestedConnectionIds;

    private List<RecordNumber> dtls13AcknowledgedRecords;

    private List<RecordNumber> dtls13ReceivedAcknowledgedRecords;

    private X509Context clientX509Context;
    private X509Context serverX509Context;

    /**
     * This constructor assumes that the config holds exactly one connection end. This is usually
     * used when working with the default connection end in single context scenarios.
     *
     * @param context The outer context that contains this layer specific context
     */
    public TlsContext(Context context) {
        super(context);
        X509CertificateConfig certConfig =
                context.getConfig()
                        .getCertificateChainConfig()
                        .get(CertificateDelegate.PREDEFINED_LEAF_CERT_INDEX);
        clientX509Context = new X509Context(certConfig);
        serverX509Context = new X509Context(certConfig);
        context.setTlsContext(this);
        init();
    }

    public List<SrtpProtectionProfile> getClientSupportedSrtpProtectionProfiles() {
        return this.clientSupportedSrtpProtectionProfiles;
    }

    public void setClientSupportedSrtpProtectionProfiles(
            List<SrtpProtectionProfile> clientSupportedSrtpProtectionProfiles) {
        this.clientSupportedSrtpProtectionProfiles = clientSupportedSrtpProtectionProfiles;
    }

    public SrtpProtectionProfile getSelectedSrtpProtectionProfile() {
        return this.selectedSrtpProtectionProfile;
    }

    public void setSelectedSrtpProtectionProfile(
            SrtpProtectionProfile selectedSrtpProtectionProfile) {
        this.selectedSrtpProtectionProfile = selectedSrtpProtectionProfile;
    }

    public void resetTalkingX509Context() {
        if (getTalkingConnectionEndType() == ConnectionEndType.CLIENT) {
            clientX509Context = new X509Context();
        } else {
            serverX509Context = new X509Context();
        }
    }

    public X509Context getTalkingX509Context() {
        if (getTalkingConnectionEndType() == ConnectionEndType.CLIENT) {
            return clientX509Context;
        } else {
            return serverX509Context;
        }
    }

    public X509Context getPeerX509Context() {
        if (getTalkingConnectionEndType() != ConnectionEndType.CLIENT) {
            return clientX509Context;
        } else {
            return serverX509Context;
        }
    }

    public void setTalkingX509Context(X509Context context) {
        if (getTalkingConnectionEndType() == ConnectionEndType.CLIENT) {
            clientX509Context = context;
        } else {
            serverX509Context = context;
        }
    }

    public X509Context getClientX509Context() {
        return clientX509Context;
    }

    public void setClientX509Context(X509Context clientX509Context) {
        this.clientX509Context = clientX509Context;
    }

    public X509Context getServerX509Context() {
        return serverX509Context;
    }

    public void setServerX509Context(X509Context serverX509Context) {
        this.serverX509Context = serverX509Context;
    }

    public void init() {
        digest = new MessageDigestCollector();
        sessionList = new LinkedList<>();
        if (getConfig().isStealthMode()) {
            random = new Random();
        } else {
            random = new Random(0);
        }
        messageBuffer = new LinkedList<>();
        recordBuffer = new LinkedList<>();
        fragmentBuffer = new LinkedList<>();
        dtlsReceivedHandshakeMessageSequences = new HashSet<>();
        dtlsReceivedChangeCipherSpecEpochs = new HashSet<>();
        readConnectionIdIndex = 0;
        writeConnectionIdIndex = 0;
        keylogfile = new Keylogfile(this);
    }

    public Chooser getChooser() {
        return getContext().getChooser();
    }

    public CertificateType getSelectedClientCertificateType() {
        return selectedClientCertificateType;
    }

    public void setSelectedClientCertificateType(CertificateType selectedClientCertificateType) {
        this.selectedClientCertificateType = selectedClientCertificateType;
    }

    public CertificateType getSelectedServerCertificateType() {
        return selectedServerCertificateType;
    }

    public void setSelectedServerCertificateType(CertificateType selectedServerCertificateType) {
        this.selectedServerCertificateType = selectedServerCertificateType;
    }

    public boolean isReversePrepareAfterParse() {
        return reversePrepareAfterParse;
    }

    public void setReversePrepareAfterParse(boolean reversePrepareAfterParse) {
        this.reversePrepareAfterParse = reversePrepareAfterParse;
    }

    public LinkedList<ProtocolMessage> getMessageBuffer() {
        return messageBuffer;
    }

    public void setMessageBuffer(LinkedList<ProtocolMessage> messageBuffer) {
        this.messageBuffer = messageBuffer;
    }

    public LinkedList<Record> getRecordBuffer() {
        return recordBuffer;
    }

    public void setRecordBuffer(LinkedList<Record> recordBuffer) {
        this.recordBuffer = recordBuffer;
    }

    public LinkedList<DtlsHandshakeMessageFragment> getFragmentBuffer() {
        return fragmentBuffer;
    }

    public void setFragmentBuffer(LinkedList<DtlsHandshakeMessageFragment> fragmentBuffer) {
        this.fragmentBuffer = fragmentBuffer;
    }

    public Session getIdSession(byte[] sessionId) {
        for (Session session : sessionList) {
            if (session.isIdSession() && Arrays.equals(((IdSession) session).getId(), sessionId)) {
                return session;
            }
        }
        return null;
    }

    public boolean hasSession(byte[] sessionId) {
        return getIdSession(sessionId) != null;
    }

    public byte[] getLatestSessionTicket() {
        for (int i = sessionList.size() - 1; i >= 0; i--) {
            Session session = sessionList.get(i);
            if (session.isTicketSession()) {
                return ((TicketSession) session).getTicket();
            }
        }
        return null;
    }

    public void addNewSession(Session session) {
        sessionList.add(session);
    }

    public List<Session> getSessionList() {
        return sessionList;
    }

    public void setSessionList(List<Session> sessionList) {
        this.sessionList = sessionList;
    }

    public byte[] getLastClientVerifyData() {
        return lastClientVerifyData;
    }

    public void setLastClientVerifyData(byte[] lastClientVerifyData) {
        this.lastClientVerifyData = lastClientVerifyData;
    }

    public byte[] getLastServerVerifyData() {
        return lastServerVerifyData;
    }

    public void setLastServerVerifyData(byte[] lastServerVerifyData) {
        this.lastServerVerifyData = lastServerVerifyData;
    }

    public List<CertificateType> getCertificateTypeClientDesiredTypes() {
        return certificateTypeClientDesiredTypes;
    }

    public void setCertificateTypeClientDesiredTypes(
            List<CertificateType> certificateTypeClientDesiredTypes) {
        this.certificateTypeClientDesiredTypes = certificateTypeClientDesiredTypes;
    }

    public boolean isSecureRenegotiation() {
        return secureRenegotiation;
    }

    public void setSecureRenegotiation(boolean secureRenegotiation) {
        this.secureRenegotiation = secureRenegotiation;
    }

    public List<ProtocolVersion> getClientSupportedProtocolVersions() {
        return clientSupportedProtocolVersions;
    }

    public void setClientSupportedProtocolVersions(
            List<ProtocolVersion> clientSupportedProtocolVersions) {
        this.clientSupportedProtocolVersions = clientSupportedProtocolVersions;
    }

    public void setClientSupportedProtocolVersions(
            ProtocolVersion... clientSupportedProtocolVersions) {
        this.clientSupportedProtocolVersions =
                new ArrayList<>(Arrays.asList(clientSupportedProtocolVersions));
    }

    public NamedGroup getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(NamedGroup selectedCurve) {
        this.selectedGroup = selectedCurve;
    }

    public BigInteger getSRPGenerator() {
        return srpGenerator;
    }

    public void setSRPGenerator(BigInteger srpGenerator) {
        this.srpGenerator = srpGenerator;
    }

    public BigInteger getSRPModulus() {
        return srpModulus;
    }

    public void setSRPModulus(BigInteger srpModulus) {
        this.srpModulus = srpModulus;
    }

    public byte[] getPSKIdentity() {
        return pskIdentity;
    }

    public void setPSKIdentity(byte[] pskIdentity) {
        this.pskIdentity = pskIdentity;
    }

    public byte[] getPSKIdentityHint() {
        return pskIdentityHint;
    }

    public void setPSKIdentityHint(byte[] pskIdentityHint) {
        this.pskIdentityHint = pskIdentityHint;
    }

    public BigInteger getServerSRPPublicKey() {
        return serverSRPPublicKey;
    }

    public void setServerSRPPublicKey(BigInteger serverSRPPublicKey) {
        this.serverSRPPublicKey = serverSRPPublicKey;
    }

    public BigInteger getServerSRPPrivateKey() {
        return serverSRPPrivateKey;
    }

    public void setServerSRPPrivateKey(BigInteger serverSRPPrivateKey) {
        this.serverSRPPrivateKey = serverSRPPrivateKey;
    }

    public BigInteger getClientSRPPublicKey() {
        return clientSRPPublicKey;
    }

    public void setClientSRPPublicKey(BigInteger clientSRPPublicKey) {
        this.clientSRPPublicKey = clientSRPPublicKey;
    }

    public BigInteger getClientSRPPrivateKey() {
        return clientSRPPrivateKey;
    }

    public void setClientSRPPrivateKey(BigInteger clientSRPPrivateKey) {
        this.clientSRPPrivateKey = clientSRPPrivateKey;
    }

    public byte[] getSRPServerSalt() {
        return srpServerSalt;
    }

    public void setSRPServerSalt(byte[] srpServerSalt) {
        this.srpServerSalt = srpServerSalt;
    }

    public byte[] getPSKKey() {
        return pskKey;
    }

    public void setPSKKey(byte[] pskKey) {
        this.pskKey = pskKey;
    }

    public byte[] getSRPPassword() {
        return srpPassword;
    }

    public void setSRPPassword(byte[] srpPassword) {
        this.srpPassword = srpPassword;
    }

    public byte[] getSRPIdentity() {
        return srpIdentity;
    }

    public void setSRPIdentity(byte[] srpIdentity) {
        this.srpIdentity = srpIdentity;
    }

    public GOSTCurve getServerGost01Curve() {
        return selectedGostCurve;
    }

    public void setServerGost01Curve(GOSTCurve serverGost01Curve) {
        this.selectedGostCurve = serverGost01Curve;
    }

    public SignatureAndHashAlgorithm getSelectedSignatureAndHashAlgorithm() {
        return selectedSignatureAndHashAlgorithm;
    }

    public void setSelectedSignatureAndHashAlgorithm(
            SignatureAndHashAlgorithm selectedSignatureAndHashAlgorithm) {
        this.selectedSignatureAndHashAlgorithm = selectedSignatureAndHashAlgorithm;
    }

    public List<NamedGroup> getClientNamedGroupsList() {
        return clientNamedGroupsList;
    }

    public void setClientNamedGroupsList(List<NamedGroup> clientNamedGroupsList) {
        this.clientNamedGroupsList = clientNamedGroupsList;
    }

    public void setClientNamedGroupsList(NamedGroup... clientNamedGroupsList) {
        this.clientNamedGroupsList = new ArrayList<>(Arrays.asList(clientNamedGroupsList));
    }

    public List<NamedGroup> getServerNamedGroupsList() {
        return serverNamedGroupsList;
    }

    public void setServerNamedGroupsList(List<NamedGroup> serverNamedGroupsList) {
        this.serverNamedGroupsList = serverNamedGroupsList;
    }

    public void setServerNamedGroupsList(NamedGroup... serverNamedGroupsList) {
        this.serverNamedGroupsList = new ArrayList<>(Arrays.asList(serverNamedGroupsList));
    }

    public List<ECPointFormat> getServerPointFormatsList() {
        return serverPointFormatsList;
    }

    public void setServerPointFormatsList(List<ECPointFormat> serverPointFormatsList) {
        this.serverPointFormatsList = serverPointFormatsList;
    }

    public void setServerPointFormatsList(ECPointFormat... serverPointFormatsList) {
        this.serverPointFormatsList = new ArrayList<>(Arrays.asList(serverPointFormatsList));
    }

    public List<SignatureAndHashAlgorithm> getClientSupportedSignatureAndHashAlgorithms() {
        return clientSupportedSignatureAndHashAlgorithms;
    }

    public void setClientSupportedSignatureAndHashAlgorithms(
            List<SignatureAndHashAlgorithm> clientSupportedSignatureAndHashAlgorithms) {
        this.clientSupportedSignatureAndHashAlgorithms = clientSupportedSignatureAndHashAlgorithms;
    }

    public void setClientSupportedSignatureAndHashAlgorithms(
            SignatureAndHashAlgorithm... clientSupportedSignatureAndHashAlgorithms) {
        this.clientSupportedSignatureAndHashAlgorithms =
                new ArrayList<>(Arrays.asList(clientSupportedSignatureAndHashAlgorithms));
    }

    public List<SignatureAndHashAlgorithm> getClientSupportedCertificateSignAlgorithms() {
        return clientSupportedCertificateSignAlgorithms;
    }

    public void setClientSupportedCertificateSignAlgorithms(
            List<SignatureAndHashAlgorithm> clientSupportedCertificateSignAlgorithms) {
        this.clientSupportedCertificateSignAlgorithms = clientSupportedCertificateSignAlgorithms;
    }

    public void setClientSupportedCertificateSignAlgorithms(
            SignatureAndHashAlgorithm... clientSupportedCertificateSignAlgorithms) {
        this.clientSupportedCertificateSignAlgorithms =
                new ArrayList<>(Arrays.asList(clientSupportedCertificateSignAlgorithms));
    }

    public List<SNIEntry> getClientSNIEntryList() {
        return clientSNIEntryList;
    }

    public void setClientSNIEntryList(List<SNIEntry> clientSNIEntryList) {
        this.clientSNIEntryList = clientSNIEntryList;
    }

    public void setClientSNIEntryList(SNIEntry... clientSNIEntryList) {
        this.clientSNIEntryList = new ArrayList<>(Arrays.asList(clientSNIEntryList));
    }

    public ProtocolVersion getLastRecordVersion() {
        return lastRecordVersion;
    }

    public void setLastRecordVersion(ProtocolVersion lastRecordVersion) {
        this.lastRecordVersion = lastRecordVersion;
    }

    public byte[] getDistinguishedNames() {
        return distinguishedNames;
    }

    public void setDistinguishedNames(byte[] distinguishedNames) {
        this.distinguishedNames = distinguishedNames;
    }

    public List<ClientCertificateType> getClientCertificateTypes() {
        return clientCertificateTypes;
    }

    public void setClientCertificateTypes(List<ClientCertificateType> clientCertificateTypes) {
        this.clientCertificateTypes = clientCertificateTypes;
    }

    public void setClientCertificateTypes(ClientCertificateType... clientCertificateTypes) {
        this.clientCertificateTypes = new ArrayList<>(Arrays.asList(clientCertificateTypes));
    }

    public boolean isReceivedFatalAlert() {
        return receivedFatalAlert;
    }

    public void setReceivedFatalAlert(boolean receivedFatalAlert) {
        this.receivedFatalAlert = receivedFatalAlert;
    }

    public List<ECPointFormat> getClientPointFormatsList() {
        return clientPointFormatsList;
    }

    public void setClientPointFormatsList(List<ECPointFormat> clientPointFormatsList) {
        this.clientPointFormatsList = clientPointFormatsList;
    }

    public void setClientPointFormatsList(ECPointFormat... clientPointFormatsList) {
        this.clientPointFormatsList = new ArrayList<>(Arrays.asList(clientPointFormatsList));
    }

    public MaxFragmentLength getMaxFragmentLength() {
        return maxFragmentLength;
    }

    public void setMaxFragmentLength(MaxFragmentLength maxFragmentLength) {
        this.maxFragmentLength = maxFragmentLength;
    }

    public HeartbeatMode getHeartbeatMode() {
        return heartbeatMode;
    }

    public void setHeartbeatMode(HeartbeatMode heartbeatMode) {
        this.heartbeatMode = heartbeatMode;
    }

    public byte[] getPaddingExtensionBytes() {
        return paddingExtensionBytes;
    }

    public void setPaddingExtensionBytes(byte[] paddingExtensionBytes) {
        this.paddingExtensionBytes = paddingExtensionBytes;
    }

    public List<CompressionMethod> getClientSupportedCompressions() {
        return clientSupportedCompressions;
    }

    public void setClientSupportedCompressions(
            List<CompressionMethod> clientSupportedCompressions) {
        this.clientSupportedCompressions = clientSupportedCompressions;
    }

    public void setClientSupportedCompressions(CompressionMethod... clientSupportedCompressions) {
        this.clientSupportedCompressions =
                new ArrayList<>(Arrays.asList(clientSupportedCompressions));
    }

    public void addDtlsReceivedHandshakeMessageSequences(int sequence) {
        dtlsReceivedHandshakeMessageSequences.add(sequence);
    }

    public Set<Integer> getDtlsReceivedHandshakeMessageSequences() {
        return dtlsReceivedHandshakeMessageSequences;
    }

    public boolean addDtlsReceivedChangeCipherSpecEpochs(int epoch) {
        return dtlsReceivedChangeCipherSpecEpochs.add(epoch);
    }

    public Set<Integer> getDtlsReceivedChangeCipherSpecEpochs() {
        return dtlsReceivedChangeCipherSpecEpochs;
    }

    public List<CipherSuite> getClientSupportedCipherSuites() {
        return clientSupportedCipherSuites;
    }

    public void setClientSupportedCipherSuites(List<CipherSuite> clientSupportedCipherSuites) {
        this.clientSupportedCipherSuites = clientSupportedCipherSuites;
    }

    public void setClientSupportedCipherSuites(CipherSuite... clientSupportedCipherSuites) {
        this.clientSupportedCipherSuites =
                new ArrayList<>(Arrays.asList(clientSupportedCipherSuites));
    }

    public List<SignatureAndHashAlgorithm> getServerSupportedSignatureAndHashAlgorithms() {
        return serverSupportedSignatureAndHashAlgorithms;
    }

    public void setServerSupportedSignatureAndHashAlgorithms(
            List<SignatureAndHashAlgorithm> serverSupportedSignatureAndHashAlgorithms) {
        this.serverSupportedSignatureAndHashAlgorithms = serverSupportedSignatureAndHashAlgorithms;
    }

    public void setServerSupportedSignatureAndHashAlgorithms(
            SignatureAndHashAlgorithm... serverSupportedSignatureAndHashAlgorithms) {
        this.serverSupportedSignatureAndHashAlgorithms =
                new ArrayList<>(Arrays.asList(serverSupportedSignatureAndHashAlgorithms));
    }

    public List<SignatureAndHashAlgorithm> getServerSupportedCertificateSignAlgorithms() {
        return serverSupportedCertificateSignAlgorithms;
    }

    public void setServerSupportedSignatureAlgorithmsCert(
            List<SignatureAndHashAlgorithm> serverSupportedCertificateSignAlgorithms) {
        this.serverSupportedCertificateSignAlgorithms = serverSupportedCertificateSignAlgorithms;
    }

    public void setServerSupportedSignatureAlgorithmsCert(
            SignatureAndHashAlgorithm... serverSupportedCertificateSignAlgorithms) {
        this.serverSupportedCertificateSignAlgorithms =
                new ArrayList<>(Arrays.asList(serverSupportedCertificateSignAlgorithms));
    }

    public ProtocolVersion getSelectedProtocolVersion() {
        return selectedProtocolVersion;
    }

    public void setSelectedProtocolVersion(ProtocolVersion selectedProtocolVersion) {
        this.selectedProtocolVersion = selectedProtocolVersion;
    }

    public ProtocolVersion getHighestClientProtocolVersion() {
        return highestClientProtocolVersion;
    }

    public void setHighestClientProtocolVersion(ProtocolVersion highestClientProtocolVersion) {
        this.highestClientProtocolVersion = highestClientProtocolVersion;
    }

    public byte[] getMasterSecret() {
        return masterSecret;
    }

    public byte[] getResumptionMasterSecret() {
        return resumptionMasterSecret;
    }

    public CipherSuite getSelectedCipherSuite() {
        return selectedCipherSuite;
    }

    public SSL2CipherSuite getSSL2CipherSuite() {
        return ssl2CipherSuite;
    }

    public void setMasterSecret(byte[] masterSecret) {
        keylogfile.writeKey("CLIENT_RANDOM", masterSecret);
        this.masterSecret = masterSecret;
    }

    public byte[] setResumptionMasterSecret(byte[] resumptionMasterSecret) {
        return this.resumptionMasterSecret = resumptionMasterSecret;
    }

    public void setSelectedCipherSuite(CipherSuite selectedCipherSuite) {
        this.selectedCipherSuite = selectedCipherSuite;
    }

    public void setSSL2CipherSuite(SSL2CipherSuite ssl2CipherSuite) {
        this.ssl2CipherSuite = ssl2CipherSuite;
    }

    public byte[] getClientServerRandom() {
        return DataConverter.concatenate(clientRandom, serverRandom);
    }

    public byte[] getClearKey() {
        return clearKey;
    }

    public void setClearKey(byte[] clearKey) {
        this.clearKey = clearKey;
    }

    public byte[] getPreMasterSecret() {
        return preMasterSecret;
    }

    public void setPreMasterSecret(byte[] preMasterSecret) {
        keylogfile.writeKey("PMS_CLIENT_RANDOM", preMasterSecret);
        this.preMasterSecret = preMasterSecret;
    }

    public byte[] getClientExtendedRandom() {
        return clientExtendedRandom;
    }

    public void setClientExtendedRandom(byte[] clientExtendedRandom) {
        this.clientExtendedRandom = clientExtendedRandom;
    }

    public byte[] getServerExtendedRandom() {
        return serverExtendedRandom;
    }

    public void setServerExtendedRandom(byte[] serverExtendedRandom) {
        this.serverExtendedRandom = serverExtendedRandom;
    }

    public byte[] getClientRandom() {
        return clientRandom;
    }

    public void setClientRandom(byte[] clientRandom) {
        this.clientRandom = clientRandom;
    }

    public byte[] getServerRandom() {
        return serverRandom;
    }

    public void setServerRandom(byte[] serverRandom) {
        this.serverRandom = serverRandom;
    }

    public CompressionMethod getSelectedCompressionMethod() {
        return selectedCompressionMethod;
    }

    public void setSelectedCompressionMethod(CompressionMethod selectedCompressionMethod) {
        this.selectedCompressionMethod = selectedCompressionMethod;
    }

    public byte[] getServerSessionId() {
        return serverSessionId;
    }

    public void setServerSessionId(byte[] serverSessionId) {
        this.serverSessionId = serverSessionId;
    }

    public byte[] getClientSessionId() {
        return clientSessionId;
    }

    public void setClientSessionId(byte[] clientSessionId) {
        this.clientSessionId = clientSessionId;
    }

    public byte[] getSSL2Iv() {
        return ssl2Iv;
    }

    public void setSSL2Iv(byte[] ssl2Iv) {
        this.ssl2Iv = ssl2Iv;
    }

    public X509CertificateChain getServerCertificateChain() {
        return serverCertificateChain;
    }

    public void setServerCertificateChain(X509CertificateChain serverCertificateChain) {
        this.serverCertificateChain = serverCertificateChain;
    }

    public X509CertificateChain getClientCertificateChain() {
        return clientCertificateChain;
    }

    public void setClientCertificateChain(X509CertificateChain clientCertificateChain) {
        this.clientCertificateChain = clientCertificateChain;
    }

    public MessageDigestCollector getDigest() {
        return digest;
    }

    public void setDigest(MessageDigestCollector digest) {
        this.digest = digest;
    }

    public byte[] getDtlsCookie() {
        return dtlsCookie;
    }

    public void setDtlsCookie(byte[] dtlsCookie) {
        this.dtlsCookie = dtlsCookie;
    }

    public DtlsFragmentLayer getDtlsFragmentLayer() {
        return (DtlsFragmentLayer) getContext().getLayerStack().getLayer(DtlsFragmentLayer.class);
    }

    public RecordLayer getRecordLayer() {
        return (RecordLayer) getContext().getLayerStack().getLayer(RecordLayer.class);
    }

    public PRFAlgorithm getPrfAlgorithm() {
        return prfAlgorithm;
    }

    public void setPrfAlgorithm(PRFAlgorithm prfAlgorithm) {
        this.prfAlgorithm = prfAlgorithm;
    }

    public byte[] getClientHandshakeTrafficSecret() {
        return clientHandshakeTrafficSecret;
    }

    public void setClientHandshakeTrafficSecret(byte[] clientHandshakeTrafficSecret) {
        keylogfile.writeKey("CLIENT_HANDSHAKE_TRAFFIC_SECRET", clientHandshakeTrafficSecret);
        this.clientHandshakeTrafficSecret = clientHandshakeTrafficSecret;
    }

    public byte[] getServerHandshakeTrafficSecret() {
        return serverHandshakeTrafficSecret;
    }

    public void setServerHandshakeTrafficSecret(byte[] serverHandshakeTrafficSecret) {
        keylogfile.writeKey("SERVER_HANDSHAKE_TRAFFIC_SECRET", serverHandshakeTrafficSecret);
        this.serverHandshakeTrafficSecret = serverHandshakeTrafficSecret;
    }

    public byte[] getClientApplicationTrafficSecret() {
        return clientApplicationTrafficSecret;
    }

    public void setClientApplicationTrafficSecret(byte[] clientApplicationTrafficSecret) {
        keylogfile.writeKey("CLIENT_TRAFFIC_SECRET_0", clientApplicationTrafficSecret);
        this.clientApplicationTrafficSecret = clientApplicationTrafficSecret;
    }

    public byte[] getServerApplicationTrafficSecret() {
        return serverApplicationTrafficSecret;
    }

    public void setServerApplicationTrafficSecret(byte[] serverApplicationTrafficSecret) {
        keylogfile.writeKey("SERVER_TRAFFIC_SECRET_0", serverApplicationTrafficSecret);
        this.serverApplicationTrafficSecret = serverApplicationTrafficSecret;
    }

    public byte[] getHandshakeSecret() {
        return handshakeSecret;
    }

    public void setHandshakeSecret(byte[] handshakeSecret) {
        this.handshakeSecret = handshakeSecret;
    }

    public List<KeyShareStoreEntry> getClientKeyShareStoreEntryList() {
        return clientKeyShareStoreEntryList;
    }

    public void setClientKeyShareStoreEntryList(
            List<KeyShareStoreEntry> clientKeyShareStoreEntryList) {
        this.clientKeyShareStoreEntryList = clientKeyShareStoreEntryList;
    }

    public void setClientKSEntryList(KeyShareStoreEntry... clientKSEntryList) {
        this.clientKeyShareStoreEntryList = new ArrayList<>(Arrays.asList(clientKSEntryList));
    }

    public KeyShareStoreEntry getServerKeyShareStoreEntry() {
        return serverKeyShareStoreEntry;
    }

    public void setServerKeyShareStoreEntry(KeyShareStoreEntry serverKeyShareStoreEntry) {
        this.serverKeyShareStoreEntry = serverKeyShareStoreEntry;
    }

    public byte[] getSignedCertificateTimestamp() {
        return signedCertificateTimestamp;
    }

    public void setSignedCertificateTimestamp(byte[] signedCertificateTimestamp) {
        this.signedCertificateTimestamp = signedCertificateTimestamp;
    }

    public byte[] getRenegotiationInfo() {
        return renegotiationInfo;
    }

    public void setRenegotiationInfo(byte[] renegotiationInfo) {
        this.renegotiationInfo = renegotiationInfo;
    }

    public TokenBindingVersion getTokenBindingVersion() {
        return tokenBindingVersion;
    }

    public void setTokenBindingVersion(TokenBindingVersion tokenBindingVersion) {
        this.tokenBindingVersion = tokenBindingVersion;
    }

    public void setTokenBindingKeyParameters(
            TokenBindingKeyParameters... tokenBindingKeyParameters) {
        this.tokenBindingKeyParameters = new ArrayList<>(Arrays.asList(tokenBindingKeyParameters));
    }

    public void setTokenBindingKeyParameters(
            List<TokenBindingKeyParameters> tokenBindingKeyParameters) {
        this.tokenBindingKeyParameters = tokenBindingKeyParameters;
    }

    public List<TokenBindingKeyParameters> getTokenBindingKeyParameters() {
        return tokenBindingKeyParameters;
    }

    public void setTokenBindingNegotiatedSuccessfully(boolean tokenBindingNegotiated) {
        this.tokenBindingNegotiatedSuccessfully = tokenBindingNegotiated;
    }

    public boolean isTokenBindingNegotiatedSuccessfully() {
        return tokenBindingNegotiatedSuccessfully;
    }

    public CertificateStatusRequestType getCertificateStatusRequestExtensionRequestType() {
        return certificateStatusRequestExtensionRequestType;
    }

    public void setCertificateStatusRequestExtensionRequestType(
            CertificateStatusRequestType certificateStatusRequestExtensionRequestType) {
        this.certificateStatusRequestExtensionRequestType =
                certificateStatusRequestExtensionRequestType;
    }

    public byte[] getCertificateStatusRequestExtensionResponderIDList() {
        return certificateStatusRequestExtensionResponderIDList;
    }

    public void setCertificateStatusRequestExtensionResponderIDList(
            byte[] certificateStatusRequestExtensionResponderIDList) {
        this.certificateStatusRequestExtensionResponderIDList =
                certificateStatusRequestExtensionResponderIDList;
    }

    public byte[] getCertificateStatusRequestExtensionRequestExtension() {
        return certificateStatusRequestExtensionRequestExtension;
    }

    public void setCertificateStatusRequestExtensionRequestExtension(
            byte[] certificateStatusRequestExtensionRequestExtension) {
        this.certificateStatusRequestExtensionRequestExtension =
                certificateStatusRequestExtensionRequestExtension;
    }

    public String getSelectedAlpnProtocol() {
        return selectedAlpnProtocol;
    }

    public void setSelectedAlpnProtocol(String selectedAlpnProtocol) {
        this.selectedAlpnProtocol = selectedAlpnProtocol;
    }

    public List<String> getProposedAlpnProtocols() {
        return proposedAlpnProtocols;
    }

    public void setProposedAlpnProtocols(List<String> proposedAlpnProtocols) {
        this.proposedAlpnProtocols = proposedAlpnProtocols;
    }

    public byte[] getSecureRemotePasswordExtensionIdentifier() {
        return secureRemotePasswordExtensionIdentifier;
    }

    public void setSecureRemotePasswordExtensionIdentifier(
            byte[] secureRemotePasswordExtensionIdentifier) {
        this.secureRemotePasswordExtensionIdentifier = secureRemotePasswordExtensionIdentifier;
    }

    public byte[] getSecureRealTimeProtocolMasterKeyIdentifier() {
        return secureRealTimeProtocolMasterKeyIdentifier;
    }

    public void setSecureRealTimeProtocolMasterKeyIdentifier(
            byte[] secureRealTimeProtocolMasterKeyIdentifier) {
        this.secureRealTimeProtocolMasterKeyIdentifier = secureRealTimeProtocolMasterKeyIdentifier;
    }

    public UserMappingExtensionHintType getUserMappingExtensionHintType() {
        return userMappingExtensionHintType;
    }

    public void setUserMappingExtensionHintType(
            UserMappingExtensionHintType userMappingExtensionHintType) {
        this.userMappingExtensionHintType = userMappingExtensionHintType;
    }

    public List<CertificateType> getCertificateTypeDesiredTypes() {
        return certificateTypeClientDesiredTypes;
    }

    public void setCertificateTypeDesiredTypes(List<CertificateType> certificateTypeDesiredTypes) {
        this.certificateTypeClientDesiredTypes = certificateTypeDesiredTypes;
    }

    public List<AuthzDataFormat> getClientAuthzDataFormatList() {
        return clientAuthzDataFormatList;
    }

    public void setClientAuthzDataFormatList(List<AuthzDataFormat> clientAuthzDataFormatList) {
        this.clientAuthzDataFormatList = clientAuthzDataFormatList;
    }

    public List<AuthzDataFormat> getServerAuthzDataFormatList() {
        return serverAuthzDataFormatList;
    }

    public void setServerAuthzDataFormatList(List<AuthzDataFormat> serverAuthzDataFormatList) {
        this.serverAuthzDataFormatList = serverAuthzDataFormatList;
    }

    public byte[] getCertificateRequestContext() {
        return certificateRequestContext;
    }

    public void setCertificateRequestContext(byte[] certificateRequestContext) {
        this.certificateRequestContext = certificateRequestContext;
    }

    public List<CertificateType> getClientCertificateTypeDesiredTypes() {
        return clientCertificateTypeDesiredTypes;
    }

    public void setClientCertificateTypeDesiredTypes(
            List<CertificateType> clientCertificateTypeDesiredTypes) {
        this.clientCertificateTypeDesiredTypes = clientCertificateTypeDesiredTypes;
    }

    public List<CertificateType> getServerCertificateTypeDesiredTypes() {
        return serverCertificateTypeDesiredTypes;
    }

    public void setServerCertificateTypeDesiredTypes(
            List<CertificateType> serverCertificateTypeDesiredTypes) {
        this.serverCertificateTypeDesiredTypes = serverCertificateTypeDesiredTypes;
    }

    public boolean isCachedInfoExtensionClientState() {
        return cachedInfoExtensionClientState;
    }

    public void setCachedInfoExtensionClientState(boolean cachedInfoExtensionClientState) {
        this.cachedInfoExtensionClientState = cachedInfoExtensionClientState;
    }

    public List<CachedObject> getCachedInfoExtensionObjects() {
        return cachedInfoExtensionObjects;
    }

    public void setCachedInfoExtensionObjects(List<CachedObject> cachedInfoExtensionObjects) {
        this.cachedInfoExtensionObjects = cachedInfoExtensionObjects;
    }

    public List<TrustedAuthority> getTrustedCaIndicationExtensionCas() {
        return trustedCaIndicationExtensionCas;
    }

    public void setTrustedCaIndicationExtensionCas(
            List<TrustedAuthority> trustedCaIndicationExtensionCas) {
        this.trustedCaIndicationExtensionCas = trustedCaIndicationExtensionCas;
    }

    public List<RequestItemV2> getStatusRequestV2RequestList() {
        return statusRequestV2RequestList;
    }

    public void setStatusRequestV2RequestList(List<RequestItemV2> statusRequestV2RequestList) {
        this.statusRequestV2RequestList = statusRequestV2RequestList;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public BadRandom getBadSecureRandom() {
        return new BadRandom(getRandom(), null);
    }

    public Config getConfig() {
        return getContext().getConfig();
    }

    public ProtocolVersion getHighestProtocolVersion() {
        return highestProtocolVersion;
    }

    public void setHighestProtocolVersion(ProtocolVersion highestProtocolVersion) {
        this.highestProtocolVersion = highestProtocolVersion;
    }

    public Boolean isClientAuthentication() {
        return clientAuthentication;
    }

    public void setClientAuthentication(Boolean clientAuthentication) {
        this.clientAuthentication = clientAuthentication;
    }

    public byte[] getLastHandledApplicationMessageData() {
        return lastHandledApplicationMessageData;
    }

    public void setLastHandledApplicationMessageData(byte[] lastHandledApplicationMessageData) {
        this.lastHandledApplicationMessageData = lastHandledApplicationMessageData;
    }

    /**
     * Check if the given TLS extension type was proposed by the client.
     *
     * @param ext The ExtensionType to check for
     * @return true if extension was proposed by client, false otherwise
     */
    public boolean isExtensionProposed(ExtensionType ext) {
        return proposedExtensionSet.contains(ext);
    }

    /**
     * Get all TLS extension types proposed by the client.
     *
     * @return set of proposed extensions. Not null.
     */
    public EnumSet<ExtensionType> getProposedExtensions() {
        return proposedExtensionSet;
    }

    /**
     * Mark the given TLS extension type as client proposed extension.
     *
     * @param ext The ExtensionType that is proposed
     */
    public void addProposedExtension(ExtensionType ext) {
        proposedExtensionSet.add(ext);
    }

    /**
     * Check if the given TLS extension type was sent by the server.
     *
     * @param ext The ExtensionType to check for
     * @return true if extension was proposed by server, false otherwise
     */
    public boolean isExtensionNegotiated(ExtensionType ext) {
        return negotiatedExtensionSet.contains(ext);
    }

    /**
     * Mark the given TLS extension type as server negotiated extension.
     *
     * @param ext The ExtensionType to add
     */
    public void addNegotiatedExtension(ExtensionType ext) {
        negotiatedExtensionSet.add(ext);
    }

    public EnumSet<ExtensionType> getNegotiatedExtensionSet() {
        return negotiatedExtensionSet;
    }

    public boolean isUseExtendedMasterSecret() {
        return useExtendedMasterSecret;
    }

    public void setUseExtendedMasterSecret(boolean useExtendedMasterSecret) {
        this.useExtendedMasterSecret = useExtendedMasterSecret;
    }

    /**
     * @return the keySetHandshake
     */
    public KeySet getkeySetHandshake() {
        return keySetHandshake;
    }

    /**
     * @param keySetHandshake the keySetHandshake to set
     */
    public void setkeySetHandshake(KeySet keySetHandshake) {
        this.keySetHandshake = keySetHandshake;
    }

    /**
     * @return the clientEarlyTrafficSecret
     */
    public byte[] getClientEarlyTrafficSecret() {
        return clientEarlyTrafficSecret;
    }

    /**
     * @param clientEarlyTrafficSecret the clientEarlyTrafficSecret to set
     */
    public void setClientEarlyTrafficSecret(byte[] clientEarlyTrafficSecret) {
        keylogfile.writeKey("CLIENT_EARLY_TRAFFIC_SECRET", clientEarlyTrafficSecret);
        this.clientEarlyTrafficSecret = clientEarlyTrafficSecret;
    }

    /**
     * @return the maxEarlyDataSize
     */
    public Integer getMaxEarlyDataSize() {
        return maxEarlyDataSize;
    }

    /**
     * @param maxEarlyDataSize the maxEarlyDataSize to set
     */
    public void setMaxEarlyDataSize(Integer maxEarlyDataSize) {
        this.maxEarlyDataSize = maxEarlyDataSize;
    }

    /**
     * @return the psk
     */
    public byte[] getPsk() {
        return psk;
    }

    /**
     * @param psk the psk to set
     */
    public void setPsk(byte[] psk) {
        this.psk = psk;
    }

    /**
     * @return the earlySecret
     */
    public byte[] getEarlySecret() {
        return earlySecret;
    }

    /**
     * @param earlySecret the earlySecret to set
     */
    public void setEarlySecret(byte[] earlySecret) {
        this.earlySecret = earlySecret;
    }

    /**
     * @return the earlyDataCipherSuite
     */
    public CipherSuite getEarlyDataCipherSuite() {
        return earlyDataCipherSuite;
    }

    /**
     * @param earlyDataCipherSuite the earlyDataCipherSuite to set
     */
    public void setEarlyDataCipherSuite(CipherSuite earlyDataCipherSuite) {
        this.earlyDataCipherSuite = earlyDataCipherSuite;
    }

    /**
     * @return the earlyDataPSKIdentity
     */
    public byte[] getEarlyDataPSKIdentity() {
        return earlyDataPSKIdentity;
    }

    /**
     * @param earlyDataPSKIdentity the earlyDataPSKIdentity to set
     */
    public void setEarlyDataPSKIdentity(byte[] earlyDataPSKIdentity) {
        this.earlyDataPSKIdentity = earlyDataPSKIdentity;
    }

    /**
     * @return the selectedIdentityIndex
     */
    public int getSelectedIdentityIndex() {
        return selectedIdentityIndex;
    }

    /**
     * @param selectedIdentityIndex the selectedIdentityIndex to set
     */
    public void setSelectedIdentityIndex(int selectedIdentityIndex) {
        this.selectedIdentityIndex = selectedIdentityIndex;
    }

    /**
     * @return the clientPskKeyExchangeModes
     */
    public List<PskKeyExchangeMode> getClientPskKeyExchangeModes() {
        return clientPskKeyExchangeModes;
    }

    /**
     * @param clientPskKeyExchangeModes the clientPskKeyExchangeModes to set
     */
    public void setClientPskKeyExchangeModes(List<PskKeyExchangeMode> clientPskKeyExchangeModes) {
        this.clientPskKeyExchangeModes = clientPskKeyExchangeModes;
    }

    /**
     * @return the pskSets
     */
    public List<PskSet> getPskSets() {
        return pskSets;
    }

    /**
     * @param pskSets the pskSets to set
     */
    public void setPskSets(List<PskSet> pskSets) {
        this.pskSets = pskSets;
    }

    /**
     * @return the activeClientKeySetType
     */
    public Tls13KeySetType getActiveClientKeySetType() {
        return activeClientKeySetType;
    }

    /**
     * @param activeClientKeySetType the activeClientKeySetType to set
     */
    public void setActiveClientKeySetType(Tls13KeySetType activeClientKeySetType) {
        this.activeClientKeySetType = activeClientKeySetType;
    }

    /**
     * @return the activeServerKeySetType
     */
    public Tls13KeySetType getActiveServerKeySetType() {
        return activeServerKeySetType;
    }

    /**
     * @param activeServerKeySetType the activeServerKeySetType to set
     */
    public void setActiveServerKeySetType(Tls13KeySetType activeServerKeySetType) {
        this.activeServerKeySetType = activeServerKeySetType;
    }

    public Tls13KeySetType getActiveKeySetTypeRead() {
        if (getChooser().getConnectionEndType() == ConnectionEndType.SERVER) {
            return activeClientKeySetType;
        } else {
            return activeServerKeySetType;
        }
    }

    public Tls13KeySetType getActiveKeySetTypeWrite() {
        if (getChooser().getConnectionEndType() == ConnectionEndType.SERVER) {
            return activeServerKeySetType;
        } else {
            return activeClientKeySetType;
        }
    }

    /**
     * @return the earlyDataPsk
     */
    public byte[] getEarlyDataPsk() {
        return earlyDataPsk;
    }

    /**
     * @param earlyDataPsk the earlyDataPsk to set
     */
    public void setEarlyDataPsk(byte[] earlyDataPsk) {
        this.earlyDataPsk = earlyDataPsk;
    }

    public boolean isReceivedTransportHandlerException() {
        return receivedTransportHandlerException;
    }

    public void setReceivedTransportHandlerException(boolean receivedTransportHandlerException) {
        this.receivedTransportHandlerException = receivedTransportHandlerException;
    }

    public void setClientPWDUsername(String username) {
        this.clientPWDUsername = username;
    }

    public String getClientPWDUsername() {
        return clientPWDUsername;
    }

    public void setServerPWDSalt(byte[] salt) {
        this.serverPWDSalt = salt;
    }

    public byte[] getServerPWDSalt() {
        return serverPWDSalt;
    }

    public Point getPwdPasswordElement() {
        return pwdPasswordElement;
    }

    public void setPwdPasswordElement(Point pwdPasswordElement) {
        this.pwdPasswordElement = pwdPasswordElement;
    }

    public BigInteger getClientPWDPrivate() {
        return clientPWDPrivate;
    }

    public void setClientPWDPrivate(BigInteger clientPWDPrivate) {
        this.clientPWDPrivate = clientPWDPrivate;
    }

    public BigInteger getServerPWDPrivate() {
        return serverPWDPrivate;
    }

    public void setServerPWDPrivate(BigInteger serverPWDPrivate) {
        this.serverPWDPrivate = serverPWDPrivate;
    }

    public BigInteger getServerPWDScalar() {
        return serverPWDScalar;
    }

    public void setServerPWDScalar(BigInteger serverPWDScalar) {
        this.serverPWDScalar = serverPWDScalar;
    }

    public Point getServerPWDElement() {
        return serverPWDElement;
    }

    public void setServerPWDElement(Point serverPWDElement) {
        this.serverPWDElement = serverPWDElement;
    }

    public GOSTCurve getSelectedGostCurve() {
        return selectedGostCurve;
    }

    public void setSelectedGostCurve(GOSTCurve selectedGostCurve) {
        this.selectedGostCurve = selectedGostCurve;
    }

    public EchConfig getEchConfig() {
        return echConfig;
    }

    public void setEchConfig(EchConfig echConfig) {
        this.echConfig = echConfig;
    }

    public ClientHelloMessage getInnerClientHello() {
        return innerClientHello;
    }

    public void setInnerClientHello(ClientHelloMessage innerClientHello) {
        this.innerClientHello = innerClientHello;
    }

    public EncryptedClientHelloMessage getOuterClientHello() {
        return outerClientHello;
    }

    public void setOuterClientHello(EncryptedClientHelloMessage outerClientHello) {
        this.outerClientHello = outerClientHello;
    }

    public boolean isSupportsECH() {
        return supportsECH;
    }

    public void setSupportsECH(boolean supportsECH) {
        this.supportsECH = supportsECH;
    }

    public KeyShareEntry getEchClientKeyShareEntry() {
        return echClientKeyShareEntry;
    }

    public void setEchClientKeyShareEntry(KeyShareEntry echClientKeyShareEntry) {
        this.echClientKeyShareEntry = echClientKeyShareEntry;
    }

    public KeyShareEntry getEchServerKeyShareEntry() {
        return echServerKeyShareEntry;
    }

    public void setEchServerKeyShareEntry(KeyShareEntry echServerKeyShareEntry) {
        this.echServerKeyShareEntry = echServerKeyShareEntry;
    }

    public byte[] getEsniClientNonce() {
        return this.esniClientNonce;
    }

    public void setEsniClientNonce(byte[] esniClientNonce) {
        this.esniClientNonce = esniClientNonce;
    }

    public byte[] getEsniServerNonce() {
        return this.esniServerNonce;
    }

    public void setEsniServerNonce(byte[] esniServerNonce) {
        this.esniServerNonce = esniServerNonce;
    }

    public byte[] getEsniRecordBytes() {
        return esniRecordBytes;
    }

    public void setEsniRecordBytes(byte[] esniRecordBytes) {
        this.esniRecordBytes = esniRecordBytes;
    }

    public EsniDnsKeyRecordVersion getEsniRecordVersion() {
        return esniRecordVersion;
    }

    public void setEsniRecordVersion(EsniDnsKeyRecordVersion esniRecordVersion) {
        this.esniRecordVersion = esniRecordVersion;
    }

    public byte[] getEsniRecordChecksum() {
        return esniRecordChecksum;
    }

    public void setEsniRecordChecksum(byte[] esniRecordChecksum) {
        this.esniRecordChecksum = esniRecordChecksum;
    }

    public byte[] getPublicName() {
        return publicName;
    }

    public void setPublicName(byte[] publicName) {
        this.publicName = publicName;
    }

    public List<KeyShareStoreEntry> getEsniServerKeyShareEntries() {
        return this.esniServerKeyShareEntries;
    }

    public void setEsniServerKeyShareEntries(List<KeyShareStoreEntry> esniServerKeyShareEntries) {
        this.esniServerKeyShareEntries = esniServerKeyShareEntries;
    }

    public List<CipherSuite> getEsniServerCipherSuites() {
        return esniServerCipherSuites;
    }

    public void setEsniServerCipherSuites(List<CipherSuite> esniServerCipherSuites) {
        this.esniServerCipherSuites = esniServerCipherSuites;
    }

    public Integer getEsniPaddedLength() {
        return esniPaddedLength;
    }

    public void setEsniPaddedLength(Integer esniPaddedLength) {
        this.esniPaddedLength = esniPaddedLength;
    }

    public Long getEsniKeysNotBefore() {
        return esniNotBefore;
    }

    public void setEsniKeysNotBefore(Long esniKeysNotBefore) {
        this.esniNotBefore = esniKeysNotBefore;
    }

    public Long getEsniNotAfter() {
        return esniNotAfter;
    }

    public void setEsniKeysNotAfter(Long esniKeysNotAfter) {
        this.esniNotAfter = esniKeysNotAfter;
    }

    public byte[] getLastClientHello() {
        return lastClientHello;
    }

    public void setLastClientHello(byte[] lastClientHello) {
        this.lastClientHello = lastClientHello;
    }

    public byte[] getExtensionCookie() {
        return extensionCookie;
    }

    public void setExtensionCookie(byte[] extensionCookie) {
        this.extensionCookie = extensionCookie;
    }

    public boolean isReceivedMessageWithWrongTls13KeyType() {
        return receivedMessageWithWrongTls13KeyType;
    }

    public void setReceivedMessageWithWrongTls13KeyType(
            boolean receivedMessageWithWrongTls13KeyType) {
        this.receivedMessageWithWrongTls13KeyType = receivedMessageWithWrongTls13KeyType;
    }

    public Integer getOutboundRecordSizeLimit() {
        return outboundRecordSizeLimit;
    }

    public void setOutboundRecordSizeLimit(Integer recordSizeLimit) {
        this.outboundRecordSizeLimit = recordSizeLimit;
    }

    public boolean isRecordSizeLimitExtensionActive() {
        return outboundRecordSizeLimit != null || getConfig().isAddRecordSizeLimitExtension();
    }

    public Boolean isRecordEncryptionActive() {
        if (getRecordLayer() == null || getRecordLayer().getEncryptorCipher() == null) {
            return false;
        }

        return !(this.getRecordLayer().getEncryptorCipher() instanceof RecordNullCipher);
    }

    public Boolean isRecordDecryptionActive() {
        if (this.getRecordLayer() == null || this.getRecordLayer().getDecryptorCipher() == null) {
            return false;
        }

        return !(this.getRecordLayer().getDecryptorCipher() instanceof RecordNullCipher);
    }

    public Integer getInboundRecordSizeLimit() {
        return inboundRecordSizeLimit;
    }

    public int getWriteEpoch() {
        return getRecordLayer().getWriteEpoch();
    }

    public int getReadEpoch() {
        return getRecordLayer().getReadEpoch();
    }

    public void setWriteEpoch(int epoch) {
        getRecordLayer().setWriteEpoch(epoch);
    }

    public void setReadEpoch(int epoch) {
        getRecordLayer().setReadEpoch(epoch);
    }

    public long getWriteSequenceNumber(int epoch) {
        return getRecordLayer()
                .getEncryptor()
                .getRecordCipher(epoch)
                .getState()
                .getWriteSequenceNumber();
    }

    public long getReadSequenceNumber(int epoch) {
        return getRecordLayer()
                .getDecryptor()
                .getRecordCipher(epoch)
                .getState()
                .getReadSequenceNumber();
    }

    public void setWriteSequenceNumber(int epoch, long sqn) {
        getRecordLayer()
                .getEncryptor()
                .getRecordCipher(epoch)
                .getState()
                .setWriteSequenceNumber(sqn);
    }

    public void setReadSequenceNumber(int epoch, long sqn) {
        getRecordLayer()
                .getDecryptor()
                .getRecordCipher(epoch)
                .getState()
                .setReadSequenceNumber(sqn);
    }

    public byte[] getWriteConnectionId() {
        if (writeConnectionIdIndex < writeConnectionIds.size()) {
            return writeConnectionIds.get(writeConnectionIdIndex);
        } else {
            return null;
        }
    }

    public void setWriteConnectionId(byte[] writeConnectionId) {
        this.writeConnectionIds.set(writeConnectionIdIndex, writeConnectionId);
    }

    public void setWriteConnectionId(byte[] writeConnectionId, int index) {
        this.writeConnectionIds.set(index, writeConnectionId);
    }

    public byte[] getReadConnectionId() {
        if (readConnectionIdIndex < readConnectionIDs.size()) {
            return readConnectionIDs.get(readConnectionIdIndex);
        } else {
            return null;
        }
    }

    public void setReadConnectionId(byte[] readConnectionID) {
        this.readConnectionIDs.set(readConnectionIdIndex, readConnectionID);
    }

    public void setReadConnectionId(byte[] readConnectionID, int index) {
        this.readConnectionIDs.set(index, readConnectionID);
    }

    public void addNewWriteConnectionId(byte[] writeConnectionId, boolean spare) {
        this.writeConnectionIds.add(writeConnectionId);
        if (!spare) {
            writeConnectionIdIndex++;
            getRecordLayer().getEncryptorCipher().getState().setConnectionId(writeConnectionId);
        }
    }

    public void addNewReadConnectionId(byte[] readConnectionId, boolean spare) {
        this.readConnectionIDs.add(readConnectionId);
        if (!spare) {
            readConnectionIdIndex++;
            getRecordLayer().getDecryptorCipher().getState().setConnectionId(readConnectionId);
        }
    }

    public Integer getNumberOfRequestedConnectionIds() {
        return numberOfRequestedConnectionIds;
    }

    public void setNumberOfRequestedConnectionIds(Integer numberOfRequestedConnectionIds) {
        this.numberOfRequestedConnectionIds = numberOfRequestedConnectionIds;
    }

    public List<RecordNumber> getDtls13AcknowledgedRecords() {
        return dtls13AcknowledgedRecords;
    }

    public void setDtls13AcknowledgedRecords(List<RecordNumber> dtlsAcknowledgedRecords) {
        this.dtls13AcknowledgedRecords = dtlsAcknowledgedRecords;
    }

    public List<RecordNumber> getDtls13ReceivedAcknowledgedRecords() {
        return dtls13ReceivedAcknowledgedRecords;
    }

    public void setDtls13ReceivedAcknowledgedRecords(
            List<RecordNumber> dtlsReceivedAcknowledgedRecords) {
        this.dtls13ReceivedAcknowledgedRecords = dtlsReceivedAcknowledgedRecords;
    }

    public BigInteger getServerEphemeralDhGenerator() {
        return serverEphemeralDhGenerator;
    }

    public void setServerEphemeralDhGenerator(BigInteger serverEphemeralDhGenerator) {
        this.serverEphemeralDhGenerator = serverEphemeralDhGenerator;
    }

    public BigInteger getServerEphemeralDhModulus() {
        return serverEphemeralDhModulus;
    }

    public void setServerEphemeralDhModulus(BigInteger serverEphemeralDhModulus) {
        this.serverEphemeralDhModulus = serverEphemeralDhModulus;
    }

    public BigInteger getServerEphemeralDhPrivateKey() {
        return serverEphemeralDhPrivateKey;
    }

    public void setServerEphemeralDhPrivateKey(BigInteger serverEphemeralDhPrivateKey) {
        this.serverEphemeralDhPrivateKey = serverEphemeralDhPrivateKey;
    }

    public BigInteger getServerEphemeralDhPublicKey() {
        return serverEphemeralDhPublicKey;
    }

    public void setServerEphemeralDhPublicKey(BigInteger serverEphemeralDhPublicKey) {
        this.serverEphemeralDhPublicKey = serverEphemeralDhPublicKey;
    }

    public BigInteger getClientEphemeralDhPrivateKey() {
        return clientEphemeralDhPrivateKey;
    }

    public void setClientEphemeralDhPrivateKey(BigInteger clientEphemeralDhPrivateKey) {
        this.clientEphemeralDhPrivateKey = clientEphemeralDhPrivateKey;
    }

    public BigInteger getClientEphemeralDhPublicKey() {
        return clientEphemeralDhPublicKey;
    }

    public void setClientEphemeralDhPublicKey(BigInteger clientEphemeralDhPublicKey) {
        this.clientEphemeralDhPublicKey = clientEphemeralDhPublicKey;
    }

    public Point getClientEphemeralEcPublicKey() {
        return clientEphemeralEcPublicKey;
    }

    public void setClientEphemeralEcPublicKey(Point clientEphemeralEcPublicKey) {
        this.clientEphemeralEcPublicKey = clientEphemeralEcPublicKey;
    }

    public Point getServerEphemeralEcPublicKey() {
        return serverEphemeralEcPublicKey;
    }

    public void setServerEphemeralEcPublicKey(Point serverEphemeralEcPublicKey) {
        this.serverEphemeralEcPublicKey = serverEphemeralEcPublicKey;
    }

    public BigInteger getServerEphemeralEcPrivateKey() {
        return serverEphemeralEcPrivateKey;
    }

    public void setServerEphemeralEcPrivateKey(BigInteger serverEphemeralEcPrivateKey) {
        this.serverEphemeralEcPrivateKey = serverEphemeralEcPrivateKey;
    }

    public BigInteger getClientEphemeralEcPrivateKey() {
        return clientEphemeralEcPrivateKey;
    }

    public void setClientEphemeralEcPrivateKey(BigInteger clientEphemeralEcPrivateKey) {
        this.clientEphemeralEcPrivateKey = clientEphemeralEcPrivateKey;
    }

    public BigInteger getServerEphemeralRsaExportModulus() {
        return serverEphemeralRsaExportModulus;
    }

    public void setServerEphemeralRsaExportModulus(BigInteger serverEphemeralRsaExportModulus) {
        this.serverEphemeralRsaExportModulus = serverEphemeralRsaExportModulus;
    }

    public BigInteger getServerEphemeralRsaExportPublicKey() {
        return serverEphemeralRsaExportPublicKey;
    }

    public void setServerEphemeralRsaExportPublicKey(BigInteger serverEphemeralRsaExportPublicKey) {
        this.serverEphemeralRsaExportPublicKey = serverEphemeralRsaExportPublicKey;
    }

    public BigInteger getServerEphemeralRsaExportPrivateKey() {
        return serverEphemeralRsaExportPrivateKey;
    }

    public void setServerEphemeralRsaExportPrivateKey(
            BigInteger serverEphemeralRsaExportPrivateKey) {
        this.serverEphemeralRsaExportPrivateKey = serverEphemeralRsaExportPrivateKey;
    }

    public Integer getPeerReceiveLimit() {
        return peerReceiveLimit;
    }

    public void setPeerReceiveLimit(Integer peerReceiveLimit) {
        this.peerReceiveLimit = peerReceiveLimit;
    }
}
