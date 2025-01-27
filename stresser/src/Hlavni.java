import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class mojeVlakno extends Thread {
    Socket s;

    public mojeVlakno(Socket s) {
        this.s = s;
    }

    public void run() {
        int pocet, pocetBajtu=0;
        InputStream inp;
        byte[] buffer=new byte[10000];
        try {
            inp = s.getInputStream();
            while ((pocet = inp.read(buffer)) != -1) {
                pocetBajtu+=pocet;
                //System.out.write(znak);
                //System.out.flush();
            }
            System.out.println("Server ukoncil spojeni = vlakno konci");
            s.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            if (s.isClosed()) System.out.println("Konec vlakna, protoze socket je zavren");
            else  System.err.println("Chyba pri cteni ze socketu");
        }
        System.out.printf("Prijal jsem %d bajtu\n", pocetBajtu);
    }
}

class mujUkol implements Runnable {
    static ExecutorService executor = Executors.newFixedThreadPool(4);

    private String host;
    private int port;
    int pozadavky;
    public mujUkol(String host, int port, int pozadavky) {
        this.host = host;
        this.port = port;
        this.pozadavky = pozadavky;
    }
    public void run() {
        int pocetPozadavku=0, pocetBajtu=0;
        try {
            Socket s = new Socket(host, port);
            System.out.println("Pripojeno OK");
            mojeVlakno v = new mojeVlakno(s);
            v.start();
            OutputStream outp = s.getOutputStream();
            byte [] request = ("GET / HTTP/1.1\r\n"
                    + "Host:"+host+"\r\n"
                    + "Connection:keep-alive\r\n\r\n").getBytes();
            while (s.isConnected()) {
                outp.write(request);
                outp.flush();
                pocetPozadavku++;
                pocetBajtu+=request.length;
            }
            System.out.println("main()> socket je closed, koncime...");
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            System.err.println("main:UnknownHostException");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.err.println("main:IOException");
        }
        System.out.printf("Odeslano %d pozadavku, %d bajtu\n",
                pocetPozadavku, pocetBajtu);

    }
}


class mujUkolUDP implements Runnable {
    String hostname;
    int port;
    int opakovani;
    public mujUkolUDP(String hostname, int port, int pocetOpakovani) {
        this.hostname = hostname;
        this.port = port;
        opakovani = pocetOpakovani;
    }


    public void run() {
        // TODO Auto-generated method stub
        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        try (DatagramSocket ds = new DatagramSocket()) {
            byte buffer[] = new byte[100];
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, addr, port);
            for (int i = 0; i < opakovani; i++)
                ds.send(dp);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
public class Hlavni {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        if (args.length < 1) {
            System.err.println("Ocekavam 1 parametr: URL textu - specifikace práce");
            return;
        }
        try {
            URL pastebin = new URL(args[0]);//"https://pastebin.com/raw/vF3UE9Pq");
            URLConnection con = pastebin.openConnection();
            Scanner s = new Scanner(con.getInputStream());
            ExecutorService executor = Executors.newFixedThreadPool(40);
            while (s.hasNext()) {
                String line = s.nextLine();
                String[] parts = line.split("\\s+");

                if (parts.length == 5) {
                    String hostname = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    int partasks = Integer.parseInt(parts[2]);
                    int pozadavky = Integer.parseInt(parts[3]);
                    String protocol = parts[4];

                    if (protocol.equalsIgnoreCase("TCP")) {
                        mujUkol u=new mujUkol(hostname, port, pozadavky);
                        for(int i = 0; i<partasks; i++){
                        executor.execute(u);
                        }
                    } else if (protocol.equalsIgnoreCase("UDP")) {
                        mujUkolUDP u=new mujUkolUDP(hostname, port, pozadavky);
                        for(int i = 0; i<partasks; i++){
                            executor.execute(u);
                        }
                    } else {
                        System.err.println("Unknown protocol: " + protocol);
                    }
                } else {
                    System.err.println("Invalid line format: " + line);
                }
            }
            executor.shutdown();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
/*
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		mujUkol u=new mujUkol(host, port);
		ExecutorService executor = Executors.newFixedThreadPool(4);
		for (int i=0; i<10; i++) {
			executor.execute(u);
		}
		executor.shutdown();
*/
    }

}