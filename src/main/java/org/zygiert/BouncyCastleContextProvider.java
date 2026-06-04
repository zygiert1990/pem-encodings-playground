package org.zygiert;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public final class BouncyCastleContextProvider implements SSLContextProvider {

    private static final String CLIENT_CERT_PATH = "client-cert.pem";
    private static final String CLIENT_KEY_PATH = "client-key.pem";
    private static final String CA_CERT_PATH = "ca-cert.pem";
    private static final String CLIENT_KEY_PASSWORD = "MyClientPassword123";

    static {
        // Register BouncyCastle as a security provider
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public SSLContext provide() {
        try {
            // Parse PEM files using BouncyCastle
            X509Certificate clientCert = parseCertificate(readResource(CLIENT_CERT_PATH));
            PrivateKey clientKey =
                    parseEncryptedPrivateKey(readResource(CLIENT_KEY_PATH), CLIENT_KEY_PASSWORD.toCharArray());
            X509Certificate caCert = parseCertificate(readResource(CA_CERT_PATH));

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

    private X509Certificate parseCertificate(String certificate) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(certificate))) {
            Object object = pemParser.readObject();
            if (object instanceof X509CertificateHolder holder) {
                return new JcaX509CertificateConverter()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .getCertificate(holder);
            }
            throw new IllegalArgumentException("Invalid certificate format");
        }
    }

    private PrivateKey parseEncryptedPrivateKey(String privateKey, char[] password) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(privateKey))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);

            if (object instanceof PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo) {
                InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(password);
                PrivateKeyInfo privateKeyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider);
                return converter.getPrivateKey(privateKeyInfo);
            }
            throw new IllegalArgumentException("Invalid private key format. Expected PKCS8EncryptedPrivateKeyInfo");
        }
    }

    private String readResource(String resourceName) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assert is != null;
            return new String(is.readAllBytes());
        }
    }
}