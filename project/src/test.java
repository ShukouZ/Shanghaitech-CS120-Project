import java.util.ArrayList;

public class test {
    public static void main(final String[] args) {
        final Speaker speaker = new Speaker();

//        SinWave wave1 = new SinWave(0, 10000, 48000);
//        SinWave wave2 = new SinWave(0, 10000, 48000);
//
//
//        ArrayList<Float> track1 = wave1.sample(5 * 48000);
//        ArrayList<Float> track2 = wave2.sample(5 * 48000);
//
//        ArrayList<Float> track = new ArrayList<>(5*48000);
//
//        for(int i = 0; i < track1.size(); i++){
//            track.add(track1.get(i) + track2.get(i));
//        }

        SinWave wave = new SinWave(0, 1000, 44100);

        ArrayList<Float> track = wave.sample(5 * 44100);

        speaker.init(track);
        speaker.start();
        try {
            Thread.sleep(5000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        speaker.stop();
    }
}
