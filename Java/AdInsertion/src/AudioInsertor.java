import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioInsertor {
	private int frameRate;
	private int bytePerVideoFrame;
	private String audioPath;
	private String ad1Path;
	private String ad2Path;
	private Set<String> insertionPointSet1;
	private Set<String> insertionPointSet2;
	private final int bytePerFrame = 2;
	private final int framePerSecond = 48000;
	private long audioFrameOffset = 0;
	private long totalAudioFrames = 0;
	
	
	private Vector<Point> ad2Remove;
	AudioInputStream inputAIS;
	
	///hardcode output path here
	private String outputPath;

	public AudioInsertor(int frameRate, String audioPath, String ad1Path, 
			String ad2Path, Set<String> insertionPointSet1, Set<String> insertionPointSet2,String wInsertionPath){
		this.frameRate = frameRate;
		bytePerVideoFrame = bytePerFrame*framePerSecond/frameRate;
		this.audioPath = audioPath;
		this.ad1Path = ad1Path;
		this.ad2Path = ad2Path;
		this.insertionPointSet1 = insertionPointSet1;
		this.insertionPointSet2 = insertionPointSet2;
		this.outputPath = wInsertionPath;
	}
	
	public AudioInsertor(int frameRate, String audioPath, String wRemovalPath, Vector<Point> removePos) {
		
		this.frameRate = frameRate;
		bytePerVideoFrame = bytePerFrame*framePerSecond/frameRate;
		this.audioPath = audioPath;
		this.outputPath = wRemovalPath;
		this.ad2Remove = removePos;
	}

	public int getFrameRate() {
		return frameRate;
	}

	public void setFrameRate(int frameRate) {
		this.frameRate = frameRate;
	}

	public String getAudioPath() {
		return audioPath;
	}

	public void setAudioPath(String audioPath) {
		this.audioPath = audioPath;
	}
	
	
	//read r
	public byte[] insertAudioBytePerFrame() throws IOException{

		File file = new File(audioPath);
		RandomAccessFile raf = new RandomAccessFile(file, "r");

		long len = bytePerVideoFrame;
		Integer flag = 0;
		long position = audioFrameOffset * len;
		byte[] bytes = new byte[(int) len];

		raf.seek(position);
		flag = raf.read(bytes);
		if (flag == -1) {
			raf.close();
			return bytes;
		}

		raf.close();
		audioFrameOffset++;
		return bytes;
	}
	
	public long getAudioFrames(String path) {
		long totalFrames = 0;
		try {
			int frameLength = bytePerVideoFrame;
			Integer count = 0;

			File file = new File(path);
			RandomAccessFile raf = new RandomAccessFile(file, "r");

			long len = frameLength;
			Integer flag = 0;
			long position = 0;

			while (flag != -1) {
				byte[] bytes = new byte[(int) len];

				raf.seek(position);
				flag = raf.read(bytes);
				if (flag == -1) {
					totalFrames = count;
					raf.close();
				}
				position += len;
				count++;
			}
			raf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return totalFrames;
	}
	
	public byte[] insertAdAllBytes(String path) throws IOException{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(path));

		int read;
		byte[] wavHeader = new byte[44];
		read = in.read(wavHeader);
		
		byte[] buff = new byte[1024];
		while ((read = in.read(buff)) > 0)
		{
		    out.write(buff, 0, read);
		}
		out.flush();
		byte[] audioBytes = out.toByteArray();
		
		return audioBytes;
	}
	
	private void modifyWavHeader(String path) throws IOException{
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(path));
		
//		int read;
//		byte[] buff = new byte[44];
//		read = in.read(buff);
//		out.write(buff, 0, read);
//		out.flush();
//		byte[] audioBytes = out.toByteArray();
		
		File file = new File(path);
		System.out.println(file.length());
		
		byte[] fileSizeInBytesArr = IntToBytes((int)file.length());
		byte[] fileSizeInBytesArr2 = IntToBytes((int)file.length()-44);
		
		int read;
		byte[] wavHeader1 = new byte[4];
		read = in.read(wavHeader1);		
		out.write(wavHeader1);
		
		byte[] DataSizeHeader1 = new byte[4];
		read = in.read(DataSizeHeader1);		
		for(int i = 0; i < 4; i++){
			DataSizeHeader1[i] = fileSizeInBytesArr[3-i];
		}		
		out.write(DataSizeHeader1);
		
		byte[] wavHeader2 = new byte[32];
		read = in.read(wavHeader2);		
		out.write(wavHeader2);
		
		byte[] DataSizeHeader2 = new byte[4];
		read = in.read(DataSizeHeader2);
		
		for(int i = 0; i < 4; i++){
			DataSizeHeader2[i] = fileSizeInBytesArr2[3-i];
		}	
		out.write(DataSizeHeader2);
			
		
		byte[] buff = new byte[1024];
		while ((read = in.read(buff)) > 0)
		{
		    out.write(buff, 0, read);
		}
		out.flush();
		byte[] audioBytes = out.toByteArray();
		
		FileOutputStream outFileWriter = new FileOutputStream(path);
		
		outFileWriter.write(audioBytes);
		
		outFileWriter.close();
	}
	
	public byte[] IntToBytes(int x) {
	    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
	    buffer.putInt(x);
	    return buffer.array();
	}
	
	public int bytesToInt(byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getInt();
	}
	public void removeAudio() throws IOException{
		totalAudioFrames = getAudioFrames(audioPath);
		FileOutputStream out = new FileOutputStream(outputPath);
		int ad1Start = ad2Remove.get(0).x;
		int ad1End = ad2Remove.get(0).y;
		int ad2Start = ad2Remove.get(1).x;
		int ad2End = ad2Remove.get(1).y;
		
		while(audioFrameOffset < totalAudioFrames){
			if(audioFrameOffset>=ad1Start && audioFrameOffset<=ad1End)
			{
//				System.out.println("Removing AudioFrame at: "+audioFrameOffset);
				insertAudioBytePerFrame();
				
			}else if(audioFrameOffset>=ad2Start && audioFrameOffset<=ad2End) {
//				System.out.println("Removing AudioFrame at: "+audioFrameOffset);
				insertAudioBytePerFrame();
			}else {
				out.write(insertAudioBytePerFrame());
			}
		}
		out.close();
		
		ByteArrayOutputStream out2 = new ByteArrayOutputStream();
		
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(outputPath));
		int read;
		byte[] buff = new byte[1024];
		while ((read = in.read(buff)) > 0)
		{
		    out2.write(buff, 0, read);
		}
		out2.flush();
		in.close();
		byte[] audioBytes = out2.toByteArray();
		genWav(audioBytes);
		
		System.out.println("Audio Removal Completed");
	}
	
	public void genWav(byte[] audioByte) throws IOException{
		totalAudioFrames = getAudioFrames(audioPath);
		
		File sourceFile = new File(audioPath);
		File outputFile = new File(outputPath);
		try {
	        inputAIS = AudioSystem.getAudioInputStream(sourceFile);
	        Clip clip = AudioSystem.getClip();
	        clip.open(inputAIS);
	        long totalMicroSecond = clip.getMicrosecondLength();
    	} catch (UnsupportedAudioFileException e) {
	
	    } catch (IOException e) {
	
	    } catch (LineUnavailableException e) {
	
	    }
		
        AudioFileFormat fileFormat = null;
        try {
            fileFormat = AudioSystem.getAudioFileFormat(sourceFile);
            AudioFileFormat.Type targetFileType = fileFormat.getType();
                AudioFormat audioFormat = fileFormat.getFormat();


                ByteArrayInputStream bais = new ByteArrayInputStream(audioByte);
                AudioInputStream outputAIS = new AudioInputStream(bais, audioFormat,
                		audioByte.length / audioFormat.getFrameSize());

                AudioSystem.write(outputAIS, targetFileType, outputFile);

        } catch (UnsupportedAudioFileException e) {

        } catch (IOException e) {

        }
        
	}
	public void insertAudio() throws IOException{
		System.out.println("Starting audio insertion");
		totalAudioFrames = getAudioFrames(audioPath);
		FileOutputStream out = new FileOutputStream(outputPath);
		while(audioFrameOffset < totalAudioFrames){
			if(insertionPointSet1.contains(String.valueOf(audioFrameOffset))){
				System.out.println("inserting ad1 in audio at " + audioFrameOffset+ "th frame");
				out.write(insertAdAllBytes(ad1Path));
			}
			if(insertionPointSet2.contains(String.valueOf(audioFrameOffset))){
				System.out.println("inserting ad2 in audio at " + audioFrameOffset+ "th frame");
				out.write(insertAdAllBytes(ad2Path));
			}
			out.write(insertAudioBytePerFrame());		
		}
//		out.write(insertAdAllBytes(ad1Path));
		
		out.close();
		modifyWavHeader(outputPath);
		System.out.println("Audio insertion Completed");
	}

}