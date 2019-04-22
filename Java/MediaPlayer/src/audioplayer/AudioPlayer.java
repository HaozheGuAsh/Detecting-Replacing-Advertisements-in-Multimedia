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
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingWorker;

public class AudioPlayer {
	String audioPath;
	FileInputStream inputStream;
	AudioInputStream waveStream;
	Boolean isPaused = false;
	
	Integer videoFps;
	
	private final int bytePerFrame = 2;
	private final int framePerSecond = 48000;
	private final long bytePerVideoFrame;
	
	
	Clip dataClip = null;
	long dataClipTime = 0;
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
			DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
//			System.out.println("Audio Info: "+info.toString());
			
			try {
				dataClip = (Clip) AudioSystem.getLine(info);
				dataClip.open(waveStream);
			} catch (LineUnavailableException e) {
			    throw new PlayWaveException(e);
			}
			
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		    System.out.println("Fail to open Audio .Wav at:"+audioPath);
		}
	}
	
	private void getDataClipPosition() {
		dataClipTime = dataClip.getMicrosecondPosition();
	}
	
	private void setDataClipPosition() {
		dataClip.setMicrosecondPosition(dataClipTime);
	}
	
	public String makeTimeBar(String totalTimeString) {
		return framesToTime((int)getAudioToVideoFrame())+" / "+totalTimeString;
	}
	
	private String framesToTime(Integer numberFrames) {
		Integer minutes = ((numberFrames / videoFps) % 3600) / 60;
		Integer seconds = Math.round((numberFrames / videoFps) % 60);

		return String.format("%02d:%02d", minutes, seconds);
	}
	
	private long curAudioOffset() {
		// 2 byte per frame
		return dataClip.getLongFramePosition()*2;
	}
	
	public long getAudioToVideoFrame() {
		return curAudioOffset()/bytePerVideoFrame;
	}
	
	public void start() {
		if(isPaused) 
			return;
		
		System.out.println("Start AudioPlayer");
		try {
			openAudioStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		audioRunner = new AudioRunner();
		audioRunner.execute();
	}
	
	public void resume() {
		System.out.println("Resume AudioPlayer");
		isPaused = false;
		setDataClipPosition();
		dataClip.start();
	}
	
	public void pause() {
		System.out.println("Pause AudioPlayer");
		isPaused = true;
		getDataClipPosition();
		dataClip.stop();
	}
	
	public void stop() {
		System.out.println("Stop AudioPlayer");
		isPaused = false;
		dataClip.stop();
	}
	
	private class AudioRunner extends SwingWorker<Boolean, Void>{
		@Override
		protected Boolean doInBackground() throws Exception {
			if(dataClip.isOpen()) {
				System.out.println("AudioRunner Begins");
				dataClip.start();

			}else {
				System.out.println("DataLine not Ready Upon Invocation");
				System.exit(1);
			}
			while(!isCancelled()) {
				Thread.sleep(freezeGap);
			}
			dataClip.close();

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
