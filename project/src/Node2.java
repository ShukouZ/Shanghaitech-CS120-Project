import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.stream.Collectors;

public class Node2 {
    public static void main(final String[] args) throws IOException {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Receiver receiver = new SW_Receiver(audioHw);
//        for (int i = 0; i < 100; i++) {
//            receiver.sendACK(i);
//        }

        DecodeThread decodeThread  = new DecodeThread(audioHw, receiver, null, Config.NODE_2_CODE);
        decodeThread.start();
        try {
            Thread.sleep(20000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        decodeThread.stopDecoding();
//        byte[] bytes = receiver.writeFile("src/OUTPUT.txt");
        audioHw.stop();
    }
}
