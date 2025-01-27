import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

class Watchdog implements Runnable {
    static Set<ChatClient> clientSet = ConcurrentHashMap.newKeySet(1000);

    public void addClient(ChatClient client) {
        clientSet.add(client);
    }

    public void run() {
        try {
            while (true) {
                Iterator<ChatClient> iterator = clientSet.iterator();
                long currentTime = System.currentTimeMillis();
                while (iterator.hasNext()) {
                    ChatClient client = iterator.next();
                    if (currentTime - client.inputTask.timeOfLastActivity > 600000) {
                        System.out.println("Disconnecting inactive user: " + client.nickname);
                        client.inputTask.sslSocket.close();
                        iterator.remove();
                    }
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public ChatClient getClientByNickname(String nickname) {
        for (ChatClient client : clientSet) {
            if (client.nickname.equals(nickname)) {
                return client;
            }
        }
        return null;
    }
}

class ChatClient {
    SocketReaderTask inputTask;
    SocketWriterTask outputTask;
    String nickname;

    public ChatClient(SSLSocket sslSocket, Watchdog watchdog) {
        inputTask = new SocketReaderTask(sslSocket, watchdog, this);
        outputTask = new SocketWriterTask(sslSocket);
        nickname = "Client-" + sslSocket.getInetAddress() + ":" + sslSocket.getPort();
    }

    static class SocketWriterTask implements Runnable {
        SSLSocket sslSocket;
        ArrayBlockingQueue<String> messageQueue = new ArrayBlockingQueue<>(1024);

        public SocketWriterTask(SSLSocket sslSocket) {
            this.sslSocket = sslSocket;
        }

        public void run() {
            try {
                OutputStream outputStream = sslSocket.getOutputStream();
                while (true) {
                    String message = messageQueue.take();
                    outputStream.write(message.getBytes());
                    outputStream.flush();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            messageQueue.offer(message);
        }
    }

    static class SocketReaderTask implements Runnable {
        public long timeOfLastActivity = System.currentTimeMillis();
        SSLSocket sslSocket;
        Watchdog watchdog;
        ChatClient self;
        BufferedReader reader;

        public SocketReaderTask(SSLSocket sslSocket, Watchdog watchdog, ChatClient self) {
            this.sslSocket = sslSocket;
            this.watchdog = watchdog;
            this.self = self;
            try {
                this.reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                System.out.println("User connected IP=" + sslSocket.getInetAddress() + ":" + sslSocket.getPort());
                String message;
                int totalByteCount = 0;
                while ((message = reader.readLine()) != null) {
                    // Existing code...
                }
                System.out.println("Disconnected IP=" + sslSocket.getInetAddress() + ":" + sslSocket.getPort());
                sslSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

public class Main {

    public static void main(String[] args) {
        String ksName = "certificates/VASEJMENO.p12";
        char[] ksPass = "testpass".toCharArray();
        char[] ctPass = "testpass".toCharArray();

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(ksName), ksPass);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, ctPass);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(8888);

            System.out.println("Server is running on port 8888.");
            var executor = Executors.newFixedThreadPool(20);
            Watchdog watchdog = new Watchdog();
            executor.execute(watchdog);

            while (true) {
                SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());

                ChatClient chatClient = new ChatClient(sslSocket, watchdog);
                watchdog.addClient(chatClient);
                executor.execute(chatClient.inputTask);
                executor.execute(chatClient.outputTask);

                // Authenticate the user
                if (!authenticateUser(chatClient)) {
                    chatClient.outputTask.sendMessage("Authentication failed. Please check your username and password.\r\n");
                    chatClient.inputTask.timeOfLastActivity = System.currentTimeMillis();  // Update last activity time
                    chatClient.inputTask.run();  // Disconnect the client
                } else {
                    chatClient.outputTask.sendMessage("Authentication successful!\r\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean authenticateUser(ChatClient chatClient) {
        try {
            String csvFile = "C:\\Users\\l_uchytil\\Documents\\test_users.csv";
            String loginMessage = "#login";
            chatClient.outputTask.sendMessage(loginMessage);

            // Prompt the user for credentials
            chatClient.outputTask.sendMessage("Please enter your username: ");
            String login = chatClient.inputTask.reader.readLine().trim();

            chatClient.outputTask.sendMessage("Please enter your password: ");
            String password = chatClient.inputTask.reader.readLine().trim();

            // Read user credentials from CSV file
            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(";");
                    String usernameFromFile = parts[0].trim();
                    String salt = parts[1].trim();
                    String hashedPasswordFromFile = parts[2].trim();

                    // Hash the provided password with the stored salt
                    String hashedPassword = hashPassword(password, salt);

                    // Check if the provided credentials match the stored credentials
                    if (login.equals(usernameFromFile) && hashedPassword.equals(hashedPasswordFromFile)) {
                        // Authentication successful
                        chatClient.outputTask.sendMessage("Login successful!\r\n");
                        return true;
                    }
                }

                // Authentication failed
                chatClient.outputTask.sendMessage("Login failed. Please check your username and password.\r\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
    private static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}