/*
 * Java port of parts of the ffmpeg Mpeg4 base decoder.
 * Copyright (c) 2003 Jonathan Hueber.
 *
 * Copyright (c) 2001 Fabrice Bellard.
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
package net.sourceforge.jffmpeg.codecs.video.mpeg4.div3;


import java.awt.Frame;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.awt.image.ColorModel;
import java.awt.Graphics;
import javax.media.Buffer;

/**
 * This class manages a single display buffer
 */
public class DisplayOutput {
    private int[] luminance;
    private int[] red;
    private int[] blue;
    
    private int[] oldLuminance;
    private int[] oldRed;
    private int[] oldBlue;
    
    private int screenX;
    private int screenY;
    private int chromX;
    private int chromY;

    private static final int W1 = 22725;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W2 = 21407;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W3 = 19266;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
//    private static final int W4 = 16384;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W4 = 16383;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W5 = 12873;      //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W6 = 8867;       //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int W7 = 4520;       //cos(i*M_PI/16)*sqrt(2)*(1<<14) + 0.5
    private static final int ROW_SHIFT = 11;
    private static final int COL_SHIFT = 20; // 6

    private static final void idctRowCondDC( int[] block, int offset ) {
        int block0 = block[ offset     ];
        int block1 = block[ offset + 1 ];
        int block2 = block[ offset + 2 ];
        int block3 = block[ offset + 3 ];
        int block4 = block[ offset + 4 ];
        int block5 = block[ offset + 5 ];
        int block6 = block[ offset + 6 ];
        int block7 = block[ offset + 7 ];

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

        int a0 = W4 * block0 + (1 << (ROW_SHIFT - 1));
        int a1 = a0 + W6 * block2 - W4 * block4 - W2 * block6;
        int a2 = a0 - W6 * block2 - W4 * block4 + W2 * block6;
        int a3 = a0 - W2 * block2 + W4 * block4 - W6 * block6;
        a0 += W2 * block2 + W4 * block4 + W6 * block6;

        int b0 = W1 * block1 + W3 * block3 + W5 * block5 + W7 * block7;
        int b1 = W3 * block1 - W7 * block3 - W1 * block5 - W5 * block7;
        int b2 = W5 * block1 - W1 * block3 + W7 * block5 + W3 * block7;
        int b3 = W7 * block1 - W5 * block3 + W3 * block5 - W1 * block7;

        block[ offset + 0 ] = (a0 + b0) >> ROW_SHIFT;
        block[ offset + 7 ] = (a0 - b0) >> ROW_SHIFT;
        block[ offset + 1 ] = (a1 + b1) >> ROW_SHIFT;
        block[ offset + 6 ] = (a1 - b1) >> ROW_SHIFT;
        block[ offset + 2 ] = (a2 + b2) >> ROW_SHIFT;
        block[ offset + 5 ] = (a2 - b2) >> ROW_SHIFT;
        block[ offset + 3 ] = (a3 + b3) >> ROW_SHIFT;
        block[ offset + 4 ] = (a3 - b3) >> ROW_SHIFT;
    }


    private static void idctSparseColAdd( int[] block, int offset, int[] destination, int destinationOffset, int destinationWidth ) {
        int block0 = block[ offset         ];
        int block1 = block[ offset + 1 * 8 ];
        int block2 = block[ offset + 2 * 8 ];
        int block3 = block[ offset + 3 * 8 ];
        int block4 = block[ offset + 4 * 8 ];
        int block5 = block[ offset + 5 * 8 ];
        int block6 = block[ offset + 6 * 8 ];
        int block7 = block[ offset + 7 * 8 ];

        int a0 = W4 * block0 + (1 << (COL_SHIFT - 1));
        int a1 = a0 + W6 * block2 - W4 * block4 - W2 * block6;
        int a2 = a0 - W6 * block2 - W4 * block4 + W2 * block6;
        int a3 = a0 - W2 * block2 + W4 * block4 - W6 * block6;
        a0 += W2 * block2 + W4 * block4 + W6 * block6;

        int b0 = W1 * block1 + W3 * block3 + W5 * block5 + W7 * block7;
        int b1 = W3 * block1 - W7 * block3 - W1 * block5 - W5 * block7;
        int b2 = W5 * block1 - W1 * block3 + W7 * block5 + W3 * block7;
        int b3 = W7 * block1 - W5 * block3 + W3 * block5 - W1 * block7;

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

    public final void idctAdd( int[] block, int[] destination, int destinationOffset, int destinationWidth ) {
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
    
    private static void idctSparseColPut( int[] block, int offset, int[] destination, int destinationOffset, int destinationWidth ) {
        int block0 = block[ offset         ];
        int block1 = block[ offset + 1 * 8 ];
        int block2 = block[ offset + 2 * 8 ];
        int block3 = block[ offset + 3 * 8 ];
        int block4 = block[ offset + 4 * 8 ];
        int block5 = block[ offset + 5 * 8 ];
        int block6 = block[ offset + 6 * 8 ];
        int block7 = block[ offset + 7 * 8 ];

        int a0 = W4 * block0 + (1 << (COL_SHIFT - 1));
        int a1 = a0 + W6 * block2 - W4 * block4 - W2 * block6;
        int a2 = a0 - W6 * block2 - W4 * block4 + W2 * block6;
        int a3 = a0 - W2 * block2 + W4 * block4 - W6 * block6;
        a0 += W2 * block2 + W4 * block4 + W6 * block6;

        int b0 = W1 * block1 + W3 * block3 + W5 * block5 + W7 * block7;
        int b1 = W3 * block1 - W7 * block3 - W1 * block5 - W5 * block7;
        int b2 = W5 * block1 - W1 * block3 + W7 * block5 + W3 * block7;
        int b3 = W7 * block1 - W5 * block3 + W3 * block5 - W1 * block7;
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
    
    public final void idctPut( int[] block, int[] destination, int destinationOffset, int destinationWidth ) {
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
    }
    /**
     * Contructor Width and Height in MB pels
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
        
        luminance = new int[ mbWidth * 16 * mbHeight * 16 ];
        red       = new int[ mbWidth * 8  * mbHeight * 8  ];
        blue      = new int[ mbWidth * 8  * mbHeight * 8  ];

        oldLuminance = new int[ mbWidth * 16 * mbHeight * 16 ];
        oldRed       = new int[ mbWidth * 8  * mbHeight * 8  ];
        oldBlue      = new int[ mbWidth * 8  * mbHeight * 8  ];
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
    
    public final void putLuminanceIdct( int mbX, int mbY, int[] block ) {
        idctPut( block, luminance, mbX * 8 + 16 + (mbY * 8 + 16) *screenX, screenX );
    }

    public final void putRedIdct(  int mbX, int mbY, int[] block ) {
        idctPut( block, red, mbX * 8 + 8 + (mbY * 8 + 8)*chromX, chromX);
    }

    public final void putBlueIdct(  int mbX, int mbY, int[] block ) {
        idctPut( block, blue, mbX * 8 + 8 + (mbY * 8 + 8)*chromX, chromX);
    }

    public final void addLuminanceIdct( int mbX, int mbY, int[] block ) {
        idctAdd( block, luminance, mbX * 8 + 16 + (mbY * 8 + 16)*screenX, screenX );
    }

    public final void addRedIdct(  int mbX, int mbY, int[] block ) {
        idctAdd( block, red, mbX * 8 + 8 + (mbY * 8 + 8)*chromX, chromX);
    }

    public final void addBlueIdct(  int mbX, int mbY, int[] block ) {
        idctAdd( block, blue, mbX * 8 + 8 + (mbY * 8 + 8) * chromX, chromX );
    }

    /**
     * Blit a block including half pixel motion
     */
    private final void blitBlock( int[] source, int[] destination, int destinationWidth,
                                  int top, int bottom, int left, int right,
                                  int dx, int dy, int halfPixels, boolean rounding ) {
        switch ( halfPixels ) {
            case 0: {
                /**
                 * This is a straight copy
                 */
                for ( int y = top; y < bottom; y++ ) {
                    for ( int x = left; x < right; x++ ) {
                        destination[ x + y * destinationWidth ] = source[ x + dx + (y + dy)* destinationWidth ];
                    }
                }
                break;
            }
            case 1: {
                /**
                 * Half a pixel offset
                 */
                for ( int y = top; y < bottom; y++ ) {
                    int rightPixel = source[ left + dx + (y + dy) * destinationWidth ];
                    for ( int x = left; x < right; x++ ) {
                        int leftPixel = rightPixel;
                        rightPixel = source[ x + dx + 1 + (y + dy) * destinationWidth ];
                        destination[ x + y * destinationWidth ] = rounding ?
                                 (leftPixel|rightPixel) - ((leftPixel^rightPixel) >>1)
                               : (leftPixel&rightPixel) + ((leftPixel^rightPixel) >>1);
                    }
                }
                break;
            }                
            case 2: {
                /**
                 * Half a pixel offset
                 */
                for ( int x = left; x < right; x++ ) {
                    int bottomPixel = source[ x + dx + (top + dy) * destinationWidth ];
                    for ( int y = top; y < bottom; y++ ) {
                        int topPixel = bottomPixel;
                        bottomPixel = source[ x + dx + (y + dy + 1) * destinationWidth ];
                        destination[ x + y * destinationWidth ] = rounding ?
                                 (topPixel|bottomPixel) - ((topPixel^bottomPixel) >>1)
                               : (topPixel&bottomPixel) + ((topPixel^bottomPixel) >>1);
                    }
                }
                break;
            }     
            case 3: 
            default: {
                /**
                 * Half a pixel offset
                 */
                for ( int x = left; x < right; x++ ) {
                    int b = source[ x + dx + (top + dy) * destinationWidth ] + source[ x + dx + 1 + (top + dy) * destinationWidth ];
                    for ( int y = top; y < bottom; y++ ) {
                        int a = b;
                        b = source[ x + dx + (y + dy + 1) * destinationWidth ] + source[ x + dx + 1 + (y + dy + 1) * destinationWidth ];
                        destination[ x + y * destinationWidth ] = rounding ?
                                 (a + b + 2) >>2
                               : (a + b + 1) >>2;
                    }
                }
                break;
            }                
        }
    }

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
     * Copy this macroblock from the previous frame
     */
    public void move( int mbX, int mbY, int motion_x, int motion_y, boolean rounding ) {
        /**
         * Manage clipping 
         */
        int top    = mbY * 16 + 16;
        int left   = mbX * 16 + 16;

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
        left = clip( left, dx, screenX    - 16 );
        top  = clip( top,  dy, screenY - 16 );
        
        int right  = left + 16;
        int bottom = top + 16;

        if ( right  + dx == screenX    ) halfPixels &= ~1;
        if ( bottom + dy == screenY ) halfPixels &= ~2;

        /**
         * Luminance
         */
        blitBlock( oldLuminance, luminance, screenX,
                   top, bottom, left, right,
                   dx, dy, halfPixels, rounding );
        /**
         * Chrominance
         */
        top    = mbY * 8 + 8;
        left   = mbX * 8 + 8;

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
        top  = clip( top,  dy, chromY - 8 );

        right  = left + 8;
        bottom = top + 8;
        
        if ( right  + dx == chromX    ) halfPixels &= ~1;
        if ( bottom + dy == chromY ) halfPixels &= ~2;

        blitBlock( oldBlue, blue, chromX,
                   top, bottom, left, right,
                   dx, dy, halfPixels, rounding );
        blitBlock( oldRed, red, chromX,
                   top, bottom, left, right,
                   dx, dy, halfPixels, rounding );
    }

    /**
     * Swap this and last frames.  Expand the picture into the boarders
     */
    public final void endFrame() {
        /** Set current as previous, etc */
        int[] t;
        t = oldLuminance; oldLuminance = luminance; luminance = t;
        t = oldRed;       oldRed =       red;       red = t;
        t = oldBlue;      oldBlue =      blue;      blue = t;        
        
        /**
         * Expand into Boarders
         */
        for ( int x = 0; x < 16; x++ ) {
            for ( int y = 16; y < displayY + 16; y++ ) {
                oldLuminance[ x + y * screenX ] = oldLuminance[ 16 + y * screenX ];
                oldLuminance[ displayX + 32 - x - 1 + y * screenX ] = oldLuminance[ displayX + 15 + y * screenX ];
                
                oldBlue[ x/2 + (y/2)*chromX ] = oldBlue[ 16 /2 + (y/2)*chromX ];
                oldBlue[ (displayX + 32 - x)/2 - 1 + (y/2)*chromX ] = oldBlue[ (displayX + 16)/2 - 1 + (y/2)*chromX ];
                
                oldRed[ x/2 + (y/2) * chromX ] = oldRed[ 16 /2 + (y/2)*chromX ];
                oldRed[ (displayX + 32 - x)/2 - 1 + (y/2)*chromX ] = oldRed[ (displayX + 16)/2 - 1 + (y/2)*chromX ];
            }
        }

        for ( int y = 0; y < 16; y++ ) {
            for ( int x = 0; x < displayX + 32; x++ ) {
                oldLuminance[ x + y * screenX ] = oldLuminance[ x + 16 * screenX ];
                oldLuminance[ x + (displayY + 32 - y - 1) * screenX ] = oldLuminance[ x + (displayY + 15) * screenX ];
                
                oldBlue[ x/2 + (y/2)*chromX ] = oldBlue[ x/2 + (16/2)*chromX ];
                oldBlue[ x /2 + ((displayY + 32 - y )/2 - 1)*chromX ] = oldBlue[ x/2 + ((displayY + 16)/2 - 1)*chromX];
                
                oldRed[ x/2 + (y/2)*chromX ] = oldRed[ x/2 + (16/2)*chromX ];
                oldRed[ x /2 + ((displayY + 32 - y)/2 - 1)*chromX ] = oldRed[ x/2 + ((displayY + 16)/2 - 1)*chromX ];
            }
        }
    }
    

    /**
     * Convert luminance and chrominance into an RGB buffer
     */
    public void showScreen( Buffer buffer ) {
        int[] data = (int[])buffer.getData();
        
        if ( data == null || data.length < displayX * displayY ) {
            data = new int[ displayX * displayY ];
            buffer.setData( data );
        }
        buffer.setLength( data.length );

        int h = 0;
        int yExtent = displayY + 16;
        int xExtent = displayX + 16;
        int l1,l2,l3,l4,r,b,g,rshift,yoff,yoff1,chromoff,x2,x,scale;
  	for (int y = 16; y < yExtent; y += 2 ) {
            yoff = screenX * y;
            yoff1 = screenX * (y + 1);
            chromoff = y/2 * chromX;
            x2 = 8;
            for ( x = 16; x < xExtent; x += 2 ) {
                r = crop( red[   x2    + chromoff ] ) - 128;
                b = crop( blue[ (x2++) + chromoff ] ) - 128;
                g = (int)(b * ((float)13954/(1<<16)) + r * ((float)34903/(1<<16)));

                r = (int)(r * ((float)117504/(1<<16)));
                b = (int)(b * ((float)138452/(1<<16)));
                
                /* l = 0.3 red + 0.59 green + 0.11 blue */
                l1 =  (int)((luminance[ x     + yoff  ]-16)*((float)76309/(1<<16)));
                l3 =  (int)((luminance[ x     + yoff1 ]-16)*((float)76309/(1<<16)));
                l2 =  (int)((luminance[ x + 1 + yoff  ]-16)*((float)76309/(1<<16)));
                l4 =  (int)((luminance[ x + 1 + yoff1 ]-16)*((float)76309/(1<<16)));
                
                data[h    ]            = (crop(l1+r)<<16)|(crop(l1-g)<<8)|crop(l1+b);
                data[h + 1]            = (crop(l2+r)<<16)|(crop(l2-g)<<8)|crop(l2+b);
                data[h + displayX    ] = (crop(l3+r)<<16)|(crop(l3-g)<<8)|crop(l3+b);
                data[h + displayX + 1] = (crop(l4+r)<<16)|(crop(l4-g)<<8)|crop(l4+b);
                h += 2;
            }
            h += displayX;
        }
    }
}
