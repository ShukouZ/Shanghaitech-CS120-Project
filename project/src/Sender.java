public class Sender {
    public static  void part2(){
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Sender sender = new SW_Sender("src/INPUT.bin",
                15, audioHw,
                40,
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

    public static void perf(){
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Sender sender = new SW_Sender("src/INPUT.bin",
                15, audioHw,
                85,
                Config.NODE_2_CODE,
                Config.NODE_1_CODE,
                Config.TYPE_DATA);
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

    public static void main(final String[] args) {
//        part2();
        perf();
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