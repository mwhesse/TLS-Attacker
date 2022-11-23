/*
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsattacker.core.protocol;

public class XmlSerializationIT {

    /* @Test
    @Tag(TestCategories.INTEGRATION_TEST)
    public void testProtocolMessages(@TempDir File tempDir) throws Exception {
        List<ProtocolMessage> messageList = MessageFactory.generateProtocolMessages();
        for (ProtocolMessage message : messageList) {
            WorkflowTrace trace = new WorkflowTrace();
            trace.addTlsAction(new SendAction(message));
            trace.addTlsAction(new ReceiveAction(message));
            File f = new File(tempDir, "testProtocolMessagesWorkflow.xml");
            assert f.exists() || f.createNewFile();
            WorkflowTraceSerializer.write(f, trace);
            WorkflowTrace newWorkflowTrace;
            try (FileInputStream fis = new FileInputStream(f)) {
                newWorkflowTrace = WorkflowTraceSerializer.secureRead(fis);
            }

            assertEquals(2, newWorkflowTrace.getTlsActions().size());

            assertEquals(
                    1,
                    ((MessageAction) newWorkflowTrace.getTlsActions().get(0)).getMessages().size(),
                    "Message failed: " + message.getClass().getName());
            assertEquals(
                    message.getClass(),
                    ((MessageAction) newWorkflowTrace.getTlsActions().get(0))
                            .getMessages()
                            .get(0)
                            .getClass(),
                    "Message failed: " + message.getClass().getName());
            assertEquals(
                    1,
                    ((ReceiveAction) newWorkflowTrace.getTlsActions().get(1))
                            .getExpectedMessages()
                            .size(),
                    "Message failed: " + message.getClass().getName());
            assertEquals(
                    message.getClass(),
                    ((ReceiveAction) newWorkflowTrace.getTlsActions().get(1))
                            .getExpectedMessages()
                            .get(0)
                            .getClass(),
                    "Message failed: " + message.getClass().getName());
        }
    }

    @Test
    @Tag(TestCategories.INTEGRATION_TEST)
    public void testExtensionMessages(@TempDir File tempDir) throws Exception {
        List<ExtensionMessage> extensionList = MessageFactory.generateExtensionMessages();
        for (ExtensionMessage extension : extensionList) {
            ClientHelloMessage message = new ClientHelloMessage();
            message.addExtension(extension);
            WorkflowTrace trace = new WorkflowTrace();
            trace.addTlsAction(new SendAction(message));
            trace.addTlsAction(new ReceiveAction(message));
            File f = new File(tempDir, "testExtensionMessagesWorkflow.xml");
            assert f.exists() || f.createNewFile();
            WorkflowTraceSerializer.write(f, trace);
            WorkflowTrace newWorkflowTrace;
            try (FileInputStream fis = new FileInputStream(f)) {
                newWorkflowTrace = WorkflowTraceSerializer.secureRead(fis);
            }
            assertEquals(2, newWorkflowTrace.getTlsActions().size());

            assertEquals(
                    1,
                    ((MessageAction) newWorkflowTrace.getTlsActions().get(0)).getMessages().size(),
                    "Extension failed: " + extension.getClass().getName());
            HandshakeMessage handshakeMessage =
                    (HandshakeMessage)
                            (((MessageAction) newWorkflowTrace.getTlsActions().get(0))
                                    .getMessages()
                                    .get(0));
            assertNotNull(handshakeMessage);
            assertEquals(
                    extension.getClass(),
                    handshakeMessage.getExtensions().get(0).getClass(),
                    "Extension failed: " + extension.getClass().getName());

            assertEquals(
                    1,
                    ((ReceiveAction) newWorkflowTrace.getTlsActions().get(1))
                            .getExpectedMessages()
                            .size(),
                    "Extension failed: " + extension.getClass().getName());
            handshakeMessage =
                    (HandshakeMessage)
                            (((ReceiveAction) newWorkflowTrace.getTlsActions().get(1))
                                    .getExpectedMessages()
                                    .get(0));
            assertNotNull(handshakeMessage);
            assertEquals(
                    handshakeMessage.getExtensions().get(0).getClass(),
                    extension.getClass(),
                    "Extension failed: " + extension.getClass().getName());
        }
    }*/
}
