import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;





public class AdRemoval {
	String dataset;
	String videoPath;
	String audioPath;
	String outputDir;
	Boolean interactiveFlag = false;
	
	Integer bufferSize = 100;
	Integer freezeGap = 500;
	Integer width = 480;
	Integer height = 270;
	Integer frameRate = 30;
	
	BlockingQueue<BufferedImage> frames = new ArrayBlockingQueue<>(bufferSize);	
	Boolean hasMoreFrames = true;
	Integer frameOffset = 0;
	Integer currentFrame = 0;
	Integer totalFrames = 0;
	String totalTimeString;

	private Boolean getVideoSize() {
		try {
			int frameLength = width * height * 3;
			Integer count = 0;

			File file = new File(videoPath);
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
					totalTimeString = framesToTime(totalFrames);
					raf.close();
					return true;
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
		return false;
	}
	
	private String framesToTime(Integer numberFrames) {
		Integer minutes = ((numberFrames / frameRate) % 3600) / 60;
		Integer seconds = Math.round((numberFrames / frameRate) % 60);

		return String.format("%02d:%02d", minutes, seconds);
	}
	
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
	
	private void interactiveRemoval() {
		EventQueue.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				new InteractiveSceneDetector().display();
			}
		});
	}
	
	private void automaticRemoval() {
		
	}
	
	@SuppressWarnings("serial")
	protected class InteractiveSceneDetector extends JPanel{
		private JLabel label1 = new JLabel();
		private JLabel label2 = new JLabel();
		private JLabel lbText1 = new JLabel();
		private JLabel lbText2 = new JLabel();
		
		JLabel timeBar = new JLabel();

		FrameLoader worker;

		JButton cut = new JButton("Cut");
		
		JButton skip = new JButton("Skip");
		
		public InteractiveSceneDetector() {
			/* Panel has format :
			 * Text1		Text2
			 * Frame1		Frame2
			 * Cut	Skip	Scene#	Time
			 */
			
			initFrameLoader();
			
			GridBagLayout gLayout = new GridBagLayout();
			this.setLayout(gLayout);
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 0.5;
			
			/* Frame 1 */
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 5;
			label1.setIcon(new ImageIcon(frames.peek()));
			this.add(label1,c);
			
			/* Frame 2 */
			c.gridx = 5;
			c.gridy = 1;
			c.gridwidth = 5;
			label2.setIcon(new ImageIcon(frames.peek()));
			this.add(label2,c);
			
			/* Text 1 */
			lbText1.setHorizontalAlignment(SwingConstants.CENTER);
			lbText1.setVerticalAlignment(SwingConstants.CENTER);
			lbText1.setFont(new Font("Courier New", Font.BOLD, 16));
			lbText1.setText("Frame: ");
			
			c.gridx = 0;
			c.gridy = 0;
			this.add(lbText1,c);
			
			/* Text 2 */
			lbText2.setHorizontalAlignment(SwingConstants.CENTER);
			lbText2.setVerticalAlignment(SwingConstants.CENTER);
			lbText2.setFont(new Font("Courier New", Font.BOLD, 16));
			lbText2.setText("Frame: ");
			
			c.gridx = 5;
			c.gridy = 0;
			this.add(lbText2,c);
			
			
			/* Time Bar */
			timeBar.setFont(new Font("Courier New", Font.BOLD, 16));
			setTimeBar();
			c.gridx = 0;
			c.gridy = 2;
			c.gridwidth = 1;
			this.add(timeBar,c);
			
			/* Cut Button */
			c.gridx = 0;
			c.gridy = 3;
			c.gridwidth = 5;
			this.add(cut,c);
			
			/* Skip Button */
			c.gridx = 5;
			c.gridy = 3;
			c.gridwidth = 5;
			this.add(skip,c);	
			

		}
		
		private void display() {
			JFrame frame = new JFrame("Scenes Detector");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(this);
			frame.setResizable(false);
			frame.pack();
			frame.setVisible(true);
		}
		
		private void initFrameLoader() {
			// Load Initial buffer and send out worker
			
			while (frames.remainingCapacity() > 10) {
				Boolean ret = loadFrame();
				if (!ret) {
					System.out.println("No More Frames!");
					hasMoreFrames = false;
					break;
				}
			}
//			System.out.println("On EventDispatchThread: "+SwingUtilities.isEventDispatchThread());
			worker = new FrameLoader();
			worker.execute();
		}
		
		private void setTimeBar() {
			timeBar.setText(framesToTime(currentFrame)+" / "+totalTimeString);
		}
	}
	public Boolean loadFrame() {
		try {
			int frameLength = width * height * 3;

			File file = new File(videoPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");

			long len = frameLength;
			Integer flag = 0;
			long position = frameOffset * len;

			byte[] bytes = new byte[(int) len];

			raf.seek(position);
			flag = raf.read(bytes);
			if (flag == -1) {
				raf.close();
				return false;
			}

			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			Integer ind = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					// byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					img.setRGB(x, y, pix);
					ind++;
				}
			}
			frames.add(img);
			raf.close();
			frameOffset++;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	private class FrameLoader extends SwingWorker<Boolean, Void> {
		@Override
		protected Boolean doInBackground() throws Exception {
			// Load Frames to buffer
			System.out.println("FrameLoader Start");
			while (!isCancelled()) {
				if (frames.remainingCapacity() > 10) {
					Boolean ret = loadFrame();
					if (!ret)
						return true;
				} else {
//					if (new Random().nextInt(10) > 8)
//						System.out.println("FrameLoader Worker sleeps");
					Thread.sleep(freezeGap);
				}
			}
			System.out.println("FrameLoader Worker got Cancel Signal");
			return false;
		}

		
		protected void done() {
			try {
				// Retrive worker status, true: Loaded All Frames  False: Worker cancelled
				Boolean flag = get();
				
				// Flag Source condition
				if(flag) {
					hasMoreFrames = false;
					System.out.println("FrameLoader Worker Finish");
				}else {
					System.out.println("FrameLoader Worker Cancelled");
				}


			} catch (CancellationException e) {
				System.out.println("FrameLoader Exception: Cancelled");
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void resolveArguments(String[] args) {
		if (args.length != 2) {
			System.out.println("Invalid Number of Input Arguments");
			System.out.println("The format should be --> java ProgramName DatasetName InteractiveMode");
			System.exit(-1);
		} else {
			dataset = args[0];
			//AdRemoval.java -> AdFilter folder -> Java folder -> Project folder -> Data folder..
			String dataDir = "../../../Data/" + dataset + "/Videos/";
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
			
			try {
				interactiveFlag = Integer.parseInt(args[1]) == 0 ? false:true;
			}catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
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
		System.out.println("Interactive Mode: "+interactiveFlag);
	}
	
	public static void main(String[] args) {
		AdRemoval remover = new AdRemoval();
		String seperator = "--------------------";
		/* Resolve Input Arguments */
		remover.resolveArguments(args);
		remover.displayConfiguration();
		remover.getVideoSize();
		
		/* Scene Transition Detection */
		System.out.println(seperator+"Interative Scene Detection"+seperator);
		remover.interactiveRemoval();
		remover.automaticRemoval();
		
	}
}
