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

package org.jboss.as.arquillian.container.managed;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.as.arquillian.container.ParameterUtils;
import org.jboss.logging.Logger;

/**
 * Fork of the wildfly integration testsuite application client container runner
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author Stuart Douglas
 */
public class AppClientWrapper {
    private static final Logger LOGGER = Logger.getLogger(AppClientWrapper.class);

    private static final String outThreadHame = "APPCLIENT-out";
    private static final String errThreadHame = "APPCLIENT-err";

    private Process appClientProcess;
    private BufferedReader outputReader;
    private BufferedReader errorReader;
    private BlockingQueue<String> outputQueue = new LinkedBlockingQueue<String>();
    private ManagedContainerConfiguration config;
    private Logger log;

    /**
     * Creates new CLI wrapper. If the connect parameter is set to true the CLI
     * will connect to the server using <code>connect</code> command.
     *
     *
     * @param config
     * @throws Exception
     */
    public AppClientWrapper(ManagedContainerConfiguration config) throws Exception {
        this(config, LOGGER);
    }

    public AppClientWrapper(ManagedContainerConfiguration config, Logger log) {
        this.config = config;
        this.log = log;
    }

    public boolean waitForExit(long timeout, TimeUnit units) throws InterruptedException {
        return appClientProcess.waitFor(timeout, units);
    }

    /**
     * Consumes all available output from App Client using the output queue filled by the process
     * stanard out reader thread.
     *
     * @param timeout number of milliseconds to wait for each subsequent line
     * @return array of App Client output lines
     */
    public String[] readAll(final long timeout) {
        ArrayList<String> lines = new ArrayList<String>();
        String line = null;
        do {
            try {
                line = outputQueue.poll(timeout, TimeUnit.MILLISECONDS);
                if (line != null)
                    lines.add(line);
            } catch (InterruptedException ioe) {
            }

        } while (line != null);
        return lines.toArray(new String[] {});
    }

    /**
     * Kills the app client
     *
     * @throws Exception
     */
    public synchronized void quit() throws Exception {
        appClientProcess.destroy();
        try {
            appClientProcess.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts the app client in a new process and creates two thread to read the process output
     * and error streams.
     *
     * @throws Exception - on failure
     */
    public void run() throws Exception {
        run(null);
    }

    public void run(String... args) throws Exception {
        String[] cmdLine = getAppClientCommand();
        if (args != null) {
            String[] newCmdLine = new String[cmdLine.length + args.length];
            System.arraycopy(cmdLine, 0, newCmdLine, 0, cmdLine.length);
            System.arraycopy(args, 0, newCmdLine, cmdLine.length, args.length);
            cmdLine = newCmdLine;
        }
        String[] envp = getAppClientEnv();
        appClientProcess = Runtime.getRuntime().exec(cmdLine, envp);
        log.info("Created process" + appClientProcess.info());
        outputReader = new BufferedReader(new InputStreamReader(appClientProcess.getInputStream(), StandardCharsets.UTF_8));
        errorReader = new BufferedReader(new InputStreamReader(appClientProcess.getErrorStream(), StandardCharsets.UTF_8));

        final Thread readOutputThread = new Thread(this::readClientOut, outThreadHame);
        readOutputThread.start();
        final Thread readErrorThread = new Thread(this::readClientErr, errThreadHame);
        readErrorThread.start();
        log.info("Started process reader threads");
    }

    private String[] getAppClientCommand() throws Exception {
        ArrayList<String> cmd = new ArrayList<>();

        final String archivePath = config.getClientAppEar();
        final String clientArchiveName = config.getClientArchiveName();

        String jbossHome = config.getJbossHome();
        if (jbossHome == null)
            throw new Exception("jbossHome config property is not set.");
        if (!new File(jbossHome).exists())
            throw new Exception("AS dir from config jbossHome doesn't exist: " + jbossHome);

        String archiveArg = String.format("%s#%s", archivePath, clientArchiveName);

        String client = config.getAppClientShForOS();
        String clientSh = String.format("%s%cbin%c%s", jbossHome, File.separatorChar, File.separatorChar, client);
        cmd.add(clientSh);
        cmd.add(archiveArg);
        if (config.getClientArguments() != null) {
            cmd.addAll(ParameterUtils.splitParams(config.getClientArguments()));
        }

        log.info("AppClient cmd: " + cmd);
        String[] cmdLine = new String[cmd.size()];
        cmd.toArray(cmdLine);
        return cmdLine;
    }

    private String[] getAppClientEnv() {
        ArrayList<String> env = new ArrayList<>();
        for (Map.Entry<String, String> entry : config.getClientEnv().entrySet()) {
            env.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }
        log.info("AppClient env: " + env);
        String[] envp = new String[env.size()];
        env.toArray(envp);
        return envp;
    }

    private void readClientOut() {
        if (outputReader == null)
            return;

        readClientProcess(outputReader, false);
        synchronized (this) {
            outputReader = null;
        }
    }

    private void readClientErr() {
        if (errorReader == null)
            return;
        readClientProcess(errorReader, true);
        synchronized (this) {
            errorReader = null;
        }
    }

    /**
     * Loop
     */
    private void readClientProcess(BufferedReader reader, boolean errReader) {
        log.info("Begin readClientProcess");
        int count = 0;
        try {
            String line = reader.readLine();
            // System.out.println("RCP: " + line);
            while (line != null) {
                count++;
                if (errReader)
                    errorLineReceived(line);
                else
                    outputLineReceived(line);
                line = reader.readLine();
            }
        } catch (Throwable e) {
            log.warn("error during read", e);
        }
        log.info(String.format("Exiting(%s), read %d lines", errReader, count));
    }

    private synchronized void outputLineReceived(String line) {
        log.info("[" + outThreadHame + "] " + line);
        outputQueue.add(line);
    }

    private synchronized void errorLineReceived(String line) {
        log.info("[" + errThreadHame + "] " + line);
    }

}
