import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import com.synthbot.jasiohost.*;

public class AudioHw implements AsioDriverListener {
	// 0 for speaking
	// 1 for recording
	// 2 for both
	private int mode;

	private AsioDriver asioDriver;
	private Set<AsioChannel> activeChannels;
	
	private AsioChannel outputChannel;
	private AsioChannel inputChannel;

	private ArrayList<Float> playList;

	private float[] output;
	private float[] input;

	private ArrayList<float[]> recorded;

	private int playLoc;

	public void init() {
		activeChannels = new HashSet<AsioChannel>();  // create a Set of AsioChannels

		if (asioDriver == null) {
			asioDriver = AsioDriver.getDriver("ASIO4ALL v2");
			asioDriver.addAsioDriverListener(this);   // add an AsioDriverListener in order to receive callbacks from the driver

			System.out.println("------------------");

			System.out.println("Output Channels");
			for (int i = 0; i < asioDriver.getNumChannelsOutput(); i++) {
				System.out.println(asioDriver.getChannelOutput(i));
			}
			outputChannel = asioDriver.getChannelOutput(0);
			output = new float[Config.HW_BUFFER_SIZE];
			activeChannels.add(outputChannel);
			playList = new ArrayList<>();



			System.out.println("Input Channels");
			for (int i = 0; i < asioDriver.getNumChannelsInput(); i++) {
				System.out.println(asioDriver.getChannelInput(i));
			}

			inputChannel = asioDriver.getChannelInput(0);

			input = new float[Config.HW_BUFFER_SIZE];
			activeChannels.add(inputChannel);
			recorded = new ArrayList<>();

			playLoc = 0;

			asioDriver.setSampleRate(Config.PHY_TX_SAMPLING_RATE);
			/*
			 * buffer size should be set either by modifying the JAsioHost source code or
			 * configuring the preferred value in ASIO native window. We choose 128 i.e.,
			 * asioDriver.getBufferPreferredSize() should be equal to Config.HW_BUFFER_SIZE
			 * = 128;
			 * 
			 */

			asioDriver.createBuffers(activeChannels);  // create the audio buffers and prepare the driver to run
			System.out.println("ASIO buffer created, size: " + asioDriver.getBufferPreferredSize());

		}
	}

	public void start() {
		if (asioDriver != null) {
			asioDriver.start();  // start the driver
			System.out.println(asioDriver.getCurrentState());
		}
	}

	public void stop() {
		asioDriver.returnToState(AsioDriverState.INITIALIZED);
		asioDriver.shutdownAndUnloadDriver();  // tear everything down
//		for(float[] buffer : recorded){
//			for(float i : buffer){
//				System.out.print(i + " ");
//			}
//		}
	}

	public void play(ArrayList<Float> track){
		playList.addAll(track);
	}

	@Override
	public void bufferSwitch(final long systemTime, final long samplePosition, final Set<AsioChannel> channels) {
		for (int i = 0; i < Config.HW_BUFFER_SIZE; i++) {
			try {
				output[i] = playList.get(playLoc);
				playLoc++;
			}catch (final IndexOutOfBoundsException e) {
				output[i] = 0.0f;
			}
		}


		for (AsioChannel channelInfo : channels) {
			if (channelInfo.isInput()){
				channelInfo.read(input);
				recorded.add(input.clone());
			}
			else {
				channelInfo.write(output);
			}
		}
	}

	@Override
	public void latenciesChanged(final int inputLatency, final int outputLatency) {
		System.out.println("latenciesChanged() callback received.");
	}

	@Override
	public void bufferSizeChanged(final int bufferSize) {
		System.out.println("bufferSizeChanged() callback received.");
	}

	@Override
	public void resetRequest() {
		/*
		 * This thread will attempt to shut down the ASIO driver. However, it will block
		 * on the AsioDriver object at least until the current method has returned.
		 */
		new Thread() {
			@Override
			public void run() {
				System.out.println("resetRequest() callback received. Returning driver to INITIALIZED state.");
				asioDriver.returnToState(AsioDriverState.INITIALIZED);
			}
		}.start();
	}

	@Override
	public void resyncRequest() {
		System.out.println("resyncRequest() callback received.");
	}

	@Override
	public void sampleRateDidChange(final double sampleRate) {
		System.out.println("sampleRateDidChange() callback received.");
	}

	public ArrayList<Float> getRecorded(){
		ArrayList<Float> rslt = new ArrayList<>(recorded.size());

		for (float[] buffer : recorded){
			for (int i = 0; i < Config.HW_BUFFER_SIZE; i++){
				rslt.add(buffer[i]);
			}
		}

		return rslt;
	}
}



