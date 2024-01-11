package org.jboss.as.arquillian.container.protocol.appclient;

import org.jboss.arquillian.container.test.spi.client.protocol.ProtocolConfiguration;

public class AppClientProtocolConfiguration implements ProtocolConfiguration {
    private boolean runClient;

    public boolean isRunClient() {
        return runClient;
    }

    public void setRunClient(boolean runClient) {
        this.runClient = runClient;
    }
}
