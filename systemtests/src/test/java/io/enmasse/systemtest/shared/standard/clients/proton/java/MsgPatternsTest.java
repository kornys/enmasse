/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.standard.clients.proton.java;

import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedStandard;
import io.enmasse.systemtest.broker.ArtemisManagement;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.resolvers.ArtemisManagementParameterResolver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;

@Tag(ACCEPTANCE)
@ExtendWith(ArtemisManagementParameterResolver.class)
class MsgPatternsTest extends ClientTestBase implements ITestSharedStandard {

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    @DisplayName("testTopicSubscribe")
    void testTopicSubscribe(ArtemisManagement artemisManagement) throws Exception {
        doTopicSubscribeTest(artemisManagement, new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath), true);
    }

    @Test
    @Disabled("selectors for queue does not work")
    void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    @DisplayName("testMessageSelectorTopic")
    void testMessageSelectorTopic(ArtemisManagement artemisManagement) throws Exception {
        doMessageSelectorTopicTest(artemisManagement, new ProtonJMSClientSender(logPath), new ProtonJMSClientSender(logPath),
                new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath), true);
    }
}
