
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
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
    public mujUkol(String host, int port) {
        this.host = host;
        this.port = port;
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

public class Hlavni {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        if (args.length < 2) {
            System.err.println("Ocekavam 2 parametry: host port");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        mujUkol u=new mujUkol(host, port);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i=0; i<40; i++) {
            executor.execute(u);
        }
        executor.shutdown();
    }

}