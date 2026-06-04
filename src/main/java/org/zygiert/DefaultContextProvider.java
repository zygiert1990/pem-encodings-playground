package org.zygiert;

import javax.net.ssl.SSLContext;

public final class DefaultContextProvider implements SSLContextProvider {

    @Override
    public SSLContext provide() {
        try {
            return SSLContext.getDefault();
        } catch (Exception e) {
            IO.println("Error when creating SSL context: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
