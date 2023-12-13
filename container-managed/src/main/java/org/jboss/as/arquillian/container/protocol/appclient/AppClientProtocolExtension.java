package org.jboss.as.arquillian.container.protocol.appclient;

import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class AppClientProtocolExtension implements LoadableExtension {

    @Override
    public void register(LoadableExtension.ExtensionBuilder builder) {
        builder.service(Protocol.class, AppClientProtocol.class);
    }
}
