/*
 * Java port of ffmpeg mpeg1/2 decoder.
 * Copyright (c) 2003 Jonathan Hueber.
 *
 * Copyright (c) 2000,2001 Fabrice Bellard.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * See Credits file and Readme for details
 */
package net.sourceforge.jffmpeg.codecs.video.mpeg;

import net.sourceforge.jffmpeg.codecs.video.mpeg12.MpegVideo;

import java.awt.Frame;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.awt.image.ColorModel;
import java.awt.Graphics;

import javax.media.Buffer;

import java.io.PrintWriter;

/**
 * This class manages three display buffers.
 *  Last frame    I/P
 *  Current frame I/P/B 
 *  Next P frame  P
 */
public class DisplayOutput {
    private static final boolean motionDebug = false;
    private static final boolean debugFT = false;
    private static final boolean debugOutput = false;


    public static PrintWriter debug = null;

    /*
     * This is the next I or P frame
     */
    private int[] luminance;
    private int[] red;
    private int[] blue;
    
    /*
     * This is the current B frame
     */
    private int[] nextPLuminance;
    private int[] nextPRed;
    private int[] nextPBlue;

    /*
     * This is the last I or P frame
     */
    private int[] oldLuminance;
    private int[] oldRed;
    private int[] oldBlue;

    /**
     * Width and height of the Luminance and Chrominance outputs
     */    
    private int screenX;
    private int screenY;
    private int chromX;
    private int chromY;

    /**
     * Actual RGB output sizes
     */
    private int displayX;
    private int displayY;
    private int[] displayArray;

    /**
     * Construct Display (width/height in macroblocks)
     * Internally a frame of width 1 macroblock is placed around the 
     * the display region
     */
    public DisplayOutput( int mbWidth, int mbHeight ) {
        /**
         * Width and height in pixels
         */
        displayX = mbWidth * 16;
        displayY = mbHeight * 16;
        
        screenX = displayX + 32;
        screenY = displayY + 32;
        chromX  = screenX / 2;
        chromY  = screenY / 2;

        /**
         * Allocate display and boarders
         */
        mbWidth  += 2;
        mbHeight += 2;
        
        luminance = new int[ screenX * screenY ];
        red       = new int[ chromX  * chromY  ];
        blue      = new int[ chromX  * chromY ];

        nextPLuminance = new int[ screenX * screenY ];
        nextPRed       = new int[ chromX  * chromY  ];
        nextPBlue      = new int[ chromX  * chromY  ];

        oldLuminance = new int[ screenX * screenY ];
        oldRed       = new int[ chromX  * chromY  ];
        oldBlue      = new int[ chromX  * chromY  ];
        
        /* Initialise I frame to 0x80 */
        for ( int i = 0; i < blue.length; i++ ) {
            blue[i] = 0x400;
        }
        System.arraycopy( blue, 0, red, 0, blue.length );
        System.arraycopy( blue, 0, luminance, 0, blue.length );
        System.arraycopy( blue, 0, luminance, blue.length, blue.length );
        System.arraycopy( blue, 0, luminance, blue.length * 2, blue.length );
        System.arraycopy( blue, 0, luminance, blue.length * 3, blue.length );
    }

    /** Optimized DCT add and put follows */
    private static final int W1 = 22725;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W2 = 21407;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W3 = 19266;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W4 = 16383;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W5 = 12873;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W6 = 8867;       //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W7 = 4520;       //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int ROW_SHIFT = 11;
    private static final int COL_SHIFT = 20;

    private int block0, block1, block2, block3, block4, block5, block6, block7;
    private int a0, a1, a2, a3;
    private int b0, b1, b2, b3;
    private final void idctRowCondDC( int[] block, int offset ) {
        block0 = block[ offset     ];
        block1 = block[ offset + 1 ];
        block2 = block[ offset + 2 ];
        block3 = block[ offset + 3 ];
        block4 = block[ offset + 4 ];
        block5 = block[ offset + 5 ];
        block6 = block[ offset + 6 ];
        block7 = block[ offset + 7 ];

        if ( (block1|block2|block3|block4|block5|block6|block7) == 0 ) {
            block0 <<= 3;
            block[ offset     ] = block0;
            block[ offset + 1 ] = block0;
            block[ offset + 2 ] = block0;
            block[ offset + 3 ] = block0;
            block[ offset + 4 ] = block0;
            block[ offset + 5 ] = block0;
            block[ offset + 6 ] = block0;
            block[ offset + 7 ] = block0;
            return;
        }

        a0 = W4 * block0 + (1 << (ROW_SHIFT - 1));
        a1 = a0 + W6 * block2 - W4 * block4 - W2 * block6;
        a2 = a0 - W6 * block2 - W4 * block4 + W2 * block6;
        a3 = a0 - W2 * block2 + W4 * block4 - W6 * block6;
        a0 += W2 * block2 + W4 * block4 + W6 * block6;

        b0 = W1 * block1 + W3 * block3 + W5 * block5 + W7 * block7;
        b1 = W3 * block1 - W7 * block3 - W1 * block5 - W5 * block7;
        b2 = W5 * block1 - W1 * block3 + W7 * block5 + W3 * block7;
        b3 = W7 * block1 - W5 * block3 + W3 * block5 - W1 * block7;

        block[ offset + 0 ] = (a0 + b0) >> ROW_SHIFT;
        block[ offset + 7 ] = (a0 - b0) >> ROW_SHIFT;
        block[ offset + 1 ] = (a1 + b1) >> ROW_SHIFT;
        block[ offset + 6 ] = (a1 - b1) >> ROW_SHIFT;
        block[ offset + 2 ] = (a2 + b2) >> ROW_SHIFT;
        block[ offset + 5 ] = (a2 - b2) >> ROW_SHIFT;
        block[ offset + 3 ] = (a3 + b3) >> ROW_SHIFT;
        block[ offset + 4 ] = (a3 - b3) >> ROW_SHIFT;
    }


    private final void idctSparseColAdd( int[] block, int offset, int[] destination, int destinationOffset, int destinationWidth ) {
        block0 = block[ offset         ];
        block1 = block[ offset + 1 * 8 ];
        block2 = block[ offset + 2 * 8 ];
        block3 = block[ offset + 3 * 8 ];
        block4 = block[ offset + 4 * 8 ];
        block5 = block[ offset + 5 * 8 ];
        block6 = block[ offset + 6 * 8 ];
        block7 = block[ offset + 7 * 8 ];

        a0 = W4 * block0 + (1 << (COL_SHIFT - 1));
        a1 = a0 + W6 * block2 - W4 * block4 - W2 * block6;
        a2 = a0 - W6 * block2 - W4 * block4 + W2 * block6;
        a3 = a0 - W2 * block2 + W4 * block4 - W6 * block6;
        a0 += W2 * block2 + W4 * block4 + W6 * block6;

        b0 = W1 * block1 + W3 * block3 + W5 * block5 + W7 * block7;
        b1 = W3 * block1 - W7 * block3 - W1 * block5 - W5 * block7;
        b2 = W5 * block1 - W1 * block3 + W7 * block5 + W3 * block7;
        b3 = W7 * block1 - W5 * block3 + W3 * block5 - W1 * block7;

        destinationOffset += offset;
        destination[ destinationOffset ] = crop(destination[ destinationOffset ] + ((a0 + b0) >> COL_SHIFT));  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop(destination[ destinationOffset ] + ((a1 + b1) >> COL_SHIFT));  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop(destination[ destinationOffset ] + ((a2 + b2) >> COL_SHIFT));  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop(destination[ destinationOffset ] + ((a3 + b3) >> COL_SHIFT));  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop(destination[ destinationOffset ] + ((a3 - b3) >> COL_SHIFT));  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop(destination[ destinationOffset ] + ((a2 - b2) >> COL_SHIFT));  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop(destination[ destinationOffset ] + ((a1 - b1) >> COL_SHIFT));  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop(destination[ destinationOffset ] + ((a0 - b0) >> COL_SHIFT));
    }

    private final void idctAdd( int[] block, int[] destination, int destinationOffset, int destinationWidth ) {
        if ( debugFT ) {
            boolean t = false;
            for ( int i = 0; i < block.length; i++ ) {
		if ( block[i] != 0 ) t = true;
	    }
            if ( t ) {
                debug.print( "Add " );
                for ( int i = 0; i < block.length; i++ ) {
	    	    debug.print( Integer.toHexString(block[i]) + " " );
	        }
                debug.println();
            }
        }

        idctRowCondDC( block, 0  );
        idctRowCondDC( block, 8  );
        idctRowCondDC( block, 16 );
        idctRowCondDC( block, 24 );
        idctRowCondDC( block, 32 );
        idctRowCondDC( block, 40 );
        idctRowCondDC( block, 48 );
        idctRowCondDC( block, 56 );

        idctSparseColAdd( block, 0, destination, destinationOffset, destinationWidth );
        idctSparseColAdd( block, 1, destination, destinationOffset, destinationWidth );
        idctSparseColAdd( block, 2, destination, destinationOffset, destinationWidth );
        idctSparseColAdd( block, 3, destination, destinationOffset, destinationWidth );
        idctSparseColAdd( block, 4, destination, destinationOffset, destinationWidth );
        idctSparseColAdd( block, 5, destination, destinationOffset, destinationWidth );
        idctSparseColAdd( block, 6, destination, destinationOffset, destinationWidth );
        idctSparseColAdd( block, 7, destination, destinationOffset, destinationWidth );
    }
    
    private final void idctSparseColPut( int[] block, int offset, int[] destination, int destinationOffset, int destinationWidth ) {
        block0 = block[ offset         ];
        block1 = block[ offset + 1 * 8 ];
        block2 = block[ offset + 2 * 8 ];
        block3 = block[ offset + 3 * 8 ];
        block4 = block[ offset + 4 * 8 ];
        block5 = block[ offset + 5 * 8 ];
        block6 = block[ offset + 6 * 8 ];
        block7 = block[ offset + 7 * 8 ];

        a0 = W4 * block0 + (1 << (COL_SHIFT - 1));
        a1 = a0 + W6 * block2 - W4 * block4 - W2 * block6;
        a2 = a0 - W6 * block2 - W4 * block4 + W2 * block6;
        a3 = a0 - W2 * block2 + W4 * block4 - W6 * block6;
        a0 += W2 * block2 + W4 * block4 + W6 * block6;

        b0 = W1 * block1 + W3 * block3 + W5 * block5 + W7 * block7;
        b1 = W3 * block1 - W7 * block3 - W1 * block5 - W5 * block7;
        b2 = W5 * block1 - W1 * block3 + W7 * block5 + W3 * block7;
        b3 = W7 * block1 - W5 * block3 + W3 * block5 - W1 * block7;
        destinationOffset += offset;

        destination[ destinationOffset ] = crop((a0 + b0) >> COL_SHIFT);  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop((a1 + b1) >> COL_SHIFT);  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop((a2 + b2) >> COL_SHIFT);  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop((a3 + b3) >> COL_SHIFT);  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop((a3 - b3) >> COL_SHIFT);  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop((a2 - b2) >> COL_SHIFT);  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop((a1 - b1) >> COL_SHIFT);  destinationOffset += destinationWidth;
        destination[ destinationOffset ] = crop((a0 - b0) >> COL_SHIFT);
    }
    
    private final void idctPut( int[] block, int[] destination, int destinationOffset, int destinationWidth ) {
        if ( debugFT ) {
            int count = 0;
            debug.print( "dct " );
            for ( int i = 0; i < block.length; i++ ) {
                debug.print( Integer.toHexString( block[i] ) + " " );
	    }
            debug.println();
        }

        idctRowCondDC( block, 0  );
        idctRowCondDC( block, 8  );
        idctRowCondDC( block, 16 );
        idctRowCondDC( block, 24 );
        idctRowCondDC( block, 32 );
        idctRowCondDC( block, 40 );
        idctRowCondDC( block, 48 );
        idctRowCondDC( block, 56 );

        idctSparseColPut( block, 0, destination, destinationOffset, destinationWidth );
        idctSparseColPut( block, 1, destination, destinationOffset, destinationWidth );
        idctSparseColPut( block, 2, destination, destinationOffset, destinationWidth );
        idctSparseColPut( block, 3, destination, destinationOffset, destinationWidth );
        idctSparseColPut( block, 4, destination, destinationOffset, destinationWidth );
        idctSparseColPut( block, 5, destination, destinationOffset, destinationWidth );
        idctSparseColPut( block, 6, destination, destinationOffset, destinationWidth );
        idctSparseColPut( block, 7, destination, destinationOffset, destinationWidth );

        if ( debugOutput ) {
            debug.print( "Out " );
            for ( int y = 0; y < 8; y++ ) {
                for ( int x = 0; x < 8; x++ ) {
                    debug.print( Integer.toHexString(destination[ destinationOffset + x + (y * destinationWidth)]) + " " );
                }
            }
            debug.println();      
	}
    }
    
    /** 
     * Move into range 0-ff
     */
    private static final int crop( int x ) {
        if ( (x & 0xff) == x ) return x;
        if ( x < 0 ) {
            x = 0; 
        } else if ( x > 0xff ) {
            x = 0xff;
        }
        return x;
    }

    /**
     * Put luminance IDCT at (mbX, mbY)
     *
     * @param interlaced Set to true for draw to single field (mbY LSB --> field number)
     */    
    public final void putLuminanceIdct( int mbX, int mbY, int[] block, boolean interlaced ) {
        int offset = interlaced ? (mbX * 8 + 16 + ((mbY&~1) * 8 + 16 + (mbY&1)) *screenX)
                     : (mbX * 8 + 16 + (mbY * 8 + 16) *screenX);
        idctPut( block, luminance, offset, interlaced ? screenX*2 : screenX );
    }

    /**
     * Put R Chrominance IDCT at (mbX, mbY)
     */    
    public final void putRedIdct(  int mbX, int mbY, int[] block ) {
        idctPut( block, red, (mbX * 8 + 8 + (mbY * 8 + 8) *chromX), chromX );
    }

    /**
     * Put B Chrominance IDCT at (mbX, mbY)
     */    
    public final void putBlueIdct(  int mbX, int mbY, int[] block ) {
        idctPut( block, blue, (mbX * 8 + 8 + (mbY * 8 + 8) *chromX), chromX );
    }

    /**
     * Non-interlaced Luminance
     */
    public final void putLuminanceIdct( int mbX, int mbY, int[] block ) {
        idctPut( block, luminance, mbX * 8 + 16 + (mbY * 8 + 16) *screenX, screenX );
    }

    /**
     * Add luminance IDCT at (mbX, mbY)
     *
     * @param interlaced Set to true for draw to single field (mbY LSB --> field number)
     */    
    public final void addLuminanceIdct( int mbX, int mbY, int[] block, boolean interlaced ) {
        int offset = interlaced ? (mbX * 8 + 16 + ((mbY&~1) * 8 + 16 + (mbY&1)) *screenX)
                     : (mbX * 8 + 16 + (mbY * 8 + 16) *screenX);
        idctAdd( block, luminance, offset, interlaced ? screenX*2 : screenX );
    }

    /**
     * Add R Chrominance IDCT at (mbX, mbY)
     */    
    public final void addRedIdct(  int mbX, int mbY, int[] block ) {
        idctAdd( block, red, (mbX * 8 + 8 + (mbY * 8 + 8) *chromX), chromX );
    }

    /**
     * Add B Chrominance IDCT at (mbX, mbY)
     */    
    public final void addBlueIdct(  int mbX, int mbY, int[] block ) {
        idctAdd( block, blue, (mbX * 8 + 8 + (mbY * 8 + 8) *chromX), chromX );
    }

    /**
     * Non-interlaced Luminance
     */
    public final void addLuminanceIdct( int mbX, int mbY, int[] block ) {
        idctAdd( block, luminance, mbX * 8 + 16 + (mbY * 8 + 16)*screenX, screenX );
    }


    

    /** The following code performs the MPEG motion bit blitting */

    /**
     * On pixel with rounding
     */
    private final void blitBlock0Round( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        dy     += top;
        for ( y = top; y < bottom; y += destinationWidth, dy += destinationWidth ) {
            for ( x = left; x < right; x++ ) {
                destination[ x + y ] = source[ x + dx + dy ];
            }
        }
    }

    /**
     * Horizontal pair pixel merge with rounding
     */
    private final void blitBlock1Round( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        int leftPixel, rightPixel;

        dy     += top;                
        int leftPlusDx = left + dx;
        int dxPlus1    = dx + 1;
        int dxPlus1PlusDy;
        for ( y = top; y < bottom; y += destinationWidth, dy += destinationWidth ) {
            rightPixel = source[ leftPlusDx + dy ];
            dxPlus1PlusDy = dxPlus1 + dy;
            for ( x = left; x < right; x++ ) {
                leftPixel = rightPixel;
                rightPixel = source[ x + dxPlus1PlusDy ];
                destination[ x + y ] = 
                         (leftPixel|rightPixel) - ((leftPixel^rightPixel) >>1);
            }
        }
    }
    
    /**
     * Vertical pair pixel merge with rounding
     */
    private final void blitBlock2Round( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        int topPixel, bottomPixel;

        int dxPlusDyPlusTop = dx + dy + top;
        int dxPlusDestWidth = dx + destinationWidth;
        int xPlusDxPlusDestWidth;
        for ( x = left; x < right; x++ ) {
            bottomPixel = source[ x + dxPlusDyPlusTop ];
            xPlusDxPlusDestWidth = x + dxPlusDestWidth + dy;

            for ( y = top; y < bottom; y += destinationWidth ) {
                topPixel = bottomPixel;
                bottomPixel = source[ xPlusDxPlusDestWidth + y ];
                destination[ x + y ] = 
                         (topPixel|bottomPixel) - ((topPixel^bottomPixel) >>1);
            }
        }
    }

    /**
     * Horizontal and vertical pair pixel merge with rounding
     */
    private final void blitBlock3Round( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;
        int a,b;
        int dxPlusDyPlusTop = dx + dy + top;
        int xPlusDxPlusDy;
        dy += destinationWidth;

        for ( x = left; x < right; x++ ) {
            b = source[ x + dxPlusDyPlusTop ] + source[ x + dxPlusDyPlusTop + 1 ];
            xPlusDxPlusDy = x + dx + dy;
            for ( y = top; y < bottom; y += destinationWidth ) {
                a = b;
                b = source[ xPlusDxPlusDy + y ] + source[ xPlusDxPlusDy + 1 + y ];
                destination[ x + y ] =  (a + b + 2) >>2;
            }
        }
    }

    /**
     * On pixel with no rounding
     */
    private final void blitBlock0NoRound( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        dy     += top;
        for ( y = top; y < bottom; y += destinationWidth, dy += destinationWidth ) {
            for ( x = left; x < right; x++ ) {
                destination[ x + y ] = source[ x + dx + dy ];
            }
        }
    }

    /**
     * Horizontal pair pixel merge with no rounding
     */
    private final void blitBlock1NoRound( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        int leftPixel, rightPixel;

        dy     += top;                
        int leftPlusDx = left + dx;
        int dxPlus1    = dx + 1;
        int dxPlus1PlusDy;
        for ( y = top; y < bottom; y += destinationWidth, dy += destinationWidth ) {
            rightPixel = source[ leftPlusDx + dy ];
            dxPlus1PlusDy = dxPlus1 + dy;
            for ( x = left; x < right; x++ ) {
                leftPixel = rightPixel;
                rightPixel = source[ x + dxPlus1PlusDy ];
                destination[ x + y ] = 
                       (leftPixel&rightPixel) + ((leftPixel^rightPixel) >>1);
            }
        }
    }

    /**
     * Vertical pair pixel merge with no rounding
     */
    private final void blitBlock2NoRound( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        int topPixel, bottomPixel;

        int dxPlusDyPlusTop = dx + dy + top;
        int dxPlusDestWidth = dx + destinationWidth;
        int xPlusDxPlusDestWidth;
        for ( x = left; x < right; x++ ) {
            bottomPixel = source[ x + dxPlusDyPlusTop ];
            xPlusDxPlusDestWidth = x + dxPlusDestWidth + dy;

            for ( y = top; y < bottom; y += destinationWidth ) {
                topPixel = bottomPixel;
                bottomPixel = source[ xPlusDxPlusDestWidth + y ];
                destination[ x + y ] = 
                      (topPixel&bottomPixel) + ((topPixel^bottomPixel) >>1);
            }
        }
    }

    /**
     * Horizontal and vertical pair pixel merge with no rounding
     */
    private final void blitBlock3NoRound( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        int a,b;
        int dxPlusDyPlusTop = dx + dy + top;
        int xPlusDxPlusDy;
        dy += destinationWidth;

        for ( x = left; x < right; x++ ) {
            b = source[ x + dxPlusDyPlusTop ] + source[ x + dxPlusDyPlusTop + 1 ];
            xPlusDxPlusDy = x + dx + dy;
            for ( y = top; y < bottom; y += destinationWidth ) {
                a = b;
                b = source[ xPlusDxPlusDy + y ] + source[ xPlusDxPlusDy + 1 + y ];
                destination[ x + y ] = (a + b + 1) >>2;
            }
        }
    }

    /**
     * Perform motion blit Source->Destination (blit area) + (dx/dy - halfPixels)
     * Boolean rounding
     */
    private final void blitBlock( int[] source, int[] destination, int destinationWidth,
                                  int top, int bottom, int left, int right,
                                  int dx, int dy, int halfPixels, boolean rounding ) {
        int x,y;
        top    *= destinationWidth;
        bottom *= destinationWidth;
        dy     *= destinationWidth;
        
        if ( rounding ) {
            /* Rounding On */
            switch ( halfPixels ) {
                case 0: {
                    /**
                     * This is a straight copy
                     */
                    blitBlock0Round( source, destination, destinationWidth, 
                                     top, bottom, left, right, dx, dy );
                    break;
                }
                case 1: {
                    /**
                     * Half a pixel offset
                     */
                    blitBlock1Round( source, destination, destinationWidth, 
                                     top, bottom, left, right, dx, dy );
                    break;
                }                
                case 2: {
                    /**
                     * Half a pixel offset
                     */
                    blitBlock2Round( source, destination, destinationWidth, 
                                     top, bottom, left, right, dx, dy );
                    break;
                }     
                case 3: 
                default: {
                    /**
                     * Half a pixel offset
                     */
                    blitBlock3Round( source, destination, destinationWidth, 
                                     top, bottom, left, right, dx, dy );
                    break;
                }                
            }
        } else {
            switch ( halfPixels ) {
                case 0: {
                    /**
                     * This is a straight copy
                     */
                    blitBlock0NoRound( source, destination, destinationWidth, 
                                       top, bottom, left, right, dx, dy );
                    break;
                }
                case 1: {
                    /**
                     * Half a pixel offset
                     */
                    blitBlock1NoRound( source, destination, destinationWidth, 
                                       top, bottom, left, right, dx, dy );
                    break;
                }                
                case 2: {
                    /**
                     * Half a pixel offset
                     */
                    blitBlock2NoRound( source, destination, destinationWidth, 
                                       top, bottom, left, right, dx, dy );
                    break;
                }     
                case 3: 
                default: {
                    /**
                     * Half a pixel offset
                     */
                    blitBlock3NoRound( source, destination, destinationWidth, 
                                       top, bottom, left, right, dx, dy );
                    break;
                }                
            }
        }
    }

    /** The following code performs the MPEG motion bit merging (B frames only) */

    /**
     * On pixel with rounding addition (B frames only)
     */
    private final void mergeBlock0Round( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        dy     += top;
        for ( y = top; y < bottom; y += destinationWidth, dy += destinationWidth ) {
            for ( x = left; x < right; x++ ) {
                destination[ x + y ] = (destination[ x + y ] + source[ x + dx + dy ]) >> 1;
            }
        }
    }

    /**
     * Horizontal pair pixel merge with rounding addition (B frames only)
     */
    private final void mergeBlock1Round( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        int leftPixel, rightPixel;

        dy     += top;                
        int leftPlusDx = left + dx;
        int dxPlus1    = dx + 1;
        int dxPlus1PlusDy;
        for ( y = top; y < bottom; y += destinationWidth, dy += destinationWidth ) {
            rightPixel = source[ leftPlusDx + dy ];
            dxPlus1PlusDy = dxPlus1 + dy;
            for ( x = left; x < right; x++ ) {
                leftPixel = rightPixel;
                rightPixel = source[ x + dxPlus1PlusDy ];
                destination[ x + y ] = 
          (destination[ x + y ] + (leftPixel|rightPixel) - ((leftPixel^rightPixel) >>1))>>1;
            }
        }
    }
    
    /**
     * Vertical pair pixel merge with rounding addition (B frames only)
     */
    private final void mergeBlock2Round( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        int topPixel, bottomPixel;

        int dxPlusDyPlusTop = dx + dy + top;
        int dxPlusDestWidth = dx + destinationWidth;
        int xPlusDxPlusDestWidth;
        for ( x = left; x < right; x++ ) {
            bottomPixel = source[ x + dxPlusDyPlusTop ];
            xPlusDxPlusDestWidth = x + dxPlusDestWidth + dy;

            for ( y = top; y < bottom; y += destinationWidth ) {
                topPixel = bottomPixel;
                bottomPixel = source[ xPlusDxPlusDestWidth + y ];
                destination[ x + y ] = 
         (destination[ x + y ] + (topPixel|bottomPixel) - ((topPixel^bottomPixel) >>1))>>1;
            }
        }
    }

    /**
     * Horizontal and vertical pair pixel merge with rounding addition (B frames only)
     */
    private final void mergeBlock3Round( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;
        int a,b;
        int dxPlusDyPlusTop = dx + dy + top;
        int xPlusDxPlusDy;
        dy += destinationWidth;

        for ( x = left; x < right; x++ ) {
            b = source[ x + dxPlusDyPlusTop ] + source[ x + dxPlusDyPlusTop + 1 ];
            xPlusDxPlusDy = x + dx + dy;
            for ( y = top; y < bottom; y += destinationWidth ) {
                a = b;
                b = source[ xPlusDxPlusDy + y ] + source[ xPlusDxPlusDy + 1 + y ];
                destination[ x + y ] = (destination[ x + y ] + ((a + b + 2) >>2))>>1;
            }
        }
    }

    /**
     * On pixel with no rounding addition (B frames only)
     */
    private final void mergeBlock0NoRound( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        dy     += top;
        for ( y = top; y < bottom; y += destinationWidth, dy += destinationWidth ) {
            for ( x = left; x < right; x++ ) {
                destination[ x + y ] = (destination[ x + y ] + source[ x + dx + dy ]) >> 1;
            }
        }
    }

    /**
     * Horizontal pair pixel merge with no rounding addition (B frames only)
     */
    private final void mergeBlock1NoRound( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        int leftPixel, rightPixel;

        dy     += top;                
        int leftPlusDx = left + dx;
        int dxPlus1    = dx + 1;
        int dxPlus1PlusDy;
        for ( y = top; y < bottom; y += destinationWidth, dy += destinationWidth ) {
            rightPixel = source[ leftPlusDx + dy ];
            dxPlus1PlusDy = dxPlus1 + dy;
            for ( x = left; x < right; x++ ) {
                leftPixel = rightPixel;
                rightPixel = source[ x + dxPlus1PlusDy ];
                destination[ x + y ] = 
             (destination[ x + y ] + (leftPixel&rightPixel) + ((leftPixel^rightPixel) >>1)) >> 1;
            }
        }
    }

    /**
     * Vertical pair pixel merge with no rounding addition (B frames only)
     */
    private final void mergeBlock2NoRound( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        int topPixel, bottomPixel;

        int dxPlusDyPlusTop = dx + dy + top;
        int dxPlusDestWidth = dx + destinationWidth;
        int xPlusDxPlusDestWidth;
        for ( x = left; x < right; x++ ) {
            bottomPixel = source[ x + dxPlusDyPlusTop ];
            xPlusDxPlusDestWidth = x + dxPlusDestWidth + dy;

            for ( y = top; y < bottom; y += destinationWidth ) {
                topPixel = bottomPixel;
                bottomPixel = source[ xPlusDxPlusDestWidth + y ];
                destination[ x + y ] = 
         (destination[ x + y ] + (topPixel&bottomPixel) + ((topPixel^bottomPixel) >>1)) >> 1;
            }
        }
    }

    /**
     * Horizontal and vertical pair pixel merge with no rounding addition (B frames only)
     */
    private final void mergeBlock3NoRound( 
            int[] source, int[] destination, int destinationWidth,
            int top, int bottom, int left, int right, int dx, int dy ) {
        int x,y;            
        int a,b;
        int dxPlusDyPlusTop = dx + dy + top;
        int xPlusDxPlusDy;
        dy += destinationWidth;

        for ( x = left; x < right; x++ ) {
            b = source[ x + dxPlusDyPlusTop ] + source[ x + dxPlusDyPlusTop + 1 ];
            xPlusDxPlusDy = x + dx + dy;
            for ( y = top; y < bottom; y += destinationWidth ) {
                a = b;
                b = source[ xPlusDxPlusDy + y ] + source[ xPlusDxPlusDy + 1 + y ];
                destination[ x + y ] = (destination[ x + y ] + ((a + b + 1) >>2)) >> 1;
            }
        }
    }

    /**
     * Perform motion addition (B frames) Source->Destination (blit area) + (dx/dy - halfPixels)
     * Boolean rounding
     */
    private final void mergeBlock( int[] source, int[] destination, int destinationWidth,
                                   int top, int bottom, int left, int right,
                                   int dx, int dy, int halfPixels, boolean rounding ) {
        int x,y;
        top    *= destinationWidth;
        bottom *= destinationWidth;
        dy     *= destinationWidth;
        
        if ( rounding ) {
            /* Rounding On */
            switch ( halfPixels ) {
                case 0: {
                    /**
                     * This is a straight copy
                     */
                    mergeBlock0Round( source, destination, destinationWidth, 
                                       top, bottom, left, right, dx, dy );
                    break;
                }
                case 1: {
                    /**
                     * Half a pixel offset
                     */
                    mergeBlock1Round( source, destination, destinationWidth, 
                                      top, bottom, left, right, dx, dy );
                    break;
                }                
                case 2: {
                    /**
                     * Half a pixel offset
                     */
                    mergeBlock2Round( source, destination, destinationWidth, 
                                      top, bottom, left, right, dx, dy );
                    break;
                }     
                case 3: 
                default: {
                    /**
                     * Half a pixel offset
                     */
                    mergeBlock3Round( source, destination, destinationWidth, 
                                      top, bottom, left, right, dx, dy );
                    break;
                }                
            }
        } else {
            switch ( halfPixels ) {
                case 0: {
                    /**
                     * This is a straight copy
                     */
                    mergeBlock0NoRound( source, destination, destinationWidth, 
                                        top, bottom, left, right, dx, dy );
                    break;
                }
                case 1: {
                    /**
                     * Half a pixel offset
                     */
                    mergeBlock1NoRound( source, destination, destinationWidth, 
                                       top, bottom, left, right, dx, dy );
                    break;
                }                
                case 2: {
                    /**
                     * Half a pixel offset
                     */
                    mergeBlock2NoRound( source, destination, destinationWidth, 
                                        top, bottom, left, right, dx, dy );
                    break;
                }     
                case 3: 
                default: {
                    /**
                     * Half a pixel offset
                     */
                    mergeBlock3NoRound( source, destination, destinationWidth, 
                                        top, bottom, left, right, dx, dy );
                    break;
                }                
            }
        }
    }
    
    /** Clip value */
    private final int clip( int position, int offset, int maximum ) {
        if ( position + offset < 0 ) {
            position = - offset;
        }
        if ( position + offset > maximum ) {
            position = maximum - offset;
        }
        return position;
    }

    /**
     * This method is the main entry point to perform a variety of pel motion schemes
     * Source YUV maps are passed in (last I/P or next I/P), the destination is always the current frame.
     *
     * Destination offset is supplied, as is the source and destination field numbers
     * Rounding is a boolean.  Average triggers the merge used for B frames only.
     */
    public void mpeg_motion( int[] sourceLuminance, int[] sourceRed, int[] sourceBlue,
                             int mbX, int mbY,
                             int dest_offset, boolean fieldBased, int sourceFieldNumber, int destFieldNumber,
                             boolean rounding, boolean average, 
                             int motion_x, int motion_y,
                             int leftOffset, int topOffset,
                             int width, int height  ) {
if ( motionDebug && (motion_x != 0||motion_y != 0) )
{
      debug.println( "mpeg_motion pos:(" + Integer.toHexString(mbX) + "," + Integer.toHexString(mbY) +
                     ") size:(" + Integer.toHexString(width) + "," + Integer.toHexString(height) +
                     ") motion:(" + Integer.toHexString(motion_x) + "," + Integer.toHexString(motion_y) +
                     ")" );
}
    
        /* field based operations work on alternate lines */
        int lineLength = fieldBased ? screenX * 2 : screenX;
        
        /**
         * Manage clipping 
         */
        int top    = mbY * 16 + 16 + topOffset;
        int left   = mbX * 16 + 16 + leftOffset;

        /*
         * Field based lines are twice the length
         */
        if ( fieldBased ) top = (top / 2);

        /**
         * Manage half pixel motion
         */
        int halfPixels = (motion_x & 1)|((motion_y & 1)<<1);
        int dx = motion_x / 2;
        int dy = motion_y / 2;
        
        if ( motion_x < 0 && ((halfPixels & 1) == 1)) {
            dx--;
        }
        if ( motion_y < 0  && ((halfPixels & 2) == 2)) {
            dy--;
        }

        /**
         * Clipped region from the edge previous buffer
         */
        left = clip( left, dx, screenX - 16 );
        top  = clip( top,  dy, (fieldBased ? (screenY / 2 - 8) : screenY - 16 ) );
        
        int right  = left + width;
        int bottom = top + height;

        if ( right  + dx == screenX    ) halfPixels &= ~1;
        if ( bottom + dy == (fieldBased ? (screenY / 2) : screenY ) ) halfPixels &= ~2;

        /*
         * Field based lines are twice the length
         */
        if ( fieldBased && destFieldNumber == 1 ) {
            left += screenX;
            right += screenX;
        }
        if ( fieldBased && sourceFieldNumber != destFieldNumber ) {
            /* Copy Field 0 <==> Field 1 */
            dx += screenX * (sourceFieldNumber - destFieldNumber);
        }
        
        /*
        System.out.println( "From" );
        for ( int n = top; n < bottom; n++ ) {
            for ( int m = left; m < right; m++ ) {
                System.out.print( (oldLuminance[ m + n * lineLength ] + " ") );
            }
        }
        System.out.println();
*/        
        /**
         * Luminance
         */
        if ( average ) {
            mergeBlock( sourceLuminance, luminance, lineLength,
                        top, bottom, left, right,
                        dx, dy, halfPixels, rounding );
        } else {
            blitBlock( sourceLuminance, luminance, lineLength,
                       top, bottom, left, right,
                       dx, dy, halfPixels, rounding );
        }
/*
        if ( left + dx< 16 || top +dy< 16 || right + dx + (halfPixels&1) > displayX + 16
             || bottom + dy + ((halfPixels&2)>>1) > displayY + 16 ) {
                 System.out.println( "motion(" + motion_x + " " + motion_y + ")" );
                 System.out.println( "move(" + (left+dx-16) +" " + (top+dy-16) + " " + halfPixels + ")" );
                     for (int y = top; y < bottom; y++ ) {
                 for ( int x = left; x < right; x++) {
                         System.out.print( Integer.toHexString( luminance[ x ][ y ] ) + " " );
                     }
                     System.out.println();
                 }
        }
 */
        /**
         * Chrominance
         */
        
        /* field based operations work on alternate luminance lines */
        lineLength = fieldBased ? chromX * 2 : chromX;

        top    = mbY * 8 + 8 + (topOffset/2);
        left   = mbX * 8 + 8 + (leftOffset/2);

        /*
         * Field based lines are twice the length
         */
        if ( fieldBased ) top = (top / 2);
        
        /* Round up */
        if ( (motion_x & 1) != 0 ) { if (motion_x < 0) {motion_x--; } else { motion_x++; } }
        if ( (motion_y & 1) != 0 ) { if (motion_y < 0) {motion_y--; } else { motion_y++; } }

        halfPixels = (motion_y & 2)|((motion_x & 2)>>1);
        dx = motion_x / 4;
        dy = motion_y / 4;

        if ( motion_x < 0 && ((halfPixels & 1) == 1)) {
            dx--;
        }
        if ( motion_y < 0  && ((halfPixels & 2) == 2)) {
            dy--;
        }
        
        /**
         * Clipped region is a straight copy from previous buffer
         */
        left = clip( left, dx, chromX    - 8 );
        top  = clip( top,  dy, (fieldBased ? (chromY / 2 - 4) : chromY - 8) );

        right  = left + width/2;
        bottom = top + height/2;
        
        if ( right  + dx == chromX    ) halfPixels &= ~1;
        if ( bottom + dy == (fieldBased ? (chromY / 2) : chromY ) ) halfPixels &= ~2;

        /*
         * Field based lines are twice the length
         */
        if ( fieldBased && destFieldNumber == 1 ) {
            left += chromX;
            right += chromX;
        }
        if ( fieldBased && sourceFieldNumber != destFieldNumber ) {
            /* Copy Field 0 <==> Field 1 */
            dx += chromX * (sourceFieldNumber - destFieldNumber);
        }

        if ( average ) {
            mergeBlock( sourceBlue, blue, lineLength,
                        top, bottom, left, right,
                        dx, dy, halfPixels, rounding );
            mergeBlock( sourceRed, red, lineLength,
                        top, bottom, left, right,
                        dx, dy, halfPixels, rounding );
        } else {
            blitBlock( sourceBlue, blue, lineLength,
                        top, bottom, left, right,
                        dx, dy, halfPixels, rounding );
            blitBlock( sourceRed, red, lineLength,
                       top, bottom, left, right,
                       dx, dy, halfPixels, rounding );
        }
    }
    
    /**
     * This is the main move block function, controlling a large number of different 
     * MV types
     */
    public void move( int[] sourceLuminance, int[] sourceRed, int[] sourceBlue,
                      int mbX, int mbY, int[] motion, int motionOffset, 
                      boolean rounding, boolean average, int mv_type, boolean[] fieldSelect, int fieldSelectOffset ) {
        switch ( mv_type ) {
            case MpegVideo.MV_TYPE_16X16: {
//                System.out.println( "MV_TYPE_16X16" );
                mpeg_motion( sourceLuminance, sourceRed, sourceBlue, 
                             mbX, mbY,
                             0, false, 0, 0,
                             rounding, average, 
                             motion[motionOffset], motion[motionOffset + 1],
                             0, 0, 16, 16 );
                break;
            }
            case MpegVideo.MV_TYPE_FIELD: {
/*             mpeg_motion( sourceLuminance, sourceRed, sourceBlue, 
                             mbX, mbY,
                             0, false, 0, 0,
                             rounding, average, (motion[0][0]+motion[1][0])/2, (motion[0][1]+motion[1][1])/2, 16 );
/*                System.out.println( "MV_TYPE_FIELD" );  */
                mpeg_motion( sourceLuminance, sourceRed, sourceBlue, 
                             mbX, mbY,
                             0, true, fieldSelect[fieldSelectOffset]?1:0, 0,  // Field based, source field, destination field ID
                             rounding, average, motion[motionOffset], (motion[motionOffset + 1]),
                             0, 0, 16, 8 );
                mpeg_motion( sourceLuminance, sourceRed, sourceBlue, 
                             mbX, mbY,
                             0, true, fieldSelect[fieldSelectOffset + 1]?1:0, 1,
                             rounding, average, motion[motionOffset + 2], motion[motionOffset + 3],
                             0, 0, 16, 8 );  
                break;
            }
            default: {
                throw new Error ( "Unrecognised " + mv_type );
            }
        }        
    }
    
    private final static int divide2( int a ) {
        if ( a < 0 ) { return a/2 - (a & 1); } else { return a/2 + (a & 1); }
    }
    
    public final void moveFromLast( int mbX, int mbY, int[] motion, int motionOffset, boolean rounding, boolean average, int mv_type, boolean[] fieldSelect, int fieldSelectOffset ) {
        move( oldLuminance, oldRed, oldBlue, mbX, mbY, motion, motionOffset, rounding, average, mv_type, fieldSelect, fieldSelectOffset );
    }

    public final void moveFromNext( int mbX, int mbY, int[] motion, int motionOffset, boolean rounding, boolean average, int mv_type, boolean[] fieldSelect,  int fieldSelectOffset ) {
        move( nextPLuminance, nextPRed, nextPBlue, mbX, mbY, motion, motionOffset, rounding, average, mv_type, fieldSelect, fieldSelectOffset );
    }

    /**
     * Copy this macroblock from the previous frame (MPEG4)
     */
    public void move( int mbX, int mbY, int motion_x, int motion_y, boolean rounding ) {
        mpeg_motion( oldLuminance, oldRed, oldBlue, mbX, mbY,
                     0, false, 0, 0,
                     rounding, false, 
                     motion_x, motion_y,
                     0, 0, 16, 16 );
    }

    /**
     * Merge this macroblock with the next frame (MPEG4)
     */
    public void moveFromNext( int mbX, int mbY, int motion_x, int motion_y, boolean rounding, boolean merge ) {
        mpeg_motion( nextPLuminance, nextPRed, nextPBlue, mbX, mbY,
                     0, false, 0, 0,
                     rounding, merge, 
                     motion_x, motion_y,
                     0, 0, 16, 16 );
    }

    /**
     * Merge this macroblock with the next frame (MPEG4)
     */
    public void moveFromIPFrame( int mbX, int mbY, int motion_x, int motion_y, boolean rounding ) {
        mpeg_motion( nextPLuminance, nextPRed, nextPBlue, mbX, mbY,
                     0, false, 0, 0,
                     rounding, false, 
                     motion_x, motion_y,
                     0, 0, 16, 16 );
    }

    /**
     * Copy a field block from the previous frame (MPEG4)
     * //TODO Checkme
     */
    public void moveField( int mbX, int mbY, int motion_x, int motion_y, boolean rounding, boolean field ) {
        mpeg_motion( oldLuminance, oldRed, oldBlue, 
                     mbX, mbY,
                     0, true, field?1:0, 0,  // Field based, source field, destination field ID
                     rounding, false, motion_x, motion_y,
                     0, 0, 16, 8 );
    }

    /**
     * Merge a field block from the next frame (MPEG4)
     * //TODO Checkme
     */
    public void moveFieldFromNext( int mbX, int mbY, int motion_x, int motion_y, boolean rounding, boolean field, boolean merge ) {
        mpeg_motion( nextPLuminance, nextPRed, nextPBlue, 
                     mbX, mbY,
                     0, true, field?1:0, 0,  // Field based, source field, destination field ID
                     rounding, merge, motion_x, motion_y,
                     0, 0, 16, 8 );
    }

    /**
     * Merge a field block from the next frame (MPEG4)
     * //TODO Checkme
     */
    public void moveFieldFromIPFrame( int mbX, int mbY, int motion_x, int motion_y, boolean rounding, boolean field ) {
        mpeg_motion( nextPLuminance, nextPRed, nextPBlue, 
                     mbX, mbY,
                     0, true, field?1:0, 0,  // Field based, source field, destination field ID
                     rounding, false, motion_x, motion_y,
                     0, 0, 16, 8 );
    }

    /**
     * Copy an 8x8 block from the previous frame (MPEG4)
     */
    public void move8x8( int mbX, int mbY, int motion_x, int motion_y, boolean rounding, int leftOffset, int topOffset ) {
        mpeg_motion( oldLuminance, oldRed, oldBlue, mbX, mbY,
                     0, false, 0, 0,
                     rounding, false, 
                     motion_x, motion_y,
                     leftOffset, topOffset, 8, 8 );
    }

    /**
     * Copy an 8x8 block from the previous frame (MPEG4)
     */
    public void move8x8FromNext( int mbX, int mbY, int motion_x, int motion_y, boolean rounding, int leftOffset, int topOffset, boolean merge ) {
        mpeg_motion( nextPLuminance, nextPRed, nextPBlue, mbX, mbY,
                     0, false, 0, 0,
                     rounding, merge, 
                     motion_x, motion_y,
                     leftOffset, topOffset, 8, 8 );
    }

    /**
     * Copy an 8x8 block from the previous frame (MPEG4)
     */
    public void move8x8FromIPFrame( int mbX, int mbY, int motion_x, int motion_y, boolean rounding, int leftOffset, int topOffset ) {
        mpeg_motion( nextPLuminance, nextPRed, nextPBlue, mbX, mbY,
                     0, false, 0, 0,
                     rounding, false, 
                     motion_x, motion_y,
                     leftOffset, topOffset, 8, 8 );
    }


/*    public static final void clear( int[] a ) {
        for ( int y = a.length - 1; y >= 0; y-- ) a[y] = 0;
    }
  */  
    /**
     * This is an I or P frame - (current -> next -> last)
     */
    public final void endIPFrame() {
//        clear( oldLuminance ); clear( oldRed ); clear( oldBlue );
        /** Set current as next, next as previous, etc */
        int[] t;
        t = oldLuminance; oldLuminance = nextPLuminance; nextPLuminance = luminance; luminance = t;
        t = oldRed;       oldRed       = nextPRed;       nextPRed       = red;       red       = t;
        t = oldBlue;      oldBlue      = nextPBlue;      nextPBlue      = blue;      blue      = t;        
        
        /**
         * Expand next frame into Boarders
         *
        for ( int x = 0; x < 16; x++ ) {
            for ( int y = 16; y < displayY + 16; y++ ) {
                nextPLuminance[ x + y * screenX ] = nextPLuminance[ 16 + y * screenX ];
                nextPLuminance[ displayX + 32 - x - 1 + y * screenX ] = nextPLuminance[ displayX + 15 + y * screenX ];
                
                nextPBlue[ x/2 + (y/2)*chromX ] = nextPBlue[ 16 /2 + (y/2)*chromX ];
                nextPBlue[ (displayX + 32 - x)/2 - 1 + (y/2)*chromX ] = nextPBlue[ (displayX + 16)/2 - 1 + (y/2)*chromX ];
                
                nextPRed[ x/2 + (y/2) * chromX ] = nextPRed[ 16 /2 + (y/2)*chromX ];
                nextPRed[ (displayX + 32 - x)/2 - 1 + (y/2)*chromX ] = nextPRed[ (displayX + 16)/2 - 1 + (y/2)*chromX ];
            }
        }

        for ( int y = 0; y < 16; y++ ) {
            for ( int x = 0; x < displayX + 32; x++ ) {
                nextPLuminance[ x + y * screenX ] = nextPLuminance[ x + 16 * screenX ];
                nextPLuminance[ x + (displayY + 32 - y - 1) * screenX ] = nextPLuminance[ x + (displayY + 15) * screenX ];
                
                nextPBlue[ x/2 + (y/2)*chromX ] = nextPBlue[ x/2 + (16/2)*chromX ];
                nextPBlue[ x /2 + ((displayY + 32 - y )/2 - 1)*chromX ] = nextPBlue[ x/2 + ((displayY + 16)/2 - 1)*chromX];
                
                nextPRed[ x/2 + (y/2)*chromX ] = nextPRed[ x/2 + (16/2)*chromX ];
                nextPRed[ x /2 + ((displayY + 32 - y)/2 - 1)*chromX ] = nextPRed[ x/2 + ((displayY + 16)/2 - 1)*chromX ];
            }
        }
        */
    }
    
    /**
     * End B frame - this gets discarded so does nothing :)
     */
    public final void endBFrame() {
    }
    
    public final void endFrame() {
        expandIntoBoarder();
        /** Set current as previous, etc */
        int[] t;
        t = oldLuminance; oldLuminance = luminance; luminance = t;
        t = oldRed;       oldRed =       red;       red = t;
        t = oldBlue;      oldBlue =      blue;      blue = t;        
    }        
        
    public final void expandIntoBoarder() {
        /**
         * Expand into Boarders
         */
        int x,y,y1,l1,l2,xoff,r1,r2,b1,b2,yoff;

        int screenX16 = 16 * screenX;
        for ( y = displayY * screenX ; y > 0; ) {
            y -= screenX;
            y1 = y + screenX16;         /* Start at bottom-left of display */
            xoff = displayX + 16 + y1;  /* First pixel in RH border */
            l1 = luminance[ 16 + y1 ];  /* Pixel at left edge of display */
            l2 = luminance[ xoff - 1 ]; /* Pixel at right edge of display */
            for ( x = 16; x > 0; ) {
                x--;
                luminance[ x + y1 ] = l1;   /* Left border */
                luminance[ xoff + x ] = l2; /* Right border */
            }
        }

        int chromXX8 = 8 * chromX;
        int dxtemp = displayX/2 + 8;
        for ( y = displayY/2 * chromX; y > 0; ) {
            y -= chromX;
            y1 = y + chromXX8;          /* Start at bottom-left of chroma */
            xoff = dxtemp + y1;         /* First pixel in RH border */
            b1 = blue[ 8 + y1 ];        /* Pixel at left edge of display  */
            b2 = blue[ xoff - 1 ];      /* Pixel at RH edge of display */
            r1 = red[ 8 + y1 ];
            r2 = red[ xoff - 1];
            for ( x = 8; x > 0; ) {
                x--;
                blue[ x + y1 ] = b1;     /* Left boarder */
                blue[ xoff + x ] = b2;   /* Right border */
                
                red[ x + y1 ] = r1;
                red[ xoff + x ] = r2;
            }
        }

        int dtemp = (displayY + 16) * screenX;
        for ( x = screenX; x > 0; ) {         /* Full screen width */
            x--;
            yoff = x + dtemp;                 /* First pixel in bottom border */
            l1 = luminance[ x + screenX16 ];  /* Pixel at top of screen */
            l2 = luminance[ yoff - screenX ]; /* Pixel at bottom of screen */
            for ( y = screenX16; y > 0; ) {
                y -= screenX;
                luminance[ x + y ] = l1;      /* Top boarder */
                luminance[ yoff + y] = l2;    /* Bottom boarder */
            }
        }
                
        int btemp = (displayY/2 + 8)*chromX;
        for ( x = chromX; x > 0; ) {      /* Full Chroma screen Width */
            x--;
            yoff = x + btemp;               /* First pixel in bottom border */
            b1 = blue[ x + chromXX8 ];      /* Pixel at top of screen */
            b2 = blue[ yoff - chromX];      /* Pixel at bottom of screen */
            r1 = red[ x + chromXX8 ];
            r2 = red[ yoff - chromX];
            for ( y = chromXX8; y > 0;  ) {
                y -= chromX;
                blue[ x + y ] = b1;       /* Top border */
                blue[ yoff + y ] = b2;    /* Bottom border */
                
                red[ x + y ] = r1;
                red[ yoff + y ] = r2;
            }
        }
    }
    

    /**
     * Show next I/P frame (will then become old frame)
     */
    public void showNextScreen( Buffer buffer ) {
        int[] data = (int[])buffer.getData();
        
        if ( data == null || data.length < displayX * displayY ) {
            data = new int[ displayX * displayY ];
            buffer.setData( data );
        }
        buffer.setLength( data.length );
        int h = 0;
        int yCount = displayY/2;
        int xCount = displayX/2;
        int l1,l2,l3,l4,r,b,g,rshift,yoff,yoff1,chromoff,x2,x,xo,scale;
        int y = 14;
  	for (int yo = yCount; yo > 0; yo-- ) {
            y += 2;
            x2 = 8 + y/2 * chromX;
            x = 14 + screenX * y;
            for ( xo = xCount; xo > 0; xo-- ) {
                x += 2;
                r = crop( nextPRed[   x2  ] ) - 128;
                b = crop( nextPBlue[ x2++ ] ) - 128;
                g = (b * 13954 + r * 34903)>>16;
                r = (r * 117504)>>16;
                b = (b * 138452)>>16;
                
                // l = 0.3 red + 0.59 green + 0.11 blue
                l1 =  ((nextPLuminance[ x               ]-16)*76309)>>16;
                l3 =  ((nextPLuminance[ x     + screenX ]-16)*76309)>>16;
                l2 =  ((nextPLuminance[ x + 1           ]-16)*76309)>>16;
                l4 =  ((nextPLuminance[ x + 1 + screenX ]-16)*76309)>>16;
                
                data[h               ] = (crop(l1+r)<<16)|(crop(l1-g)<<8)|crop(l1+b);
                data[h + 1           ] = (crop(l2+r)<<16)|(crop(l2-g)<<8)|crop(l2+b);
                data[h     + displayX] = (crop(l3+r)<<16)|(crop(l3-g)<<8)|crop(l3+b);
                data[h + 1 + displayX] = (crop(l4+r)<<16)|(crop(l4-g)<<8)|crop(l4+b);
                h += 2;
            }
            h += displayX;
        }
    }
    

    /**
     * Show current B frame 
     */
    public void showScreen( Buffer buffer ) {
        int[] data = (int[])buffer.getData();
        
        if ( data == null || data.length < displayX * displayY ) {
            data = new int[ displayX * displayY ];
            buffer.setData( data );
        }
        buffer.setLength( data.length );
        int h = 0;
        int yCount = displayY/2;
        int xCount = displayX/2;
        int l1,l2,l3,l4,r,b,g,rshift,yoff,yoff1,chromoff,x2,x,xo,scale;
        int y = 14;
  	for (int yo = yCount; yo > 0; yo-- ) {
            y += 2;
            x2 = 8 + y/2 * chromX;
            x = 14 + screenX * y;
            for ( xo = xCount; xo > 0; xo-- ) {
                x += 2;
                r = crop( red[   x2  ] ) - 128;
                b = crop( blue[ x2++ ] ) - 128;
                g = (b * 13954 + r * 34903)>>16;
                r = (r * 117504)>>16;
                b = (b * 138452)>>16;
                
                // l = 0.3 red + 0.59 green + 0.11 blue 
                l1 =  ((luminance[ x               ]-16)*76309)>>16;
                l3 =  ((luminance[ x     + screenX ]-16)*76309)>>16;
                l2 =  ((luminance[ x + 1           ]-16)*76309)>>16;
                l4 =  ((luminance[ x + 1 + screenX ]-16)*76309)>>16;
                
                data[h               ] = (crop(l1+r)<<16)|(crop(l1-g)<<8)|crop(l1+b);
                data[h + 1           ] = (crop(l2+r)<<16)|(crop(l2-g)<<8)|crop(l2+b);
                data[h     + displayX] = (crop(l3+r)<<16)|(crop(l3-g)<<8)|crop(l3+b);
                data[h + 1 + displayX] = (crop(l4+r)<<16)|(crop(l4-g)<<8)|crop(l4+b);
                h += 2;
            }
            h += displayX;
        }
    }
    
    public void dumpMB( int x, int y ) {
        System.out.println( "x " + (x++) + ", y "+ (y++) );
        for ( int n = y * 16; n < y * 16 + 16; n++ ) {
            for ( int m = x * 16; m < x * 16 + 16; m++ ) {
                System.out.print( luminance[ m + n * screenX ] + " " );
            }
        System.out.println();            
        }
        System.out.println();
        for ( int n = y * 8; n < y * 8 + 8; n+=2 ) {
            for ( int m = x * 8; m < x * 8 + 8; m++ ) {
                System.out.print( blue[ m + n * chromX ] + " " );
            }
        System.out.println();            
        }
        System.out.println();
        for ( int n = y * 8; n < y * 8 + 8; n+=2 ) {
            for ( int m = x * 8; m < x * 8 + 8; m++ ) {
                System.out.print( red[ m + n * chromX ] + " " );
            }
        System.out.println();            
        }
        System.out.println();            
    }
}
