package audioplayer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class AudioPlayer {
	String audioPath;
	FileInputStream waveStream;
	
	public AudioPlayer(String path) {
		audioPath = path;
		// opens the inputStream
		try {
		    waveStream = new FileInputStream(audioPath);
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		}
	}
}
