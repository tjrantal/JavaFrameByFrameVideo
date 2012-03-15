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
import java.net.URL;

/*implements AL antaa mahdollisuuden kayttaa eventtteja koneelta. Kayttis toteuttaa...*/
/*extends = inherit, voi peria vain yhden*/
public class JavaVideoAnalysis extends JPanel implements ActionListener {	
	public JButton videoToOpen;
	public JButton fileToOpen;
	public JButton fileToSave;
	public JButton openFile;
	public JTextField lowPass;
	public JLabel status;
	public JPanel sliderPane;
	public JSlider slider;
	public File selectedFile;
	public File videoFile;
	public String savePath;
	public String initPath;
	public Vector<String[]> calibrations;
	public int videoFileNo;
	public DrawImage drawImage;
	public int width;
	public int height;
	public JavaVideoAnalysis(){
		videoFile = null;
		savePath = null;
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
		buttons.setLayout(new GridLayout(4,2,5,5));	/*Set button layout...*/
		videoToOpen= new JButton("Video file to Open");
		videoToOpen.setMnemonic(KeyEvent.VK_C);
		videoToOpen.setActionCommand("videoFile");
		videoToOpen.addActionListener(this);
		videoToOpen.setToolTipText("Press to select file.");
		buttons.add(new JLabel(new String("Video file to use")));
		buttons.add(videoToOpen);
		
		fileToSave= new JButton("Result save Path");
		fileToSave.setMnemonic(KeyEvent.VK_R);
		fileToSave.setActionCommand("fileToSave");
		fileToSave.addActionListener(this);
		fileToSave.setToolTipText("Press to select savePath.");
		buttons.add(new JLabel(new String("Select Save Path")));
		buttons.add(fileToSave);

		
		openFile = new JButton("JavaVideoAnalysis");
		openFile.setMnemonic(KeyEvent.VK_I);
		openFile.setActionCommand("openFile");
		openFile.addActionListener(this);
		openFile.setToolTipText("Press to Open file.");
		buttons.add(new JLabel(new String("Click to Open File")));
		buttons.add(openFile);
		
		status = new JLabel(new String("Ready to Rumble"));
		buttons.add(status);
		add(buttons);
		
		
		/*ADD DrawImage*/
		width = 100;
		height = 100;
		 drawImage = new DrawImage();
		drawImage.setBackground(new Color(0, 0, 0));
		drawImage.setPreferredSize(new Dimension(width,height));
		drawImage.setOpaque(true);
		add(drawImage);
			
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
			
		}	
		
		if ("fileToSave".equals(e.getActionCommand())){
			JFileChooser chooser = new JFileChooser(initPath);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				File savePathFile = chooser.getSelectedFile();
				savePath = savePathFile.getAbsolutePath();
				System.out.println("Save Path "+savePath);
				status.setText(new String("SavePathChosen"));
			}
		}
		if ("openFile".equals(e.getActionCommand())) {
			videoToOpen.setEnabled(false);
			openFile.setEnabled(false);
			fileToSave.setEnabled(false);
			if (videoFile == null){
				videoFile =new File("H:/UserData/winMigrationBU/Deakin/JGREYADJUST/CMJ.avi");
			}
			if (savePath == null){
				savePath = new String("H:/UserData/winMigrationBU/Deakin/JGREYADJUST/figs/");
			}
			System.out.println("Open file "+videoFile.getName());
			System.out.println("Save path "+savePath);
			try{
				AnalysisThread analysisThread = new AnalysisThread(this);
				Thread anaThread = new Thread(analysisThread,"analysisThread");
				anaThread.start();	//All of the analysis needs to be done within this thread from hereafter
				//anaThread.join();
			}catch (Exception err){System.out.println("Failed analysis thread"+err);}
		
			//System.gc();	//Try to enforce carbage collection
		}		
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


