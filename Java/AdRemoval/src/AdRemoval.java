import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.Vector;
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
import javax.swing.Timer;





public class AdRemoval {
	String dataset;
	String videoPath;
	String audioPath;
	String outputDir;
	Boolean interactiveFlag = false;
	
	Integer bufferSize = 50;
	Integer freezeGap = 500;
	Integer width = 480;
	Integer height = 270;
	Integer frameRate = 30;
	
	BlockingQueue<BufferedImage> frames = new ArrayBlockingQueue<>(bufferSize);	
	Boolean hasMoreFrames = true;
	Integer frameOffset = 0;
	
	Boolean detectorFinished = false;
	// Frame number on top
	Integer currentFrame = 0;
	Integer totalFrames = 0;
	String totalTimeString;
	
	// Scene Cut
	BufferedImage lastFrame;
	BufferedImage last2Frame;
	BufferedImage last3Frame;
	// Threshold for Entropy Difference percent
	float[][] lastL = new float[width][height];
	float[][] lastA = new float[width][height];
	float[][] lastB = new float[width][height];
	double sceneEntropyThreshold = 5;
	double sceneColorThreshold = 5;
	double sceneColorOverflowThreshold = 3;
	double sceneADColorThreshold = 0.04;
	int	sceneLengthThreshold = 100;
	int sceneUnderflowLengthThreshold = 500;
	double sceneColorUnderflowThreshold = 0.01;
	double lastEntropy = 0;
	double entropyDiffPercent = 0;
	double colorDistancePercent = 0;
	double overflowVote = 0;
	double underflowVote = 0;
	double vote = 0;
	Vector<Double> voteRecord = new Vector<Double>();
	int sceneStart = -1;
	Vector<Point> adFrame = new Vector<Point>();
	double averateVote = 0;
	double sceneSumVote = 0;
	
	// Color Histogram Similarity
	
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
	
	private BufferedImage popFrame() {
		last3Frame = last2Frame;
		last2Frame = lastFrame;
		lastFrame = frames.remove();
		currentFrame++;
		return lastFrame;
	}
	
    private class Holder {
        final CIELab INSTANCE = new CIELab();
        private CIELab getInstance() {
       	 return INSTANCE;
        }
   }
    
	public class CIELab extends ColorSpace {

	    public CIELab getInstance() {
	        return new Holder().getInstance();
	    }

	    @Override
	    public float[] fromCIEXYZ(float[] colorvalue) {
	        double l = f(colorvalue[1]);
	        double L = 116.0 * l - 16.0;
	        double a = 500.0 * (f(colorvalue[0]) - l);
	        double b = 200.0 * (l - f(colorvalue[2]));
	        return new float[] {(float) L, (float) a, (float) b};
	    }

	    @Override
	    public float[] fromRGB(float[] rgbvalue) {
	        float[] xyz = CIEXYZ.fromRGB(rgbvalue);
	        return fromCIEXYZ(xyz);
	    }

	    @Override
	    public float getMaxValue(int component) {
	        return 128f;
	    }

	    @Override
	    public float getMinValue(int component) {
	        return (component == 0)? 0f: -128f;
	    }    

	    @Override
	    public String getName(int idx) {
	        return String.valueOf("Lab".charAt(idx));
	    }

	    @Override
	    public float[] toCIEXYZ(float[] colorvalue) {
	        double i = (colorvalue[0] + 16.0) * (1.0 / 116.0);
	        double X = fInv(i + colorvalue[1] * (1.0 / 500.0));
	        double Y = fInv(i);
	        double Z = fInv(i - colorvalue[2] * (1.0 / 200.0));
	        return new float[] {(float) X, (float) Y, (float) Z};
	    }

	    @Override
	    public float[] toRGB(float[] colorvalue) {
	        float[] xyz = toCIEXYZ(colorvalue);
	        return CIEXYZ.toRGB(xyz);
	    }

	    CIELab() {
	        super(ColorSpace.TYPE_Lab, 3);
	    }

	    private double f(double x) {
	        if (x > 216.0 / 24389.0) {
	            return Math.cbrt(x);
	        } else {
	            return (841.0 / 108.0) * x + N;
	        }
	    }

	    private double fInv(double x) {
	        if (x > 6.0 / 29.0) {
	            return x*x*x;
	        } else {
	            return (108.0 / 841.0) * (x - N);
	        }
	    }

	    private Object readResolve() {
	        return getInstance();
	    }

	    private static final long serialVersionUID = 5027741380892134289L;

	    private final ColorSpace CIEXYZ =
	        ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

	    private static final double N = 4.0 / 29.0;

	}
	
	private void getLab(BufferedImage img, float[][] l, float[][] a, float[][] b) {
		 for(int w = 0; w < width; w++) {
			 for(int h = 0; h < height; h++) {
				int col = img.getRGB(w, h);
				float[] lab = new float[3];
				lab[2] = col & 0xff;
				lab[1] = (col & 0xff00) >> 8;
			 	lab[0] = (col & 0xff0000) >> 16;
			 	
			 	lab = new CIELab().fromRGB(lab);
			 	l[w][h] = lab[0];
			 	a[w][h] = lab[1];
			 	b[w][h] = lab[2];
			 
			 }
		 }
	}
	
	private double getColorDiffPercent(float[][] l1, float[][] a1, float[][] b1, float[][] l2, float[][] a2, float[][] b2) {
		 double diff = 0;
		 for(int w = 0; w < width; w++) {
			 for(int h = 0; h < height; h++) {
				 diff += Math.sqrt(Math.pow(l1[w][h] - l2[w][h], 2)+Math.pow(a1[w][h] - a2[w][h], 2)+Math.pow(b1[w][h] - b2[w][h], 2));
			 }
		 }
		 diff /= Math.sqrt(Math.pow(255,2)*3)*width*height;
		return diff;
	}
	
	private double getColorBlockDiffPercent(float[][] l1, float[][] a1, float[][] b1, float[][] l2, float[][] a2, float[][] b2) {
		 int blockSize = 10;
		 double diff = 0;
		 for(int w = 0; w < width/blockSize; w++) {
			 for(int h = 0; h < height/blockSize; h++) {
				 float avgl1 = 0,avga1 = 0,avgb1 = 0,avgl2 = 0,avga2 = 0,avgb2 = 0;
				 for(int ww = 0; ww<blockSize;ww++) {
					 for(int hh = 0;hh<blockSize;hh++) {
						 int idxW = w*blockSize+ww;
						 int idxH = h*blockSize+hh;
						 
						 avgl1 += l1[idxW][idxH];
						 avga1 += a1[idxW][idxH];
						 avgb1 += b1[idxW][idxH];
						 
						 avgl2 += l1[idxW][idxH];
						 avga2 += a2[idxW][idxH];
						 avgb2 += b2[idxW][idxH];
					 }
				 }
				 
				 diff += Math.sqrt(Math.pow((avgl1 - avgl2)/(blockSize*blockSize), 2)+
						 Math.pow((avga1 - avga2)/(blockSize*blockSize), 2)+
						 Math.pow((avgb1 - avgb2)/(blockSize*blockSize), 2));
			 }
		 }
		 diff /= Math.sqrt(Math.pow(255,2)*3)*(width/blockSize)*(height/blockSize);
		return diff;
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
				InteractiveSceneDetector jp = new InteractiveSceneDetector();
				jp.display();

			}
		});
	}
	
	private void printAdFrame() {
		while(!detectorFinished) {
			
		}
		System.out.println("Save Ad Frame Info");
		for(int i = 0; i < adFrame.size();i++) {
			int startPos = adFrame.get(i).x;
			int endPos = adFrame.get(i).y;
			System.out.println("Advertisement Scene "+(i+1)+"  StartFrame: "+ startPos+" EndFrame: "+endPos);
		}
	}
	
	@SuppressWarnings("serial")
	protected class InteractiveSceneDetector extends JPanel{
		private JLabel label1 = new JLabel();
		private JLabel label2 = new JLabel();
		private JLabel label3 = new JLabel();
		private JLabel lbText1 = new JLabel();
		private JLabel lbText2 = new JLabel();
		private JLabel lbText3 = new JLabel();
		
		private JLabel timeBar = new JLabel();
		private JLabel diffBar = new JLabel();

		private FrameLoader frameWorker;

		private JButton cut = new JButton("Start");
		private Boolean start = false;
		private Boolean stopPoint = false;
		private Boolean finished = false;
		private Boolean backWind = false;
		
		private Timer timer = new Timer(1000 / 30, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Integer flag = checkScene();
				if(flag ==2) {
					backWind = true;
					timer.stop();
					stopPoint = true;
				}else {
					update(flag ==1);
				}
				
			}
		});
		
		public InteractiveSceneDetector() {
			/* Panel has format :
			 * Text1		Text2
			 * Frame1		Frame2
			 * Cut	Skip	Scene#	Time
			 */
			
			initFrameLoader();
			// Set Start Frame
			lastFrame = frames.peek();
			last2Frame = frames.peek();
			last3Frame = frames.peek();
			
			cut.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("Pressed CUT");
					onPressCut();
				}
			});
			
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
			label1.setIcon(new ImageIcon(lastFrame));
			this.add(label1,c);
			
			/* Frame 2 */
			c.gridx = 5;
			c.gridy = 1;
			c.gridwidth = 5;
			label2.setIcon(new ImageIcon(lastFrame));
			this.add(label2,c);
			
			/* Frame 3 */
			c.gridx = 10;
			c.gridy = 1;
			c.gridwidth = 5;
			label3.setIcon(new ImageIcon(lastFrame));
			this.add(label3,c);
			
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
			
			/* Text 3 */
			lbText3.setHorizontalAlignment(SwingConstants.CENTER);
			lbText3.setVerticalAlignment(SwingConstants.CENTER);
			lbText3.setFont(new Font("Courier New", Font.BOLD, 16));
			lbText3.setText("Frame: ");
			
			c.gridx = 10;
			c.gridy = 0;
			this.add(lbText3,c);
			
			
			/* Time Bar */
			timeBar.setFont(new Font("Courier New", Font.BOLD, 16));
			setTimeBar();
			c.gridx = 0;
			c.gridy = 2;
			c.gridwidth = 1;
			this.add(timeBar,c);
			
			/* Statistic Bar */
			diffBar.setFont(new Font("Courier New", Font.BOLD, 16));
			setDiffBar();
			c.gridx = 5;
			c.gridy = 2;
			c.gridwidth = 10;
			this.add(diffBar,c);
			
			
			/* Continue Button */
			c.gridx = 0;
			c.gridy = 3;
			c.gridwidth = 15;
			this.add(cut,c);
				
			
		}
		private void onPressCut() {
			if(finished) {
				return;
			}
			
			if(start && stopPoint) {
				if(backWind) {
					backWind = false;
					int tmp = currentFrame;
					while(tmp%10 !=0) {
						tmp--;
						System.out.println("new Frame num: "+tmp);
					}
					recordScene(tmp);
				}else {
					recordScene(currentFrame-1);
				}
				
				stopPoint = false;
				timer.start();
			}
			
			if(!start) {
				cut.setText("Continue");
				start = true;
				stopPoint = true;
				timer.start();
			}

		}
		
		private void recordScene(int frameNumber) {
			if(sceneStart == -1) {
				// Initial Scene 
				sceneStart = frameNumber;
				System.out.println("Add Starting Point: "+sceneStart);
				return;
			}

			// Make Frame, need to -1
			Boolean adScene = (sceneSumVote/(currentFrame-sceneStart+1))<sceneADColorThreshold?true:false;
			System.out.println("New Scene. Start From: "+sceneStart+" End at: "+(frameNumber - 1)+
					" Record Length: "+(frameNumber - sceneStart)+
					" Type of Scene: "+(adScene?"Advertisement":"Normal")
					);
			if(adScene) {
				adFrame.add(new Point(sceneStart,frameNumber-1));
			}
			sceneStart = frameNumber;
			System.out.println("Add Starting Point: "+sceneStart);
			sceneSumVote = 0;

		}
		
		private Integer checkScene() {
			if(frames.isEmpty()) {
				if(!hasMoreFrames) {
					System.out.println("Finishing SceneDector");
					finished = true;
					return 0;
				}else {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

			// Look forward
			double curEntropy = getEntropy(frames.peek());
			Boolean flag = false;
			
			// Length
			double lengthVote = (currentFrame - sceneStart +1) > sceneLengthThreshold ? 0:1;
			
			
			// Entropy
			entropyDiffPercent = 100*(Math.abs(curEntropy - lastEntropy)/curEntropy);
			double entropyVote = entropyDiffPercent / sceneEntropyThreshold;
			
			
			// Color
			double lastColorDistancePercent = colorDistancePercent;
			float[][] curL = new float[width][height];
			float[][] curA = new float[width][height];
			float[][] curB = new float[width][height];
			
			getLab(frames.peek(), curL, curA, curB);
			colorDistancePercent= 100*getColorDiffPercent(lastL, lastA, lastB, curL, curA, curB);
			lastL = curL;lastA = curA;lastB = curB;
			double colorVote = colorDistancePercent / sceneColorThreshold;
			
			// Color Overflow Compensation
			overflowVote = currentFrame > 10 &&
						   colorDistancePercent - entropyDiffPercent > sceneColorOverflowThreshold?
					  	   (entropyDiffPercent - colorDistancePercent):0;
							  		
			// Color Underflow
		  	underflowVote = 0;
			double underflowNum = Double.parseDouble(String.format("%.2f", colorDistancePercent));
			double underflowCheck = (currentFrame - sceneStart +1) > sceneUnderflowLengthThreshold && 
							(underflowNum<sceneColorUnderflowThreshold)?1:0;
			if(underflowCheck == 1) {
				double lastUnderflowNum = Double.parseDouble(String.format("%.2f", lastColorDistancePercent));
				if(lastUnderflowNum <sceneColorUnderflowThreshold && (currentFrame - sceneStart +1) > sceneUnderflowLengthThreshold) {
					underflowVote = 1;
				}
			}
			
		    
		    // Vote
			vote = entropyVote*0.4+ colorVote*0.6 +overflowVote*0.1;
			
			if(vote>1)
				vote = 1;
			if(vote<0)
				vote = 0;
			
			voteRecord.add(vote);
			sceneSumVote += vote;
			
			
			if(underflowVote ==1) {
				vote = 1;
			}
			if(lengthVote == 1){
				vote = 0;
			}
			vote = currentFrame == 0? 1:vote;
			
			if(vote>1)
				vote = 1;
			if(vote<0)
				vote = 0;
			
			
//			if(vote>0.8) {
				System.out.println(currentFrame+" With Ent: "+  
						String.format("%.2f", entropyDiffPercent)+"%"+ "  Lab: "+
						String.format("%.2f", colorDistancePercent)+"%"+
						"  Over: "+String.format("%.2f", overflowVote)+"%"+
						"  Under: "+String.format("%.2f", underflowVote)+"%"+
						"  L: "+ String.format("%d", (sceneStart != -1?(currentFrame-sceneStart+1):0))+
						"  aV: "+ String.format("%.2f", (sceneSumVote/(currentFrame-sceneStart+1)))+
						"  Vote: "+String.format("%.2f", vote)
						);	
//			}

			if(vote >= 0.9 ) {
				// Entropy vote for Cut Scenes
				System.out.println("Detect Scene Change! "+currentFrame+" With Ent: "+  
						String.format("%.2f", entropyDiffPercent)+"%"+ "  Lab: "+
						String.format("%.2f", colorDistancePercent)+"%"+
						"  Over: "+String.format("%.2f", overflowVote)+"%"+
						"  Under: "+String.format("%.2f", underflowVote)+"%"+
						"  L: "+ String.format("%d", (sceneStart != -1?(currentFrame-sceneStart+1):0))+
						"  aV: "+ String.format("%.2f", (sceneSumVote/(currentFrame-sceneStart+1)))+
						"  Vote: "+String.format("%.2f", vote)
						);
				flag = true;
			}

			lastEntropy = curEntropy;
			if(underflowVote ==1 && lengthVote == 0) {
				return 2;
			}
			if(flag){
				return 1;
			}

			return 0;
			
		}
		
		private void display() {
			JFrame frame = new JFrame("Scenes Detector");
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.add(this);
			frame.setResizable(false);
			frame.pack();
			frame.setVisible(true);
		}

		private void update(Boolean stop) {
			if(finished) {
				timer.stop();
				detectorFinished = true;
				recordScene(currentFrame-1);
				printAdFrame();
				return;
			}

			// Update Frame Image Title
			lbText1.setText("Frame: "+(currentFrame - 2));
			lbText2.setText("Frame: "+(currentFrame - 1));
			lbText3.setText("Frame: "+(currentFrame));
			
			// Statistics

			
			// Update Statistics
			setTimeBar();
			setDiffBar();
			
			// Update Image
			label1.setIcon(new ImageIcon(last2Frame));
			label2.setIcon(new ImageIcon(lastFrame));
			label3.setIcon(new ImageIcon(popFrame()));
			if(stop) {
				timer.stop();
				stopPoint = true;
			}
			
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
			frameWorker = new FrameLoader();
			frameWorker.execute();
		}
		
		private void setTimeBar() {
			timeBar.setText(framesToTime(currentFrame)+" / "+totalTimeString);
		}
		
		private void setDiffBar() {
			diffBar.setText("e: "+ String.format("%.2f", entropyDiffPercent)+"%"+ 
							"  c: "+ String.format("%.2f", colorDistancePercent)+"%"+
							"  o: "+ String.format("%.0f", overflowVote)+
							"  u: "+ String.format("%.0f", underflowVote)+
							"  L: "+ String.format("%d", (sceneStart != -1?(currentFrame-sceneStart+1):0))+
							"  aV: "+ String.format("%.2f", (sceneSumVote/(currentFrame-sceneStart+1)))+
							"  V: "+ String.format("%.2f", vote)+
							"    Type: "+((sceneSumVote/(currentFrame-sceneStart+1))<sceneADColorThreshold?"Ad":"N")
							);
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
		System.out.println("\n Parsing Input Argument -> OK");
		System.out.println("\n Dataset: " + dataset);
		System.out.println("\n Video file: " + videoPath);
		System.out.println("\n Audio file: " + audioPath);
		System.out.println("\n Output Directory Path: "+outputDir);
		System.out.println("\n Interactive Mode: "+interactiveFlag);
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
		remover.printAdFrame();
		
	}
}
