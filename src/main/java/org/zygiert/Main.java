package org.zygiert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

import static org.zygiert.BundleBasedSSLContextProvider.BundleType.JKS;
import static org.zygiert.BundleBasedSSLContextProvider.BundleType.PKCS12;

public class Main {

    private static final String SERVER_URL = "https://localhost:8443/hello";

    static void main(String[] args) {
        if (args.length != 1) {
            IO.println("Please provide a way to handle SSLContext creation!");
            IO.println("Supported modes: JKS, PKCS12, PEM, JKS-BY-SYS-PROPS, PKCS12-BY-SYS-PROPS, BC, PEM");
            return;
        }
        SSLContextProvider sslContextProvider = prepareEnvAndPickSSLContextProvider(args[0]);
        try (HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContextProvider.provide())
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL))
                    .GET()
                    .build();

            // Send request and get response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Print response
            IO.println("=============================================");
            IO.println("Status Code: " + response.statusCode());
            IO.println("Response Headers:");
            response.headers().map()
                    .forEach((k, v) -> IO.println("  " + k + ": " + String.join(", ", v)));
            IO.println("Response Body: " + response.body());
            IO.println("=============================================");
        } catch (Exception e) {
            IO.println("Error when executing main method: " + e.getMessage());
            e.printStackTrace();
        } finally {
            runBashScript("scripts/cleanup-bundles.sh");
        }
    }

    private static SSLContextProvider prepareEnvAndPickSSLContextProvider(String mode) {
        return switch (mode) {
            case "JKS" -> {
                runBashScript("scripts/create-jks-bundles.sh");
                yield new BundleBasedSSLContextProvider(JKS);
            }
            case "PKCS12" -> {
                runBashScript("scripts/create-pkcs12-bundles.sh");
                yield new BundleBasedSSLContextProvider(PKCS12);
            }
            case "JKS-BY-SYS-PROPS" -> {
                // -Djavax.net.ssl.keyStore=src/main/resources/client-keystore.jks -Djavax.net.ssl.trustStore=src/main/resources/client-truststore.jks -Djavax.net.ssl.keyStorePassword=keystorePassword -Djavax.net.ssl.trustStorePassword=truststorePassword
                Objects.requireNonNull(System.getProperty("javax.net.ssl.keyStore"), "System property 'javax.net.ssl.keyStore' is not set");
                Objects.requireNonNull(System.getProperty("javax.net.ssl.keyStorePassword"), "System property 'javax.net.ssl.keyStorePassword' is not set");
                Objects.requireNonNull(System.getProperty("javax.net.ssl.trustStore"), "System property 'javax.net.ssl.trustStore' is not set");
                Objects.requireNonNull(System.getProperty("javax.net.ssl.trustStorePassword"), "System property 'javax.net.ssl.trustStorePassword' is not set");
                runBashScript("scripts/create-jks-bundles.sh");
                yield new DefaultContextProvider();
            }
            case "PKCS12-BY-SYS-PROPS" -> {
                // -Djavax.net.ssl.keyStore=src/main/resources/client-keystore.p12 -Djavax.net.ssl.trustStore=src/main/resources/client-truststore.p12 -Djavax.net.ssl.keyStorePassword=keystorePassword -Djavax.net.ssl.trustStorePassword=truststorePassword
                Objects.requireNonNull(System.getProperty("javax.net.ssl.keyStore"), "System property 'javax.net.ssl.keyStore' is not set");
                Objects.requireNonNull(System.getProperty("javax.net.ssl.keyStorePassword"), "System property 'javax.net.ssl.keyStorePassword' is not set");
                Objects.requireNonNull(System.getProperty("javax.net.ssl.trustStore"), "System property 'javax.net.ssl.trustStore' is not set");
                Objects.requireNonNull(System.getProperty("javax.net.ssl.trustStorePassword"), "System property 'javax.net.ssl.trustStorePassword' is not set");
                runBashScript("scripts/create-pkcs12-bundles.sh");
                yield new DefaultContextProvider();
            }
            case "BC" -> new BouncyCastleContextProvider();
            case "PEM" -> new PEMBasedContextProvider();
            default -> throw new IllegalArgumentException("Unsupported SSLContext mode: " + mode);
        };
    }

    private static void runBashScript(String script) {
        String scriptPath = System.getProperty("user.dir") + "/" + script;
        IO.println("Running script: " + scriptPath);
        ProcessBuilder processBuilder = new ProcessBuilder("bash", scriptPath);
        processBuilder.redirectErrorStream(true);
        try (Process process = processBuilder.start()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    IO.println(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Script execution failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            IO.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        IO.println("Script executed successfully");
    }
}
