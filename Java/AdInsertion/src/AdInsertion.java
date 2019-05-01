import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdInsertion {
	Set<String> ad1Pos = new HashSet<String>();
	Set<String> ad2Pos = new HashSet<String>();
	String ad1Name = "ae_ad_15s";
	String ad2Name = "hrc_ad_15s";
	String ad1Video;
	String ad2Video;
	String ad1Audio;
	String ad2Audio;
	
	Vector<Point> adToRemove = new Vector<Point>();
	
	String wRemovalPath;
	String wInsertionPath;
	
	String audioPath;
	String videoPath;
	
	String dataset;
	
	private void resolveArguments(String[] args) {
		if (args.length != 1) {
			System.out.println("Invalid Number of Input Arguments");
			System.out.println("The format should be --> java ProgramName DatasetUsed");
			System.exit(-1);
		} else {
			dataset = args[0];
			//AdInsertion.java -> src,bin folder -> Java folder -> Project folder -> Data folder..
			String dataDir = "../../../Data/" + dataset + "/Videos/";
			String adDir = Paths.get("../../../Data/" + dataset+"/Ads").toAbsolutePath().normalize().toString();
			String outDir = "../../../Data/"+"outputDataset";
			
			
			Path wRemovalOutDirPath = Paths.get(outDir+"/wRemoval").toAbsolutePath().normalize();
			if(Files.notExists(wRemovalOutDirPath)) {
				try {
					Files.createDirectories(wRemovalOutDirPath);
					if(Files.notExists(Paths.get(wRemovalOutDirPath.toString()+"/Videos"))) {
						Files.createDirectories(Paths.get(wRemovalOutDirPath.toString()+"/Videos"));
					}		
					Files.createDirectories(Paths.get(wRemovalOutDirPath.toString()+"/Videos"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Creating wRemoval Output Directory");
			}else {
				System.out.println("wRemoval Output Scenes Directory Already Exist");
			}
			wRemovalPath = wRemovalOutDirPath.toString()+"/Videos";
			
			
			Path wInsertionOutDirPath = Paths.get(outDir+"/wInsertion").toAbsolutePath().normalize();
			if(Files.notExists(wInsertionOutDirPath)) {
				try {
					Files.createDirectories(wInsertionOutDirPath);
					if(Files.notExists(Paths.get(wInsertionOutDirPath.toString()+"/Videos"))) {
						Files.createDirectories(Paths.get(wInsertionOutDirPath.toString()+"/Videos"));
					}		
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Creating wInsertion Output Directory");
			}else {
				System.out.println("wInsertion Output Scenes Directory Already Exist");
			}
			wInsertionPath = wInsertionOutDirPath.toString()+"/Videos";
			
			
			
			
			/* Read Removal and Insertion Position */
			BufferedReader reader;
			try {
				reader = new BufferedReader(new FileReader(
						outDir+"/adRemovalFrame.txt"));
				String[] pos1 = reader.readLine().split("\t");
				String[] pos2 = reader.readLine().split("\t");
				adToRemove.add(new Point(Integer.parseInt(pos1[0]),Integer.parseInt(pos1[1])));
				adToRemove.add(new Point(Integer.parseInt(pos2[0]),Integer.parseInt(pos2[1])));
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				reader = new BufferedReader(new FileReader(
						outDir+"/adInsertionFrame.txt"));
				ad1Pos.add(reader.readLine());
				ad2Pos.add(reader.readLine());
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			
			/* set Ad file path */
			ad1Audio = adDir+"/"+ad1Name+".wav";
			ad1Video = adDir+"/"+ad1Name+".rgb";
			
			ad2Audio = adDir+"/"+ad2Name+".wav";
			ad2Video = adDir+"/"+ad2Name+".rgb";
			
			/* Find Original Data */
			List<String> fileList = new ArrayList<String>();

			try (Stream<Path> paths = Files.walk(Paths.get(dataDir))) {
				fileList = paths.filter(path -> path.toString().endsWith(".rgb") || path.toString().endsWith(".wav"))
						.map(path -> path.toAbsolutePath().normalize().toString()).collect(Collectors.toList());
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (fileList.size() == 2) {
				videoPath = fileList.get(0).endsWith(".rgb") ? fileList.get(0) : fileList.get(1);
				audioPath = fileList.get(0).endsWith(".wav") ? fileList.get(0) : fileList.get(1);
				
				
			} else {
				System.out.println("Invalid Number of Valid Files in Given Dataset");
				System.exit(-1);
			}
		}
	}
	
	
	private void displayConfiguration() {
		String seperator = "--------------------";
		System.out.println(seperator + "Parsing Input Argument" + seperator);
		System.out.println("Parsing Input Argument -> OK");
		System.out.println("Dataset: " + dataset);
		System.out.println("Video file: " + videoPath);
		System.out.println("Audio file: " + audioPath);
		System.out.println("AD1 Video file: " + ad1Video);
		System.out.println("AD1 Audio file: " + ad1Audio);
		System.out.println("AD2 Video file: " + ad2Video);
		System.out.println("AD2 Audio file: " + ad2Audio);
		System.out.println("wRemoval Path: "+wRemovalPath);
		System.out.println("wInsertion Path: "+wInsertionPath);
		
		for(int i = 0; i < 2; i++) {
			System.out.println("Removing AD from: "+ adToRemove.get(i).x+" to: "+adToRemove.get(i).y);
		}
		
		for(String pos : ad1Pos) {
			System.out.println("Inserting AD1 at: "+pos);
		}
		for(String pos : ad2Pos) {
			System.out.println("Inserting AD2 at: "+pos);
		}
	}
	
	private void remove() {
		String seperator = "--------------------";
		System.out.println(seperator+" Removing Audio "+seperator);
		AudioInsertor audioRemover = new AudioInsertor(30,audioPath,wRemovalPath+"/wRemoval.wav",adToRemove);
		try {
			audioRemover.removeAudio();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(seperator+" Removing Video "+seperator);
		VideoInsertor videoRemover = new VideoInsertor(videoPath,wRemovalPath+"/wRemoval.rgb",adToRemove);
		videoRemover.removeVideo();
	}
	
	private void insert() {
		String seperator = "--------------------";
		System.out.println(seperator+" Inserting Audio "+seperator);
		
		AudioInsertor audioInsertor = new AudioInsertor(30,audioPath,ad1Audio,
										ad2Audio,ad1Pos,ad2Pos,wInsertionPath+"/wInsertion.wav");
		try {
			audioInsertor.insertAudio();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(seperator+" Inserting Video "+seperator);
		
		
		VideoInsertor videoInsertor = new VideoInsertor(videoPath,ad1Video,
										ad2Video,ad1Pos,ad2Pos,wInsertionPath+"/wInsertion.rgb");
		
		videoInsertor.insertVideo();
		
	}
	public static void main(String[] args) throws IOException{
		AdInsertion insertor = new AdInsertion();

		/* Resolve Input Arguments */
		insertor.resolveArguments(args);
		insertor.displayConfiguration();
		insertor.remove();
		insertor.insert();
		
	}
}
