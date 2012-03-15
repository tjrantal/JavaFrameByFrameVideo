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
import java.net.URL;
import net.sourceforge.jffmpeg.*;	//jffmpeg decoders
import com.sun.media.parser.video.*;
import com.sun.media.codec.video.cinepak.*;
import com.sun.media.*; //BasicSourceModule for getting a demuxer and SimpleGraphBuilder to get a codec
import mjpeg.*;
import ui.*;
import javax.media.Time;	//For rewinding the video..
public class FrameByFrame implements ControllerListener
{
	private boolean goOn =false;
	private MpngWriter writer;
	private URL sourceVideo;
	private String targetVideo;
	private JavaVideoAnalysis mainProgram;
	private Demultiplexer deMultiplexer;
	private Track[] tracks;
	private int videoTrack;
	private Codec decoder;
	
	/*Public vars*/
	public int framesProcessed;
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
		
		Dimension videoSize = null;
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
		Buffer buffer = new Buffer();
		Buffer oBuf = new Buffer();
		
		decoder.setInputFormat(tracks[videoTrack].getFormat());
		Format dof = decoder.setOutputFormat(new RGBFormat(videoSize,-1,(new int[0]).getClass(),25.0f,32, 0xff0000, 0x00ff00, 0x0000ff));
		try{
			decoder.open();		
		}catch (Exception err){System.out.println("Couldn't open codecs "+err.toString());}
		//Read first frame to intialize other codecs
		try{
			tracks[videoTrack].readFrame(buffer);
			while((buffer.isDiscard() || buffer.getLength()==0) && !buffer.isEOM()){
				tracks[videoTrack].readFrame(buffer);
			}
		}catch(Exception err){System.out.println("ReadFrame failed"); System.exit(0);}
	
		oBuf.setFormat(dof);
		int check = decoder.process(buffer,oBuf);
		System.out.println("Decoded");
		Format[] oFormats = null;
		framesProcessed = 0;
		//for (int framesExtracted = 0; framesExtracted<10; ++framesExtracted){
		writer = null;
		System.out.println("Set buffer formats");
		
		/*Implement slider*/
		mainProgram.slider = new JSlider(JSlider.HORIZONTAL,0, frameCount-1, 0);
		mainProgram.add(mainProgram.slider);
		mainProgram.drawImage.setPreferredSize(videoSize);
		mainProgram.setPreferredSize(new Dimension(videoSize.width, videoSize.height+300));
		((JFrame) SwingUtilities.getRoot(mainProgram)).pack();	/*Resize the window*/
		((JFrame) SwingUtilities.getRoot(mainProgram)).setSize(videoSize.width, videoSize.height+300);
		/*Test saving mPNG*/
		try{
			//writer = new MpngWriter(amTVstring,videoSize); 
			//writer.writeHeader();
		}catch(Exception err){System.out.println("Couldn't open writer"); System.exit(0);}
		
		/*Test resizing to 176x144 and then using the h264 encoder...*/
		
		System.out.println("Set codec formats");
		
		System.out.println("Codecs opened");
		frameData = (int[]) oBuf.getData();

		printImage(frameData,framesProcessed,videoSize);
		
		do{
			++framesProcessed;
		  	check = decoder.process(buffer,oBuf);
		  	//System.out.print("Decoded "+framesProcessed+" len "+oBuf.getLength()+" Ok? "+check);
			//System.out.print("Decoded "+framesProcessed+" pos "+deMultiplexer.getMediaTime().getSeconds()+" of "+deMultiplexer.getDuration().getSeconds());
			mainProgram.status.setText("Processed frame No. "+framesProcessed+" Seq#: "+buffer.getSequenceNumber());//+" pos "+deMultiplexer.getMediaTime().getSeconds());			
			//mainProgram.status.setText("Processed frame No. "+framesProcessed+" pos "+deMultiplexer.getMediaTime().getSeconds());			
			//Process raw RGB data here
			frameData = (int[]) oBuf.getData();

			printImage(frameData,framesProcessed,videoSize);
			try{
				tracks[videoTrack].readFrame(buffer);
				while((buffer.isDiscard() || buffer.getLength()==0) && !buffer.isEOM()){
					tracks[videoTrack].readFrame(buffer);
				}
		   }catch(Exception err){System.out.println("ReadFrame failed"); System.exit(0);}
		}while(!buffer.isEOM());
		
	}
	
	private void printImage(int[] data, int sNumber, Dimension size){
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
