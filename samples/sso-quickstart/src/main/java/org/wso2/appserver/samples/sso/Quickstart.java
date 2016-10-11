/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.appserver.samples.sso;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import sun.misc.BASE64Encoder;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Tool to run the sso quick start.
 */
public class Quickstart {
    private static final Log log;
    private static final String WSO2_IS_VERSION = "wso2is-5.1.0";
    private static final String WSO2_IS_URL = "https://localhost:9443";
    private static final String USERNAME = "peter";
    private static final String PASSWORD = "peter123";

    static {
        System.setProperty("org.apache.juli.formatter", "org.apache.juli.VerbatimFormatter");
        log = LogFactory.getLog(Quickstart.class);
    }

    private Path wso2asPath = Paths.get("..", "..");
    private Path wso2isPath = Paths.get("packs", WSO2_IS_VERSION);
    private Path wso2isZipPath = Paths.get("packs", WSO2_IS_VERSION + ".zip");
    private Process wso2asProcess;
    private Process wso2isProcess;
    private ProcessBuilder wso2asProcessBuilder;
    private String operatingSystem = System.getProperty("os.name");

    public static void main(String[] args) throws IOException, InterruptedException {
        new Quickstart().runSample();
    }

    private void runSample() throws IOException, InterruptedException {
        log.info("Starting sample...\n");

        wso2asProcessBuilder = new ProcessBuilder().directory(wso2asPath.resolve("bin").toFile())
                .redirectErrorStream(true);

        String wso2isZipPathProperty = System.getProperty("wso2is.zip.path");
        if (wso2isZipPathProperty != null) {
            wso2isZipPath = Paths.get(wso2isZipPathProperty);
            if (Files.notExists(wso2isZipPath)) {
                log.error("WSO2 Identity Server path: " + wso2isZipPath + " could not be found. ");
                return;
            }
            Path parentisPath = wso2isZipPath.getParent();
            if (parentisPath != null) {
                wso2isPath = parentisPath.resolve(WSO2_IS_VERSION);
            } else {
                log.error("WSO2 Identity Server path: " + wso2isZipPath + " could not be found. ");
                return;
            }
        }

        if (Files.notExists(wso2isZipPath)) {
            log.error("WSO2 Identity server has not been found in the packs directory, "
                    + "Please copy " + WSO2_IS_VERSION + ".zip in to the packs directory and restart the sample.");
            return;
        }

        unzip(wso2isZipPath.toString(), wso2isZipPath.getParent().toString());
        makeFilesInDirExecutable(wso2isPath.resolve("bin"));

        registerShutdownHook();

        log.info("Following files will be changed during this sample\n1. <AS_HOME>/conf/server.xml\n2. "
                + "<AS_HOME>/conf/wso2/wso2as_web.xml\n3. <IS_HOME>/repository/conf/identity/sso-idp-config.xml\n");

        Path webappsDir = wso2asPath.resolve("webapps");

        log.info("Deploying bookstore-app");
        deployWebapp("http://product-dist.wso2.com/downloads/application-server/6.0.0/sso-samples/bookstore-app.war",
                webappsDir.resolve("bookstore-app.war"));
        log.info("Deploying musicstore-app\n");
        deployWebapp("http://product-dist.wso2.com/downloads/application-server/6.0.0/sso-samples/musicstore-app.war",
                webappsDir.resolve("musicstore-app.war"));

        // store original files
        Path serverxmlOriginalSrc = wso2asPath.resolve("conf").resolve("server.xml");
        Path wso2aswebxmlOriginalSrc = wso2asPath.resolve("conf").resolve("wso2").resolve("wso2as-web.xml");
        Path ssoidpconfigxmlOriginalSrc = wso2isPath.resolve("repository").resolve("conf").resolve("identity")
                .resolve("sso-idp-config.xml");

        Path originalsLocation = Paths.get("configfiles", "originals");
        if (Files.notExists(originalsLocation)) {
            Files.createDirectory(originalsLocation);
        }
        if (Files.notExists(originalsLocation.resolve("wso2as"))) {
            Files.createDirectory(originalsLocation.resolve("wso2as"));
        }
        if (Files.notExists(originalsLocation.resolve("wso2is"))) {
            Files.createDirectory(originalsLocation.resolve("wso2is"));
        }

        Path serverxmlOriginalDest = originalsLocation.resolve("wso2as").resolve("server.xml");
        Path wso2aswebxmlOriginalDest = originalsLocation.resolve("wso2as").resolve("wso2as-web.xml");
        Path ssoidpconfigxmlOriginalDest = originalsLocation.resolve("wso2is").resolve("sso-idp-config.xml");

        if (Files.notExists(serverxmlOriginalDest)) {
            Files.copy(serverxmlOriginalSrc, serverxmlOriginalDest, StandardCopyOption.REPLACE_EXISTING);
        }
        if (Files.notExists(wso2aswebxmlOriginalDest)) {
            Files.copy(wso2aswebxmlOriginalSrc, wso2aswebxmlOriginalDest, StandardCopyOption.REPLACE_EXISTING);
        }
        if (Files.notExists(ssoidpconfigxmlOriginalDest)) {
            Files.copy(ssoidpconfigxmlOriginalSrc, ssoidpconfigxmlOriginalDest, StandardCopyOption.REPLACE_EXISTING);
        }

        // copy sample files
        Path serverxmlSampleSrc = Paths.get("configfiles", "sampleconfigfiles", "wso2as", "server.xml");
        Path wso2aswebxmlSampleSrc = Paths.get("configfiles", "sampleconfigfiles", "wso2as", "wso2as-web.xml");
        Path ssoidpconfigxmlSampleSrc = Paths.get("configfiles", "sampleconfigfiles", "wso2is", "sso-idp-config.xml");

        Path serverxmlSampleDest = wso2asPath.resolve("conf").resolve("server.xml");
        Path wso2aswebxmlSampleDest = wso2asPath.resolve("conf").resolve("wso2").resolve("wso2as-web.xml");
        Path ssoidpconfigxmlSampleDest = wso2isPath.resolve("repository").resolve("conf").resolve("identity")
                .resolve("sso-idp-config.xml");

        Files.copy(serverxmlSampleSrc, serverxmlSampleDest, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(wso2aswebxmlSampleSrc, wso2aswebxmlSampleDest, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(ssoidpconfigxmlSampleSrc, ssoidpconfigxmlSampleDest, StandardCopyOption.REPLACE_EXISTING);

        // starting AS
        startasServer();

        // starting IS
        startisServer();

        //creating new user in identity server
        createNewUser();

        log.info("Go to following web app URLs to check the sso functionality.");
        log.info("Webapp1 URL: http://localhost:8080/musicstore-app/");
        log.info("Webapp2 URL: http://localhost:8080/bookstore-app/");
        log.info("\nUse the following credentials to log into the webapps: ");
        log.info("Username: " + USERNAME);
        log.info("Password: " + PASSWORD);

        if (operatingSystem.toLowerCase(Locale.ENGLISH).contains("windows")) {
            log.info("\nPlease run clean.bat file to revert the changes after you exit from the sample.");
        } else {
            log.info("\nPlease run clean.sh file to revert the changes after you exit from the sample.");
        }
        log.info("\nPress ctrl+c to exit from the sample....");

        while (true) {
            Thread.sleep(1000);
        }
    }

    private void createNewUser() throws IOException {
        String trustStorePath = wso2isPath.resolve("repository").resolve("resources")
                .resolve("security").resolve("wso2carbon.jks").toString();
        String trustStorePass = "wso2carbon";

        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePass);
        String encoding = new BASE64Encoder().encode("admin:admin".getBytes(UTF_8));

        URL requestUrlPost = new URL(WSO2_IS_URL + "/wso2/scim/Users");
        HttpURLConnection connectionPost = (HttpURLConnection) requestUrlPost.openConnection();
        connectionPost.setDoOutput(true);
        connectionPost.setRequestMethod("POST");
        connectionPost.setRequestProperty("Authorization", "Basic " + encoding);

        byte[] out = ("{\"schemas\":[],\"name\":{\"familyName\":\"family\",\"givenName\":\"" + USERNAME + "\"},"
                + "\"userName\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\","
                + "\"emails\":[{\"primary\":true,\"value\":\"wso2_home.com\",\"type\":\"home\"},"
                + "{\"value\":\"wso2_work.com\",\"type\":\"work\"}]}")
                .getBytes(UTF_8);
        int length = out.length;

        connectionPost.setFixedLengthStreamingMode(length);
        connectionPost.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connectionPost.setRequestProperty("Accept", "application/json; charset=UTF-8");

        try (OutputStream os = connectionPost.getOutputStream()) {
            os.write(out);
        }

        int responseCodePost = connectionPost.getResponseCode();
        if (responseCodePost == 201) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader((connectionPost.getInputStream()), UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    sb.append(output);
                }
                if (!sb.toString().contains("created")) {
                    throw new IOException("Error occured while creating a user in WSO2 Identity Server.");
                }
            }
        } else if (responseCodePost != 409) {
            throw new IOException("Error occured while creating a user in WSO2 Identity Server.");
        }
    }

    /**
     * Register shutdownhook for the sample to revert the changes.
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                // killing the application server process
                if (wso2asProcess != null) {
                    wso2asProcess.destroy();
                }
                // killing the identity server process
                if (wso2isProcess != null) {
                    wso2isProcess.destroy();
                }
            }
        });
    }

    private void deployWebapp(String sourceUrl, Path target) throws IOException {
        URL url = new URL(sourceUrl);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(target.toString());
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }

    private void unzip(String zipFilePath, String destDirectory) throws IOException {
        log.info("Extracting WSO2 Identity Server zip file...");
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            if (!destDir.exists()) {
                Files.createDirectory(destDir.toPath());
            }
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                if (!dir.exists()) {
                    Files.createDirectory(dir.toPath());
                }
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
        log.info("Extraction completed.\n");
    }

    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    private void makeFilesInDirExecutable(Path path) {
        File[] files = path.toFile().listFiles();
        if (files != null) {
            Arrays.asList(files).forEach(file -> file.setExecutable(true));
        }
    }

    /**
     * Start the Application server.
     *
     * @throws IOException
     */
    private void startasServer() throws IOException {
        wso2asProcessBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new File("wso2asServer.log")));
        if (operatingSystem.toLowerCase(Locale.ENGLISH).contains("windows")) {
            wso2asProcess = wso2asProcessBuilder.command("cmd.exe", "/C", "catalina.bat", "run").start();
        } else {
            wso2asProcess = wso2asProcessBuilder.command("./catalina.sh", "run").start();
        }

        waitForServerStartup(8080);
    }

    /**
     * Start the Identity server.
     *
     * @throws IOException
     */
    private void startisServer() throws IOException {
        log.info("Starting WSO2 Identity Server...(This will take few seconds)");
        if (operatingSystem.toLowerCase(Locale.ENGLISH).contains("windows")) {
            wso2isProcess = Runtime.getRuntime()
                    .exec("cmd.exe /C " + wso2isPath.resolve("bin").resolve("wso2server.bat"));
        } else {
            wso2isProcess = Runtime.getRuntime().exec(wso2isPath.resolve("bin").resolve("wso2server.sh")
                    .toAbsolutePath().toString());
        }

        // waiting for IS server startup
        String line;
        boolean isasStarted = false;
        try (BufferedReader input = new BufferedReader(
                new InputStreamReader(wso2isProcess.getInputStream(), UTF_8))) {
            while ((line = input.readLine()) != null) {
                if (line.contains("WSO2 Carbon started")) {
                    isasStarted = true;
                    break;
                }
            }
        }

        if (!isasStarted) {
            String message = "Error during WSO2 Identity server startup.";
            log.error(message);
            throw new IOException(message);
        }

        log.info("WSO2 Identity Server started.\n");
    }

    /**
     * Waiting for the corresponding port.
     *
     * @param port listening port
     * @throws IOException
     */
    private void waitForServerStartup(int port) throws IOException {
        int serverStartCheckTimeout = 120;
        log.info("Starting WSO2 Application Server...(This will take few seconds)");
        int startupCounter = 0;
        boolean isTimeout = false;
        while (!isServerListening("localhost", port)) {
            if (startupCounter >= serverStartCheckTimeout) {
                isTimeout = true;
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            startupCounter++;
        }

        if (!isTimeout) {
            log.info("WSO2 Application Server started.\n");
        } else {
            String message = "Server startup timeout.";
            log.error(message);
            throw new IOException(message);
        }
    }

    /**
     * Helper method for waitForServerStartup.
     *
     * @param host listening host
     * @param port listening port
     * @return boolean
     */
    private boolean isServerListening(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    log.error("Error while closing the socket.", e);
                }
            }
        }
    }
}
