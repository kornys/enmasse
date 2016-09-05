package enmasse.storage.controller.admin;

import com.openshift.restclient.model.IReplicationController;
import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.openshift.OpenshiftClient;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author lulf
 */
public class ClusterManagerTest {
    private OpenshiftClient mockClient;
    private ClusterManager manager;

    @Before
    public void setUp() {
        mockClient = mock(OpenshiftClient.class);
        manager = new ClusterManager(mockClient, new StorageGenerator(mockClient));
    }

    public void testModifiedBrokerDoesNotResetReplicaCount() {
        // Create simple queue and capture generated replication controller
        ArgumentCaptor<IReplicationController> arg = ArgumentCaptor.forClass(IReplicationController.class);

        Destination queue = new Destination("myqueue", true, false, "vanilla");
        manager.destinationsUpdated(Collections.singletonList(queue));
        verify(mockClient, VerificationModeFactory.atLeast(1)).createResource(arg.capture());

        IReplicationController controller = arg.getValue();
//        when(mockClient.listClusters()).thenReturn(Collections.singletonList(new StorageCluster(mockClient, queue,

        // Modify replicas and update controller
        controller.setReplicas(3);
        Destination modifiedQueue = new Destination("myqueue", true, true, "vanilla");
        manager.destinationsUpdated(Collections.singletonList(modifiedQueue));

        verify(mockClient).updateResource(arg.capture());
        assertThat(arg.getValue().getReplicas(), is(3));
    }
}
