package org.jboss.as.arquillian.container.protocol.appclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;

public class AppClientDeploymentPackager implements DeploymentPackager {
    private static String SCRIPT_TEMPLATE = "RULE check main throws\n" +
            "CLASS __APP_CLIENT_MAIN__\n" +
            "METHOD main\n" +
            "HELPER org.jboss.as.arquillian.container.protocol.appclient.AppMainHelper\n" +
            "AT EXCEPTION EXIT\n" +
            "IF true\n" +
            "DO exceptionResult($^)\n" +
            "ENDRULE\n";

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> processors) {
        Archive<?> archive = testDeployment.getApplicationArchive();

        EnterpriseArchive ear = (EnterpriseArchive) archive;
        String mainClass = extractAppMainClient(ear);
        if(mainClass != null) {
            // Add a byteman script to catch
            String src = SCRIPT_TEMPLATE.replace("__APP_CLIENT_MAIN__", mainClass);
            ear.add(new StringAsset(src), "scripts/AppClientMain.btm");
            try {
                Files.write(Paths.get("/tmp/AppClientMain.btm"), src.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return archive;
    }

    private static String extractAppMainClient(EnterpriseArchive ear) {
        String mainClass = null;
        Map<ArchivePath, Node> contents = ear.getContent();
        for (Node node : contents.values()) {
            Asset asset = node.getAsset();
            if(asset instanceof ArchiveAsset) {
                ArchiveAsset jar = (ArchiveAsset) asset;
                Node mfNode = jar.getArchive().get("META-INF/MANIFEST.MF");
                if(mfNode == null) continue;

                StringAsset manifest = (StringAsset) mfNode.getAsset();
                String source = manifest.getSource();
                String[] lines = source.split("\n");
                for (String line : lines) {
                    if(line.startsWith("Main-Class:")) {
                        mainClass = line.substring(11).trim();
                        break;
                    }
                }
            }
        }
        return mainClass;
    }
}
