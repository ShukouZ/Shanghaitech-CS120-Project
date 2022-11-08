public class Receiver {

    public static void main(final String[] args) {
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
            Thread.sleep(25000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        decodeThread.stopDecoding();
        receiver.writeFile();
        audioHw.stop();
    }
}
