package enmasse.storage.controller.generator;

import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.openshift.StorageCluster;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author lulf
 */
public class StorageClusterGeneratorTest {
    private OpenshiftClient mockClient;

    @Before
    public void setUp() {
        mockClient = mock(OpenshiftClient.class);
    }

    public void testSkipNoStore() {
        StorageGenerator generator = new StorageGenerator(mockClient);
        List<StorageCluster> resources = generator.generate(Arrays.asList(new Destination("foo", true, false, "foo"), new Destination("bar", false, false, "bar")));
        assertThat(resources.size(), is(1));
    }

    public void testGenerate() {
        StorageGenerator generator = new StorageGenerator(mockClient);
        List<StorageCluster> clusterList = generator.generate(Arrays.asList(new Destination("foo", true, false, "vanilla"), new Destination("bar", false, false, "chili")));
        assertThat(clusterList.size(), is(1));
        StorageCluster cluster = clusterList.get(0);
        assertThat(cluster.getDestination().address(), is("foo"));
    }

    private IReplicationController getController(List<IResource> resources) {
        return resources.stream()
                .filter(resource -> resource instanceof IReplicationController)
                .map(resource -> (IReplicationController)resource)
                .findAny()
                .get();
    }
}
