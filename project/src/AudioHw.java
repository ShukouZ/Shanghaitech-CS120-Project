import java.util.*;

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
	List<Float> syncFIFO = new ArrayList<>();
	private float syncPower_localMax;

	private boolean frameDetected;

	private ArrayList<float[]> frame_table;
	private int frame_stored_size;

	private int frame_recorded_num;

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
			frame_table = new ArrayList<>();
			frame_stored_size = 0;
			syncPower_localMax = 0.0f;

			playLoc = 0;
			frameDetected = false;
			frame_recorded_num = 0;

			for(int i=0; i<Config.preamble.length; ++i){
				syncFIFO.add(0.0f);
			}

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

	public void PHYSend(ArrayList<Float> track){
		playList.addAll(track);
	}

	// Detect preamble.
	// If not detected, return -1.
	// If detected, set frameDetected to true, return the start index.
	private void detectPreamble(){
		float sum, syncPower_debug, current_sample;
		int start_index=-1;
		for(int i=0; i<Config.HW_BUFFER_SIZE; i++) {
			current_sample=input[i];

			syncFIFO.remove(0);
			syncFIFO.add(current_sample);

			sum = 0.0f;
			for (int j = 0; j < Config.preamble.length; j++) {
				sum += syncFIFO.get(j) * Config.preamble[j];
			}

			syncPower_debug = sum / 200.0f;

			if ((syncPower_debug > syncPower_localMax) && (syncPower_debug > 0.2f)) {
				syncPower_localMax = syncPower_debug;
				start_index = i;
				break;
			}
		}
		if(start_index != -1){
			frameDetected = true;
			syncPower_localMax = 0;
			float[] new_frame = new float[Config.SAMPLE_SIZE];
			frame_table.add(new_frame);
			if(start_index != Config.HW_BUFFER_SIZE - 1)
			{
				System.arraycopy(input, start_index + 1, new_frame, 0, Config.HW_BUFFER_SIZE - start_index - 1);
				frame_stored_size = Config.HW_BUFFER_SIZE - start_index - 1;
				System.out.println(frame_recorded_num + ": " + start_index);
			}
			else
			{
				frame_stored_size = 0;
			}
		}

//		if (start_index == 0){
//			System.out.println("0>>>>>>>>>>>>>> "+frame_recorded_num);
//		}if (start_index == 511){
//			System.out.println("511>>>>>>>>>>>>>> "+frame_recorded_num);
//		}if (start_index == 1){
//			System.out.println("1>>>>>>>>>>>>>> "+frame_recorded_num);
//		}if (start_index == 510){
//			System.out.println("510>>>>>>>>>>>>>> "+frame_recorded_num);
//		}

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
				if(frameDetected) {
					for(float input_data: input){
						if(frame_stored_size < Config.SAMPLE_SIZE){
							frame_table.get(frame_table.size()-1)[frame_stored_size] = input_data;
							frame_stored_size += 1;
						}else{
							syncFIFO.remove(0);
							syncFIFO.add(input_data);
						}
					}

				}else{
					detectPreamble();
				}
			}
			else {
				channelInfo.write(output);
			}


		}
		if (frame_stored_size == Config.SAMPLE_SIZE){
			frame_stored_size=0;
			frame_recorded_num++;
			frameDetected=false;
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

	public float[] getFrame(int id){
		if (id < frame_recorded_num){
			return frame_table.get(id);
		}
		else
		{
			throw new ArrayIndexOutOfBoundsException();
		}
	}
}



