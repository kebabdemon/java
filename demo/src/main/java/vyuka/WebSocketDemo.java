package com.example.serveletim;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/echo")
public class WebSocketDemo {
    static HashMap<Session, String> users = new HashMap<>();

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
}
