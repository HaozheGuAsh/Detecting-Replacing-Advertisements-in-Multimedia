
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class VideoInsertor {
	private String videoPath;
	private String Ad1Path;
	private String Ad2Path;
	private Set<String> insertionPointSet1;
	private Set<String> insertionPointSet2;
	private final int width = 480;
	private final int height =270;
	public long videoFrameOffset = 0;
	public long ad1FrameOffset = 0;
	private long ad2FrameOffset = 0;
	private long videoTotalFrames = 0;
	private long ad2TotalFrames = 0;
	private long ad1TotalFrames = 0;
	private boolean isDone = false;
	private Vector<Point> ad2Remove;
	private String outputPath;
	
	//initiate imgArr to save the img frames; 
	public VideoInsertor(String videoPath, String Ad1Path,  String Ad2Path, Set<String> insertionPointSet1, Set<String> insertionPointSet2,String outputPath){
		this.videoPath = videoPath;
		this.Ad1Path = Ad1Path;
		this.Ad2Path = Ad2Path;
		this.insertionPointSet1 = insertionPointSet1;
		this.insertionPointSet2 = insertionPointSet2;
		this.outputPath = outputPath;
	}
	
	
	public VideoInsertor(String videoPath, String outputPath, Vector<Point> adToRemove) {
		this.videoPath = videoPath;
		this.outputPath = outputPath;
		this.ad2Remove = adToRemove;
	}


	public Set<String> getAdSet1(){
		return insertionPointSet1;
	}
	public long getVideoFrameOffset() {
		return videoFrameOffset;
	}


	public void setVideoFrameOffset(long videoFrameOffset) {
		this.videoFrameOffset = videoFrameOffset;
	}



	public long getAd1FrameOffset() {
		return ad1FrameOffset;
	}



	public void setAd1FrameOffset(long ad1FrameOffset) {
		this.ad1FrameOffset = ad1FrameOffset;
	}



	public long getAd2FrameOffset() {
		return ad2FrameOffset;
	}



	public void setAd2FrameOffset(long ad2FrameOffset) {
		this.ad2FrameOffset = ad2FrameOffset;
	}
	
	


	private void resetvideoFrameOffset(){
		videoFrameOffset = 0;
	}
	
	private void resetAd1FrameOffset(){
		ad1FrameOffset = 0;
	}
	
	private void resetAd2FrameOffset(){
		ad2FrameOffset = 0;
	}
	
	public String getVideoPath() {
		return videoPath;
	}

	public void setVideoPath(String videoPath) {
		this.videoPath = videoPath;
	}

	
	private boolean IsAtInsertPoint1(String n){
		return insertionPointSet1.contains(n);
	}
	
	private boolean IsAtInsertPoint2(String n){
		return insertionPointSet2.contains(n);
	}
	
	public void insertVideo(){
		videoTotalFrames = this.getVideoSize(videoPath);
		ad1TotalFrames = this.getVideoSize(Ad1Path);
		ad2TotalFrames = this.getVideoSize(Ad2Path);
		
//		System.out.println("video total frames: "+videoTotalFrames);
//		System.out.println("ad1 total frames: "+ ad1TotalFrames);
//		System.out.println("ad2 total frames: "+ ad2TotalFrames);

	    BlockingQueue<byte[]> VideoDataQueue = new ArrayBlockingQueue<byte[]>(100);

	    ReaderThread reader = new ReaderThread(VideoDataQueue);
	    WriterThread writer = new WriterThread(VideoDataQueue);

	    new Thread(reader).start();
	    new Thread(writer).start();
	}
	
	public long getVideoSize(String path) {
		long totalFrames = 0;
		try {
			int frameLength = width * height * 3;
			Integer count = 0;

			File file = new File(path);
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
					raf.close();
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
		
		return totalFrames;
	}
	
	//read r
	public byte[] insertVideoBytePerFrame() throws IOException{
		int frameLength = width * height * 3;

		File file = new File(videoPath);
		RandomAccessFile raf = new RandomAccessFile(file, "r");

		long len = frameLength;
		Integer flag = 0;
		long position = videoFrameOffset * len;
//		System.out.println("reading video at postion at " + position);

		byte[] bytes = new byte[(int) len];

		raf.seek(position);
		flag = raf.read(bytes);
		if (flag == -1) {
			raf.close();
			return bytes;
		}

		raf.close();
		videoFrameOffset++;
		return bytes;
	}
	
	public byte[] insertAd1BytePerFrame() throws IOException{
		int frameLength = width * height * 3;

		File file = new File(Ad1Path);
		RandomAccessFile raf = new RandomAccessFile(file, "r");

		long len = frameLength;
		Integer flag = 0;
		long position = ad1FrameOffset * len;

		byte[] bytes = new byte[(int) len];

		raf.seek(position);
		flag = raf.read(bytes);
		if (flag == -1) {
			raf.close();
			return bytes;
		}

		raf.close();
		ad1FrameOffset++;
		return bytes;
	}
	
	public byte[] insertAd2BytePerFrame() throws IOException{
		int frameLength = width * height * 3;

		File file = new File(Ad2Path);
		RandomAccessFile raf = new RandomAccessFile(file, "r");

		long len = frameLength;
		Integer flag = 0;
		long position = ad2FrameOffset * len;

		byte[] bytes = new byte[(int) len];

		raf.seek(position);
		flag = raf.read(bytes);
		if (flag == -1) {
			raf.close();
			return bytes;
		}

		raf.close();
		ad2FrameOffset++;
		return bytes;
	}
	
	public void removeVideo() {
		videoTotalFrames = this.getVideoSize(videoPath);
		System.out.println("video total frames: "+videoTotalFrames);
	    BlockingQueue<byte[]> VideoDataQueue = new ArrayBlockingQueue<byte[]>(100);

	    RemoveReaderThread reader = new RemoveReaderThread(VideoDataQueue);
	    WriterThread writer = new WriterThread(VideoDataQueue);

	    new Thread(reader).start();
	    new Thread(writer).start();
	}
	
	public class RemoveReaderThread implements Runnable{

		  protected BlockingQueue<byte[]> blockingQueue = null;

		  public RemoveReaderThread(BlockingQueue<byte[]> blockingQueue){
		    this.blockingQueue = blockingQueue;     
		  }

		  @Override
		  public void run() {
		    BufferedReader br = null;
		    
		    //read R value in the video file to bytes[] and put it in the blockingQueue;
		    //if there is insertion point, read from the ad file;
		    while(videoFrameOffset < videoTotalFrames){
		          try {
//		        	  System.out.println("now videoFrameOffset is "+ videoFrameOffset + "at insertionpoint is " + insertionPointSet1.contains(String.valueOf(videoFrameOffset)));
			      		int ad1Start = ad2Remove.get(0).x;
			    		int ad1End = ad2Remove.get(0).y;
			    		int ad2Start = ad2Remove.get(1).x;
			    		int ad2End = ad2Remove.get(1).y;
		        	if(videoFrameOffset>=ad1Start &&videoFrameOffset<=ad1End){
	        			insertVideoBytePerFrame();
		        	} else if(videoFrameOffset>=ad2Start &&videoFrameOffset<=ad2End){
		        		insertVideoBytePerFrame();
		        	}else {
						blockingQueue.put(insertVideoBytePerFrame());
		        	}
//					System.out.println("reading oringinal video at  " + videoFrameOffset + "th frame");
				} catch (InterruptedException e) {
					System.out.println("I'm interupted in reader");
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					e.printStackTrace();
				}
		     }
		    
		    isDone = true;
		}
	}
	
	public class ReaderThread implements Runnable{

		  protected BlockingQueue<byte[]> blockingQueue = null;

		  public ReaderThread(BlockingQueue<byte[]> blockingQueue){
		    this.blockingQueue = blockingQueue;     
		  }

		  @Override
		  public void run() {
		    BufferedReader br = null;
		    
		    //read R value in the video file to bytes[] and put it in the blockingQueue;
		    //if there is insertion point, read from the ad file;
		    while(videoFrameOffset < videoTotalFrames){
		          try {
//		        	  System.out.println("now videoFrameOffset is "+ videoFrameOffset + "at insertionpoint is " + insertionPointSet1.contains(String.valueOf(videoFrameOffset)));
		        	if(insertionPointSet1.contains(String.valueOf(videoFrameOffset))){
		        		while(ad1FrameOffset < ad1TotalFrames){
//		        			System.out.println("inserting oringinal video at  " + ad1FrameOffset + "th frame");
		        			blockingQueue.put(insertAd1BytePerFrame());
		        		}
		        		//finish the insertion,reset the ad frame offset
		        		resetAd1FrameOffset();
		        	} 
		        	if(insertionPointSet2.contains(String.valueOf(videoFrameOffset))){
		        		while(ad2FrameOffset < ad2TotalFrames){
		        			blockingQueue.put(insertAd2BytePerFrame());
		        		}
		        		//finish the insertion,reset the ad frame offset
		        		resetAd2FrameOffset();
		        	}
					blockingQueue.put(insertVideoBytePerFrame());
//					System.out.println("reading oringinal video at  " + videoFrameOffset + "th frame");
				} catch (InterruptedException e) {
					System.out.println("I'm interupted in reader");
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					e.printStackTrace();
				}
		     }
		    
//		    //reset the frameOffset, start to read G value / insert G value 
//		    resetvideoFrameOffset();
//		    
//		    while(videoFrameOffset < videoTotalFrames){
//		          try {
//		        	if(insertionPointSet1.contains(String.valueOf(videoFrameOffset))){
//		        		while(ad1FrameOffset < ad1TotalFrames){
//		        			System.out.println("inserting oringinal video at G " + ad1FrameOffset + "th frame");
//		        			blockingQueue.put(insertAd1BytePerFrame(ad1TotalFrames));
//		        		}
//		        		//finish the insertion,reset the ad frame offset
//		        		resetAd1FrameOffset();
//		        	} 
//		        	if(insertionPointSet2.contains(String.valueOf(videoFrameOffset))){
//		        		while(ad2FrameOffset < ad2TotalFrames){
//		        			blockingQueue.put(insertAd2BytePerFrame(ad2TotalFrames));
//		        		}
//		        		//finish the insertion,reset the ad frame offset
//		        		resetAd2FrameOffset();
//		        	}
//		        	System.out.println("reading oringinal video at G " + videoFrameOffset + "th frame");
//					blockingQueue.put(insertVideoBytePerFrame(videoTotalFrames));
//				} catch (InterruptedException e) {
//					System.out.println("I'm interupted in reader");
//					Thread.currentThread().interrupt();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//		     }
//		    
//		    //reset the frameOffset, start to read G value / insert G value 
//		    resetvideoFrameOffset();
//		    
//		    while(videoFrameOffset < videoTotalFrames){
//		          try {
//		        	if(insertionPointSet1.contains(String.valueOf(videoFrameOffset))){
//		        		while(ad1FrameOffset < ad1TotalFrames){
//		        			System.out.println("inserting oringinal video at B " + ad1FrameOffset + "th frame");
//		        			blockingQueue.put(insertAd1BytePerFrame(ad1TotalFrames*2));
//		        		}
//		        		//finish the insertion,reset the ad frame offset
//		        		resetAd1FrameOffset();
//		        	} 
//		        	if(insertionPointSet2.contains(String.valueOf(videoFrameOffset))){
//		        		while(ad2FrameOffset < ad2TotalFrames){
//		        			blockingQueue.put(insertAd2BytePerFrame(ad2TotalFrames*2));
//		        		}
//		        		//finish the insertion,reset the ad frame offset
//		        		resetAd2FrameOffset();
//		        	}
//		        	System.out.println("reading oringinal video at B " + videoFrameOffset + "th frame");
//					blockingQueue.put(insertVideoBytePerFrame(videoTotalFrames*2));
//				} catch (InterruptedException e) {
//					System.out.println("I'm interupted in reader");
//					Thread.currentThread().interrupt();
//					
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//		     }
		    // tell the writer kill itSelf
		    isDone = true;
		}
	}
	
	
	public class WriterThread implements Runnable{
		  long processedVideoFrame = 0;
		  protected BlockingQueue<byte[]> blockingQueue = null;
		  FileOutputStream out = null;
		  
		  public WriterThread(BlockingQueue<byte[]> blockingQueue){
		    this.blockingQueue = blockingQueue;     
		  }

		  @Override
		  public void run() {

		    try {
		    	out = new FileOutputStream(outputPath);
		        while(true){
		            byte[] videoFrame = blockingQueue.take();
//		            System.out.println(videoFrame.length);
		            processedVideoFrame++;
//		        	System.out.println("writing oringinal video at  " + processedVideoFrame + "th frame");
		            out.write(videoFrame);
		            if(blockingQueue.size() == 0 && isDone){
		            	return;
		            }
		        }               
		    } catch(InterruptedException e){
		    	System.out.println("I'm interupted in writer");
		    	Thread.currentThread().interrupt();
		    } catch (IOException e) {
			} finally{
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		  }
		  
		  

		}

}
