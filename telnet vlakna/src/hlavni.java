import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

class MojeVlakno extends Thread {
    Socket s;

    public MojeVlakno(Socket s) {
        this.s = s;
    }

    public void run() {
        try {
            OutputStream outp = s.getOutputStream();
            byte buffer[] = new byte[2048];
            int pocet;
            while ((pocet = System.in.read(buffer)) != -1) {
                outp.write(buffer, 0, pocet);
                outp.flush();
                sleep(100);
                if (s.isClosed()) return;
            }
            s.close();
            System.out.println("Vlakno skoncilo OK.");
        } catch (InterruptedException e) {
            System.err.println("Vlakno skoncilo vyjimkou.");
        }
        catch (IOException e){
            System.err.println("chyba IOExeption v main.c");
        }
    }
}

public class hlavni {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        if (args.length < 2) {
            System.err.println("Potrebuji 2 parametry: hostname port");
            return;
        }
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        try {
            Socket s = new Socket(hostname, port);
            MojeVlakno v = new MojeVlakno(s);
            v.start();
            System.out.println("OK, pripojeno na " + hostname + ":" + port);
            InputStream inp = s.getInputStream();
            byte buffer[] = new byte[2048];
            int pocet;
            while ((pocet = inp.read(buffer)) != -1) {
                System.out.write(buffer, 0, pocet);
                System.out.flush();
            }
            s.close();
            System.out.println("Spojeni skoncilo OK.");
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("Vyjimka IO.");
        }
    }

}