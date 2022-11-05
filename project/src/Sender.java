import com.synthbot.jasiohost.AsioDriverState;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.exit;

public class Sender {
    public static void main(final String[] args) {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        DecodeThread decodeThread = new DecodeThread(audioHw, null);
        SW_Sender sender = new SW_Sender("src/INPUT.bin", 10, audioHw, 50);
        decodeThread.setACKList(sender.getACKList());
        decodeThread.start();

        sender.sendWindowedFrame();

        decodeThread.stopDecoding();
        audioHw.stop();
    }
}
