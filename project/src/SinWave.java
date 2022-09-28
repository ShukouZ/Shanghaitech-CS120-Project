import java.util.ArrayList;

public class SinWave {

    private float phase;
    private float freq;  // Hz
    private float sampleRate;
    private final float dphase;

    SinWave(float _phase, float _freq, float _sampleRate){
        phase = _phase;
        freq = _freq;
        sampleRate = _sampleRate;
        dphase = (2 * (float)Math.PI * freq) / sampleRate;
    }


    public ArrayList<Float> sample(int num){
        ArrayList<Float> samples = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            phase = phase + dphase;
            samples.add((float) (Math.sin((double) phase)));
        }
        return samples;
    }
}
