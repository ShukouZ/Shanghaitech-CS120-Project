public class Receiver {

    public static void main(final String[] args) {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        DecodeThread decodeThread  = new DecodeThread(audioHw);
        decodeThread.start();
        try {
            Thread.sleep(10000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        decodeThread.stopDecoding();
        audioHw.stop();
    }
}
