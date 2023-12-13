package org.jboss.as.arquillian.container.protocol.appclient;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;

import java.util.Collection;

public class AppClientDeploymentPackager implements DeploymentPackager {
    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> processors) {
        return testDeployment.getApplicationArchive();
    }
}
