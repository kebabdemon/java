import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

class watchdogTask implements Runnable{
    Set<socketTask> set= ConcurrentHashMap.newKeySet(0);
    public void addTask(socketTask t){
        set.add(t);
    }
    public void removeTask(socketTask t){
        set.remove(t);
    }
    public void run() {

        while(true){
            try {

                Iterator<socketTask> i = set.iterator();
                long time=System.currentTimeMillis();
                while (i.hasNext()){
                   socketTask task=i.next();
                   if (time - task.timeOfLastActivity > 15000){
                       System.out.println("zabijim neaktivni task"+task.s);
                       task.s.close();
                       i.remove();
                   }
                }

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
class socketTask implements Runnable{
    Socket s;
    long timeOfLastActivity=System.currentTimeMillis();
    public socketTask(Socket s){
        this.s = s;
    }

    public void run() {
        try {
            var in= s.getInputStream();
            var out=s.getOutputStream();
            int pocet;
            int znak;
            int konec = 0;
            byte buffer[]=new byte[2048];
            while((pocet=in.read(buffer))!=-1 && (znak=in.read())!=-1){
                long curTime=System.currentTimeMillis();
                double delta=(curTime-timeOfLastActivity)/1000.0;
                if(delta>0 && pocet/delta<1){
                    System.err.println("Zrychli");
                    konec++;
                }if(curTime > 10000 && konec > 3){
                    s.close();
                    System.err.println("mene nez 10");
                }
                timeOfLastActivity=curTime;
                out.write("Server odpovidá:".getBytes());
                out.write(znak);
                out.write("\r\n".getBytes());
                out.flush();
            }
            s.close();
            System.out.println("klient ukoncil spojeni");
        } catch (IOException e) {
            System.out.println("Watchdog ukončil spojeni");
        }
    }
}

public class Main {
    public static void main(String[] args) {
        try {
            ServerSocket serverSock= new ServerSocket(8888);
            System.out.println("Server spusten na portu 8888");
            var executor= Executors.newFixedThreadPool(10);
            watchdogTask watchdog = new watchdogTask();
            executor.execute(watchdog);
            while (true) {
                Socket s = serverSock.accept();
                System.out.println("Pripojil se" + s);
                socketTask task = new socketTask(s);
                watchdog.addTask(task);
                executor.execute(task);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}