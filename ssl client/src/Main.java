import java.io.*;
import java.security.*;
import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

class CustomTrustManager implements X509TrustManager {
    private final X509TrustManager defaultTrustManager;

    CustomTrustManager(X509TrustManager defaultTrustManager) {
        this.defaultTrustManager = defaultTrustManager;
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        throw new UnsupportedOperationException();
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        defaultTrustManager.checkServerTrusted(chain, authType);
    }

}

class CustomThread extends Thread {
    private final SSLSocket sslSocket;

    CustomThread(SSLSocket sslSocket) {
        this.sslSocket = sslSocket;
    }

    public void run() {
        try (InputStream input = sslSocket.getInputStream()) {
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                System.out.write(buffer, 0, bytesRead);
                System.out.flush();
            }
            sslSocket.close();
            System.out.println("Uspěšně ukončeno spojení");
        } catch (IOException e) {
            System.err.println("IOException");
        }
    }
}

public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Chybí parametry host a port");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            String keyStoreFile = "verejny.p12";
            char[] passphrase = "testpass".toCharArray();
            System.out.println("Načítání " + keyStoreFile + "...");

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream keyStoreInput = new FileInputStream(keyStoreFile)) {
                keyStore.load(keyStoreInput, passphrase);
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            X509TrustManager defaultTrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
            CustomTrustManager customTrustManager = new CustomTrustManager(defaultTrustManager);
            sslContext.init(null, new TrustManager[]{customTrustManager}, null);

            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(hostname, port);

            CustomThread customThread = new CustomThread(sslSocket);
            customThread.start();
            System.out.println("Připojeno na " + hostname + ":" + port);

            OutputStream output = sslSocket.getOutputStream();
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = System.in.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                output.flush();
                if (sslSocket.isClosed()) return;
            }

            sslSocket.close();
            System.out.println("ukončení");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
