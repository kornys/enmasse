/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.watcher;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.CommonResourcesManager;
import io.enmasse.systemtest.manager.SharedResourceManager;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TestWatcher implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler, BeforeTestExecutionCallback, BeforeEachCallback, AfterTestExecutionCallback {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private TestInfo testInfo = TestInfo.getInstance();
    private CommonResourcesManager commonResourcesManager = CommonResourcesManager.getInstance();
    private SharedResourceManager sharedResourcesManager = SharedResourceManager.getInstance();

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
      LOGGER.info("Teardown section: ");
        if ((testInfo.getTests().size() != 1) && testInfo.getCurrentTestIndex() != testInfo.getTests().size() - 1) {
            if (testInfo.isTestShared() && sharedResourcesManager.getSharedAddressSpace() != null) {
                tearDownSharedResources();
            } else {
                tearDownCommonResources();
            }
        } else {
            tearDownCommonResources();
        }
    }

    private void tearDownCommonResources() throws Exception {
        LOGGER.info("Admin resource manager teardown");
        commonResourcesManager.tearDown(testInfo.getActualTest());
        commonResourcesManager.unsetReuseAddressSpace();
        commonResourcesManager.deleteAddressspacesFromList();
    }

    private void tearDownSharedResources() throws Exception {
        if (testInfo.isAddressSpaceDeletable()) {
            LOGGER.info("Teardown shared!");
            sharedResourcesManager.tearDown(testInfo.getActualTest());
        }
        if (sharedResourcesManager.getSharedAddressSpace() != null) {
            LOGGER.info("Deleting addresses");
            sharedResourcesManager.deleteAddresses(sharedResourcesManager.getSharedAddressSpace());
        }

    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState(context, throwable);
    }

    private void saveKubernetesState(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        if (isSkipSaveState()) {
            throw throwable;
        }

        Method testMethod = extensionContext.getTestMethod().orElse(null);
        Class<?> testClass = extensionContext.getRequiredTestClass();
        try {
            LOGGER.warn("Test failed: Saving pod logs and info...");
            Kubernetes kube = Kubernetes.getInstance();
            Path path = getPath(testMethod, testClass);
            Files.createDirectories(path);
            List<Pod> pods = kube.listPods();
            for (Pod p : pods) {
                try {
                    List<Container> containers = kube.getContainersFromPod(p.getMetadata().getName());
                    for (Container c : containers) {
                        Path filePath = path.resolve(String.format("%s_%s.log", p.getMetadata().getName(), c.getName()));
                        try {
                            Files.writeString(filePath, kube.getLog(p.getMetadata().getName(), c.getName()));
                        } catch (IOException e) {
                            LOGGER.warn("Cannot write file {}", filePath, e);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Cannot access logs from container: ", ex);
                }
            }

            kube.getLogsOfTerminatedPods(kube.getInfraNamespace()).forEach((name, podLogTerminated) -> {
                Path filePath = path.resolve(String.format("%s.terminated.log", name));
                try {
                    Files.writeString(filePath, podLogTerminated);
                } catch (IOException e) {
                    LOGGER.warn("Cannot write file {}", filePath, e);
                }
            });

            Files.writeString(path.resolve("describe_pods.txt"), KubeCMDClient.describePods(kube.getInfraNamespace()).getStdOut());
            Files.writeString(path.resolve("describe_nodes.txt"), KubeCMDClient.describeNodes().getStdOut());
            Files.writeString(path.resolve("events.txt"), KubeCMDClient.getEvents(kube.getInfraNamespace()).getStdOut());
            LOGGER.info("Pod logs and describe successfully stored into {}", path);
        } catch (Exception ex) {
            LOGGER.warn("Cannot save pod logs and info: ", ex);
        }
        throw throwable;
    }

    public static Path getPath(Method testMethod, Class<?> testClass) {
        Path path = Paths.get(
                Environment.getInstance().testLogDir(),
                "failed_test_logs",
                testClass.getName());
        if (testMethod != null) {
            path = path.resolve(testMethod.getName());
        }
        return path;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        LOGGER.warn("Before test exec");
        testInfo.setActualTest(context);
        if (testInfo.isTestShared()) {
            Environment.getInstance().getDefaultCredentials().setUsername("test").setPassword("test");
            Environment.getInstance().setManagementCredentials(new UserCredentials("artemis-admin", "artemis-admin"));
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        if (testInfo.isTestShared() && SharedResourceManager.getInstance().getAmqpClientFactory() == null) {
            sharedResourcesManager.setup();
        } else {
            commonResourcesManager.setup();
        }
    }

    private boolean isSkipSaveState() {
        return Environment.getInstance().isSkipSaveState();
    }
}