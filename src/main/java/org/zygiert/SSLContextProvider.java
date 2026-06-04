package org.zygiert;

import javax.net.ssl.SSLContext;

public sealed interface SSLContextProvider permits
        BundleBasedSSLContextProvider,
        DefaultContextProvider,
        PEMBasedContextProvider,
        BouncyCastleContextProvider {

    SSLContext provide();

}
