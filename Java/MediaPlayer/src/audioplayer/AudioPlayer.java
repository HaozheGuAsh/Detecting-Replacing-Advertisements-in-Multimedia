package audioplayer;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;
import javax.swing.SwingWorker;

public class AudioPlayer {
	String audioPath;
	FileInputStream inputStream;
	AudioInputStream waveStream;
	
	Integer videoFps;
	
	private final int bytePerFrame = 2;
	private final int framePerSecond = 48000;
	private final long bytePerVideoFrame;
	
	private long curAudioOffset;
	private long totalBytes = 0;
	
	SourceDataLine dataLine = null;
	private final int EXTERNAL_BUFFER_SIZE = 524288;// 128Kb
	Integer freezeGap = 500;
	
	AudioRunner audioRunner;

	
	
	private void openAudioStream() throws Exception {
		// opens the inputStream
		try {
			inputStream = new FileInputStream(audioPath);
			try {

				InputStream bufferedIn = new BufferedInputStream(inputStream);
				waveStream = AudioSystem.getAudioInputStream(bufferedIn);

			} catch (UnsupportedAudioFileException e1) {
			    throw new PlayWaveException(e1);
			} catch (IOException e1) {
			    throw new PlayWaveException(e1);
			}
			
			// Obtain the information about the AudioInputStream
			AudioFormat audioFormat = waveStream.getFormat();
			Info info = new Info(SourceDataLine.class, audioFormat);
//			System.out.println("Audio Info: "+info.toString());
			
			try {
			    dataLine = (SourceDataLine) AudioSystem.getLine(info);
			    dataLine.open(audioFormat, EXTERNAL_BUFFER_SIZE);
			} catch (LineUnavailableException e) {
			    throw new PlayWaveException(e);
			}
			
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		    System.out.println("Fail to open Audio .Wav at:"+audioPath);
		}
	}
	
	public void start() {
		try {
			openAudioStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Finish Preparing Audio DataLine");
		
		audioRunner = new AudioRunner();
		audioRunner.execute();
	}
	
	private class AudioRunner extends SwingWorker<Boolean, Void>{
		@Override
		protected Boolean doInBackground() throws Exception {
			if(dataLine.isOpen()) {
				System.out.println("AudioRunner Begins");
				dataLine.start();
				
				int readBytes = 0;
				byte[] audioBuffer = new byte[EXTERNAL_BUFFER_SIZE];

				try {
				    while (readBytes != -1) {
						readBytes = waveStream.read(audioBuffer, 0,audioBuffer.length);
						if (readBytes >= 0){
						    dataLine.write(audioBuffer, 0, readBytes);
						    totalBytes += EXTERNAL_BUFFER_SIZE;
						}
				    }
				} catch (IOException e1) {
				    throw new PlayWaveException(e1);
				}  finally {
				    // plays what's left and and closes the audioChannel
				    dataLine.drain();
				    dataLine.close();
				}
			}else {
				System.out.println("DataLine not Ready Upon Invocation");
				System.exit(1);
			}
			while(!isCancelled()) {
				Thread.sleep(freezeGap);
			}

			return true;
		}
		
		protected void done() {
			try {
				Boolean flag = get();
				
				if(flag) {
					System.out.println("AudioRunner Finished ");
				}else {
					System.out.println("AudioRunner Aborted with Exception");
				}
			} catch (CancellationException e) {
				System.out.println("audioRunner Exception: Cancelled");
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public AudioPlayer(String path, Integer frameRate){
		videoFps = frameRate;
		bytePerVideoFrame = bytePerFrame*framePerSecond/videoFps;
		audioPath = path;

	}
}
