package videoplayer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class VideoPlayer {
	JFrame frame;
	JLabel lbIm1;
	
	String videoPath;
	Integer width = 480;
	Integer height = 270;
	Integer frameRate = 30;
	Integer totalFrames = 0;
	String totalTimeString;
	
	// Because video is too large to fit in memory, load them use work thread
	Integer frameOffset = 0;
	Integer currentFrame = 0;
	Integer bufferSize = 100;
	Integer freezeGap = 500;
	
	Boolean hasMoreFrames = true;
	
	FrameLoader worker;
	
	BlockingQueue<BufferedImage> frames = new ArrayBlockingQueue<>(bufferSize);

	double lastEntropy = 0;
	
//	private double getShannonEntropy(BufferedImage actualImage){
//		 List<String> values= new ArrayList<String>();
//		             int n = 0;
//		             HashMap<Integer, Integer> occ = new HashMap<Integer,Integer>();
//		             for(int i=0;i<actualImage.getHeight();i++){
//		                 for(int j=0;j<actualImage.getWidth();j++){
//		                   int pixel = actualImage.getRGB(j, i);
//		                   int alpha = (pixel >> 24) & 0xff;
//		                   int red = (pixel >> 16) & 0xff;
//		                   int green = (pixel >> 8) & 0xff;
//		                   int blue = (pixel) & 0xff;
//
//		                   int d= (int)Math.round(0.2989 * red + 0.5870 * green + 0.1140 * blue);
//		                  if(!values.contains(String.valueOf(d)))
//		                      values.add(String.valueOf(d));
//		                  if (occ.containsKey(d)) {
//		                      occ.put(d, occ.get(d) + 1);
//		                  } else {
//		                      occ.put(d, 1);
//		                  }
//		                  ++n;
//		           }
//		        }
//		        double e = 0.0;
//		        for (HashMap.Entry<Integer, Integer> entry : occ.entrySet()) {
//		             int cx = entry.getKey();
//		             double p = (double) entry.getValue() / n;
//		             e += p * (Math.log(p)/Math.log(2));
//		        }
//		 return -e;
//	}
	
	public Integer getFrameRate() {
		return frameRate;
	}
	
	public String getTotalTimeString() {
		return totalTimeString;
	}
	
	
	public Integer getBufferSize() {
		return bufferSize;
	}
	
	public Integer getCurFrameOffset() {
		return currentFrame ;
	}
	public BufferedImage popFrame() {
//		double curEntropy = getShannonEntropy(frames.peek());
//		if(Math.abs(curEntropy - lastEntropy)> 0.1*curEntropy)
//			System.out.println("Seems like a cut at frame: "+currentFrame);
//		lastEntropy = curEntropy;
		
		currentFrame++;
		return frames.remove();
	}
	
	public String makeTimeBar() {
		return framesToTime(currentFrame)+" / "+totalTimeString;
	}
	
	public Boolean isQueueEmpty() {
		return frames.isEmpty();
	}
	public Boolean hasMoreFrames() {
		return hasMoreFrames;
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
					// int pix = ((a << 24) + (r << 16) + (g << 8) + b);
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
	
	public void start() {
		// Load Initial buffer and send out worker
		
		while (frames.remainingCapacity() > 10) {
			Boolean ret = loadFrame();
			if (!ret) {
				System.out.println("No More Frames!");
				hasMoreFrames = false;
				break;
			}
		}
//		System.out.println("On EventDispatchThread: "+SwingUtilities.isEventDispatchThread());
		worker = new FrameLoader();
		worker.execute();
	}
	
	public void cancelWorker() {
		System.out.println("On EventDispatchThread: "+SwingUtilities.isEventDispatchThread());

		while(!worker.isCancelled()) {
			worker.cancel(true);
		}	
	}
	
	public void pause() {
//		System.out.println("Sending Cancel Signal to worker");
		cancelWorker();
	}
	
	public void stop() {
		frameOffset = 0;
		currentFrame = 0;
		hasMoreFrames = true;
		frames.clear();
		System.out.println("Sending Cancel Signal to worker");
		cancelWorker();
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
	
	public VideoPlayer(String path) {
		videoPath = path;
		getVideoSize();
	}


}
