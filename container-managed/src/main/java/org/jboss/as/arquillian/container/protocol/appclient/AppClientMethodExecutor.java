package org.jboss.as.arquillian.container.protocol.appclient;

import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.as.arquillian.container.managed.AppClientWrapper;
import org.jboss.logging.Logger;

public class AppClientMethodExecutor implements ContainerMethodExecutor {
    static Logger log = Logger.getLogger(AppClientMethodExecutor.class);
    private AppClientWrapper appClient;

    public AppClientMethodExecutor(AppClientWrapper appClient) {
        this.appClient = appClient;
    }

    @Override
    public TestResult invoke(TestMethodExecutor testMethodExecutor) {
        TestResult result = TestResult.passed();
        String[] lines = appClient.readAll(5000);

        log.info(String.format("AppClient(%s) readAll returned %d lines\n", testMethodExecutor.getMethodName(), lines.length));
        boolean sawStart = false, sawEnd = false, sawResult = false, sawSuccess = false, sawFailed = false;
        String failedLine = null;
        for (String line : lines) {
            System.out.println(line);
            if (line.contains("AppClientMain.begin")) {
                sawStart = true;
            } else if (line.contains("AppClientMain.end")) {
                sawEnd = true;
            } else if (line.contains("AppClientMain.FAILED")) {
                sawFailed = true;
                failedLine = line;
            } else if (line.contains("AppClientMain.SUCCESS")) {
                sawSuccess = true;
            } else if (line.contains("AppClientMain.RESULT: clientCall(testAppClientRunViaArq)")) {
                sawResult = true;
                result.addDescription(line);
            }
        }
        if (!sawEnd) {
            Throwable ex = AppMainHelper.exitThrowable();
            result = TestResult.failed(ex);
        } else if(sawFailed) {
            result = TestResult.failed(new Exception(failedLine));
        }
        return result;
    }
}
