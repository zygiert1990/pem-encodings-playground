package org.zygiert;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PEMDecoder;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public final class PEMBasedContextProvider implements SSLContextProvider {

    private static final String CLIENT_CERT_PATH = "client-cert.pem";
    private static final String CLIENT_KEY_PATH = "client-key.pem";
    private static final String CA_CERT_PATH = "ca-cert.pem";
    private static final String CLIENT_KEY_PASSWORD = "MyClientPassword123";

    @Override
    public SSLContext provide() {
        try {
            // decode pem files
            PEMDecoder decoder = PEMDecoder.of();
            X509Certificate clientCert = decoder.decode(readResource(CLIENT_CERT_PATH), X509Certificate.class);
            PrivateKey clientKey = decoder
                    .withDecryption(CLIENT_KEY_PASSWORD.toCharArray())
                    .decode(readResource(CLIENT_KEY_PATH), PrivateKey.class);
            X509Certificate caCert = decoder.decode(readResource(CA_CERT_PATH), X509Certificate.class);
            // init keystore
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry(
                    "client",
                    clientKey,
                    CLIENT_KEY_PASSWORD.toCharArray(),
                    new Certificate[]{clientCert}
            );
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, CLIENT_KEY_PASSWORD.toCharArray());
            // init truststore
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            // init SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            IO.println("Error when creating SSL context: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String readResource(String resourceName) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assert is != null;
            return new String(is.readAllBytes());
        }
    }

}
