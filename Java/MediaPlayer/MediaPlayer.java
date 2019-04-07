import java.util.concurrent.CancellationException;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.*;
import java.io.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.*;
import java.util.Queue;
import java.util.Random;

public class MediaPlayer {
	JFrame frame;
	JLabel lbIm1;
	Queue<BufferedImage> frames = new LinkedList<>();

	String dataset;
	String videoPath;
	String audioPath;

	Integer width = 480;
	Integer height = 270;
	Integer frameRate = 30;

	// Because video is too large to fit in memory, load them use work thread
	Integer frameOffset = 0;
	Integer bufferSize = 100;
	Integer freezeGap = 500;

	private void resolveArguments(String[] args) {
		if (args.length != 1) {
			System.out.println("Invalid Number of Input Arguments");
			System.exit(-1);
		} else {
			dataset = args[0];
			String dataDir = "../../Data/" + dataset + "/Videos/";
			List<String> fileList = new ArrayList<String>();

			try (Stream<Path> paths = Files.walk(Paths.get(dataDir))) {
				fileList = paths.filter(path -> path.toString().endsWith(".rgb") || path.toString().endsWith(".wav"))
						.map(path -> path.toString()).collect(Collectors.toList());
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

		/* Testing Input Resolver */
		String seperator = "--------------------";
		System.out.println(seperator + "Parsing Input Argument" + seperator);
		System.out.println("Parsing Input Argument -> OK");
		System.out.println("Dataset: " + dataset);
		System.out.println("Video file: " + videoPath);
		System.out.println("Audio file: " + audioPath);
		System.out.println("Frame Rate: " + frameRate);
		System.out.println("buffer Size: " + bufferSize);
	}

	private Boolean loadFrame() {
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
					//						byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
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

	private void displayVideo() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				new FramePanel().display();
			}
		});

	}

	class FramePanel extends JPanel {

		private Boolean hasMoreFrames = true;
		private JLabel label = new JLabel();
		private Boolean timerStarted = false;
		private Boolean isPaused = false;
		private FrameLoader worker;

		private class FrameLoader extends SwingWorker<Boolean, Void> {
			@Override
			protected Boolean doInBackground() throws Exception {
				// Load Frames to buffer
				System.out.println("Worker Start");
				while (true) {
					if (frames.size() < bufferSize) {
						Boolean ret = loadFrame();
						if (!ret)
							return true;
					} else {
						if (new Random().nextInt(10) > 8)
							System.out.println("Worker sleeps");
						Thread.sleep(freezeGap);
					}
				}
			}

			protected void done() {
				try {
					// Retrive worker status
					get();
					// Flag Source condition
					hasMoreFrames = false;
					System.out.println("Worker Finish");

				} catch (CancellationException e) {
					System.out.println("FrameLoader Cancelled");
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}

		// Timer part
		private Timer timer;

		public FramePanel() {
			// Buttons Part
			JButton start = new JButton("START");
			start.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("Pressed START");
					onButtonStart();
				}
			});

			JButton pause = new JButton("PAUSE");
			pause.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("Pressed PAUSE");
					onButtonPause();
				}
			});

			JButton resume = new JButton("RESUME");
			resume.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("Pressed RESUME");
					onButtonResume();
				}
			});

			JButton stop = new JButton("STOP");
			stop.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("Pressed STOP");
					onButtonStop();
				}
			});

			loadFrame();
			this.setLayout(new GridBagLayout());
			label.setIcon(new ImageIcon(frames.remove()));
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 1;
			c.gridx = 0;
			c.gridwidth = 6;
			c.gridy = 0;
			this.add(label, c);

			// Button UI
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.ipady = 0;
			c.weighty = 0;
			c.anchor = GridBagConstraints.LAST_LINE_END;
			c.insets = new Insets(0, 0, 0, 0);
			c.gridx = 0;
			c.gridwidth = 1;
			c.gridy = 1;
			this.add(start, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.ipady = 0;
			c.weighty = 0;
			c.anchor = GridBagConstraints.LAST_LINE_END;
			c.insets = new Insets(0, 0, 0, 0);
			c.gridx = 1;
			c.gridwidth = 1;
			c.gridy = 1;
			this.add(pause, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.ipady = 0;
			c.weighty = 0;
			c.anchor = GridBagConstraints.LAST_LINE_END;
			c.insets = new Insets(0, 0, 0, 0);
			c.gridx = 2;
			c.gridwidth = 1;
			c.gridy = 1;
			this.add(resume, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.ipady = 0;
			c.weighty = 0;
			c.anchor = GridBagConstraints.LAST_LINE_END;
			c.insets = new Insets(0, 0, 0, 0);
			c.gridx = 3;
			c.gridwidth = 1;
			c.gridy = 1;
			this.add(stop, c);
		}

		private void update() {
			// System.out.println("Current Frame Queue Size: " + frames.size());
			if (frames.isEmpty() && !hasMoreFrames) {
				timer.stop();
			} else if (frames.isEmpty()) {
				System.out.println("Waiting for Loader to Finish! Freeze the screen");
				return;
			} else {
				label.setIcon(new ImageIcon(frames.remove()));
			}
		}

		private void display() {
			JFrame frame = new JFrame("MediaPlayer");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(this);
			frame.pack();
			frame.setVisible(true);
		}

		private void onButtonPause() {
			if (!timerStarted)
				return;
			timer.stop();
			worker.cancel(true);

			timerStarted = false;
			isPaused = true;
		}

		private void onButtonResume() {
			if (!isPaused)
				return;
			onButtonStart();
			isPaused = false;
		}

		private void onButtonStart() {
			if (timerStarted)
				return;
			// Load Initial buffer and send out worker
			while (frames.size() < bufferSize) {
				Boolean ret = loadFrame();
				if (!ret) {
					hasMoreFrames = false;
					break;
				}
			}
			worker = new FrameLoader();
			worker.execute();

			timer = new Timer(1000 / frameRate, new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					update();
				}
			});
			timerStarted = true;
			timer.start();
		}

		private void onButtonStop() {
			if (timerStarted) {
				timer.stop();
				worker.cancel(true);
			}
			timerStarted = false;
			frameOffset = 0;
			hasMoreFrames = true;
			isPaused = false;
			frames.clear();
		}
	}

	public static void main(String[] args) {

		/* Procedures
		 * - Resolve input arguments
		 * - Read Images from input video file, frame by frame
		 * - Perform scaling/anti-aliasing image by image
		 * - Display movie with altered images at user-decided frameRate
		 */

		MediaPlayer player = new MediaPlayer();

		/* Resolve Input Arguments */
		player.resolveArguments(args);

		String seperator = "--------------------";

		/* Read Videos From File */
		// System.out.println(seperator + "Reading Partial Video" + seperator);
		// player.loadVideo(player.frameVector);

		/* Display  Media */
		System.out.println(seperator + "Playing Media" + seperator);
		player.displayVideo();
	}
}
