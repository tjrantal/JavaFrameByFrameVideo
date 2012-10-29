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

/*Program for analysing activity indices from vertical GRFs collected
from underneath animal housing. Only useful for University of Jyvaskyla, who
actually have such measurement apparatus. The WDQ file reading may be of use
to DataQ or old Codas users.
*/

package ui;
import javax.swing.*;		//GUI komennot swing
import java.awt.event.*; 	//Eventit ja Actionlistener
import java.io.*;				//File IO
import java.lang.Math;
import java.awt.*;
import java.awt.geom.Line2D;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.font.*;
import java.text.*;
import analysis.*;	//FrameByFrame
import ui.*;			//AnalysisThread
import DrawImage.*;			//AnalysisThread
import dlt.*;		//Direct linear transformation
import Jama.*;		//Jama Matrix
import java.net.URL;

/*implements AL antaa mahdollisuuden kayttaa eventtteja koneelta. Kayttis toteuttaa...*/
/*extends = inherit, voi peria vain yhden*/
public class JavaVideoAnalysis extends JPanel implements ActionListener, ChangeListener, MouseListener,WindowListener {	
	public JButton videoToOpen;
	public JButton fileToOpen;
	public JButton calibrationFile;
	public JButton openFile;
	public JButton closeFile;
	public JButton trackMarker;
	public JTextField lowPass;
	public JLabel status;
	public JPanel sliderPane;
	
	
	public JSlider slider;
	public File selectedFile;
	public File videoFile;
	public String savePath;
	public String initPath;
	
	public JFrame videoFrame;	//Frame for video
	//public JFrame calibrationFrame;
	public SaveableFrame pointFrame;	//Frame for digitized points
	public JTextArea pointTextArea;
	
	public SaveableFrame calibrationFrame;	//Frame for digitized points
	public JTextArea calibrationTextArea;
	
	public Vector<String[]> calibrations;
	public int pointsDigitized;
	public int[][] digitizedCalibration;
	public AnalysisThread analysisThread;
	public int[] currentVideoFrame = null;
	public DrawImage drawImage;
	public DLT2D dlt2d = null;
	public int width;
	public int height;
	public int[] lastCoordinates = null;
	public DigitizedPoints digitizedPoints = null;
	private Thread anaThread;
	
	public boolean autotrack;
	
	public JavaVideoAnalysis(){
		videoFile = null;
		savePath = null;
		pointsDigitized = 0;
		digitizedCalibration = null;
		autotrack = false;
		/*Preset path*/
		String videoSourceString =new String("");
		String videoSavePath = new String("");
		File vali = new File("user.dir");
		boolean current = true;	//true = using current, false = preset path
		if (!current){
			vali = new File("/home/rande/programming/javaVideo/winTesti");
		}
		/*CURRENT PATH*/
		initPath = new String();
		if (current){
			initPath = System.getProperty("user.dir");
		}else{
			initPath = vali.getAbsolutePath();
		}
		/*Add buttons and textfield...*/
		JPanel buttons = new JPanel();
		buttons.setLayout(new GridLayout(5,2,5,5));	/*Set button layout...*/
		videoToOpen= new JButton("Video file to Open");
		videoToOpen.setMnemonic(KeyEvent.VK_C);
		videoToOpen.setActionCommand("videoFile");
		videoToOpen.addActionListener(this);
		videoToOpen.setToolTipText("Press to select file.");
		buttons.add(new JLabel(new String("Video file to use")));
		buttons.add(videoToOpen);
/*
		openFile = new JButton("JavaVideoAnalysis");
		openFile.setMnemonic(KeyEvent.VK_I);
		openFile.setActionCommand("openFile");
		openFile.addActionListener(this);
		openFile.setToolTipText("Press to Open file.");
		buttons.add(new JLabel(new String("Click to Open File")));
		buttons.add(openFile);
	*/	
		calibrationFile= new JButton("CalibrationFile");
		calibrationFile.setMnemonic(KeyEvent.VK_R);
		calibrationFile.setActionCommand("calibrate");
		calibrationFile.addActionListener(this);
		calibrationFile.setToolTipText("Press to select calibration object file.");
		buttons.add(new JLabel(new String("Select calibration object")));
		buttons.add(calibrationFile);
		
		trackMarker= new JButton("AutoTrack");
		trackMarker.setMnemonic(KeyEvent.VK_T);
		trackMarker.setActionCommand("autoTrack");
		trackMarker.addActionListener(this);
		trackMarker.setToolTipText("Press to autoTrack marker.");
		trackMarker.setEnabled(false);	//Disable for now
		buttons.add(new JLabel(new String("Autotrack Marker")));
		buttons.add(trackMarker);
		
		closeFile = new JButton("CloseVideo");
		closeFile.setMnemonic(KeyEvent.VK_S);
		closeFile.setActionCommand("closeFile");
		closeFile.addActionListener(this);
		closeFile.setToolTipText("Press to Close file.");
		buttons.add(new JLabel(new String("Click to Close File")));
		buttons.add(closeFile);
		
		status = new JLabel(new String("Ready to Rumble"));
		buttons.add(status);
		add(buttons);
		
	}

	public static void initAndShowGU(){
		JFrame frame = new JFrame("JavaVideoAnalysis");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JComponent newContentPane = new JavaVideoAnalysis();
		newContentPane.setOpaque(true); //content panes must be opaque
		frame.setContentPane(newContentPane);
		frame.pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = 300;
		int h = 200;
		frame.setLocation(20, 20);
		//f.setLocation(screenSize.width/2 - w/2, screenSize.height/2 - h/2);
		//f.setSize(w, h);
		frame.setVisible(true);		
	}
	
	/*ChangeListener*/
	public void stateChanged(ChangeEvent e) {
		if (analysisThread != null){
			if (analysisThread.frameByFrame != null){
				JSlider source = (JSlider)e.getSource();
				if (!source.getValueIsAdjusting()) {
					int targetFrame = (int)source.getValue();
					System.out.println("Target Frame "+targetFrame);
					currentVideoFrame = analysisThread.frameByFrame.readFrame(targetFrame);
				}
				
			}
		}
	}
	
	/*WindowListener*/
	public void 	windowActivated(WindowEvent e){
		//System.out.println("Activated");
	}
	public void 	windowClosed(WindowEvent e){
		//System.out.println("Closed");
	}
	/*If window is closed, close associated classes*/
	public void 	windowClosing(WindowEvent e){
		//System.out.println("Closing");
		if (analysisThread != null){
			if (analysisThread.frameByFrame != null){
				closeFrameByFame();
			}
		}		
	}
	public void 	windowDeactivated(WindowEvent e){}
	public void 	windowDeiconified(WindowEvent e){}
	public void 	windowIconified(WindowEvent e){}
    public void 	windowOpened(WindowEvent e){}
	
	/*MouseListener*/
	public void mouseClicked(MouseEvent me) {
	}
	public void mousePressed(MouseEvent me) {
	}
	public void mouseExited(MouseEvent me) {
	}
	public void mouseEntered(MouseEvent me) {
	}
	public void mouseReleased(MouseEvent me) {
		if (analysisThread != null){
			if (analysisThread.frameByFrame != null){
				if (me.getButton() == MouseEvent.BUTTON1){
					if (lastCoordinates == null){		
						lastCoordinates = new int[2];
					}
					lastCoordinates[0] = me.getX();
					lastCoordinates[1] = me.getY();
					
					trackMarker.setEnabled(true);
					if (digitizedCalibration != null){ //Next clicks after initializing calibration will be calibration
						System.out.println("adding calibrated point");
						for (int i = 0;i<2;++i){
							digitizedCalibration[pointsDigitized][i] = lastCoordinates[i];
							calibrationFrame.data.get(pointsDigitized+1).add(Integer.toString(lastCoordinates[i]));
						}
						++pointsDigitized;
						calibrationFrame.writeCalibrationFile();
						if (pointsDigitized <calibrationFrame.data.size()-1){
							status.setText(new String("Digitize "+calibrationFrame.data.get(pointsDigitized+1).get(0)));
						}else{
							calculateCoefficients();
						}
						/*Draw x and y -coordinate?*/
					}else{
						if (dlt2d == null){
							System.out.println("screen(X,Y) = " + lastCoordinates[0] + "," + lastCoordinates[1]);
						} else {
							double[] digitizedPoint = {(double) lastCoordinates[0], (double) lastCoordinates[1]};
							
							Matrix coordinates = dlt2d.scaleCoordinates(digitizedPoint);
							double[][] scaledPoint = coordinates.getArray();
							System.out.println("screen(X,Y) = " + lastCoordinates[0] + "," + lastCoordinates[1] +" calibratred = "+coordinates.get(0,0) +","+coordinates.get(1,0));
							//Add the
							digitizedPoints.addPoint(digitizedPoint,scaledPoint, currentVideoFrame[0]);
							pointFrame.writeDigitizedPoints(digitizedPoints);							
							//Go to next frame
							currentVideoFrame = analysisThread.frameByFrame.readFrame();
						}
					}
				}
			}
		}
	}

	public void calculateCoefficients(){
		/*Get the DLT coefficients and store calibration in dlt2d*/
		dlt2d = null;	//Get rid of existing calibration
		double[][] global = new  double[calibrationFrame.data.size()-1][2];  //Global coordinates of the calibration object
		double[][] digitizedPoints = new double[calibrationFrame.data.size()-1][2];
		/*Copy the calibration object*/
		for (int r = 0; r< calibrationFrame.data.size()-1;++r){
			for (int c = 0; c<2;++c){
				System.out.println(calibrationFrame.data.get(r+1).get(c+1));
				global[r][c] = Double.parseDouble(calibrationFrame.data.get(r+1).get(c+1));
			}
		}
		/*Copy the calibration points*/
		for (int r = 0; r< digitizedPoints.length;++r){
			for (int c = 0; c< digitizedPoints[r].length;++c){
				digitizedPoints[r][c] = (double) digitizedCalibration[r][c];
			}
		}

		digitizedCalibration = null;
		dlt2d = new DLT2D(global,digitizedPoints);
		/*Print coefficients...*/
		Matrix coeffs = dlt2d.getCurrentDltCoefficients();
		String resultString = "Coefficients\n";
		for (int i = 0; i< coeffs.getRowDimension();++i){
			resultString += "\tCoeff "+i+": "+Double.toString(coeffs.get(i,0))+"\n";
		}
		System.out.println(resultString);		
		status.setText(new String("2D DLT coefficients"));
		/*Create text pane for coordinates*/
		pointFrame = new SaveableFrame(this);
		pointFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pointFrame.pack();
		pointFrame.setLocation(10, 600);
		pointFrame.setVisible(true);	
		
		
	}
	
	/*ActionListener*/
	public void actionPerformed(ActionEvent e) {
		if ("videoFile".equals(e.getActionCommand())){
			JFileChooser chooser = new JFileChooser(initPath);
			//chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				videoFile = chooser.getSelectedFile();
				System.out.println("Video file "+videoFile.getName());
				initPath = videoFile.getAbsolutePath();
				status.setText(new String("videoFileChosen"));
			}
			
			videoToOpen.setEnabled(false);
			//openFile.setEnabled(false);
			if (videoFile == null){
				videoFile =new File("H:/UserData/winMigrationBU/Deakin/JGREYADJUST/CMJ.avi");
			}
			if (savePath == null){
				savePath = new String("H:/UserData/winMigrationBU/Deakin/JGREYADJUST/figs/");
			}
			System.out.println("Open file "+videoFile.getName());
			System.out.println("Save path "+savePath);
			try{
				currentVideoFrame = new int[1];
				currentVideoFrame[0] = 0;
				analysisThread = new AnalysisThread(this);
				anaThread = new Thread(analysisThread,"analysisThread");
				anaThread.start();	//All of the analysis needs to be done within this thread from hereafter
				//anaThread.join();
			}catch (Exception err){System.out.println("Failed analysis thread"+err);}
			
		}	
		
		if ("calibrate".equals(e.getActionCommand())){
			calibrationFrame = new SaveableFrame(this);
			calibrationFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			calibrationFrame.pack();
			calibrationFrame.setLocation(600, 600);
			calibrationFrame.setVisible(true);	
			calibrationFrame.callLoad();
		}

		/*Try to autotrack marker*/
		if ("autoTrack".equals(e.getActionCommand())){
			autotrack = true;
			trackMarker.setEnabled(false);
			/*Attempt auto tracking...*/
			
		}
		
		if ("openFile".equals(e.getActionCommand())) {
			videoToOpen.setEnabled(false);
			//openFile.setEnabled(false);
			if (videoFile == null){
				videoFile =new File("H:/UserData/winMigrationBU/Deakin/JGREYADJUST/CMJ.avi");
			}
			if (savePath == null){
				savePath = new String("H:/UserData/winMigrationBU/Deakin/JGREYADJUST/figs/");
			}
			System.out.println("Open file "+videoFile.getName());
			System.out.println("Save path "+savePath);
			try{
				currentVideoFrame = new int[1];
				currentVideoFrame[0] = 0;
				analysisThread = new AnalysisThread(this);
				anaThread = new Thread(analysisThread,"analysisThread");
				anaThread.start();	//All of the analysis needs to be done within this thread from hereafter
				//anaThread.join();
			}catch (Exception err){System.out.println("Failed analysis thread"+err);}
		
			//System.gc();	//Try to enforce carbage collection
		}
		//closeFile
		if ("closeFile".equals(e.getActionCommand())) {
			WindowEvent wev = new WindowEvent(this.videoFrame, WindowEvent.WINDOW_CLOSING);
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
			
			/*Close the digitized points window as well*/
			wev = new WindowEvent(this.pointFrame, WindowEvent.WINDOW_CLOSING);
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
			
			/*Close the calibration window also*/
			wev = new WindowEvent(this.calibrationFrame, WindowEvent.WINDOW_CLOSING);
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
		}
	}
	
	private void closeFrameByFame(){
			videoToOpen.setEnabled(true);
			//openFile.setEnabled(true);
			try{
				analysisThread.frameByFrame.close();
				analysisThread = null;
				anaThread.join();
				anaThread = null;
				currentVideoFrame = null;
				digitizedPoints = null;
				dlt2d = null;
				System.gc();	//Try to enforce carbage collection
			}catch (Exception err){System.out.println("Failed analysis thread"+err);}
	}
	
	public static void main(String[] args){
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run(){
				initAndShowGU();
			}
		}
		);
	}
}


