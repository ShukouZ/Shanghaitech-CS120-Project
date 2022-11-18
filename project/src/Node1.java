public class Node1 {
    public static void node1_send(){
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Sender sender = new SW_Sender("src/INPUT.txt",
                10,
                audioHw,
                50,
                300,
                Config.NODE_2_CODE,
                Config.NODE_1_CODE,
                Config.TYPE_DATA,
                false,
                Config.node3_IP,
                Config.node1_IP,
                Config.node3_Port,
                Config.node1_Port);
        DecodeThread decodeThread = new DecodeThread(audioHw, null, sender, Config.NODE_1_CODE);

        decodeThread.start();

        long t1 = System.currentTimeMillis();
        sender.sendFrame();
        long t2 = System.currentTimeMillis();
        System.out.println("\nDone, time passed: "+(t2-t1)+"ms.");

        decodeThread.stopDecoding();
        audioHw.stop();
    }

    public static void node1_receive(){
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Receiver receiver = new SW_Receiver(audioHw);

        DecodeThread decodeThread  = new DecodeThread(audioHw, receiver, null, Config.NODE_1_CODE);
        decodeThread.start();
        try {
            Thread.sleep(20000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        decodeThread.stopDecoding();
        byte[] bytes = receiver.writeFile("src/OUTPUT.txt");
        audioHw.stop();
    }
    public static void main(final String[] args) {
        node1_send();
    }
}