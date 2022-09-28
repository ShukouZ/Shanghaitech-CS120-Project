import java.util.ArrayList;

public class Chk2 {

    public static void main(final String[] args) {
        final AudioHw r = new AudioHw();
        r.init();


        SinWave wave = new SinWave(0, 1000, Config.PHY_TX_SAMPLING_RATE);

        ArrayList<Float> track = wave.sample(5 * Config.PHY_TX_SAMPLING_RATE);

        r.play(track);

        r.start();
        try {
            Thread.sleep(5000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        r.stop();

        final AudioHw speaker = new AudioHw();
        speaker.init();
        speaker.play(r.getRecorded());
        speaker.start();
        try {
            Thread.sleep(5000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        speaker.stop();
    }
}
