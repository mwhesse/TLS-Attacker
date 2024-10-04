/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.integration.handshakes;

import static org.junit.Assume.assumeNotNull;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import de.rub.nds.tls.subject.ConnectionRole;
import de.rub.nds.tls.subject.TlsImplementationType;
import de.rub.nds.tls.subject.constants.TransportType;
import de.rub.nds.tls.subject.docker.*;
import de.rub.nds.tls.subject.docker.DockerTlsManagerFactory.TlsClientInstanceBuilder;
import de.rub.nds.tls.subject.docker.DockerTlsManagerFactory.TlsServerInstanceBuilder;
import de.rub.nds.tls.subject.docker.build.DockerBuilder;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlgorithmResolver;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.layer.constant.StackConfiguration;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.util.ProviderUtil;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutorFactory;
import de.rub.nds.tlsattacker.core.workflow.action.executor.WorkflowExecutorType;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsattacker.transport.TransportHandlerType;
import de.rub.nds.tlsattacker.util.FreePortFinder;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractHandshakeIT {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Integer PORT = FreePortFinder.getPossiblyFreePort();
    private static List<Image> localImages;

    private final TlsImplementationType implementation;
    private final TransportType transportType;
    private final ConnectionRole dockerConnectionRole;
    private final String version;
    private final String additionalParameters;

    private DockerTlsInstance dockerInstance;

    public AbstractHandshakeIT(
            TlsImplementationType implementation,
            ConnectionRole dockerConnectionRole,
            String version,
            String additionalParameters) {
        this(
                implementation,
                dockerConnectionRole,
                version,
                additionalParameters,
                TransportType.TCP);
    }

    public AbstractHandshakeIT(
            TlsImplementationType implementation,
            ConnectionRole dockerConnectionRole,
            String version,
            String additionalParameters,
            TransportType transportType) {
        this.implementation = implementation;
        this.dockerConnectionRole = dockerConnectionRole;
        this.version = version;
        this.additionalParameters = additionalParameters;
        this.transportType = transportType;
    }

    @BeforeAll
    public void loadList() throws InterruptedException {
        try {
            DockerClientManager.getDockerClient().listContainersCmd().exec();
        } catch (Exception ex) {
            Assume.assumeNoException(ex);
        }
        localImages = DockerTlsManagerFactory.getAllImages();

        ProviderUtil.addBouncyCastleProvider();

        DockerClientManager.setDockerServerUsername(System.getenv("DOCKER_USERNAME"));
        DockerClientManager.setDockerServerPassword(System.getenv("DOCKER_PASSWORD"));

        prepareContainer();
    }

    private void prepareContainer() throws DockerException, InterruptedException {
        Image image =
                DockerTlsManagerFactory.getMatchingImage(
                        localImages,
                        implementation,
                        version,
                        DockerBuilder.NO_ADDITIONAL_BUILDFLAGS,
                        dockerConnectionRole);
        getDockerInstance(image);
    }

    private void getDockerInstance(Image image) throws DockerException, InterruptedException {
        DockerTlsManagerFactory.TlsInstanceBuilder instanceBuilder;
        if (dockerConnectionRole == ConnectionRole.SERVER) {
            if (image != null) {
                instanceBuilder = new TlsServerInstanceBuilder(image, transportType);
            } else {
                instanceBuilder =
                        new TlsServerInstanceBuilder(implementation, version, transportType).pull();
                localImages = DockerTlsManagerFactory.getAllImages();
                assumeNotNull(
                        image,
                        String.format(
                                "TLS implementation %s %s not available",
                                implementation.name(), version));
            }
            instanceBuilder
                    .containerName("client-handshake-test-server-" + UUID.randomUUID())
                    .additionalParameters(additionalParameters);
        } else {
            TlsClientInstanceBuilder clientInstanceBuilder;
            if (image != null) {
                clientInstanceBuilder = new TlsClientInstanceBuilder(image, transportType);
            } else {
                clientInstanceBuilder =
                        new TlsClientInstanceBuilder(implementation, version, transportType).pull();
                localImages = DockerTlsManagerFactory.getAllImages();
                assumeNotNull(
                        image,
                        String.format(
                                "TLS implementation %s %s not available",
                                implementation.name(), version));
            }
            clientInstanceBuilder
                    .containerName("server-handshake-test-client-" + UUID.randomUUID())
                    .ip("172.17.0.1")
                    .port(PORT)
                    .connectOnStartup(false)
                    .additionalParameters(additionalParameters);
            instanceBuilder = clientInstanceBuilder;
        }
        dockerInstance = instanceBuilder.build();
        dockerInstance.start();
    }

    @ParameterizedTest
    @MethodSource("provideTestVectors")
    public final void testHandshakeSuccessfull(
            ProtocolVersion protocolVersion,
            NamedGroup namedGroup,
            CipherSuite cipherSuite,
            WorkflowTraceType workflowTraceType,
            boolean addEncryptThenMac,
            boolean addExtendedMasterSecret)
            throws InterruptedException {
        System.out.println(
                getParameterString(
                        protocolVersion,
                        namedGroup,
                        cipherSuite,
                        workflowTraceType,
                        addEncryptThenMac,
                        addExtendedMasterSecret));
        Config config = new Config();
        prepareConfig(
                cipherSuite,
                namedGroup,
                config,
                workflowTraceType,
                addExtendedMasterSecret,
                addEncryptThenMac,
                protocolVersion);

        State state = new State(config);
        modifyWorkflowTrace(state);
        WorkflowExecutor executor =
                WorkflowExecutorFactory.createWorkflowExecutor(
                        config.getWorkflowExecutorType(), state);
        setCallbacks(executor);

        executeTest(
                config,
                executor,
                state,
                protocolVersion,
                namedGroup,
                cipherSuite,
                workflowTraceType,
                addEncryptThenMac,
                addExtendedMasterSecret);
    }

    protected void executeTest(
            Config config,
            WorkflowExecutor executor,
            State state,
            ProtocolVersion protocolVersion,
            NamedGroup namedGroup,
            CipherSuite cipherSuite,
            WorkflowTraceType workflowTraceType,
            boolean addEncryptThenMac,
            boolean addExtendedMasterSecret)
            throws InterruptedException {

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            try {
                executor.executeWorkflow();
            } catch (Exception ignored) {
                System.out.println(
                        "Encountered exception during handshake (" + ignored.getMessage() + ")");
            }
            if (!state.getWorkflowTrace().executedAsPlanned() && (i + 1) < MAX_ATTEMPTS) {
                System.out.println("Failed to complete handshake, reexecuting...");
                killContainer();
                prepareContainer();
                setConnectionTargetFields(config);
                state = new State(config);
                modifyWorkflowTrace(state);
                executor =
                        WorkflowExecutorFactory.createWorkflowExecutor(
                                config.getWorkflowExecutorType(), state);
                setCallbacks(executor);
            } else if (state.getWorkflowTrace().executedAsPlanned()) {
                return;
            } else {
                failTest(
                        state,
                        protocolVersion,
                        namedGroup,
                        cipherSuite,
                        workflowTraceType,
                        addEncryptThenMac,
                        addExtendedMasterSecret);
            }
        }
    }

    private static final int MAX_ATTEMPTS = 3;

    private void failTest(
            State state,
            ProtocolVersion protocolVersion,
            NamedGroup namedGroup,
            CipherSuite cipherSuite,
            WorkflowTraceType workflowTraceType,
            boolean addEncryptThenMac,
            boolean addExtendedMasterSecret) {
        LOGGER.error("Failed trace: " + state.getWorkflowTrace().toString());
        try {
            LOGGER.error("Instance Feedback: " + dockerInstance.getLogs());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        printFailedContainerLogs();
        Assert.fail(
                "Failed to handshake with "
                        + implementation
                        + " parameters: "
                        + getParameterString(
                                protocolVersion,
                                namedGroup,
                                cipherSuite,
                                workflowTraceType,
                                addEncryptThenMac,
                                addExtendedMasterSecret));
    }

    private void printFailedContainerLogs() {
        String dockerId = dockerInstance.getId();
        System.out.println("Failed container docker logs:");
        DockerClientManager.getDockerClient()
                .logContainerCmd(dockerId)
                .withSince(0)
                .withStdOut(true)
                .withStdErr(true)
                .exec(
                        new ResultCallback.Adapter<Frame>() {
                            @Override
                            public void onNext(Frame frame) {
                                String log = (new String(frame.getPayload())).trim();
                                System.out.print(log);
                            }
                        });
    }

    public Stream<Arguments> provideTestVectors() {
        boolean[] addEncryptThenMacValues = getCryptoExtensionsValues();
        boolean[] addExtendedMasterSecretValues = getCryptoExtensionsValues();
        CipherSuite[] cipherSuites = getCipherSuitesToTest();
        NamedGroup[] namedGroups = getNamedGroupsToTest();
        ProtocolVersion[] protocolVersions = getProtocolVersionsToTest();
        WorkflowTraceType[] workflowTraceTypes = getWorkflowTraceTypesToTest();

        Builder<Arguments> builder = Stream.builder();
        for (boolean addEncryptThenMac : addEncryptThenMacValues) {
            for (boolean addExtendedMasterSecret : addExtendedMasterSecretValues) {
                for (CipherSuite cipherSuite : cipherSuites) {
                    for (NamedGroup namedGroup : namedGroups) {
                        for (ProtocolVersion protocolVersion : protocolVersions) {
                            for (WorkflowTraceType workflowTraceType : workflowTraceTypes) {
                                if (!cipherSuite.isSupportedInProtocol(protocolVersion)) {
                                    continue;
                                }
                                builder.add(
                                        Arguments.of(
                                                protocolVersion,
                                                namedGroup,
                                                cipherSuite,
                                                workflowTraceType,
                                                addEncryptThenMac,
                                                addExtendedMasterSecret));
                            }
                        }
                    }
                }
            }
        }
        return builder.build();
    }

    protected void modifyWorkflowTrace(State state) {
        return;
    }

    protected NamedGroup[] getNamedGroupsToTest() {
        return new NamedGroup[] {
            NamedGroup.SECP256R1, NamedGroup.SECP384R1, NamedGroup.SECP521R1, NamedGroup.ECDH_X25519
        };
    }

    protected ProtocolVersion[] getProtocolVersionsToTest() {
        return new ProtocolVersion[] {
            ProtocolVersion.TLS10, ProtocolVersion.TLS11, ProtocolVersion.TLS12
        };
    }

    protected CipherSuite[] getCipherSuitesToTest() {
        return new CipherSuite[] {
            CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
        };
    }

    protected WorkflowTraceType[] getWorkflowTraceTypesToTest() {
        return new WorkflowTraceType[] {
            WorkflowTraceType.HANDSHAKE, WorkflowTraceType.FULL_RESUMPTION
        };
    }

    protected boolean[] getCryptoExtensionsValues() {
        return new boolean[] {true, false};
    }

    protected void setCallbacks(WorkflowExecutor executor) {
        if (dockerConnectionRole == ConnectionRole.CLIENT) {
            executor.setBeforeTransportInitCallback(
                    (State state) -> {
                        ((DockerTlsClientInstance) dockerInstance).connect();
                        return 0;
                    });
        }
    }

    protected void prepareConfig(
            CipherSuite cipherSuite,
            NamedGroup namedGroup,
            Config config,
            WorkflowTraceType workflowTraceType,
            boolean addExtendedMasterSecret,
            boolean addEncryptThenMac,
            ProtocolVersion protocolVersion) {
        if (protocolVersion.isDTLS()) {
            config.getDefaultClientConnection().setTransportHandlerType(TransportHandlerType.UDP);
            config.getDefaultServerConnection().setTransportHandlerType(TransportHandlerType.UDP);
            config.setWorkflowExecutorType(WorkflowExecutorType.DTLS);
            config.setDefaultLayerConfiguration(StackConfiguration.DTLS);
            config.setFinishWithCloseNotify(true);
            config.setIgnoreRetransmittedCssInDtls(true);
            config.setAddRetransmissionsToWorkflowTraceInDtls(false);
        }
        if (cipherSuite.isTLS13()
                || AlgorithmResolver.getKeyExchangeAlgorithm(cipherSuite).isEC()) {
            config.setAddECPointFormatExtension(Boolean.TRUE);
            config.setAddEllipticCurveExtension(Boolean.TRUE);
        } else {
            config.setAddECPointFormatExtension(Boolean.FALSE);
            config.setAddEllipticCurveExtension(Boolean.FALSE);
        }
        config.setWorkflowTraceType(workflowTraceType);
        if (cipherSuite.isTLS13()) {
            config.setAddExtendedMasterSecretExtension(false);
            config.setAddEncryptThenMacExtension(false);
            config.setAddSupportedVersionsExtension(true);
            config.setAddKeyShareExtension(true);
            if (workflowTraceType == WorkflowTraceType.FULL_TLS13_PSK
                    || workflowTraceType == WorkflowTraceType.FULL_ZERO_RTT) {
                config.setAddPSKKeyExchangeModesExtension(true);
                config.setAddPreSharedKeyExtension(true);
            }
            if (workflowTraceType == WorkflowTraceType.FULL_ZERO_RTT) {
                config.setAddEarlyDataExtension(true);
            }
        } else {
            config.setAddExtendedMasterSecretExtension(addExtendedMasterSecret);
            config.setAddEncryptThenMacExtension(addEncryptThenMac);
        }
        config.setDefaultClientSupportedCipherSuites(cipherSuite);
        config.setDefaultServerSupportedCipherSuites(cipherSuite);
        config.setDefaultSelectedCipherSuite(cipherSuite);
        config.setDefaultServerNamedGroups(namedGroup);
        config.setDefaultSelectedNamedGroup(namedGroup);
        config.setDefaultEcCertificateCurve(namedGroup);
        config.setHighestProtocolVersion(protocolVersion);
        config.setDefaultSelectedProtocolVersion(protocolVersion);
        config.setSupportedVersions(protocolVersion);
        config.setRetryFailedClientTcpSocketInitialization(true);

        setConnectionTargetFields(config);
    }

    private void setConnectionTargetFields(Config config) {
        if (dockerConnectionRole == ConnectionRole.SERVER) {
            config.getDefaultClientConnection().setHostname("localhost");
            config.getDefaultClientConnection()
                    .setPort(((DockerTlsServerInstance) dockerInstance).getPort());
            config.getDefaultClientConnection().setTimeout(3000);
        } else {
            config.setDefaultRunningMode(RunningModeType.SERVER);
            config.getDefaultServerConnection().setHostname("server-handshake-test-host");
            config.getDefaultServerConnection().setPort(PORT);
            config.getDefaultServerConnection().setTimeout(3000);
        }
    }

    @AfterAll
    public void tearDown() {
        killContainer();
    }

    private void killContainer() {
        if (dockerInstance != null && dockerInstance.getId() != null) {
            dockerInstance.kill();
        }
    }

    private String getParameterString(
            ProtocolVersion protocolVersion,
            NamedGroup namedGroup,
            CipherSuite cipherSuite,
            WorkflowTraceType workflowTraceType,
            boolean addEncryptThenMac,
            boolean addExtendedMasterSecret) {
        return "PeerType="
                + dockerConnectionRole.name()
                + " Version="
                + protocolVersion
                + " NamedGroup="
                + namedGroup
                + " CipherSuite="
                + cipherSuite
                + " WorkflowTraceType="
                + workflowTraceType
                + " EncryptThenMac="
                + addEncryptThenMac
                + " ExtendedMasterSecret="
                + addExtendedMasterSecret;
    }
}
