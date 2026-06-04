# PEM Encodings Playground

A demonstration project for working with mutual TLS (mTLS) authentication using different ways of getting SSLContext.

## Prerequisites

- Java 26 (preview features enabled)
- Maven 3.x
- Docker (for running nginx server)
- OpenSSL (for certificate generation)
- Bash shell

## Setup Instructions

### 1. Generate Certificates

Run the certificate generation script to create CA, server, and client certificates in PEM format:

```bash
./scripts/generate-certs.sh
```

### 2. Run nginx Server

Run the nginx server container:

```bash
./scripts/start-nginx.sh
```

### 3. Build fat jar

```bash
mvn clean install
```

### 4. Run the Application – FROM THE ROOT DIRECTORY!

This project allows you to compare multiple approaches to mTLS client configuration:

- `JKS`
- `PKCS12`
- `JKS-BY-SYS-PROPS`
- `PKCS12-BY-SYS-PROPS`
- `BC`
- `PEM`

You can run the application by executing the following commands:

1. JKS:

```bash 
java -jar target/pem-encodings-playground-1.0-SNAPSHOT-jar-with-dependencies.jar JKS
```

2. PKCS12:

```bash
java -jar target/pem-encodings-playground-1.0-SNAPSHOT-jar-with-dependencies.jar PKCS12
```

3. JKS-BY-SYS-PROPS:

```bash
java -Djavax.net.ssl.keyStore=src/main/resources/client-keystore.jks -Djavax.net.ssl.trustStore=src/main/resources/client-truststore.jks -Djavax.net.ssl.keyStorePassword=keystorePassword -Djavax.net.ssl.trustStorePassword=truststorePassword -jar target/pem-encodings-playground-1.0-SNAPSHOT-jar-with-dependencies.jar JKS-BY-SYS-PROPS
```

4. PKCS12-BY-SYS-PROPS:

```bash
java -Djavax.net.ssl.keyStore=src/main/resources/client-keystore.p12 -Djavax.net.ssl.trustStore=src/main/resources/client-truststore.p12 -Djavax.net.ssl.keyStorePassword=keystorePassword -Djavax.net.ssl.trustStorePassword=truststorePassword -jar target/pem-encodings-playground-1.0-SNAPSHOT-jar-with-dependencies.jar PKCS12-BY-SYS-PROPS
```

5. BC:

```bash
java -jar target/pem-encodings-playground-1.0-SNAPSHOT-jar-with-dependencies.jar BC
```

6. PEM

```bash
java --enable-preview -jar target/pem-encodings-playground-1.0-SNAPSHOT-jar-with-dependencies.jar PEM
```

### 5. Cleanup

To stop and remove the nginx docker container and also to remove all generated certificates/private keys, please run:

```bash
./scripts/cleanup.sh
```