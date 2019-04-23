import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;



public class AdRemoval {
	String dataset;
	String videoPath;
	String audioPath;
	String outputDir;
	
	private double getEntropy(BufferedImage actualImage){
		// Shannon Entropy
		 List<String> values= new ArrayList<String>();
		             int n = 0;
		             HashMap<Integer, Integer> occ = new HashMap<Integer,Integer>();
		             for(int i=0;i<actualImage.getHeight();i++){
		                 for(int j=0;j<actualImage.getWidth();j++){
		                   int pixel = actualImage.getRGB(j, i);
		                   int alpha = (pixel >> 24) & 0xff;
		                   int red = (pixel >> 16) & 0xff;
		                   int green = (pixel >> 8) & 0xff;
		                   int blue = (pixel) & 0xff;

		                   int d= (int)Math.round(0.2989 * red + 0.5870 * green + 0.1140 * blue);
		                  if(!values.contains(String.valueOf(d)))
		                      values.add(String.valueOf(d));
		                  if (occ.containsKey(d)) {
		                      occ.put(d, occ.get(d) + 1);
		                  } else {
		                      occ.put(d, 1);
		                  }
		                  ++n;
		           }
		        }
		        double e = 0.0;
		        for (HashMap.Entry<Integer, Integer> entry : occ.entrySet()) {
		             int cx = entry.getKey();
		             double p = (double) entry.getValue() / n;
		             e += p * (Math.log(p)/Math.log(2));
		        }
		 return -e;
	}
	
	private void resolveArguments(String[] args) {
		if (args.length != 1) {
			System.out.println("Invalid Number of Input Arguments");
			System.exit(-1);
		} else {
			dataset = args[0];
			//AdRemoval.java -> AdFilter folder -> Java folder -> Project folder -> Data folder..
			String dataDir = "../../../../Data/" + dataset + "/Videos/";
			String outDir = dataDir+"Output_Scenes";
			Path outDirPath = Paths.get(outDir).toAbsolutePath().normalize();
			if(Files.notExists(outDirPath)) {
				try {
					Files.createDirectories(outDirPath);
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Creating Output Scenes Directory");
			}else {
				System.out.println("Output Scenes Directory Already Exist");
			}
			outputDir = outDirPath.toString();
			
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
		System.out.println("Output Directory Path: "+outputDir);
	}
	
	public static void main(String[] args) {
		AdRemoval remover = new AdRemoval();
		
		/* Resolve Input Arguments */
		remover.resolveArguments(args);
		remover.displayConfiguration();
		
		/* Scene Transition Detection */
		System.out.println("String Interative Scene Detection");
		
	}
}
