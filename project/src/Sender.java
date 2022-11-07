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

        SW_Sender sender = new SW_Sender("src/INPUT.bin", 15, audioHw, 50);
        DecodeThread decodeThread = new DecodeThread(audioHw, null, sender);

        decodeThread.setACKList(sender.getACKList());
        decodeThread.start();
        macperf macperf_thread = new macperf(sender.getACKList());
        macperf_thread.start();

        long t1 = System.currentTimeMillis();
        sender.sendWindowedFrame();
        long t2 = System.currentTimeMillis();
        System.out.println("Time passed: "+(t2-t1)+"ms.");
        boolean[] a = sender.getACKList();
        for (int i =0; i<a.length;i++){
            if(!a[i])
                System.out.println("idx="+(i+1));
        }

        decodeThread.stopDecoding();
        macperf_thread.stop_running();
        audioHw.stop();
    }
}

class macperf extends Thread{
    private boolean[] ACKList;
    int frame_num = 0;
    boolean running = true;

    macperf(boolean[] ackList){
        ACKList = ackList;
    }
    @Override
    public void run(){
        while (running)
        {
            int new_frame_num = 0;
            for (boolean i : ACKList) {
                if (i) {
                    new_frame_num++;
                }
            }

            System.out.println("Speed: " + ((new_frame_num - frame_num) / 4.0f) + "kbps");
            frame_num = new_frame_num;
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop_running(){
        running = false;
    }
}