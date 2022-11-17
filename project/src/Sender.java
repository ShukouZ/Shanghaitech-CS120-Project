public class Sender {
    public static void main(final String[] args) {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Sender sender = new SW_Sender("src/INPUT.txt",
                10,
                audioHw,
                50,
                800,
                Config.NODE_2_CODE,
                Config.NODE_1_CODE,
                Config.TYPE_DATA,
                false);
        DecodeThread decodeThread = new DecodeThread(audioHw, null, sender, Config.NODE_1_CODE);

        decodeThread.start();

        long t1 = System.currentTimeMillis();
        sender.sendWindowedFrame();
        long t2 = System.currentTimeMillis();
        System.out.println("\nDone, time passed: "+(t2-t1)+"ms.");

        decodeThread.stopDecoding();
        audioHw.stop();
    }
}