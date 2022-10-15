import java.util.ArrayList;

public class CK1_2 {

    public static void main(final String[] args) {
        final AudioHw r = new AudioHw();
        r.init();


        SinWave wave = new SinWave(0, 1000, Config.PHY_TX_SAMPLING_RATE);

        ArrayList<Float> track = wave.sample(10 * Config.PHY_TX_SAMPLING_RATE);

//        ArrayList<Float> track = new ArrayList<>(Arrays.asList(Config.preamble));


        r.play(track);

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
