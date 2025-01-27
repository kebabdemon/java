import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class hlavni {
    public static void main(String[] args) {

        if (args.length < 2){
            System.out.println("Potrebuji 2 parametry : hostname port");
            return;
        }
        String hostname=args[0];
        int port = Integer.parseInt(args[1]);
        try {
            Socket s = new Socket(hostname, port);
            System.out.println("OK, pripojeno na "+hostname+":"+port);
            InputStream inp = s.getInputStream();
            OutputStream outp = s.getOutputStream();
            while (true) {
                int pocet;
                byte buffer[]=new byte[2048];
                if(System.in.available()>0){
                    pocet = System.in.read(buffer);
                    if(pocet == -1)
                        break;
                    outp.write(buffer, 0, pocet);
                    outp.flush();
                }
                if (inp.available() > 0){
                    pocet = inp.read(buffer);
                    System.out.write(buffer, 0,pocet);
                    System.out.flush();
                    if(buffer[2]=='1'){ //221 2.0.0 Bye
                        System.out.println("spojeni zruseno");
                        break;
                    }


                }
                Thread.sleep(1);
            }
            s.close();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }
}