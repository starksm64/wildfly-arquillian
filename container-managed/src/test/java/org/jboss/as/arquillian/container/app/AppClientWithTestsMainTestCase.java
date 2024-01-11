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

import java.io.File;

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
import org.junit.runner.RunWith;

/**
 * This version of the app client main includes junit tests that are to be run outside of the test vm
 *
 * To run in an IDE, set the -Darquillian.xml=appclient-arquillian.xml -Darquillian.launch=jboss-client-ee11-tck
 * properties the test VM arguments
 */
@RunWith(Arquillian.class)
public class AppClientWithTestsMainTestCase extends AppClientWithTestsMain {
    private static final Logger logger = Logger.getLogger("org.jboss.as.test.appclient");

    @TargetsContainer("jboss-client-ee11-tck")
    @OverProtocol("appclient")
    @Deployment(testable = true)
    public static EnterpriseArchive createDeployment() throws Exception {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "appClient" + ".ear");

        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "myejb.jar")
                .addClasses(EjbBean.class, EjbBusiness.class);
        ear.addAsModule(ejbJar);

        final JavaArchive appClient = ShrinkWrap.create(JavaArchive.class, "client-annotation.jar");
        appClient.addClasses(AppClientWithTestsMain.class, AnAppException.class);
        appClient.addAsManifestResource(new StringAsset("Main-Class: " + AppClientWithTestsMain.class.getName() + "\n"),
                "MANIFEST.MF");
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

}
