import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Node1 {
    public static void node1_send() throws IOException {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        Scanner in = new Scanner(System.in);
        String s = in.nextLine().strip();
        int type=-1;
        String command, content;
        while (!s.equals("bye")) {
            command = s.split(" ")[0];
            command = CommandFix.command_fix(command);
            content = "";
            switch (command) {
                case "USER":
                    type = Config.TYPE_COMMAND_USER;
                    content = s.split(" ")[1];
                    break;
                case "PASS":
                    type = Config.TYPE_COMMAND_PASS;
                    content = s.split(" ")[1];
                    break;
                case "PWD":
                    type = Config.TYPE_COMMAND_PWD;
                    break;
                case "CWD":
                    type = Config.TYPE_COMMAND_CWD;
                    content = s.split(" ")[1];
                    break;
                case "PASV":
                    type = Config.TYPE_COMMAND_PASV;
                    break;
                case "LIST":
                    type = Config.TYPE_COMMAND_LIST;
                    break;
                case "RETR":
                    type = Config.TYPE_COMMAND_RETR;
                    content = s.split(" ")[1];
                    break;
                default:
                    s = in.nextLine().strip();
                    continue;
            }

            // send commands
            SW_Sender sender = new SW_Sender("",
                    10,
                    audioHw,
                    50,
                    300,
                    Config.NODE_2_CODE,
                    Config.NODE_1_CODE,
                    type,
                    false,
                    Config.node3_IP,
                    Config.node1_IP,
                    Config.node3_Port,
                    Config.node1_Port,
                    content);
            DecodeThread decodeThread = new DecodeThread(audioHw, null, sender, Config.NODE_1_CODE);
            decodeThread.start();
            sender.sendFrame();
            decodeThread.stopDecoding();
            // next round
            s = in.nextLine().strip();
        }

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

        long time_stamp = System.currentTimeMillis();

        byte[] bytes = "abcdefg".getBytes();

        byte[] payload = new byte[2 + bytes.length];
        payload[0] = (byte)(time_stamp & 0xFF);
        payload[1] = (byte)((time_stamp & 0xFF00) >> 8);

        System.arraycopy(payload, 0, payload, 2, bytes.length);

        ArrayList<Integer> data = (ArrayList<Integer>) Arrays.stream(Util.bytesToBits(payload)).boxed().collect(Collectors.toList());
        float[] track = SW_Sender.frameToTrack(data, Config.NODE_2_CODE, Config.NODE_1_CODE, Config.TYPE_ICMP_ECHO, 0, false,
                Util.ipToLong(Config.node3_IP), Util.ipToLong(Config.node1_IP), 1, 0, data.size());

        audioHw.PHYSend(track);
        decodeThread.stopDecoding();
        audioHw.stop();
    }

    public static void main(final String[] args) throws IOException {
        node1_send();
    }
}