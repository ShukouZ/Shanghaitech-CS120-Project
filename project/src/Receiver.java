public class Receiver {

    public static void main(final String[] args) {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Receiver receiver = new SW_Receiver(audioHw);

        DecodeThread decodeThread  = new DecodeThread(audioHw, receiver);
        decodeThread.start();
        try {
            Thread.sleep(15000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        decodeThread.stopDecoding();
        audioHw.stop();
    }
}
