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

/*
	Frame with save as and open menu
*/
package ui;
import java.awt.Dimension;
import java.awt.event.*; 	//Eventit ja Actionlistener
import java.io.*;
import javax.swing.*;
import java.util.*;
import analysis.*;

public class SaveableFrame extends JFrame implements ActionListener{
	private JMenu fileMenu = new JMenu("File");
	private JMenuBar menuBar = new JMenuBar();
	private JMenuItem openItem = new JMenuItem("Open");
	private JMenuItem saveItem = new JMenuItem("Save");
	private JMenuItem saveAsItem = new JMenuItem("Save As");
	public String fileName = null;
	private JTextArea textArea;
	public Vector<Vector<String>> data;
	private JavaVideoAnalysis mainProgram;
	public SaveableFrame(){
		init();
	}
	
	public SaveableFrame(JavaVideoAnalysis mainProgram){
		this.mainProgram = mainProgram;
		init();
	}
	
	private void init(){
		this.textArea = new JTextArea();
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.add(saveAsItem);
		openItem.addActionListener(this);
		saveItem.addActionListener(this);
		saveAsItem.addActionListener(this);
		menuBar.add(fileMenu);
		setJMenuBar(menuBar);
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textArea); 
		scrollPane.setPreferredSize(new Dimension(400,200));		
		scrollPane.setOpaque(true);
		setContentPane(scrollPane);
	}
	
	// Handle menu events
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == openItem){ loadFile();
		}else{
			if (e.getSource() == saveItem){
				saveFile(fileName);
			}else{
				if (e.getSource() == saveAsItem){
					saveFile(null);
				}
			}
		}
	}
	
	private void saveFile(String fileName){
		if (fileName == null) {  // get filename from user
			JFileChooser fc = null;
			if (this.fileName == null){
				fc = new JFileChooser(System.getProperty("user.dir"));
			}else{
				fc = new JFileChooser(new File(this.fileName).getParent());
			}
			if (fc.showSaveDialog(null) != JFileChooser.CANCEL_OPTION){
				fileName = fc.getSelectedFile().getAbsolutePath();
				this.fileName = fileName;
			}
		}
		if (fileName != null) {  // else user cancelled
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
				writer.write(textArea.getText());
				writer.close();
			}
			catch (Exception e) {
				System.out.println("Coudln't write the file "+e.toString());
			}
		}
	}
	
	public void callLoad(){
		loadFile();
	}
	
	private void loadFile(){
		JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		if (fc.showOpenDialog(null) != JFileChooser.CANCEL_OPTION){
			String fileName = fc.getSelectedFile().getAbsolutePath();
			CSVReader reader = new CSVReader(new File(fileName),"\t");
			data = reader.data;
			writeCalibrationFile();
			/*Check if digitized points were read*/
			System.out.println("Loaded file "+data.get(0).get(0));
			/*Digitzed points*/
			if (data.get(0).get(0).indexOf("Frame#") > -1){	
				/*Read digitized points*/
				mainProgram.digitizedPoints = new DigitizedPoints();	//Erase the existing points
				int scaledPoints = data.get(0).size()-3;
				double[] digitizedPoint = new double[scaledPoints];
				System.out.println("Start adding points into DigitizedPoints ");
				for (int i = 1;i<data.size();++i){
					for (int j = 0;j<digitizedPoint.length;++j){
						digitizedPoint[j] = Double.parseDouble(data.get(i).get(j+3));
					}
					double[] tempPoint = {Double.parseDouble(data.get(i).get(1)), Double.parseDouble(data.get(i).get(2))};
					mainProgram.digitizedPoints.addPoint(tempPoint,digitizedPoint, Integer.valueOf(data.get(i).get(0)));
					System.out.println("Added "+Integer.valueOf(data.get(i).get(0)));
				}
			}else{
				/*Calibration object*/
				/*
				If calibrationFrame.data.size()
				== 3, 2D calibration object
				== 4, 3D calibration object
				== 5, 2D calibration object and digitized points
				>= 6, 3D calibration object and digitized points
				*/
				mainProgram.status.setText(new String("Calibration chosen"));
				/*Add calibration to a JTextArea*/
				if (data.get(0).size() ==3 || data.get(0).size() ==5){	
					/*2D calibration object*/
					mainProgram.digitizedCalibration = new int[data.size()-1][2];	//First row is headers...
				} else {
					/*3D calibration object*/
					mainProgram.digitizedCalibration = new int[data.size()-1][2];	//First row is headers...
				}
				if (data.get(0).size() >=5){
					if (data.get(0).size() ==5){
						for (int i = 0; i<data.size()-1;++i){
							for (int j = 0; j<2;++j){
								mainProgram.digitizedCalibration[i][j] = (int) Double.parseDouble(data.get(i+1).get(j+3));
							}
						}
						mainProgram.calculateCoefficients();
					}
					
				} else {
					mainProgram.pointsDigitized = 0;
					data.get(0).add("DX");
					data.get(0).add("DY");
					mainProgram.status.setText(new String("Digitize "+data.get(mainProgram.pointsDigitized+1).get(0)));
				}
			}
		}
	}
	
	public void writeCalibrationFile(){
		textArea.setText("");	//Empty the text prior to adding the latest results...
		for (int r = 0;r<data.size();++r){
			String tempText = "";
			for (int c = 0;c<data.get(r).size();++c){
				tempText+=data.get(r).get(c);
				if (c <data.get(r).size()-1){tempText+="\t";}
			}
			if (r < data.size()-1){
				tempText+="\n";
			}
			textArea.append(tempText);
		}
	}
	
	public void writeDigitizedPoints(DigitizedPoints digitizedPoints){
		textArea.setText("");	//Empty the text prior to adding the latest results...
		textArea.append("Frame#\tX\tY\tScaledX\tScaledY\n");
		for (int r = 0;r<digitizedPoints.points.size();++r){
			String tempText = "";
			tempText+=digitizedPoints.points.get(r).frameNo+"\t";
			tempText+=digitizedPoints.points.get(r).x+"\t";
			tempText+=digitizedPoints.points.get(r).y+"\t";
			for (int c = 0;c<digitizedPoints.points.get(r).scaledPoints.length;++c){
				tempText+=digitizedPoints.points.get(r).scaledPoints[c];
				if (c <digitizedPoints.points.get(r).scaledPoints.length-1){tempText+="\t";}
			}
			if (r < digitizedPoints.points.size()-1){
				tempText+="\n";
			}
			textArea.append(tempText);
		}
	}
}