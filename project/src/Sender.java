public class Sender {
    public static void main_part2(){
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Sender sender = new SW_Sender("src/INPUT.bin",
                15,
                audioHw,
                40,
                500,
                Config.NODE_2_CODE,
                Config.NODE_1_CODE,
                Config.TYPE_DATA);
        DecodeThread decodeThread = new DecodeThread(audioHw, null, sender, Config.NODE_1_CODE);

        decodeThread.start();

        long t1 = System.currentTimeMillis();
        sender.sendWindowedFrame();
        long t2 = System.currentTimeMillis();
        System.out.println("\nDone, time passed: "+(t2-t1)+"ms.");

        decodeThread.stopDecoding();
        audioHw.stop();
    }

    public static void main_perf(){
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Sender sender = new SW_Sender("src/INPUT.bin",
                15,
                audioHw,
                85,
                1400,
                Config.NODE_2_CODE,
                Config.NODE_1_CODE,
                Config.TYPE_PERF);
        DecodeThread decodeThread = new DecodeThread(audioHw, null, sender, Config.NODE_1_CODE);

        decodeThread.start();
        macperf macperf_thread = new macperf(sender);
        macperf_thread.start();

        long t1 = System.currentTimeMillis();
        sender.sendWindowedFrame();
        long t2 = System.currentTimeMillis();
        System.out.println("\nDone, time passed: "+(t2-t1)+"ms.");

        decodeThread.stopDecoding();
        macperf_thread.stop_running();
        audioHw.stop();
    }

    public static void main_ping(){
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        DecodeThread decodeThread = new DecodeThread(audioHw, null, null, Config.NODE_1_CODE);
        decodeThread.start();
        DecodeThread.sendACK(Config.NODE_2_CODE, Config.NODE_1_CODE, Config.TYPE_PING_REQ, (int)System.currentTimeMillis() % 256);

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        decodeThread.stopDecoding();
        audioHw.stop();
    }

    public static void main(final String[] args) {
//        main_part2();
        main_perf();
//        main_ping();
    }
}

class macperf extends Thread{
    int frame_num = -1;
    boolean running = true;

    private final SW_Sender sender;

    macperf(SW_Sender _s ){
        sender = _s;
    }
    @Override
    public void run(){
        while (running)
        {
            int new_frame_num = sender.LAR;
            System.out.print("\rSpeed: " + ((new_frame_num - frame_num) / 1000.0f * Config.PAYLOAD_SIZE) + "kbps");
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