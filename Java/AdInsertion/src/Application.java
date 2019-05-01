import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class Application {
	
	public static void main(String[] args) throws IOException{
		Set<String> set1 = new HashSet<String>();
		Set<String> set2 = new HashSet<String>();
		set1.add("225");
		set2.add("100");
		VideoInsertor videoInsertor = new VideoInsertor(
				"D:\\cs576-final-project\\dataset\\dataset\\Ads\\Starbucks_Ad_15s.rgb", 
				"D:\\cs576-final-project\\dataset\\dataset\\Ads\\Subway_Ad_15s.rgb", 
				"D:\\cs576-final-project\\dataset\\dataset\\Ads\\Subway_Ad_15s.rgb",set1, set2);
		
		AudioInsertor audioInsertor = new AudioInsertor(30,
				"D:\\cs576-final-project\\dataset\\dataset\\Ads\\Starbucks_Ad_15s.wav", 
				"D:\\cs576-final-project\\dataset\\dataset\\Ads\\Subway_Ad_15s.wav", 
				"D:\\cs576-final-project\\dataset\\dataset\\Ads\\Starbucks_Ad_15s.wav",set1, set2);
		audioInsertor.insertAudio();
		videoInsertor.insertVideo();
		
		
		
		
		
		

		
	}
	
	
	
}
