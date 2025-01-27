package vyuka;

import java.io.IOException;
import java.util.Set;

import jakarta.websocket.*;
import jakarta.websocket.server.*;

@ServerEndpoint("/echo")
public class WebSocketDemo {
	void printSessions(Session session) {
		Set<Session> sessions = session.getOpenSessions();
		System.out.println("Opened sessions: " + sessions + ".\nSession IDs:");
		for (Session s : sessions)
			System.out.print(s.getId() + ", ");
	}

	void sendMessageToAll(String message, Session session) {
		Set<Session> sessions = session.getOpenSessions();
		for (Session s : sessions)
			if (s != session)
				try {
					s.getBasicRemote().sendText(message);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
		sendMessageToAll(message, session);
		return;// "Message sent to all clients!";
	}

	@OnError
	public void onError(Throwable e) {
		e.printStackTrace();
	}

}
