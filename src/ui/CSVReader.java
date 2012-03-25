/*
The software is licensed under a Creative Commons Attribution 3.0 Unported License.
Copyright (C) 2012 Timo Rantalainen
*/
/*CSV file reader.
Reads data as columns into columns Vector Vector where the outermost
vector is row and innermost is column
*/
package ui;
import java.io.*;
import java.util.*;
public class CSVReader{
	public Vector<Vector<String>> data;
	public CSVReader(File fileIn,String separator){
		try {
			BufferedReader br = new BufferedReader( new FileReader(fileIn));
			String strLine = "";
			StringTokenizer st = null;
			/*Read data row by row*/
			data = new Vector<Vector<String>>();
			while( (strLine = br.readLine()) != null){
				data.add(new Vector<String>()); //Add new row vector
				st = new StringTokenizer(strLine, separator);
				while(st.hasMoreTokens()){
					data.lastElement().add(st.nextToken());	//Add column data
				}
			}
			br.close();
		} catch (Exception err){System.err.println("Error: "+err.getMessage());}
	}
}