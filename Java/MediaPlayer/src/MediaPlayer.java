import java.util.stream.Collectors;
import java.nio.file.Path;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.*;
import videoplayer.VideoPlayer;
import audioplayer.AudioPlayer;

public class MediaPlayer {
	VideoPlayer videoPlayer;
	AudioPlayer audioPlayer;
	
	String dataset;
	String videoPath;
	String audioPath;

	private void displayConfiguration() {
		String seperator = "--------------------";
		System.out.println(seperator + "Parsing Input Argument" + seperator);
		System.out.println("Parsing Input Argument -> OK");
		System.out.println("Dataset: " + dataset);
		System.out.println("Video file: " + videoPath);
		System.out.println("Audio file: " + audioPath);
		System.out.println("Frame Rate: " + videoPlayer.getFrameRate());
		System.out.println("Total Length: " + videoPlayer.getTotalTimeString());
		System.out.println("buffer Size: " + videoPlayer.getBufferSize());
	}

	private void resolveArguments(String[] args) {
		if (args.length != 1) {
			System.out.println("Invalid Number of Input Arguments");
			System.exit(-1);
		} else {
			dataset = args[0];
			//MediaPlayer.java -> MediaPlayer folder -> Java folder -> Project folder -> Data folder..
			String dataDir = "../../../Data/" + dataset + "/Videos/";
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
				
//				Initialize VideoPlayer and AudioPlayer
				videoPlayer = new VideoPlayer(videoPath);
				audioPlayer = new AudioPlayer(audioPath,videoPlayer.getFrameRate());

				
			} else {
				System.out.println("Invalid Number of Valid Files in Given Dataset");
				System.exit(-1);
			}

		}
	}

	private void displayMedia() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				new FramePanel().display();
			}
		});

	}

	@SuppressWarnings("serial")
	protected class FramePanel extends JPanel {

		private JLabel label = new JLabel();
		private Boolean timerStarted = false;
		private Boolean isPaused = false;

		JLabel statusBar = new JLabel();
		String idleString = "Waiting";
		String playingString = "Playing";
		String pauseString = "Pausing";

		JLabel timeBar = new JLabel();


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

			videoPlayer.loadFrame();
			this.setLayout(new GridBagLayout());
			label.setIcon(new ImageIcon(videoPlayer.popFrame()));
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 1;
			c.gridx = 0;
			c.gridwidth = 9;
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

			// Status Bar
			statusBar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
			statusBar.setText(idleString);
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.ipady = 0;
			c.weighty = 0;
			c.anchor = GridBagConstraints.LAST_LINE_END;
			c.insets = new Insets(0, 10, 6, 0);
			c.gridx = 5;
			c.gridwidth = 2;
			c.gridy = 1;
			this.add(statusBar, c);

			// Time Bar
			timeBar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
			setTimeBar();
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.ipady = 0;
			c.weighty = 0;
			c.anchor = GridBagConstraints.LAST_LINE_END;
			c.insets = new Insets(0, 10, 5, 0);
			c.gridx = 7;
			c.gridwidth = 2;
			c.gridy = 1;
			this.add(timeBar, c);
		}

		private void setTimeBar() {
			timeBar.setText(videoPlayer.makeTimeBar());
		}

		private void update() {
			// System.out.println("Current Frame Queue Size: " + frames.size());
//			System.out.println("Current Frame Offset: "+ videoPlayer.getCurFrameOffset());
//			System.out.println("Current Audio Per Video Offset: "+ audioPlayer.getAudioToVideoFrame());

			setTimeBar();
			if (videoPlayer.isQueueEmpty() && !videoPlayer.hasMoreFrames()) {
				timer.stop();
			} else if (videoPlayer.isQueueEmpty()) {
				System.out.println("Waiting for Loader to Finish! Freeze the screen");
				return;
			} else {
				label.setIcon(new ImageIcon(videoPlayer.popFrame()));
			}
		}

		private void display() {
			JFrame frame = new JFrame("MediaPlayer");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(this);
			frame.setResizable(false);
			frame.pack();
			frame.setVisible(true);
		}

		private void onButtonPause() {
			if (!timerStarted)
				return;
			timer.stop();
			timerStarted = false;
			isPaused = true;
			statusBar.setText(pauseString);
			audioPlayer.pause();
		}

		private void onButtonResume() {
			if (!isPaused)
				return;
			onButtonStart();
			isPaused = false;
			audioPlayer.resume();
		}

		private void onButtonStart() {
			if (timerStarted)
				return;
			videoPlayer.start();
			audioPlayer.start();
			timerStarted = true;
			statusBar.setText(playingString);
			
			timer = new Timer(1000 / videoPlayer.getFrameRate(), new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					update();
				}
			});
			timer.start();

		}

		private void onButtonStop() {
			if (timerStarted) {
				videoPlayer.cancelWorker();
			}
			timer.stop();
			audioPlayer.stop();
			videoPlayer.stop();
			timerStarted = false;
			isPaused = false;
			statusBar.setText(idleString);
		}
	}

	public static void main(String[] args) {

		MediaPlayer player = new MediaPlayer();

		/* Resolve Input Arguments */
		player.resolveArguments(args);
		player.displayConfiguration();

		String seperator = "--------------------";

		/* Display Media */
		System.out.println(seperator + "Playing Media" + seperator);
		player.displayMedia();
	}
}

