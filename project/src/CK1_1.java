public class CK1_1 {

    public static void main(final String[] args) {

        final AudioHw r = new AudioHw();
        r.init();

        r.start();
        try {
            Thread.sleep(10000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        r.stop();

        final AudioHw speaker = new AudioHw();
        speaker.init();
        speaker.play(r.getRecorded());
        speaker.start();
        try {
            Thread.sleep(10000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        speaker.stop();

    }
}
