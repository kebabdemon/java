package vyuka;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/echo")
public class WebSocketDemo {



    public class ChatRoom {
        private Map<String, Set<String>> rooms = new HashMap<>();

        public String createRoom(String roomName) {
            if (!rooms.containsKey(roomName)) {
                rooms.put(roomName, new HashSet<>());
                return "Room created: " + roomName;
            } else {
                return "Room already exists: " + roomName;
            }
        }

        public String joinRoom(String roomName, String userName) {
            Set<String> roomUsers = rooms.get(roomName);

            if (roomUsers != null) {
                roomUsers.add(userName);
                return "Joined room: " + roomName + " as " + userName;
            } else {
                return "Room not found: " + roomName;
            }
        }

        public String leaveRoom(String roomName, String userName) {
            Set<String> roomUsers = rooms.get(roomName);

            if (roomUsers != null && roomUsers.contains(userName)) {
                roomUsers.remove(userName);
                return "Left room: " + roomName;
            } else {
                return "User not in room or room not found: " + roomName;
            }
        }

        public String listRooms() {
            return "Available rooms: " + String.join(", ", rooms.keySet());
        }

        public String listUsers(String roomName) {
            Set<String> roomUsers = rooms.get(roomName);

            if (roomUsers != null) {
                return "Users in room " + roomName + ": " + String.join(", ", roomUsers);
            } else {
                return "Room not found: " + roomName;
            }
        }
    }
    static HashMap<Session, String> users = new HashMap<>();
    ChatRoom chatRoom = new ChatRoom();

    void printSessions(Session session) {
        Set<Session> sessions = session.getOpenSessions();
        System.out.println("Opened sessions: " + sessions + ".\nSession IDs:");
        for (Session s : sessions)
            System.out.print(s.getId() + ", ");
    }

    void sendPrivateMessage(String message, String to) {
        Session toSession = null;

        for (Map.Entry<Session, String> entry : users.entrySet()) {
            System.out.print(entry.getValue() + " ");
            if (entry.getValue().trim().equalsIgnoreCase(to.trim())) {
                toSession = entry.getKey();
                System.out.println("Found session: " + toSession.getId());
            }
        }

        if (toSession != null) {
            System.out.println("Sending private message to: " + to + " message: " + message);
            sendMessageToSession("[Private] " + message, toSession);
        } else {
            System.out.println("User not found: " + to);
        }
    }

    void sendMessageToSession(String message, Session session) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendMessageToAll(String message, Session session) {
        Set<Session> sessions = session.getOpenSessions();
        String username = getUsername(session);

        for (Session s : sessions)
            if (s != session)
                sendMessageToSession("[" + username + "] " + message, s);
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Open Connection ...");
        printSessions(session);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Close Connection ..." + session.getId());
        printSessions(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        printSessions(session);
        System.out.println("Message from the client id: " + session.getId() + " = " + message);

        if (message.startsWith("/setUsername")) {
            String username = message.substring(message.indexOf(" ") + 1);
            setUsername(session, username);
            sendMessageToSession("Username set to: " + getUsername(session), session);
        } else if (message.startsWith("/pm")) {
            String username = message.substring(message.indexOf(" ") + 1, message.indexOf(" ", message.indexOf(" ") + 1));
            String privateMessage = message.substring(message.indexOf(" ", message.indexOf(" ") + 1) + 1);
            sendPrivateMessage(privateMessage, username.trim());
        } else if (message.startsWith("#")) {
            // Handle ChatRoom commands
            handleChatRoomCommand(message, session);
        } else {
            sendMessageToAll(message, session);
        }
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    public void setUsername(Session session, String username) {
        Session s = users.keySet()
                .stream()
                .filter(x -> x.getId().equals(session.getId()))
                .findFirst()
                .orElse(null);

        if (s == null) {
            users.put(session, username.trim());
            System.out.println("Username set to: " + username + " SessionId: " + session.getId());
            System.out.println("Try get username: " + getUsername(session));
        }
    }

    public String getUsername(Session session) {
        Session s = users.keySet().stream().filter(x -> x.getId().equals(session.getId())).findFirst().orElse(null);

        if (s == null) {
            return session.getId();
        }

        return users.get(s);
    }

    // New method to handle ChatRoom commands
    void handleChatRoomCommand(String command, Session session) {
        String[] commandParts = command.split("\\s+");
        String result;

        switch (commandParts[0]) {
            case "#createRoom":
                result = chatRoom.createRoom(commandParts[1]);
                sendMessageToSession(result, session);
                break;
            case "#joinRoom":
                result = chatRoom.joinRoom(commandParts[1], getUsername(session));
                sendMessageToSession(result, session);
                break;
            case "#leaveRoom":
                result = chatRoom.leaveRoom(commandParts[1], getUsername(session));
                sendMessageToSession(result, session);
                break;
            case "#list":
                result = chatRoom.listRooms();
                sendMessageToSession(result, session);
                break;
            case "#users":
                result = chatRoom.listUsers(commandParts[1]);
                sendMessageToSession(result, session);
                break;
            default:
                sendMessageToSession("Invalid ChatRoom command.", session);
        }
    }
}
