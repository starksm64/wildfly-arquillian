/*
 * Copyright 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.app;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/** This version of the app client main includes junit tests that are to be run outside of the test vm
 *
 * To run in an IDE, set the -Darquillian.xml=appclient-arquillian.xml -Darquillian.launch=jboss-client-ee11-tck
 * properties the test VM arguments
 */
@RunWith(Arquillian.class)
public class AppClientWithTestsMainTestCase {
    private static final Logger logger = Logger.getLogger("org.jboss.as.test.appclient");

    @Resource(lookup = "java:comp/InAppClientContainer")
    private static boolean appclient;

    private String param;

    @EJB
    private static EjbBusiness appClientSingletonRemote;

    @TargetsContainer("jboss-client-ee11-tck")
    @OverProtocol("appclient")
    @Deployment(testable = true)
    public static EnterpriseArchive createDeployment() throws Exception {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "appClient" + ".ear");

        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "myejb.jar")
                .addClasses(EjbBean.class, EjbBusiness.class);
        ear.addAsModule(ejbJar);

        final JavaArchive appClient = ShrinkWrap.create(JavaArchive.class, "client-annotation.jar");
        appClient.addClasses(AppClientMain.class);
        appClient.addAsManifestResource(new StringAsset("Main-Class: " + AppClientMain.class.getName() + "\n"), "MANIFEST.MF");
        ear.addAsModule(appClient);

        File archiveOnDisk = new File("target" + File.separator + ear.getName());
        if (archiveOnDisk.exists()) {
            archiveOnDisk.delete();
        }
        final ZipExporter exporter = ear.as(ZipExporter.class);
        exporter.exportTo(archiveOnDisk);
        String archivePath = archiveOnDisk.getAbsolutePath();
        System.out.printf("archivePath: %s\n", archivePath);

        return ear;
    }

    public static void main(final String[] params) throws Exception {
        logger.info("AppClientMain.begin");

        if (!appclient) {
            logger.error("InAppClientContainer was not true, FAILED");
            throw new RuntimeException("InAppClientContainer was not true");
        }

        String testName = null;
        for(int n = 0; n < params.length; n ++) {
            String p = params[n];
            if(p.equals("-t")) {
                testName = params[n+1];
                break;
            }
        }
        if(testName == null) {
            throw new IllegalStateException("No test name given, use -t <test-name>");
        }
        AppClientWithTestsMainTestCase instance  = new AppClientWithTestsMainTestCase();
        instance.param = testName;
        if(testName.equals("testCallEjb")) {
            instance.testCallEjb();
        } else if(testName.equals("testWithException")) {
                instance.testWithException();
        }

        logger.info("AppClientMain.end");
    }

    @Test
    @TargetsContainer("jboss-client-ee11-tck")
    public void testCallEjb() {
        try {
            String result = appClientSingletonRemote.clientCall(param);
            logger.info("AppClientMain.RESULT: " + result);
            logger.info("AppClientMain.SUCCESS");
        } catch (Exception e) {
            logger.error("AppClientMain.FAILED", e);
        }
    }

    @Test(expected = AnAppException.class)
    @TargetsContainer("jboss-client-ee11-tck")
    public void testWithException() throws AnAppException {
        throw new AnAppException("testWithException has failed");
    }
}
