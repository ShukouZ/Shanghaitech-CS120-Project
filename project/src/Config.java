public class Config {

	/*
	 * Audio HW
	 */
	public final static int HW_BUFFER_SIZE = 512;
	
	
	/*
	 * PHY 
	 */
	public final static short MAX_SHORT = (short) 0x7FFF;
	public final static short MIN_SHORT = (short) 0x8000;
	public final static int PHY_HWLATENCY = 17;

	//frame buffer
	public final static int TNBF_MAX_BUFFER_LEN = 74; //74 bytes
	
	//frame buffer phy payload size
	public final static int TNBF_MAX_PHY_DATA_LEN = 72; //71 bytes
	
	//frame buffer mac payload size
	public final static int TNBF_MAX_MAC_DATA_LEN = 69; //71 bytes
			
	
	// Sampling Rate
	public final static int PHY_RX_SAMPLING_RATE = 48000;
	public final static int PHY_TX_SAMPLING_RATE = 48000;
	
	// Carrier Frequency
	public final static int PHY_CARRIER_FREQ = 6000;
	
	// Symbol duration
	public final static int PHY_SYMBOL_RATE = 12000;
	
	//Samples per Symbol
	public final static int PHY_RX_SAMPLES_PER_SYMBOL = PHY_RX_SAMPLING_RATE/PHY_SYMBOL_RATE;
	public final static int PHY_TX_SAMPLES_PER_SYMBOL = PHY_TX_SAMPLING_RATE/PHY_SYMBOL_RATE;
	
	//Samples per byte
	public final static int PHY_RX_SAMPLES_PER_BYTE = PHY_RX_SAMPLES_PER_SYMBOL*8;
	
	//Preamble Size (Unit: Symbols)
	public final static int PHY_PRE_SIZE = 16;
	public final static int PHY_PRE_FREQ_MIN = 2000;
	public final static int PHY_PRE_FREQ_MAX = 20000;
	
	//Rx dsp buffer size (unit: samples)
	public final static int PHY_RX_SYNC_BF_SIZE = PHY_RX_SAMPLES_PER_SYMBOL*PHY_PRE_SIZE;
	public final static int PHY_RX_DEC_BF_SIZE = PHY_RX_SAMPLES_PER_SYMBOL*PHY_PRE_SIZE;
	
	//Rx dsp state
	public final static int PHY_RX_DSP_STATE_SYNC = 0;
	public final static int PHY_RX_DSP_STATE_DECODE = 1;
	
	
	//read and write buffer size in sample
	public final static int PHY_RX_LINEBUFFER_SIZE = 8; //unit: sample
	public final static int PHY_TX_LINEBUFFER_SIZE = 8; //unit: sample
	
	
	/*
	 * 	  HW	   Dump.bin
	 *		/\	    /\
	 *		|_______|
	 *			|
	 *			|Sink
	 *		____|____
	 *		/\	    /\
	 *		|		|
	 *	TXDSP		Inject.bin
	 * 
	*/
	
	// Tx Sink output
	public final static int PHY_TX_SINK_OUTPUT_HW = 1<<0; // tx to hw sound card
	public final static int PHY_TX_SINK_OUTPUT_FILE = 1<<1; // to file for debug
	public final static String TX_SINK_DUMP_FILE = "PHY_Tx_Dump.bin";
	public final static int PHY_TX_SINK_OUTPUT_DEFAULT = PHY_TX_SINK_OUTPUT_HW | PHY_TX_SINK_OUTPUT_FILE;
//	public final static int PHY_TX_SINK_OUTPUT_DEFAULT = PHY_TX_SINK_OUTPUT_HW;
	
	// Tx Sink input
	public final static int PHY_TX_SINK_INPUT_DSP = 0; // from tx signal generator 
	public final static int PHY_TX_SINK_INPUT_FILE = 1; // from offline .bin for debug
	public final static String TX_SINK_INJECT_FILE = "PHY_Tx_Inject.bin";
	public final static int PHY_TX_SINK_INPUT_DEFAULT = PHY_TX_SINK_INPUT_DSP;
	
	/*
	 * 	  RXDSP		Dump.bin
	 *		/\	    /\
	 *		|_______|
	 *			|
	 *			|Source
	 *		____|____
	 *		/\	    /\
	 *		|		|
	 *	HW		Inject.bin
	 * 
	*/
	
	// Rx Source input
	public final static int PHY_RX_SRC_INPUT_HW = 0; // from sound card
	public final static int PHY_RX_SRC_INPUT_FILE = 1; // from offline file for debug
	public final static String PHY_RX_SRC_INJECT_FILE = "PHY_Rx_Inject.bin";
	public final static int PHY_RX_SRC_INPUT_DEFAULT = PHY_RX_SRC_INPUT_HW; 
	
	// Rx Source output
	public final static int PHY_RX_SRC_OUTPUT_DSP = 1<<0; // to rx DSP 
	public final static int PHY_RX_SRC_OUTPUT_FILE = 1<<1; // dump to offline file for debug
	public final static String PHY_RX_SRC_DUMP_FILE = "PHY_Rx_Dump.bin";	
	public final static int PHY_RX_SRC_OUTPUT_DEFAULT = PHY_RX_SRC_OUTPUT_DSP | PHY_RX_SRC_OUTPUT_FILE;
//	public final static int PHY_RX_SRC_OUTPUT_DEFAULT = PHY_RX_SRC_OUTPUT_DSP;
	
	
	/*
	 * MAC
	 */
	
	public final static byte MAC_ADDR = (byte) 0x1;
	public final static int MAC_ACK_BYTE = 6;
	public final static int MAC_ACK_DURATION = MAC_ACK_BYTE*8*PHY_RX_SAMPLES_PER_SYMBOL*1000/PHY_TX_SAMPLING_RATE;
	public final static int MAC_TIMEOUT = MAC_ACK_BYTE*8*PHY_RX_SAMPLES_PER_SYMBOL*1000/PHY_TX_SAMPLING_RATE+8+2*PHY_HWLATENCY;
	
	public final static int MAC_STATE_IDLE = 0;
	public final static int MAC_STATE_TX = 1;
	public final static int MAC_STATE_RX = 2;
	public final static int MAC_STATE_TX_WAIT = 3;
	public final static int MAC_RETRY_LIMIT = 10;

	public final static Float[] preamble = new Float[]{0f, 0.292253526175365f, 0.578122783243689f, 0.818922660270921f, 0.970161926668346f, 0.989544983431239f, 0.848517715952487f, 0.545534901210549f, 0.117344292610228f, -0.357003508983781f, -0.764968689564477f, -0.985481794895020f, -0.927976905060567f, -0.575510066122383f, -0.0142471037071029f, 0.571222023679993f, 0.950081427341218f, 0.938869470333259f, 0.503207155293130f, -0.186875748105925f, -0.799540168386921f, -0.993099820722154f, -0.619560518615570f, 0.139691369070056f, 0.825870058964336f, 0.967460603412741f, 0.423112307246551f, -0.447976166786221f, -0.983545741210552f, -0.720520364164739f, 0.165839780514338f, 0.917215078917872f, 0.820837110419320f, -0.0779916705884160f, -0.909582575237464f, -0.790635054245411f, 0.197576105990015f, 0.971830260976502f, 0.606350896114201f, -0.504714014439706f, -0.986901842894986f, -0.180730773803864f, 0.876512514792645f, 0.716982566819799f, -0.477104458997627f, -0.969737457844485f, 0.0191894477786350f, 0.982936151445094f, 0.357818192231311f, -0.866025403784439f, -0.620701049842434f, 0.676862057430894f, 0.845891666947310f, -0.350748625161952f, -0.991442489165308f, -0.119509698603470f, 0.923468687012125f, 0.637314203744580f, -0.530584878870207f, -0.976862951871701f, -0.150910875152358f, 0.856796432414299f, 0.816495739994673f, -0.177727157919935f, -0.960670426145684f, -0.692333076911826f, 0.295033011038164f, 0.973584214362580f, 0.705743347840584f, -0.209390400709074f, -0.925409018128916f, -0.847670377495459f, -0.0868298699162950f, 0.727340384642205f, 0.992596562155616f, 0.559831537962604f, -0.235187331126267f, -0.865297571169692f, -0.966385237512086f, -0.521807317088066f, 0.188446554729839f, 0.786343043643835f, 0.999659509758474f, 0.768142982358121f, 0.228398994225729f, -0.381860589947797f, -0.836458493453407f, -0.999990803051026f, -0.853480345490665f, -0.473651007057697f, 0.0125026576448291f, 0.475186778764769f, 0.816075837350483f, 0.983833397094085f, 0.973016923647968f, 0.812446897032303f, 0.549670922632993f, 0.236600137364560f, -0.0808901506354841f, -0.368654375682168f};
	public final static int FRAME_SIZE = 250;
	public final static int ID_SIZE = 8;
	public final static int SAMPLE_PER_BIT = 4;
	public final static int CRC_SIZE = 8;
	public final static int ACK_SIZE = ID_SIZE + CRC_SIZE;
	public final static int FRAME_SAMPLE_SIZE = SAMPLE_PER_BIT*(FRAME_SIZE + ID_SIZE + CRC_SIZE);
	public final static int ACK_SAMPLE_SIZE = SAMPLE_PER_BIT*(ACK_SIZE) + HW_BUFFER_SIZE;
}