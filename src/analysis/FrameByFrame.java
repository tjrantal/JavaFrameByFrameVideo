/*
Frame by frame jmf video read and write written by Timo Rantalainen.
jffmpeg decoders taken from http://jffmpeg.sourceforge.net/
h.264 encoder taken from http://sourceforge.net/projects/h264avcjavaenco
*/

/*
	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

	N.B.  the above text was copied from http://www.gnu.org/licenses/gpl.html
	unmodified. I have not attached a copy of the GNU license to the source...

    Copyright (C) 2011 Timo Rantalainen, tjrantal@gmail.com
*/

package analysis;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;
import javax.media.format.*;
import javax.media.util.*;
import javax.swing.SwingUtilities;		//SwingUtilities.root()
import javax.swing.JFrame;
import javax.swing.JSlider;		//Slider
import javax.swing.JPanel;		//GUI komennot swing
import javax.swing.DefaultBoundedRangeModel;
import java.net.URL;
import net.sourceforge.jffmpeg.*;	//jffmpeg decoders
import com.sun.media.parser.video.*;
import com.sun.media.codec.video.cinepak.*;
import com.sun.media.*; //BasicSourceModule for getting a demuxer and SimpleGraphBuilder to get a codec
import mjpeg.*;
import ui.*;
import DrawImage.*;
import javax.media.Time;	//For rewinding the video..
public class FrameByFrame implements ControllerListener
{
	/*Private vars*/
	private boolean goOn =false;
	private MpngWriter writer;
	private URL sourceVideo;
	private String targetVideo;
	private JavaVideoAnalysis mainProgram;
	private Demultiplexer deMultiplexer;
	private Track[] tracks;
	private int videoTrack;
	private Codec decoder;
	private Buffer buffer;
	private Buffer oBuf;
	private Dimension videoSize;
	
	/*Public vars*/
	public float frameRate;
	public Time duration;
	public int frameCount;
	public int[] frameData;
	//public FrameByFrame(URL sourceVideo,File tempOutFile, JavaVideoAnalysis mainProgram){
	public void close(){
		decoder.close();
		if (writer != null){
			try{
				writer.finalize_mjpeg();
			}catch (Exception err){
				System.out.println("Couldn't finalize");
				System.exit(0);
			}
			writer = null;
			deMultiplexer = null;
			tracks = null;
			buffer = null;
			oBuf = null;
		}
	}
	
	public FrameByFrame(URL sourceVideo,String targetVideo, JavaVideoAnalysis mainProgram){
		this.sourceVideo = sourceVideo;
		this.targetVideo = targetVideo;
		String amTVstring =  targetVideo.substring(0,targetVideo.length()-4);
		amTVstring+="_avimux.avi"; 
		System.out.println("Avimux target string "+amTVstring);
	
		this.mainProgram = mainProgram;
		Vector handlers;
		
		
		/*Add video decoders*/
		Format[] tempIF = new Format[2];
		System.out.println("Add formats");
		tempIF[0] = new YUVFormat();
		tempIF[1] = new RGBFormat();
		Codec[] codecsToAdd = new Codec[2];
		System.out.println("Add Codecs");
		codecsToAdd[0] = (Codec) new VideoEncoder();	//Takes in YUV
		codecsToAdd[1] = (Codec) new VideoDecoder();	//jffmpeg decoders spits out RGB

		/*Add codecs*/
		System.out.println("Use plugInManager");
		PlugInManager pim = new PlugInManager();
		for (int c = 0; c<codecsToAdd.length;++c){
			String name = codecsToAdd[c].getClass().getName();
			System.out.println(name);
			Format[] inFormats = codecsToAdd[c].getSupportedInputFormats();
			
			System.out.println("il "+inFormats.length);
			for (int i = 0;i<inFormats.length;++i){
				System.out.println("\tif "+inFormats[i].toString());
			}
						
			Format[] outFormats = codecsToAdd[c].getSupportedOutputFormats(tempIF[c]);
			
			System.out.println("ol "+outFormats.length);
			for (int i = 0;i<outFormats.length;++i){
				System.out.println("\tof "+outFormats[i].toString());
			}
			
			pim.addPlugIn(name,inFormats,outFormats,PlugInManager.CODEC);
			System.out.println("Added plug-in");
		}
		System.out.println("Plugins added");
		try{
			pim.commit();
		}catch(Exception err){System.out.println("Couldn't commit pim");}
		System.out.println("Start opening "+sourceVideo.toString());
	   //Start opening source video
		DataSource dSource = null;
		try{
			//dSource = Manager.createDataSource(new MediaLocator(sourceVideo));
			System.out.println("Adding datasource");
			dSource = Manager.createDataSource(sourceVideo);
			System.out.println("Adding datasource");
			
		}catch(Exception err){System.out.println("DataSource failed"); System.exit(0);}
		BasicSourceModule bsm = null;
		try{
			bsm = BasicSourceModule.createModule(dSource);
		}catch(Exception err){System.out.println("SetSource failed"); System.exit(0);}
		deMultiplexer = bsm.getDemultiplexer();
		

		
		tracks = null;
		try{
			tracks= deMultiplexer.getTracks();
		}catch(Exception err){System.out.println("GetTracks failed"); System.exit(0);}
		videoTrack = 0;
		for (int i = 0; i< tracks.length; ++i){
			if (tracks[i].getFormat() instanceof VideoFormat){
				videoTrack = i;
			}else{
				tracks[i].setEnabled(false);
			}
		}
		
		System.out.println("Encoding "+tracks[videoTrack].getFormat().getEncoding());
		System.out.println("DeMultiplexer "+deMultiplexer.toString());
		
		videoSize = null;
		VideoFormat vf = (VideoFormat) tracks[videoTrack].getFormat();
		videoSize =vf.getSize();
		/*Check clip length*/
		frameRate = vf.getFrameRate();
		duration = deMultiplexer.getDuration();
		frameCount = (int) (duration.getSeconds()*frameRate);
		System.out.println("Duration "+(duration.getSeconds())+" s Frames: "+frameCount+" frameRate "+frameRate);
		System.out.println("TrackFrames "+tracks[videoTrack].mapTimeToFrame(tracks[videoTrack].getDuration()));

		
		System.out.println("Format "+tracks[videoTrack].getFormat().toString());
		/*Create codecs for decoding, colorspace conversion and encoding*/
		decoder = SimpleGraphBuilder.findCodec(tracks[videoTrack].getFormat(), null, null, null);
		Vector dNames = PlugInManager.getPlugInList(tracks[videoTrack].getFormat(), null,PlugInManager.CODEC);
		for (int i = 0; i<dNames.size();++i){
			System.out.println("Matching plugins "+i+": "+dNames.get(i));
		}
		
		System.out.println("Decoder name "+decoder.getName());

		/*init decoder*/
		buffer = new Buffer();
		oBuf = new Buffer();
		
		decoder.setInputFormat(tracks[videoTrack].getFormat());
		Format dof = decoder.setOutputFormat(new RGBFormat(videoSize,-1,(new int[0]).getClass(),25.0f,32, 0xff0000, 0x00ff00, 0x0000ff));
		oBuf.setFormat(dof);
		try{
			decoder.open();		
		}catch (Exception err){System.out.println("Couldn't open codecs "+err.toString());}
		//Read first frame to intialize other codecs


		//for (int framesExtracted = 0; framesExtracted<10; ++framesExtracted){
		writer = null;
		
		/*Add JFRAME for image and slider...*/
		mainProgram.videoFrame = new JFrame("Video");
		mainProgram.videoFrame.addWindowListener(mainProgram);
		mainProgram.videoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JPanel contentPane = new JPanel();
		/*ADD DrawImage*/
		mainProgram.drawImage = new DrawImage();
		mainProgram.drawImage.setBackground(new Color(0, 0, 0));
		mainProgram.drawImage.setPreferredSize(new Dimension(videoSize.width, videoSize.height));
		mainProgram.drawImage.setOpaque(true);
		mainProgram.drawImage.addMouseListener(mainProgram);
		contentPane.add(mainProgram.drawImage);
		/*Add Slider*/
		mainProgram.slider = new JSlider(JSlider.HORIZONTAL,0, frameCount-1, 0);
		mainProgram.slider.addChangeListener(mainProgram);
		contentPane.add(mainProgram.slider);
		contentPane.setOpaque(true); //content panes must be opaque
		mainProgram.videoFrame.setContentPane(contentPane);
		mainProgram.videoFrame.pack();
		mainProgram.videoFrame.setLocation(100, 200);
		mainProgram.videoFrame.setVisible(true);		
		
		
		
		/*Implement slider*/
		/*		
		mainProgram.slider.setModel(new DefaultBoundedRangeModel(0,0,0,frameCount-1));
		mainProgram.drawImage.setPreferredSize(videoSize);
		mainProgram.setPreferredSize(new Dimension(videoSize.width, videoSize.height+300));
		((JFrame) SwingUtilities.getRoot(mainProgram)).pack();	//Resize the window/
		((JFrame) SwingUtilities.getRoot(mainProgram)).setSize(videoSize.width, videoSize.height+300);
		*/
		/*Saving mPNG*/
		try{
			//writer = new MpngWriter(amTVstring,videoSize); 
			//writer.writeHeader();
		}catch(Exception err){System.out.println("Couldn't open writer"); System.exit(0);}
		readFrame(0);
	}
	
	public int[] readFrame(int frameNo){
		Time currentTime = null;
		Time targetTime = tracks[videoTrack].mapFrameToTime(frameNo);

		if (buffer != null){ //check whether seek is needed
			currentTime = new Time(buffer.getTimeStamp());
			if ((targetTime.getSeconds() - currentTime.getSeconds()) == 1.0/frameRate){
				System.out.println("No seek required");
				return readFrame();
			}
		}
		
		try{
			currentTime = deMultiplexer.setPosition(targetTime,javax.media.protocol.Positionable.RoundDown);
		}catch(Exception err){System.out.println("Search failed"); System.exit(0);}
		try{
			tracks[videoTrack].readFrame(buffer);
			decoder.process(buffer,oBuf);
			currentTime = new Time(buffer.getTimeStamp());
			while ((buffer.isDiscard() || buffer.getLength()==0 ||currentTime.getSeconds() < targetTime.getSeconds() )&& !buffer.isEOM()) { // && buffer.getSequenceNumber()<frameNo){
				tracks[videoTrack].readFrame(buffer);
				decoder.process(buffer,oBuf);
				currentTime = new Time(buffer.getTimeStamp());
			}
		}catch(Exception err){System.out.println("ReadFrame failed"); System.exit(0);}
	   
	   System.out.println("Time stamp after loop"+currentTime.getSeconds());
	   if (!buffer.isEOM()){
			decoder.process(buffer,oBuf);
			frameData = (int[]) oBuf.getData();
			printImage(frameData,videoSize);
			mainProgram.status.setText("Frame Seq#: "+tracks[videoTrack].mapTimeToFrame(currentTime));
			int[] returnVal = new int[1];
			returnVal[0] = tracks[videoTrack].mapTimeToFrame(currentTime);
			return returnVal;
		}else{
			mainProgram.status.setText("End of file reached");
			return null;
		}
		
	}
	
	public int[] readFrame(){
		Time currentTime = null;
		try{
			tracks[videoTrack].readFrame(buffer);
			decoder.process(buffer,oBuf);
			currentTime = new Time(buffer.getTimeStamp());
			while ((buffer.isDiscard() || buffer.getLength()==0)&& !buffer.isEOM()) { // && buffer.getSequenceNumber()<frameNo){
				tracks[videoTrack].readFrame(buffer);
				decoder.process(buffer,oBuf);
				currentTime = new Time(buffer.getTimeStamp());				}
	   }catch(Exception err){System.out.println("ReadFrame failed"); System.exit(0);}

	   System.out.println("Time stamp "+currentTime.getSeconds());
	   if (!buffer.isEOM()){
			decoder.process(buffer,oBuf);
			frameData = (int[]) oBuf.getData();
			printImage(frameData,videoSize);
			mainProgram.status.setText("Frame Seq#: "+tracks[videoTrack].mapTimeToFrame(currentTime));
			int[] returnVal = new int[1];
			returnVal[0] = tracks[videoTrack].mapTimeToFrame(currentTime);
			return returnVal;
		}else{
			mainProgram.status.setText("End of file reached");
			return null;
		}
		
	}
	
	private void printImage(int[] data, Dimension size){
		//BufferedImage buffImg = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		BufferedImage buffImg = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_BGR);
		for (int j = 0; j<size.height;++j){
			for (int i = 0; i<size.width;++i){
				buffImg.setRGB(i,j,data[i+j*size.width]);
			}
		}
		mainProgram.drawImage.drawImage(buffImg, mainProgram.width, mainProgram.height);
		if (writer != null){
			try{
				writer.write_frame(buffImg);
			}catch (Exception err){
				System.out.println("Couldn't write frame");
				System.exit(0);
			}
		}
	}
	
	public void controllerUpdate(ControllerEvent event){
		//Configure event
		if (event instanceof ConfigureCompleteEvent){
			ConfigureCompleteEvent test = (ConfigureCompleteEvent) event;
			if (test.getCurrentState() == Processor.Configured){
				goOn = true;
				System.out.println("Configure, moving On");
			} else {System.out.println("Got notification, not configured");}
		}
		
		//Realize event
		if (event instanceof RealizeCompleteEvent){
			RealizeCompleteEvent test = (RealizeCompleteEvent) event;
			if (test.getCurrentState() == Controller.Realized){
				goOn = true;
				System.out.println("Realized, moving on");
			} else {System.out.println("Got notification, not Realized");}
		}
				//Realize event
		if (event instanceof PrefetchCompleteEvent){
			PrefetchCompleteEvent test = (PrefetchCompleteEvent) event;
			if (test.getCurrentState() == Controller.Prefetched){
				goOn = true;
				System.out.println("Prefetched, moving on");
			} else {System.out.println("Got notification, not Prefetched");}
		}	
	}
	
}
