// Borrowed from https://github.com/youcunhan/CS120ComputerNetwork/blob/master/CS120ComputerNetwork-proj2/Util.java

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import javax.sound.sampled.AudioFormat;

public class Util {
	public static byte[] readFileByBytes(String fileName,int datalen) {
		byte[] bytes = new byte[datalen];
		int index = 0;
		FileInputStream input = null;
		try {
			input = new FileInputStream(fileName);
			byte[] buffer = new byte[1024];
			while (true) {
				int len = input.read(buffer);
				if (len == -1) {
					break;
				}
				for (int i = 0; i < buffer.length && index < datalen; i++) {
					bytes[index] = buffer[i];
					index++;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				input.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return bytes;
	}

	public static int[] bytesToBits(byte[] bytes) {
		int[] bits = new int[8 * bytes.length];
		int index = 0;
		for (byte aByte : bytes) {
			byte[] bit = byteToBit(aByte);
			for (int j = 0; j < 8; j++) {
				bits[index] = bit[j];
				index++;
			}
		}
		return bits;
	}

	public static byte[] BitsTobytes(byte[] bits) {
		byte[] bytes = new byte[bits.length / 8];
		for (int i = 0; i < bits.length; i += 8) {
			String bitStr = "";
			for (int j = i; j < i + 8; j++) {
				bitStr += String.valueOf(bits[j]);
			}
			bytes[i / 8] = BitToByte(bitStr);
		}
		return bytes;
	}

	public static byte[] byteToBit(byte b) {
		byte[] bit = new byte[8];
		bit[0] = (byte) ((b >> 7) & 0x1);
		bit[1] = (byte) ((b >> 6) & 0x1);
		bit[2] = (byte) ((b >> 5) & 0x1);
		bit[3] = (byte) ((b >> 4) & 0x1);
		bit[4] = (byte) ((b >> 3) & 0x1);
		bit[5] = (byte) ((b >> 2) & 0x1);
		bit[6] = (byte) ((b >> 1) & 0x1);
		bit[7] = (byte) ((b >> 0) & 0x1);
		return bit;
	}

	public static byte[] intToBit(int b) {
		byte[] bit = new byte[32];
		for (int i = 0; i < 32; i++) {
			bit[i] = (byte) ((b >> (31 - i)) & 0x1);
		}
		return bit;
	}

	public static byte BitToByte(String byteStr) {
		int re, len;
		if (null == byteStr) {
			return 0;
		}
		len = byteStr.length();
		if (len != 4 && len != 8) {
			return 0;
		}
		if (len == 8) {
			if (byteStr.charAt(0) == '0') {
				re = Integer.parseInt(byteStr, 2);
			} else {
				re = Integer.parseInt(byteStr, 2) - 256;
			}
		} else {
			re = Integer.parseInt(byteStr, 2);
		}
		return (byte) re;
	}

	public static byte[] frameToBytes(ArrayList<Integer> frame){
		StringBuilder s = new StringBuilder();
		for(int datum: frame){
			s.append(datum);
		}
		byte[] bytes = new byte[s.length() / 8];
		for (int i = 0; i < s.length() / 8; i++) {
			bytes[i] = Util.BitToByte(s.substring(i*8,(i+1)*8));
		}
		return bytes;
	}
	public static void writeFileByBytes(byte[] bytes, String filename) {
		try {
			DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
			os.write(bytes);
			os.flush();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void outputbits(byte[] receivedbits, int receivedbitsIndex) {
		byte[] bytes = new byte[receivedbitsIndex / 8];
		for (int i = 0; i < receivedbitsIndex / 8; i++) {
			String bitStr = "";
			for (int j = 0; j < 8; j++) {
				bitStr += String.valueOf(receivedbits[8 * i + j]);
			}
			// System.out.print(bitStr+"|");
			bytes[i] = Util.BitToByte(bitStr);
		}
		Util.writeFileByBytes(bytes, "OUTPUT.bin");
	}

	public static void main(String[] args) {
		byte[] bits = new byte[5000*8];
		for(int i=0;i<5000*8;i++){
			int max = 2, min = 0;
			byte ran2 = (byte) (Math.random() * (max - min) + min);
			bits[i] = ran2;
		}
		byte[] databytes = Util.BitsTobytes(bits);
		Util.writeFileByBytes(databytes, "INPUT2to1.bin");
		
	}



}
