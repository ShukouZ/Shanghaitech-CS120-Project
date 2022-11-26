import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Node1 {
    public static void node1_send() throws IOException {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Sender sender = new SW_Sender("src/INPUT.txt",
                10,
                audioHw,
                50,
                300,
                Config.NODE_2_CODE,
                Config.NODE_1_CODE,
                Config.TYPE_DATA,
                false,
                Config.node3_IP,
                Config.node1_IP,
                Config.node3_Port,
                Config.node1_Port);
        DecodeThread decodeThread = new DecodeThread(audioHw, null, sender, Config.NODE_1_CODE);

        decodeThread.start();

        long t1 = System.currentTimeMillis();
        sender.sendFrame();
        long t2 = System.currentTimeMillis();
        System.out.println("\nDone, time passed: "+(t2-t1)+"ms.");

        decodeThread.stopDecoding();
        audioHw.stop();
    }

    public static void node1_receive() throws IOException {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Receiver receiver = new SW_Receiver(audioHw);

        DecodeThread decodeThread  = new DecodeThread(audioHw, receiver, null, Config.NODE_1_CODE);
        decodeThread.start();
        try {
            Thread.sleep(60000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        decodeThread.stopDecoding();
        byte[] bytes = receiver.writeFile("src/OUTPUT.txt");
        audioHw.stop();
    }

    public static void node1_ICMP_ECHO() throws IOException {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        DecodeThread decodeThread  = new DecodeThread(audioHw, null, null, Config.NODE_1_CODE);
        decodeThread.start();

        byte[] payload = Config.DEFAULT_PAYLOAD.getBytes();
        ArrayList<Integer> data = (ArrayList<Integer>) Arrays.stream(Util.bytesToBits(payload)).boxed().collect(Collectors.toList());
        float[] track = SW_Sender.frameToTrack(data, Config.NODE_2_CODE, Config.NODE_1_CODE, Config.TYPE_ICMP_ECHO, 0, false,
                Util.ipToLong(Config.node3_IP), Util.ipToLong(Config.node1_IP), 1, 0, data.size(), (int)System.currentTimeMillis());

        audioHw.PHYSend(track, false);
        decodeThread.stopDecoding();
        audioHw.stop();
    }

    public static void main(final String[] args) throws IOException {
        node1_receive();
    }
}