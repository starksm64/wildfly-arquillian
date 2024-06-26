/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.arquillian.domain.container.controller.command;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.wildfly.arquillian.domain.api.DomainContainerController;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DomainContainerCommandObserver {

    @Inject
    private Instance<DomainContainerController> controllerInst;

    public void serverGroupLifecycle(@Observes ServerGroupLifecycleCommand event) {
        switch (event.getLifecycle()) {
            case RELOAD:
                controllerInst.get().reloadServers(event.getContainerQualifier(), event.getServerGroupName());
                break;
            case RESTART:
                controllerInst.get().restartServers(event.getContainerQualifier(), event.getServerGroupName());
                break;
            case RESUME:
                controllerInst.get().resumeServers(event.getContainerQualifier(), event.getServerGroupName());
                break;
            case START:
                controllerInst.get().startServers(event.getContainerQualifier(), event.getServerGroupName());
                break;
            case STOP:
                controllerInst.get().stopServers(event.getContainerQualifier(), event.getServerGroupName());
                break;
            case SUSPEND:
                controllerInst.get().suspendServers(event.getContainerQualifier(), event.getServerGroupName(),
                        event.getTimeout());
                break;
            default:
                // Shouldn't ever hit this
                throw new IllegalArgumentException("Could not determine how to execute the lifecycle " + event.getLifecycle());
        }
        event.setResult(Boolean.TRUE);
    }

    public void serverLifecycle(@Observes ServerLifecycleCommand event) {
        switch (event.getLifecycle()) {
            case RESTART:
                controllerInst.get().restartServer(event.getContainerQualifier(), event.getHostName(), event.getServerName());
                break;
            case RESUME:
                controllerInst.get().resumeServer(event.getContainerQualifier(), event.getHostName(), event.getServerName());
                break;
            case START:
                controllerInst.get().startServer(event.getContainerQualifier(), event.getHostName(), event.getServerName());
                break;
            case STOP:
                controllerInst.get().stopServer(event.getContainerQualifier(), event.getHostName(), event.getServerName());
                break;
            case SUSPEND:
                controllerInst.get().suspendServer(event.getContainerQualifier(), event.getHostName(), event.getServerName(),
                        event.getTimeout());
                break;
            default:
                // Shouldn't ever hit this
                throw new IllegalArgumentException("Could not determine how to execute the lifecycle " + event.getLifecycle());
        }
        event.setResult(Boolean.TRUE);
    }

    public void checkServerStatus(@Observes GetServerStatusCommand event) {
        event.setResult(controllerInst.get().isServerStarted(event.getContainerQualifier(), event.getHostName(),
                event.getServerName()));
    }
}
