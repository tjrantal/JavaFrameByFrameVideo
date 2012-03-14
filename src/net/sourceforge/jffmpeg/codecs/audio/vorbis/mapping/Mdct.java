/*
 * Java port of ogg demultiplexer.
 * Copyright (c) 2004 Jonathan Hueber.
 *
 * License conditions are the same as OggVorbis.  See README.
 * 1a39e335700bec46ae31a38e2156a898
 */
/********************************************************************
 *                                                                  *
 * THIS FILE IS PART OF THE OggVorbis SOFTWARE CODEC SOURCE CODE.   *
 * USE, DISTRIBUTION AND REPRODUCTION OF THIS LIBRARY SOURCE IS     *
 * GOVERNED BY A BSD-STYLE SOURCE LICENSE INCLUDED WITH THIS SOURCE *
 * IN 'COPYING'. PLEASE READ THESE TERMS BEFORE DISTRIBUTING.       *
 *                                                                  *
 * THE OggVorbis SOURCE CODE IS (C) COPYRIGHT 1994-2002             *
 * by the XIPHOPHORUS Company http://www.xiph.org/                  *
 *                                                                  *
 ********************************************************************/
/*
 * Original algorithm adapted long ago from _The use of multirate filter
 * banks for coding of high quality digital audio_, by T. Sporer,
 * K. Brandenburg and B. Edler, collection of the European Signal
 * Processing Conference (EUSIPCO), Amsterdam, June 1992, Vol.1, pp
 * 211-214
 */

package net.sourceforge.jffmpeg.codecs.audio.vorbis.mapping;

public class Mdct {
    public static final float cPI3_8 = .38268343236508977175F;
    public static final float cPI2_8 = .70710678118654752441F;
    public static final float cPI1_8 = .92387953251128675613F;

    private int n;
    private int log2n;
    private float[] trig;
    private int[] bitrev;
    private float scale;

    /**
     * Construct class for this blocksize
     */
    public Mdct( int n ) {
        this.n = n;
        bitrev = new int[n/4];
        float[] T    = new float[ n + n/4 ];
  
        int n2 = n >> 1;
        log2n = (int)Math.rint( Math.log((double)n)/Math.log(2.f));
        trig=T;

        /* trig lookups... */
        for( int i = 0; i < n/4; i++ ) {
            T[i*2]      = (float)( Math.cos((Math.PI/n)*(4*i)));
            T[i*2+1]    = (float)(-Math.sin((Math.PI/n)*(4*i)));
            T[n2+i*2]   = (float)( Math.cos((Math.PI/(2*n))*(2*i+1)));
            T[n2+i*2+1] = (float)( Math.sin((Math.PI/(2*n))*(2*i+1)));
        }
        for( int i = 0; i < n/8; i++ ) {
            T[n+i*2]   = (float)( Math.cos((Math.PI/n)*(4*i+2))*.5);
            T[n+i*2+1] = (float)(-Math.sin((Math.PI/n)*(4*i+2))*.5);
        }

        /* bitreverse lookup... */
        {
            int mask = ( 1 << (log2n-1) )-1;
            int msb  =   1 << (log2n-2);
            for( int i = 0; i < n/8; i++ ) {
                int acc=0;
                for( int j = 0; (msb >> j) != 0; j++) {
                    if ( ((msb >> j) & i) != 0 ) {
                        acc |= (1<<j);
                    }
                }
                bitrev[ i * 2     ] = ( (~acc) & mask ) - 1;
                bitrev[ i * 2 + 1 ] = acc;
            }
        }
        scale = (float)(4.f/n);
    }


    public void mdct_backward( float[] in, float[] out ){
        int n2 = n >> 1;
        int n4 = n >> 2;

        /* rotate */
        int iX = n2 - 7;
        int oX = n2 + n4;
        int T  = n4;

        do {
            oX         -= 4;
            out[oX  ]   = (-in[iX+2] * trig[T+3] - in[iX+0] * trig[T+2]);
            out[oX+1]   = ( in[iX+0] * trig[T+3] - in[iX+2] * trig[T+2]);
            out[oX+2]   = (-in[iX+6] * trig[T+1] - in[iX+4] * trig[T+0]);
            out[oX+3]   = ( in[iX+4] * trig[T+1] - in[iX+6] * trig[T+0]);
            iX         -= 8;
            T          += 4;
        } while( iX >= 0 );

        iX = n2 - 8;
        oX = n2 + n4;
        T  = n4;

        do {
            T        -= 4;
            out[oX+0] = (in[iX+4] * trig[T+3] + in[iX+6] * trig[T+2]);
            out[oX+1] = (in[iX+4] * trig[T+2] - in[iX+6] * trig[T+3]);
            out[oX+2] = (in[iX+0] * trig[T+1] + in[iX+2] * trig[T+0]);
            out[oX+3] = (in[iX+0] * trig[T+0] - in[iX+2] * trig[T+1]);
// System.out.println( "ox " + ((int)( out[oX+3] * 10000000 )) );      
            iX       -= 8;
            oX       += 4;
        } while( iX >= 0 );

        mdct_butterflies(out, n2, n2);  //float, offset, n2

//	for ( int k = 0; k < n/2; k++ ) {
//             System.out.print( " " + ((int)( out[k] * 10000000 )) );
//        }
//        System.out.println();

        mdct_bitreverse( out );

        /* roatate + window */
        {
            int oX1 = n2 + n4;
            int oX2 = n2 + n4;
            iX      = 0;
            T       = n2;
     
            do {
                oX1 -= 4;

                out[oX1+3] = (out[iX+0] * trig[T+1] - out[iX+1] * trig[T+0]);
                out[oX2+0] =-(out[iX+0] * trig[T+0] + out[iX+1] * trig[T+1]);

                out[oX1+2] = (out[iX+2] * trig[T+3] - out[iX+3] * trig[T+2]);
                out[oX2+1] =-(out[iX+2] * trig[T+2] + out[iX+3] * trig[T+3]);

                out[oX1+1] = (out[iX+4] * trig[T+5] - out[iX+5] * trig[T+4]);
                out[oX2+2] =-(out[iX+4] * trig[T+4] + out[iX+5] * trig[T+5]);

                out[oX1+0] = (out[iX+6] * trig[T+7] - out[iX+7] * trig[T+6]);
                out[oX2+3] =-(out[iX+6] * trig[T+6] + out[iX+7] * trig[T+7]);

                oX2 += 4;
                iX  += 8;
                T   += 8;
            } while( iX < oX1 );

            iX  = n2 + n4;
            oX1 = n4;
            oX2 = oX1;

            do {
                oX1 -= 4;
                iX  -= 4;

                out[oX1+3] = out[iX+3];
                out[oX2+0] =-out[iX+3];

                out[oX1+2] = out[iX+2];
                out[oX2+1] =-out[iX+2];

                out[oX1+1] = out[iX+1];
                out[oX2+2] =-out[iX+1];

                out[oX1+0] = out[iX+0];
                out[oX2+3] =-out[iX+0];

                oX2 += 4;
            } while( oX2 < iX );

            iX  = n2 + n4;
            oX1 = n2 + n4;
            oX2 = n2;
            do {
                oX1 -= 4;
                out[oX1+0] = out[iX+3];
                out[oX1+1] = out[iX+2];
                out[oX1+2] = out[iX+1];
                out[oX1+3] = out[iX+0];
                iX+=4;
            } while( oX1 > oX2 );
        }
    }

    private void mdct_butterflies( float x[], int offset, int points ) {
        int stages = log2n - 5;
        if( --stages > 0 ) {
            mdct_butterfly_first( x, offset, points );
        }

        for( int i = 1; --stages > 0; i++ ) {
            for(int j = 0; j < (1<<i); j++ ) {
                mdct_butterfly_generic( x, offset + (points>>i)*j,
                                        points >> i, 4 << i );
            }
        }

        for( int j = 0; j < points; j += 32 ) {
            mdct_butterfly_32( x, offset + j );
        }
    }

    private void mdct_butterfly_first( float[] data, int x, int points) {
        int x1 = x +  points     - 8;
        int x2 = x + (points>>1) - 8;
        float r0;
        float r1;

        int T = 0;
        do {    
            r0          = data[x1+6] - data[x2+6];
	    r1          = data[x1+7] - data[x2+7];
            data[x1+6] += data[x2+6];
            data[x1+7] += data[x2+7];
            data[x2+6]  = (r1 * trig[T+1] + r0 * trig[T+0]);
            data[x2+7]  = (r1 * trig[T+0] - r0 * trig[T+1]);
	       
            r0          = data[x1+4] - data[x2+4];
            r1          = data[x1+5] - data[x2+5];
            data[x1+4] += data[x2+4];
            data[x1+5] += data[x2+5];
            data[x2+4]  = (r1 * trig[T+5] + r0 * trig[T+4]);
            data[x2+5]  = (r1 * trig[T+4] - r0 * trig[T+5]);
	       
            r0          = data[x1+2] - data[x2+2];
            r1          = data[x1+3] - data[x2+3];
            data[x1+2] += data[x2+2];
            data[x1+3] += data[x2+3];
            data[x2+2]  = (r1 * trig[T+9]  +  r0 * trig[T+8]);
            data[x2+3]  = (r1 * trig[T+8]  -  r0 * trig[T+9]);
	       
            r0          = data[x1+0] - data[x2+0];
            r1          = data[x1+1] - data[x2+1];
            data[x1+0] += data[x2+0];
            data[x1+1] += data[x2+1];
            data[x2+0]  = (r1 * trig[T+13] + r0 * trig[T+12]);
            data[x2+1]  = (r1 * trig[T+12] - r0 * trig[T+13]);
	    //System.out.println( "bff " + ((int)( data[x2+1] * 10000000 )) );      
             
            x1 -= 8;
            x2 -= 8;
            T  += 16;
        } while( x2 >= x );
    }


    private void mdct_butterfly_generic( float[] data, int x, int points, 
                                         int trigint ) {
        int   x1 = x +  points     - 8;
        int   x2 = x + (points>>1) - 8;
        float r0;
        float r1;
        int   T = 0;

        do {
    
            r0          = data[x1+6] - data[x2+6];
	    r1          = data[x1+7] - data[x2+7];
            data[x1+6] += data[x2+6];
            data[x1+7] += data[x2+7];
            data[x2+6]  = (r1 * trig[T+1] + r0 * trig[T+0]);
            data[x2+7]  = (r1 * trig[T+0] - r0 * trig[T+1]);
	       
            T += trigint;
	       
            r0          = data[x1+4] - data[x2+4];
            r1          = data[x1+5] - data[x2+5];
            data[x1+4] += data[x2+4];
            data[x1+5] += data[x2+5];
            data[x2+4]  = (r1 * trig[T+1] + r0 * trig[T+0]);
            data[x2+5]  = (r1 * trig[T+0] - r0 * trig[T+1]);
	       
            T += trigint;
	       
            r0          = data[x1+2] - data[x2+2];
            r1          = data[x1+3] - data[x2+3];
            data[x1+2] += data[x2+2];
            data[x1+3] += data[x2+3];
            data[x2+2]  = (r1 * trig[T+1] + r0 * trig[T+0]);
            data[x2+3]  = (r1 * trig[T+0] - r0 * trig[T+1]);
	       
            T += trigint;
	       
            r0          = data[x1+0] - data[x2+0];
            r1          = data[x1+1] - data[x2+1];
            data[x1+0] += data[x2+0];
            data[x1+1] += data[x2+1];
            data[x2+0]  = (r1 * trig[T+1] + r0 * trig[T+0]);
            data[x2+1]  = (r1 * trig[T+0] - r0 * trig[T+1]);
	    //System.out.println( "gen " + ((int)( data[x2+1] * 10000000 )) );      

            T  += trigint;
            x1 -= 8;
            x2 -= 8;

        } while( x2 >= x );
    }

    private void mdct_butterfly_32( float[] data, int x ) {
        float r0    = data[x+30] - data[x+14];
        float r1    = data[x+31] - data[x+15];

        data[x+30] +=  data[x+14];
        data[x+31] +=  data[x+15];
        data[x+14]  =  r0;
        data[x+15]  =  r1;

        r0          = data[x+28] - data[x+12];   
	r1          = data[x+29] - data[x+13];
        data[x+28] += data[x+12];           
	data[x+29] += data[x+13];
        data[x+12]  = ( r0 * cPI1_8  -  r1 * cPI3_8 );
	data[x+13]  = ( r0 * cPI3_8  +  r1 * cPI1_8 );

        r0          = data[x+26] - data[x+10];
        r1          = data[x+27] - data[x+11];
        data[x+26] += data[x+10];
        data[x+27] += data[x+11];
        data[x+10]  = (( r0  - r1 ) * cPI2_8);
        data[x+11]  = (( r0  + r1 ) * cPI2_8);

        r0          = data[x+24] - data[x+8];
        r1          = data[x+25] - data[x+9];
        data[x+24] += data[x+8];
        data[x+25] += data[x+9];
        data[x+8]   = ( r0 * cPI3_8  -  r1 * cPI1_8 );
        data[x+9]   = ( r1 * cPI3_8  +  r0 * cPI1_8 );

        r0          = data[x+22] - data[x+6];
        r1          = data[x+7]  - data[x+23];
        data[x+22] += data[x+6];
        data[x+23] += data[x+7];
        data[x+6]   = r1;
        data[x+7]   = r0;

        r0          = data[x+4] - data[x+20];
        r1          = data[x+5] - data[x+21];
        data[x+20] += data[x+4];
        data[x+21] += data[x+5];
        data[x+4]   = ( r1 * cPI1_8  +  r0 * cPI3_8 );
        data[x+5]   = ( r1 * cPI3_8  -  r0 * cPI1_8 );

        r0          = data[x+2] - data[x+18];
        r1          = data[x+3] - data[x+19];
        data[x+18] += data[x+2];
        data[x+19] += data[x+3];
        data[x+2]   = (( r1  + r0 ) * cPI2_8);
        data[x+3]   = (( r1  - r0 ) * cPI2_8);

        r0          = data[x+0] - data[x+16];
        r1          = data[x+1] - data[x+17];
        data[x+16] += data[x+0];
        data[x+17] += data[x+1];
        data[x+0]   = ( r1 * cPI3_8  +  r0 * cPI1_8 );
        data[x+1]   = ( r1 * cPI1_8  -  r0 * cPI3_8 );
	//System.out.println( "b32 " + ((int)( data[x+1] * 10000000 )) );      

        mdct_butterfly_16(data, x);
        mdct_butterfly_16(data, x+16);
    }

    private void mdct_butterfly_16( float[] data, int x ) {
        float r0 = data[x+1] - data[x+9];
        float r1 = data[x+0] - data[x+8];

        data[x+8] += data[x+0];
        data[x+9] += data[x+1];
        data[x+0]  = ((r0 + r1) * cPI2_8);
        data[x+1]  = ((r0 - r1) * cPI2_8);

        r0          = data[x+3]  - data[x+11];
        r1          = data[x+10] - data[x+2];
        data[x+10] += data[x+2];
        data[x+11] += data[x+3];
        data[x+2]   = r0;
        data[x+3]   = r1;

        r0          = data[x+12] - data[x+4];
        r1          = data[x+13] - data[x+5];
        data[x+12] += data[x+4];
        data[x+13] += data[x+5];
        data[x+4]   = ((r0   - r1) * cPI2_8);
        data[x+5]   = ((r0   + r1) * cPI2_8);

        r0          = data[x+14] - data[x+6];
        r1          = data[x+15] - data[x+7];
        data[x+14] += data[x+6];
        data[x+15] += data[x+7];
        data[x+6]   = r0;
        data[x+7]   = r1;

        mdct_butterfly_8(data, x);
        mdct_butterfly_8(data, x+8);
    }

    private void mdct_butterfly_8( float[] data, int x ) {
        float r0 = data[x+6] + data[x+2];
        float r1 = data[x+6] - data[x+2];
        float r2 = data[x+4] + data[x+0];
        float r3 = data[x+4] - data[x+0];

        data[x+6] = r0 + r2;
        data[x+4] = r0 - r2;
          
        r0        = data[x+5] - data[x+1];
        r2        = data[x+7] - data[x+3];
        data[x+0] = r1 + r0;
        data[x+2] = r1 - r0;
     
        r0        = data[x+5] + data[x+1];
        r1        = data[x+7] + data[x+3];
        data[x+3] = r2 + r3;
        data[x+1] = r2 - r3;
        data[x+7] = r1 + r0;
        data[x+5] = r1 - r0;
	//System.out.println( "b8 " + ((int)( data[x+5] * 10000000 )) );      
    }

    private void mdct_bitreverse( float[] data ) {
        int bit = 0;
        int w0 = 0; 
        int w1 = n >> 1;
        int x  = w1;
        int T  = n;
	//System.out.println( "n " + n );
        do {
            int x0 = x + bitrev[bit+0];
            int x1 = x + bitrev[bit+1];

            float r0 = data[x0+1] - data[x1+1];
            float r1 = data[x0+0] + data[x1+0];
            float r2 = (r1 * trig[T+0] + r0 * trig[T+1]);
            float r3 = (r1 * trig[T+1] - r0 * trig[T+0]);

            w1    -= 4;

            r0     = (data[x0+1] + data[x1+1])/2;
            r1     = (data[x0+0] - data[x1+0])/2;
	    //System.out.println( "mid " + ((int)( r0 * 10000000 )) + " " + bitrev[bit+1]);      

            data[w0+0] = r0 + r2;
            data[w1+2] = r0 - r2;
            data[w0+1] = r1 + r3;
            data[w1+3] = r3 - r1;

            x0 = x + bitrev[bit+2];
            x1 = x + bitrev[bit+3];

            r0 = data[x0+1] - data[x1+1];
            r1 = data[x0+0] + data[x1+0];
            r2 = (r1 * trig[T+2] + r0 * trig[T+3]);
            r3 = (r1 * trig[T+3] - r0 * trig[T+2]);

	    //System.out.println( "mid2a " + ((int)( data[x0+1] * 10000000 )) + " " + bitrev[bit+2]);      
            r0 = (data[x0+1] + data[x1+1])/2;
            r1 = (data[x0+0] - data[x1+0])/2;
      
            data[w0+2] = r0 + r2;
            data[w1+0] = r0 - r2;
            data[w0+3] = r1 + r3;
            data[w1+1] = r3 - r1;
	    //System.out.println( "mid2 " + ((int)( data[w1+1] * 10000000 )));      

            T   += 4;
            bit += 4;
            w0  += 4;
        } while( w0 < w1 );
    }
}
