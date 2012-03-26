/*
Digitized points written by Timo Rantalainen.
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
import java.util.Vector;
public class DigitizedPoints{
	public Vector<Point> points = null;	
	public DigitizedPoints(){
		points = new Vector<Point>();
	}
	public void addPoint(double x, double y, int frameNo){
		/*Check whether the frame has already been digitized 
			or if frame after this one has been digitized.
			Keeps the frames in order...*/
		for (int i = 0; i< points.size();++i){
			if (points.get(i).frameNo == frameNo){
				points.set(i, new Point(x,y,frameNo));				
				return;
			}
			if (points.get(i).frameNo > frameNo){
				points.add(i, new Point(x,y,frameNo));	//Insert into			
				return;
			}
		}
		points.add(new Point(x,y,frameNo));	//If we get to here, it is the last frame digitized -> just add
	}
	
		public void addPoint(double x, double y,double[] scaledPoints, int frameNo){
		/*Check whether the frame has already been digitized 
			or if frame after this one has been digitized.
			Keeps the frames in order...*/
		for (int i = 0; i< points.size();++i){
			if (points.get(i).frameNo == frameNo){
				points.set(i, new Point(x,y,scaledPoints,frameNo));				
				return;
			}
			if (points.get(i).frameNo > frameNo){
				points.add(i, new Point(x,y,scaledPoints,frameNo));	//Insert into			
				return;
			}
		}
		points.add(new Point(x,y,scaledPoints,frameNo));	//If we get to here, it is the last frame digitized -> just add
	}
	
}