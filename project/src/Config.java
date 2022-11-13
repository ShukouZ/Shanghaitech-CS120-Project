public class Config {

	/*
	 * Audio HW
	 */
	public final static int HW_BUFFER_SIZE = 512;
	
	public final static int PHY_SAMPLING_RATE = 48000;
	
	// Carrier Frequency
	public final static int PHY_CARRIER_FREQ = 6000;
	
	public final static int MAC_RETRY_LIMIT = 50;

	public final static int SAMPLE_PER_BIT = 4;

	public final static int MAX_OFFSET = 16;

	// type value
	public final static int TYPE_DATA = 0;
	public final static int TYPE_ACK = 1;
	public final static int TYPE_PERF = 2;
	public final static int TYPE_PING_REQ = 3;
	public final static int TYPE_PING_REPLY = 4;
	public final static int TYPE_SEND_REQ = 5;
	public final static int TYPE_SEND_REPLY = 6;

	public final static int STATE_FRAME_DETECTION = 0;
	public final static int STATE_FRAME_TX = 1;
	public final static int STATE_FRAME_RX = 2;


	// device node code
	public final static int NODE_1_CODE = 1;
	public final static int NODE_2_CODE = 2;

	//                  	  FRAME DATA
	// 		not	encoded							encoded
	//  [[ preamble | len ] 		[ dest | src | type | seq | payload | crc ]]

	//                  	  ACK DATA
	// 		not	encoded							encoded
	//  [[ preamble | len ] 		[ dest | src | type | seq | crc ]]
	public final static float[] preamble = new float[]{0f, 0.276215829365293f, 0.561187065362382f, 0.812188872780211f, 0.972580268465123f, 0.982083682742156f, 0.796093065705644f, 0.411678906944964f, -0.108119018423941f, -0.626509999835986f, -0.958562947497441f, -0.938659164747151f, -0.515553857177023f, 0.170683923896615f, 0.796093065705643f, 0.990845596578807f, 0.576038818505240f, -0.241337891299704f, -0.907575419670957f, -0.879247512675953f, -0.108119018423946f, 0.779413382041589f, 0.935507835925603f, 0.135000013853296f, -0.827688998156889f, -0.852168127999820f, 0.161781996552759f, 0.982083682742155f, 0.484283153387626f, -0.707106781186545f, -0.866025403784442f, 0.241337891299700f, 0.997393231517949f, 0.411678906944975f, -0.687699458853415f, -0.944732079836048f, -0.126049711220294f, 0.812188872780204f, 0.915008234974015f, 0.152866884562162f, -0.725995491923124f, -0.985325833480102f, -0.484283153387627f, 0.344846302627958f, 0.922142776925929f, 0.925597464791844f, 0.419889101560278f, -0.276215829365286f, -0.817422338558888f, -0.999633286223284f, -0.806889216550073f, -0.361736788979584f, 0.161781996552755f, 0.612336239797600f, 0.899846753957505f, 0.999633286223284f, 0.935507835925605f, 0.756286755957730f, 0.515553857177025f, 0.258819045102527f};
	public final static int LEN_SIZE = 4;
	public final static int DEST_SIZE = 4;
	public final static int SRC_SIZE = 4;
	public final static int TYPE_SIZE = 4;
	public final static int SEQ_SIZE = 8;
	public final static int PAYLOAD_SIZE = 250;
	public final static int CRC_SIZE = 32;
	public final static int ACK_SIZE = DEST_SIZE + SRC_SIZE + TYPE_SIZE + SEQ_SIZE;
	public final static int FRAME_SIZE = DEST_SIZE + SRC_SIZE + TYPE_SIZE + SEQ_SIZE + PAYLOAD_SIZE;

	public final static int FRAME_SAMPLE_SIZE = SAMPLE_PER_BIT * (FRAME_SIZE + CRC_SIZE);
	public final static int ACK_SAMPLE_SIZE = SAMPLE_PER_BIT * (ACK_SIZE + CRC_SIZE);
	public final static int FILE_BYTES = 6250;
}