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
        DecodeThread decodeThread = new DecodeThread(audioHw, null, null, null, Config.NODE_1_CODE);
        decodeThread.start();

        while (!s.toLowerCase().equals("bye")) {
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
                    try{
                        content = s.split(" ")[1];
                    }catch(Exception e){
                        content = "";
                    }
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
            decodeThread.updateSender((sender));
            sender.sendFrame();
            // next round
            s = in.nextLine().strip();
        }
        decodeThread.stopDecoding();
        audioHw.stop();
    }


    public static void main(final String[] args) throws IOException {
        node1_send();
    }
}