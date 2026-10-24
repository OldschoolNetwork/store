package com.oldschoolnetwork.store;

import android.app.Application;
import android.util.Log;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.*;

public class StoreApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        StoreRepository.getInstance().initialize(this);
        try {
            TrustManagerFactory system = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            system.init((KeyStore) null);
            KeyStore roots = KeyStore.getInstance(KeyStore.getDefaultType()); roots.load(null);
            CertificateFactory certificates = CertificateFactory.getInstance("X.509");
            int[] resources = {R.raw.isrgrootx1, R.raw.isrg_root_x2};
            for (int i = 0; i < resources.length; i++) try (InputStream input = getResources().openRawResource(resources[i])) {
                roots.setCertificateEntry("isrg-" + i, certificates.generateCertificate(input));
            }
            TrustManagerFactory extra = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); extra.init(roots);
            X509TrustManager nativeTrust = (X509TrustManager) system.getTrustManagers()[0];
            X509TrustManager isrgTrust = (X509TrustManager) extra.getTrustManagers()[0];
            X509TrustManager trust = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String type) throws CertificateException { nativeTrust.checkClientTrusted(chain, type); }
                public void checkServerTrusted(X509Certificate[] chain, String type) throws CertificateException {
                    try { nativeTrust.checkServerTrusted(chain, type); }
                    catch (CertificateException failure) { isrgTrust.checkServerTrusted(chain, type); }
                }
                public X509Certificate[] getAcceptedIssuers() {
                    List<X509Certificate> all = new ArrayList<>(Arrays.asList(nativeTrust.getAcceptedIssuers()));
                    all.addAll(Arrays.asList(isrgTrust.getAcceptedIssuers())); return all.toArray(new X509Certificate[0]);
                }
            };
            SSLContext context = SSLContext.getInstance("TLS"); context.init(null, new TrustManager[]{trust}, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(new TlsFactory(context.getSocketFactory()));
        } catch (Exception error) { Log.e("ONStore", "Could not initialize legacy TLS support", error); }
    }
    private static class TlsFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        TlsFactory(SSLSocketFactory delegate) { this.delegate = delegate; }
        private Socket enable(Socket socket) {
            if (socket instanceof SSLSocket) {
                SSLSocket ssl = (SSLSocket) socket;
                List<String> protocols = new ArrayList<>();
                for (String protocol : ssl.getSupportedProtocols()) if (protocol.startsWith("TLS")) protocols.add(protocol);
                ssl.setEnabledProtocols(protocols.toArray(new String[0]));
            }
            return socket;
        }
        public String[] getDefaultCipherSuites() { return delegate.getDefaultCipherSuites(); }
        public String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites(); }
        public Socket createSocket() throws java.io.IOException { return enable(delegate.createSocket()); }
        public Socket createSocket(Socket s, String host, int port, boolean close) throws java.io.IOException { return enable(delegate.createSocket(s, host, port, close)); }
        public Socket createSocket(String host, int port) throws java.io.IOException { return enable(delegate.createSocket(host, port)); }
        public Socket createSocket(String host, int port, InetAddress local, int localPort) throws java.io.IOException { return enable(delegate.createSocket(host, port, local, localPort)); }
        public Socket createSocket(InetAddress host, int port) throws java.io.IOException { return enable(delegate.createSocket(host, port)); }
        public Socket createSocket(InetAddress host, int port, InetAddress local, int localPort) throws java.io.IOException { return enable(delegate.createSocket(host, port, local, localPort)); }
    }
}
