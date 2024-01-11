package org.jboss.as.arquillian.container.app;

import org.jboss.as.arquillian.container.managed.ManagedContainerConfiguration;
import org.junit.Assert;
import org.junit.Test;

public class AppConfigTestCase {
    @Test
    public void testEnvConfig() {
        ManagedContainerConfiguration config = new ManagedContainerConfiguration();
        config.setClientEnvString(
                "JAVA_OPTS=-Djboss.modules.system.pkgs=com.sun.ts.lib,com.sun.javatest;CLASSPATH=${project.build.directory}/appclient/libutil.jar:${project.build.directory}/appclient/libcommon.jar:${project.build.directory}/appcient/jakartaee-api.jar");
        String javaOpts = config.getClientEnv().get("JAVA_OPTS");
        Assert.assertEquals("parsed correctly", "-Djboss.modules.system.pkgs=com.sun.ts.lib,com.sun.javatest", javaOpts);
    }
}
