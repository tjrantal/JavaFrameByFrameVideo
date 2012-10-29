/*Bicubic interpolation copied from imageJ imageProcessor.*/

/*Some filtering functions reproduced here to enable using the code without ImageJ
	2D arrays, first pointer x (i.e. width), second pointer y (i.e. height): data[x][y]

*/

package	utils;

import java.text.DecimalFormat;	/*For debugging*/

public class Filters{

	public static double[][] getGradientImage(double[][] imagePixels){
		int rows = imagePixels.length;
		int columns = imagePixels[0].length;
		double[][] gradientrows = new double[rows][columns];
		double[][] gradientcolumns = new double[rows][columns];
		double[][] gradientr = new double[rows][columns];
		//Using sobel
		//for gx convolutes the following matrix
		//   
		//     |-1 0 1|
		//Gx = |-2 0 2|
		//     |-1 0 1|
		for(int i=1;i<rows-1;++i){
			for(int j=1;j<columns-1;++j){
				gradientrows[i][j] =
				-1*(imagePixels[i-1][j-1]) +1*(imagePixels[i+1][j-1])
				-2*(imagePixels[i-1][j]) +2*(imagePixels[i+1][j])
				-1*(imagePixels[i-1][j+1]) +1*(imagePixels[i+1][j+1]);
			}
		}

		//for gy convolutes the following matrix
		//
		//     |-1 -2 -1| 
		//Gy = | 0  0  0|
		//     |+1 +2 +1|
		//
		for(int i=1;i<rows-1;++i){
			for(int j=1;j<columns-1;++j){
				gradientcolumns[i][j] = 
				-1*(imagePixels[i-1][j-1]) +1*(imagePixels[i-1][j+1])
				-2*(imagePixels[i][j-1]) +2*(imagePixels[i][j+1])
				-1*(imagePixels[i+1][j-1]) +1*(imagePixels[i+1][j+1]);
			}
		}
		for(int i=1;i<rows-1;i++){
			for(int j=1;j<columns-1;j++){
				gradientr[i][j] = Math.sqrt(gradientrows[i][j]*gradientrows[i][j]+gradientcolumns[i][j]*gradientcolumns[i][j]);				
			}
		}
		return gradientr;
    }
	
	public static double[][] getVarianceImage(double[][] data, int radius){
		int width = data.length;
		int height = data[0].length;
		double[][] varianceImage = new double[width][height];
		double[] coordinates = new double[2];
		for (int i = 0+radius;i<width-radius;++i){
			for (int j = 0+radius;j<height-radius;++j){
				coordinates[0] = i;
				coordinates[1] = j;
				//System.out.println("source x "+coordinates[0]+" y "+coordinates[1]);
				varianceImage[i][j] = getLocalVariance(data,coordinates,radius);
			}
		}
		return varianceImage;
	}
	
	/*Local variance with circular sampling. Eight samples per integer increment of radius*/
	public static double getLocalVariance(double[][] data,double[] coordinates,int radius){
		/*Init sampling coordinates*/
		double[][] samplingCoordinates = new double[8*radius+1][2];
		samplingCoordinates[8*radius] = coordinates;
		final double sqrt05 = Math.sqrt(0.5);
		final double[][] directions = {{1,0},{sqrt05,sqrt05},{0,1},{-sqrt05,sqrt05},{-1,0},{-sqrt05,-sqrt05},{0,-1},{sqrt05,-sqrt05}};
		for (int r=0;r<radius;++r){
			for (int t = 0;t <8; ++t){
				samplingCoordinates[t+(r*8)][0] = coordinates[0]+directions[t][0]*((double)(r+1));
				samplingCoordinates[t+(r*8)][1] = coordinates[1]+directions[t][1]*((double)(r+1));
				
			}
		}
		/*Get the values*/
		double[] values = new double[8*radius+1];
		//DecimalFormat f = new DecimalFormat("0.#");
		for (int i = 0; i<samplingCoordinates.length;++i){
			values[i] = getBicubicInterpolatedPixel(samplingCoordinates[i][0],samplingCoordinates[i][1],data);
			//System.out.println("\tsampling x\t"+f.format(samplingCoordinates[i][0])+"\ty\t"+f.format(samplingCoordinates[i][1])+"\tval\t"+f.format(values[i]));
		}
		return getVariance(values);
	}
	
	public static double getMean(double[] data){
		double sum = 0;
		for (int i = 0; i<data.length; ++i){
			sum+= data[i];
		}
		sum/=((double) data.length);
		return sum;
	}
	
	public static double getVariance(double[] data){
		double variance = 0;
		double mean = getMean(data);
		for (int i = 0; i<data.length; ++i){
			variance+= Math.pow(data[i]-mean,2.0);
		}
		variance/=((double) data.length);
		return variance;
	}
	
	/** This method is from Chapter 16 of "Digital Image Processing:
		An Algorithmic Introduction Using Java" by Burger and Burge
		(http://www.imagingbook.com/). */
	public static double getBicubicInterpolatedPixel(double x0, double y0, double[][] data) {
		int u0 = (int) Math.floor(x0);	//use floor to handle negative coordinates too
		int v0 = (int) Math.floor(y0);
		int width = data.length;
		int height = data[0].length;
		if (u0<1 || u0>width-3 || v0< 1 || v0>height-3){
			if ((u0 == 0 || u0 < width-1) && (v0 == 0 || v0 < height-1)){ /*Use bilinear interpolation http://en.wikipedia.org/wiki/Bilinear_interpolation*/
				double x = (x0-(double)u0);
				double y = (y0-(double)v0);
				return data[u0][v0]*(1-x)*(1-y) 	/*f(0,0)(1-x)(1-y)*/
						+data[u0+1][v0]*(1-y)*x	/*f(1,0)x(1-y)*/
						+data[u0][v0+1]*(1-x)*y	/*f(0,1)(1-x)y*/
						+data[u0+1][v0+1]*x*y;	/*f(1,1)xy*/
			}
			return 0; /*Return zero for points outside the interpolable area*/
		}
		double q = 0;
		for (int j = 0; j < 4; ++j) {
			int v = v0 - 1 + j;
			double p = 0;
			for (int i = 0; i < 4; ++i) {
				int u = u0 - 1 + i;
				p = p + data[u][v] * cubic(x0 - u);
			}
			q = q + p * cubic(y0 - v);
		}
		return q;
	}
	
	/*Min, max and mean*/
	public static int min(int a, int b){return a < b ? a:b;}
	public static int max(int a, int b){return a > b ? a:b;}
	public static double min(double a, double b){return a < b ? a:b;}
	public static double max(double a, double b){return a > b ? a:b;}
	/*1D mean*/
	public static double mean(int[] a){
		double returnVal = 0;
		for (int i = 0;i<a.length;++i){
			returnVal+=(double) a[i];
		}
		return returnVal/=(double) a.length;	
	}
	public static double mean(double[] a){
		double returnVal = 0;
		for (int i = 0;i<a.length;++i){
			returnVal+= a[i];
		}
		return returnVal/=(double) a.length;	
	}
	/*2D mean*/
	public static double mean(int[][] a){
		double returnVal = 0;
		for (int i = 0;i<a.length;++i){
			for (int j = 0;j<a[i].length;++j){
				returnVal+= (double) a[i][j];
			}
		}
		return returnVal/=((double) (a.length*a[0].length));	
	}
	public static double mean(double[][] a){
		double returnVal = 0;
		for (int i = 0;i<a.length;++i){
			for (int j = 0;j<a[i].length;++j){
				returnVal+= a[i][j];
			}
		}
		return returnVal/=((double) (a.length*a[0].length));	
	}
	/*3D mean*/
	public static double mean(int[][][] a){
		double returnVal = 0;
		for (int i = 0;i<a.length;++i){
			for (int j = 0;j<a[i].length;++j){
				for (int k = 0;k<a[i][j].length;++k){
					returnVal+= (double) a[i][j][k];
				}
			}
		}
		return returnVal/=((double) (a.length*a[0].length));	
	}
	public static double mean(double[][][] a){
		double returnVal = 0;
		for (int i = 0;i<a.length;++i){
			for (int j = 0;j<a[i].length;++j){
				for (int k = 0;k<a[i][j].length;++k){
					returnVal+= a[i][j][k];
				}
			}
		}
		return returnVal/=((double) (a.length*a[0].length));	
	}
	
	
	/*Cross-correlation analysis, equations taken from http://paulbourke.net/miscellaneous/correlate/*/
	/*Calculate 1D cross-correlation for two arrays with same length. No wrapping, i.e. correlation length is limited*/
	public static double[] xcorr(double[] series1,double[] series2, int maxDelay){
		double[] xcor = new double[maxDelay*2+1];
		double ms1 =0;
		double ms2 =0;
		int length = min(series1.length,series2.length);
		/*calculate means*/
		ms1 = mean(series1);
		ms2 = mean(series2);
		double mx;
		double my;
		double summxmy;
		double summxSq;
		double summySq;
		double summxmySq;
		for (int i =-maxDelay;i<=maxDelay;i++){//ignore beginning and end of the signal...
			summxmy=0;
			summxSq=0;
			summySq=0;
			for (int j = maxDelay; j< length-maxDelay; j++){
				mx = series1[j]-ms1;
				my = series2[j+i]-ms2;
				summxmy+=mx*my;
				summxSq+=mx*mx;
				summySq+=my*my;
			}
			xcor[i+maxDelay]=summxmy/Math.sqrt(summxSq*summySq);
		}
		return xcor;
	}
	
	
	
	/*
		Calculate 2D cross-correlation for two 2D arrays. matrix2 needs to be smaller in both dimensions. Not calculated for non-overlapping positions.	
	*/
	public static double[][] xcorr(double[][] matrix1,double[][] matrix2){
		double[][] xcor = new double[matrix1.length-matrix2.length+1][matrix1[0].length-matrix2[0].length+1];
		double ms1 =0;
		double ms2 =0;
		int width = matrix1.length;
		int height = matrix1[0].length;
		/*calculate means*/
		ms1 = mean(matrix1);
		ms2 = mean(matrix2);
		double mx;
		double my;
		double summxmy;
		double summxSq;
		double summySq;
		double summxmySq;
		for (int i =0;i<=width-matrix2.length;++i){
			for (int j =0;j<=height-matrix2[0].length;++j){//ignore beginning and end of the signal...
				summxmy=0;
				summxSq=0;
				summySq=0;
				for (int i2 = 0; i2< matrix2.length; ++i2){
					for (int j2 = 0; j2< matrix2[i2].length; ++j2){
						mx = matrix1[i+i2][j+j2]-ms1;
						my = matrix2[i2][j2]-ms2;
						summxmy+=mx*my;
						summxSq+=mx*mx;
						summySq+=my*my;
					}
				}
				xcor[i][j]=summxmy/((Math.sqrt(summxSq))*(Math.sqrt(summySq)));
			}
		}
		return xcor;
	}
	
	/*Get 2D Stack max and maxIndice*/
	public static Max getMax(double[][] dataSlice){
		double max = Double.NEGATIVE_INFINITY;
		int[] indices = new int[2];
		for (int i = 0; i<dataSlice.length; ++i){
			for (int j = 0; j<dataSlice[i].length; ++j){
					if (dataSlice[i][j] > max){
						max = dataSlice[i][j];
						indices[0] = i;
						indices[1] = j;
					}
			}
		}
		Max returnValue = new Max(max,indices);
		return returnValue;
	}
	
	/*
		Calculate 3D cross-correlation for two 3D arrays. matrix2 needs to be smaller in all dimensions. Not calculated for non-overlapping positions.	
	*/
	public static double[][][] xcorr(double[][][] matrix1,double[][][] matrix2){
		double[][][] xcor = new double[matrix1.length-matrix2.length+1][matrix1[0].length-matrix2[0].length+1][matrix1[0][0].length-matrix2[0][0].length+1];
		double ms1 =0;
		double ms2 =0;
		int width = matrix1.length;
		int height = matrix1[0].length;
		int depth = matrix1[0][0].length;
		/*calculate means*/
		//System.out.println("Calc means");
		ms1 = mean(matrix1);
		ms2 = mean(matrix2);
		double mx;
		double my;
		double summxmy;
		double summxSq;
		double summySq;
		double summxmySq;
		for (int i =0;i<=width-matrix2.length;++i){
			for (int j =0;j<=height-matrix2[0].length;++j){//ignore beginning and end of the signal...
				for (int k =0;k<=depth-matrix2[0][0].length;++k){//ignore beginning and end of the signal...
					summxmy=0;
					summxSq=0;
					summySq=0;
					for (int i2 = 0; i2< matrix2.length; ++i2){
						for (int j2 = 0; j2< matrix2[i2].length; ++j2){
							for (int k2 = 0; k2< matrix2[i2][j2].length; ++k2){
								mx = matrix1[i+i2][j+j2][k+k2]-ms1;
								my = matrix2[i2][j2][k2]-ms2;
								summxmy+=mx*my;
								summxSq+=mx*mx;
								summySq+=my*my;
							}
						}
					}
					xcor[i][j][k]=summxmy/((Math.sqrt(summxSq))*(Math.sqrt(summySq)));
				}
			}
		}
		return xcor;
	}
	
	
	
	
	public static final double cubic(double x) {
		final double a = 0.5; // Catmull-Rom interpolation
		if (x < 0.0) x = -x;
		double z = 0.0;
		if (x < 1.0) 
			z = x*x*(x*(-a+2.0) + (a-3.0)) + 1.0;
		else if (x < 2.0) 
			z = -a*x*x*x + 5.0*a*x*x - 8.0*a*x + 4.0*a;
		return z;
	}
	
	public static void main(String[] ar){
		/*
		double[][] data = {{0,1,2,3},
							{2,3,4,5},
							{2,3,4,5},
							{3,4,5,6}};
							*/
		
		/*	//2D xcorr
		double[][] data = {{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,2,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1}};
							
		double[][] mask = {{0,0,0},
							{0,1,0},
							{0,0,0}};
		printMatrix(data);
		
		double[][] xcorrResults = xcorr(data,mask);
		*/
		//3D xcorr test
				double[][][] data = {{{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1}},
							{{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1}},
							{{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,2,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1}},
							{{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1}},
							{{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1},
							{1,1,1,1,1,1,1}}};
							
							double[][][] mask = {
							{{0,0,0},
							{0,0,0},
							{0,0,0}},
							{{0,0,0},
							{0,1,0},
							{0,0,0}},
							{{0,0,0},
							{0,0,0},
							{0,0,0}}};
		
		
		System.out.println("data");
		printMatrix(data);
		System.out.println("mask");
		printMatrix(mask);
		System.out.println("XCORR");
		double[][][] xcorrResults=xcorr(data,mask);
		printMatrix(xcorrResults);
		/*
		double[][] interpolated = new double[14][14];
		for (int i = 0; i< 14;++i){
			for (int j = 0; j< 14;++j){
				interpolated[i][j] = getBicubicInterpolatedPixel(((double) i)*0.5,((double) j)*0.5,data);
			}
		}
		System.out.println("InterpolatedImage");
		printMatrix(interpolated);
		*/
		/*
		double[][] variance = getVarianceImage(data,1);
		System.out.println("VarianceImage");
		printMatrix(variance);
		*/
	}
	
	public static void printMatrix(double[][] matrix){
		DecimalFormat f = new DecimalFormat("0.##");
		for (int x = 0; x< matrix.length;++x){
			for (int y = 0; y<matrix[x].length;++y){
				System.out.print(f.format(matrix[x][y])+"\t");
			}
			System.out.println();
		}
	}

	public static void printMatrix(double[][][] matrix){
		DecimalFormat f = new DecimalFormat("0.##");
		for (int d = 0; d< matrix[0][0].length;++d){
			System.out.println("D = "+d);
			for (int x = 0; x< matrix.length;++x){
				for (int y = 0; y<matrix[x].length;++y){
					System.out.print(f.format(matrix[x][y][d])+"\t");
				}
				System.out.println();
			}
		}
	}	
}