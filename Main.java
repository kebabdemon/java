import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

class Watchdog implements Runnable {
    private static Watchdog instance;

    static Set<ChatClient> clientSet = ConcurrentHashMap.newKeySet(1000);
    static Set<ChatRoom> roomSet = ConcurrentHashMap.newKeySet(100);
    static Set<String> existingRoomNames = ConcurrentHashMap.newKeySet();

    private Watchdog() {
        ChatRoom defaultRoom = new ChatRoom("DefaultRoom");
        roomSet.add(defaultRoom);
        existingRoomNames.add("DefaultRoom");
    }

    public static Watchdog getInstance() {
        if (instance == null) {
            instance = new Watchdog();
        }
        return instance;
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

    public void addClient(ChatClient client) {
        clientSet.add(client);
    }

    public void addRoom(ChatRoom room) {
        if (!existingRoomNames.contains(room.roomName)) {
            roomSet.add(room);
            existingRoomNames.add(room.roomName);
        }
    }

    public Set<ChatRoom> getAllRooms() {
        return roomSet;
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

class ChatRoom {
    String roomName;
    Set<ChatClient> members = ConcurrentHashMap.newKeySet();

    public ChatRoom(String roomName) {
        this.roomName = roomName;
    }

    public void addMember(ChatClient client) {
        members.add(client);
    }

    public void removeMember(ChatClient client) {
        members.remove(client);
    }

    public Set<ChatClient> getMembers() {
        return members;
    }

    public void sendMessage(ChatClient sender, String message) {
        for (ChatClient client : members) {
            if (client != sender) {
                client.outputTask.sendMessage(sender.nickname + ": " + message + "\r\n");
            }
        }
    }
}

class ChatClient {
    Watchdog watchdog;
    SocketReaderTask inputTask;
    SocketWriterTask outputTask;
    String nickname;
    Set<ChatRoom> joinedRooms = ConcurrentHashMap.newKeySet();

    public ChatClient(SSLSocket sslSocket, Watchdog watchdog) {
        inputTask = new SocketReaderTask(sslSocket, watchdog, this);
        outputTask = new SocketWriterTask(sslSocket);
        nickname = "Client-" + sslSocket.getInetAddress() + ":" + sslSocket.getPort();
        joinRoom("DefaultRoom");
    }

    public void createRoom(String roomName) {
        if (!Watchdog.existingRoomNames.contains(roomName)) {
            outputTask.sendMessage("#createRoom " + roomName);
        } else {
            outputTask.sendMessage("Room with name '" + roomName + "' already exists.\r\n");
        }
    }

    public void joinRoom(String roomName) {

        boolean roomExists = false;
        boolean alreadyJoined = false;

        for (ChatRoom existingRoom : Watchdog.roomSet) {
            switch (existingRoom.roomName.equals(roomName) ? "EXIST" : "NOT_EXIST") {
                case "EXIST":
                    roomExists = true;

                    if (existingRoom.members.contains(this)) {
                        alreadyJoined = true;
                        break;
                    }

                    existingRoom.addMember(this);
                    joinedRooms.add(existingRoom);
                    outputTask.sendMessage("Připojeno do '" + roomName + "'\r\n");
                    break;

                case "NOT_EXIST":
                    outputTask.sendMessage("Room with name '" + roomName + "' does not exist.\r\n");
                    break;
            }

            if (alreadyJoined) {
                outputTask.sendMessage("You are already a member of the room '" + roomName + "'.\r\n");
                break;
            }
        }

        if (!roomExists) {
            outputTask.sendMessage("Room with name '" + roomName + "' does not exist.\r\n");
        } else if (alreadyJoined) {
            outputTask.sendMessage("You are already a member of the room '" + roomName + "'.\r\n");
        }
    }

    public void leaveRoom(String roomName) {
        for (ChatRoom room : joinedRooms) {
            if (room.roomName.equals(roomName)) {
                room.removeMember(this);
                outputTask.sendMessage("#leaveRoom " + roomName);
                joinedRooms.remove(room);
                break;
            }
        }
    }

    public void sendMessageToRoom(String message) {
        ChatRoom lastJoinedRoom = getLastJoinedRoom();
        if (lastJoinedRoom != null) {
            lastJoinedRoom.sendMessage(this, message);
        }
    }

    private ChatRoom getLastJoinedRoom() {
        ChatRoom lastJoinedRoom = null;
        for (ChatRoom room : joinedRooms) {
            lastJoinedRoom = room;
        }
        return lastJoinedRoom;
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

        public SocketReaderTask(SSLSocket sslSocket, Watchdog watchdog, ChatClient self) {
            this.sslSocket = sslSocket;
            this.watchdog = watchdog;
            this.self = self;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                System.out.println("User connected IP=" + sslSocket.getInetAddress() + ":" + sslSocket.getPort());
                String message;
                int totalByteCount = 0;

                while ((message = reader.readLine()) != null) {
                    switch (message.split(" ", 2)[0]) {
                        case "#setNickname":
                            self.nickname = message.substring("#setNickname ".length());
                            break;

                        case "#private":
                            int colonIndex = message.indexOf(":");
                            if (colonIndex != -1) {
                                String targetNickname = message.substring("#private ".length(), colonIndex).trim();
                                String privateMessage = message.substring(colonIndex + 1).trim();
                                ChatClient targetClient = watchdog.getClientByNickname(targetNickname);
                                if (targetClient != null) {
                                    targetClient.outputTask.sendMessage("PM from " + self.nickname + ": " + privateMessage + "\r\n");
                                }
                            }
                            break;

                        case "#joinRoom":
                            String roomName = message.substring("#joinRoom ".length());
                            boolean roomExists = false;
                            boolean alreadyJoined = false;

                            for (ChatRoom existingRoom : watchdog.getAllRooms()) {
                                if (existingRoom.roomName.equals(roomName)) {
                                    roomExists = true;

                                    // Check if the user is already a member of the room
                                    if (existingRoom.members.contains(self)) {
                                        alreadyJoined = true;
                                        break;
                                    }

                                    existingRoom.addMember(self);
                                    self.joinedRooms.add(existingRoom);
                                    self.outputTask.sendMessage("Připojeno do '" + roomName + "'\r\n");
                                    break;
                                }
                            }

                            if (!roomExists) {
                                self.outputTask.sendMessage("Room with name '" + roomName + "' does not exist.\r\n");
                            } else if (alreadyJoined) {
                                self.outputTask.sendMessage("You are already a member of the room '" + roomName + "'.\r\n");
                            }
                            break;

                        case "#leaveRoom":
                            String leaveRoomName = message.substring("#leaveRoom ".length());
                            for (ChatRoom room : self.joinedRooms) {
                                if (room.roomName.equals(leaveRoomName)) {
                                    room.removeMember(self);
                                    self.outputTask.sendMessage("#leaveRoom " + leaveRoomName);
                                    self.joinedRooms.remove(room);
                                    break;
                                }
                            }
                            break;

                        case "#createRoom":
                            String createRoomName = message.substring("#createRoom ".length());
                            ChatRoom newRoom = new ChatRoom(createRoomName);


                            watchdog.addRoom(newRoom);
                            self.outputTask.sendMessage("Room '" + createRoomName + "' was created.\r\n");
                            break;

                        case "#roomlist":
                            StringBuilder roomList = new StringBuilder("List of chat rooms:\r\n");
                            for (ChatRoom room : watchdog.getAllRooms()) {
                                roomList.append(room.roomName).append("\r\n");
                            }
                            self.outputTask.sendMessage(roomList.toString());
                            break;

                        case "#users":
                            String usersRoomName = message.substring("#users ".length());
                            self.handleUsersCommand(usersRoomName);
                            break;

                        case "#place":
                            StringBuilder placeMessage = new StringBuilder("You are currently in the following rooms:\r\n");
                            for (ChatRoom room : self.joinedRooms) {
                                placeMessage.append(room.roomName).append("\r\n");
                            }
                            self.outputTask.sendMessage(placeMessage.toString());
                            break;

                        default:
                            self.sendMessageToRoom(message);
                            break;
                    }

                    totalByteCount += message.getBytes().length;
                    timeOfLastActivity = System.currentTimeMillis();

                    if (totalByteCount > 100) {
                        System.out.println("Connection terminated due to more than 100 bytes");
                        break;
                    }
                }

                System.out.println("Disconnected IP=" + sslSocket.getInetAddress() + ":" + sslSocket.getPort());
                sslSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleUsersCommand(String roomName) {
        for (ChatRoom room : joinedRooms) {
            if (room.roomName.equals(roomName)) {
                StringBuilder usersList = new StringBuilder("Users in room '" + roomName + "':\r\n");
                for (ChatClient member : room.getMembers()) {
                    usersList.append(member.nickname).append("\r\n");
                }
                outputTask.sendMessage(usersList.toString());
                return;
            }
        }

        outputTask.sendMessage("You are not in room '" + roomName + "'.\r\n");
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
            Watchdog watchdog = Watchdog.getInstance();
            executor.execute(watchdog);

            while (true) {
                SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
                ChatClient chatClient = new ChatClient(sslSocket, watchdog);
                watchdog.addClient(chatClient);
                executor.execute(chatClient.inputTask);
                executor.execute(chatClient.outputTask);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
