import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class reader implements Runnable{
    Socket sock;
    public reader(Socket s){
        sock=s;

    }
    public void run(){
        InputStream inp;
        try {
            inp = sock.getInputStream();

        int celkem=0, pocet;
        byte buffer[] = new byte[10000];
        while ((pocet = inp.read(buffer))!= -1)

            celkem += pocet;
//					System.out.write(buffer, 0, pocet);
//					System.out.flush();
        System.out.println("reader ukol skoncil..");
            sock.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    }

class mujUkol implements Runnable {
    String hostname;
    int port;
    static ExecutorService executor = Executors.newFixedThreadPool(100);

    public mujUkol(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    int celkem = 0;

    public void run() {
        try (Socket s = new Socket(hostname, port)) { // try with resource
            reader ukolCteni= new reader(s);
            executor.execute(ukolCteni);
            OutputStream outp = s.getOutputStream();
            InputStream inp = s.getInputStream();
            byte request[] = ("GET / HTTP/1.1\r\n" + "Connection: keep-alive\r\n" + "Host: localhost\r\n\r\n")
                    .getBytes();

            int pocet;
            for (int i = 0; i < 100; i++) {
                outp.write(request);
                outp.flush();

            }
            System.out.println("Vlakno skoncilo, precetl jsem: " + celkem / 1024 + " kB.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Vlakno skoncilo vyjimkou.");
        }
    }
}

public class Hlavni {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        if (args.length < 2) {
            System.err.println("Potrebuji 2 parametry: hostname port");
            return;
        }
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        ExecutorService executor = Executors.newFixedThreadPool(100);
        mujUkol v = new mujUkol(hostname, port);
        for (int i = 0; i < 1500; i++) {
            executor.execute(v);
            System.out.println("Spusten task "+i);

        }
        System.out.println("OK, vsechny tasky bezi.");
    }

}