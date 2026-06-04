package org.zygiert;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public final class BundleBasedSSLContextProvider implements SSLContextProvider {

    private static final String KEYSTORE_PASSWORD = "keystorePassword";
    private static final String TRUSTSTORE_PASSWORD = "truststorePassword";

    private final String keystorePath;
    private final String truststorePath;
    private final String type;

    public BundleBasedSSLContextProvider(BundleType bundleType) {
        this.type = bundleType.type;
        this.keystorePath = System.getProperty("user.dir") + "/src/main/resources/client-keystore" + bundleType.fileExtension;
        this.truststorePath = System.getProperty("user.dir") + "/src/main/resources/client-truststore" + bundleType.fileExtension;
    }

    @Override
    public SSLContext provide() {
        try {
            KeyStore keyStore = KeyStore.getInstance(type);
            try (InputStream is = new FileInputStream(keystorePath)) {
                keyStore.load(is, KEYSTORE_PASSWORD.toCharArray());
            }
            KeyStore trustStore = KeyStore.getInstance(type);
            try (InputStream is = new FileInputStream(truststorePath)) {
                trustStore.load(is, TRUSTSTORE_PASSWORD.toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            IO.println("Error when creating SSL context: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public enum BundleType {
        JKS("JKS", ".jks"),
        PKCS12("PKCS12", ".p12"),;

        private final String type;
        private final String fileExtension;

        BundleType(String type, String fileExtension) {
            this.type = type;
            this.fileExtension = fileExtension;
        }
    }

}
