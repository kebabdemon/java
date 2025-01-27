import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

class WatchdogTask implements Runnable {
    private final Set<SocketTask> set = ConcurrentHashMap.newKeySet();

    public void addTask(SocketTask t) {
        set.add(t);
    }

    public void run() {
        while (true) {
            try {
                Iterator<SocketTask> i = set.iterator();
                long currentTime = System.currentTimeMillis();
                while (i.hasNext()) {
                    SocketTask task = i.next();
                    if (currentTime - task.getTimeOfLastActivity() > 500_000) {
                        System.out.println("Disconnecting inactive client: " + task.getSocket());
                        task.closeSocket();
                        i.remove();
                    }
                }
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class SocketTask implements Runnable {
    private final SSLSocket sslSocket;
    private long timeOfLastActivity = System.currentTimeMillis();

    public SocketTask(SSLSocket s) {
        this.sslSocket = s;
    }

    public SSLSocket getSocket() {
        return sslSocket;
    }

    public long getTimeOfLastActivity() {
        return timeOfLastActivity;
    }

    public void closeSocket() {
        try {
            sslSocket.close();
        } catch (IOException e) {
            System.err.println("Error when closing the socket: " + e.getMessage());
        }
    }

    public void run() {
        try {
            InputStream in = sslSocket.getInputStream();
            OutputStream out = sslSocket.getOutputStream();
            byte[] buffer = new byte[2048];
            int bytesRead;
            int totalByteCount = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                long currentTime = System.currentTimeMillis();
                double elapsedSeconds = (currentTime - timeOfLastActivity) / 1000.0;

                if (elapsedSeconds > 0) {
                    double bytesPerSecond = bytesRead / elapsedSeconds;
                    if (bytesPerSecond < 1) {
                        System.err.println("Slow connection!");
                    }
                }

                timeOfLastActivity = currentTime;

                totalByteCount += bytesRead;
                if (totalByteCount > 100) {
                    System.out.println("Client transferred more than 100 bytes of data, closing the connection.");
                    break;
                }

                out.write("Server responds: ".getBytes());
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            closeSocket();
        }
    }
}

public class hlavni {

    public static void main(String[] args) {
        String ksName = "VASEJMENO.p12";
        char[] ksPass = "testpass".toCharArray();
        char[] ctPass = "testpass".toCharArray();
        try {
            KeyStore ks = KeyStore.getInstance("pkcs12");
            ks.load(new FileInputStream(ksName), ksPass);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, ctPass);
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), null, null);
            SSLServerSocketFactory ssf = sc.getServerSocketFactory();
            SSLServerSocket sslServerSocket = (SSLServerSocket) ssf.createServerSocket(8888);
            sslServerSocket.setNeedClientAuth(false);

            System.out.println("Server started on port 8888");
            var executor = Executors.newFixedThreadPool(10);
            WatchdogTask watchdog = new WatchdogTask();
            executor.execute(watchdog);

            while (true) {
                SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                System.out.println("New client connected " + sslSocket.getInetAddress() + " port: " + sslSocket.getPort());


                SocketTask task = new SocketTask(sslSocket);
                watchdog.addTask(task);
                executor.execute(task);
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }


}
