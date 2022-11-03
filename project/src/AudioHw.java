import java.util.*;

import com.synthbot.jasiohost.*;

public class AudioHw implements AsioDriverListener {
	// hardware
	private AsioDriver asioDriver;
	private Set<AsioChannel> activeChannels;

	private AsioChannel outputChannel;
	private AsioChannel inputChannel;

	// soundtrack
	private ArrayList<Float> playList;

	// IO buffer
	private float[] output;
	private float[] input;

	// MAC state machine
	private int state;

	// for preamble use
	myQueue syncFIFO = new myQueue(Config.preamble.length);
	private float syncPower_localMax;

	// frame detection
	private Boolean frameDetected;
	private ArrayList<float[]> frame_table;
	private int frame_stored_size;
	private int frame_recorded_num;
	private float length;

	private int playLoc;

	public void init() {
		activeChannels = new HashSet<AsioChannel>();  // create a Set of AsioChannels

		if (asioDriver == null) {
			// init IO channels
			activeChannels = new HashSet<>();
			asioDriver = AsioDriver.getDriver("ASIO4ALL v2");
			asioDriver.addAsioDriverListener(this);
			System.out.println("------------------");
			//// Output
			System.out.println("Output Channels");
			for (int i = 0; i < asioDriver.getNumChannelsOutput(); i++) {
				System.out.println(asioDriver.getChannelOutput(i));
			}
			outputChannel = asioDriver.getChannelOutput(0);
			output = new float[Config.HW_BUFFER_SIZE];
			activeChannels.add(outputChannel);
			playList = new ArrayList<>();
			//// Input
			System.out.println("Input Channels");
			for (int i = 0; i < asioDriver.getNumChannelsInput(); i++) {
				System.out.println(asioDriver.getChannelInput(i));
			}
			inputChannel = asioDriver.getChannelInput(0);
			input = new float[Config.HW_BUFFER_SIZE];
			activeChannels.add(inputChannel);

			// asioDriver settings
			asioDriver.setSampleRate(Config.PHY_TX_SAMPLING_RATE);
			asioDriver.createBuffers(activeChannels);  // create the audio buffers and prepare the driver to run
			System.out.println("ASIO buffer created, size: " + asioDriver.getBufferPreferredSize());

			// frame detection
			frameDetected = false;
			syncPower_localMax = 0.0f;
			frame_table = new ArrayList<>();
			frame_stored_size = 0;
			frame_recorded_num = 0;
			length = 0;

			playLoc = 0;
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
	}

	public void PHYSend(ArrayList<Float> track){
		playList.addAll(track);
	}

	// Detect preamble.
	// If not detected, return -1.
	// If detected, set frameDetected to true, return the start index.
	private void detectPreamble(int start_point){
		float syncPower_debug, current_sample;
		int start_index=-1;
		for(int i=start_point; i<Config.HW_BUFFER_SIZE; i++) {
			current_sample=input[i];

			syncFIFO.add(current_sample);

			syncPower_debug = syncFIFO.dot_product(Config.preamble);

			if ((syncPower_debug > syncPower_localMax) && (syncPower_debug > 30.0f)) {
				syncPower_localMax = syncPower_debug;
				start_index = i;
				break;
			}
		}
		if(start_index != -1){
			frameDetected = true;
			syncPower_localMax = 0;
			float[] new_frame = new float[Config.SAMPLE_SIZE + 8];
			frame_table.add(new_frame);
			if(start_index != Config.HW_BUFFER_SIZE - 1)
			{
				System.arraycopy(input, start_index + 1, new_frame, 0, Config.HW_BUFFER_SIZE - start_index - 1);
				frame_stored_size = Config.HW_BUFFER_SIZE - start_index - 1;
			}
			else
			{
				frame_stored_size = 0;
			}
		}
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
					for(int id = 0; id < Config.HW_BUFFER_SIZE; id++){
						float input_data = input[id];
						if(frame_stored_size < Config.SAMPLE_SIZE + 8){

							if (frame_stored_size >= 4 && length == 0){
								for (int len_id = 0; len_id < 4; len_id++){
									length += frame_table.get(frame_table.size()-1)[len_id];
								}
								if (length > 0){
									System.out.println(frame_recorded_num + " frame length");
								}
								else {
									System.out.println(frame_recorded_num + " ack length");
								}

							}

							frame_table.get(frame_table.size()-1)[frame_stored_size] = input_data;
							frame_stored_size += 1;
						}else{
							frame_stored_size=0;
							frame_recorded_num++;
							frameDetected=false;
							length = 0;
							syncFIFO.clear();
							detectPreamble(id);
							break;
						}
					}

				}else{
					detectPreamble(0);
				}
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



