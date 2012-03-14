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
package net.sourceforge.jffmpeg.codecs.video.mpeg12.data;

/**
 *
 */
public class Tables {
    
    public static int[] getDspIdctPermutation() {
        int[] y = new int[ 64 ];
        for ( int i = 0; i < 64; i++ ) y[i] = i;
        return y;
         /*
 return new int[] {
            0x00, 0x08, 0x04, 0x09, 0x01, 0x0C, 0x05, 0x0D, 
            0x10, 0x18, 0x14, 0x19, 0x11, 0x1C, 0x15, 0x1D, 
            0x20, 0x28, 0x24, 0x29, 0x21, 0x2C, 0x25, 0x2D, 
            0x12, 0x1A, 0x16, 0x1B, 0x13, 0x1E, 0x17, 0x1F, 
            0x02, 0x0A, 0x06, 0x0B, 0x03, 0x0E, 0x07, 0x0F, 
            0x30, 0x38, 0x34, 0x39, 0x31, 0x3C, 0x35, 0x3D, 
            0x22, 0x2A, 0x26, 0x2B, 0x23, 0x2E, 0x27, 0x2F, 
            0x32, 0x3A, 0x36, 0x3B, 0x33, 0x3E, 0x37, 0x3F,
        };
         */
    }

    public static int[] getMpeg1DefaultIntraMatrix() {
        return new int[] {
            8, 16, 19, 22, 26, 27, 29, 34,
            16, 16, 22, 24, 27, 29, 34, 37,
            19, 22, 26, 27, 29, 34, 34, 38,
            22, 22, 26, 27, 29, 34, 37, 40,
            22, 26, 27, 29, 32, 35, 40, 48,
            26, 27, 29, 32, 35, 40, 48, 58,
            26, 27, 29, 34, 38, 46, 56, 69,
            27, 29, 35, 38, 46, 56, 69, 83
        };
    }
    
    public static int[] getMpeg1DefaultNonIntraMatrix() {
        return new int[] {
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16,
        };
    }
    
    public static int[] getNonLinearQscale() {
        return new int[] {
             0,  1,  2,  3,  4,  5,   6,   7,
             8, 10, 12, 14, 16, 18,  20,  22,
            24, 28, 32, 36, 40, 44,  48,  52,
            56, 64, 72, 80, 88, 96, 104, 112
        };
    }
    
    public static float[] getFrameRateTable() {
        return new float[] {
            0, (float)24000/1001, 24, 25,
            (float)30000/1001, 30, 50, (float)60000/1001,
            60, 15, 5, 10, 12, 15, 0
        };
    }
    
    public static final int MB_TYPE_INTRA    = 0x01;
    public static final int MB_TYPE_INTER    = 0x02;
    public static final int MB_TYPE_INTER4V  = 0x04;
    public static final int MB_TYPE_SKIPED   = 0x08;

    public static final int MB_IS_INTRA_MASK = 0x07;
    
    public static final int MB_TYPE_DIRECT   = 0x10;
    public static final int MB_TYPE_FORWARD  = 0x20;
    public static final int MB_TYPE_BACKWARD = 0x40;
    public static final int MB_TYPE_BIDIR    = 0x80;

    public static final int MB_TYPE_INTRA4x4   = 0x0001;
    public static final int MB_TYPE_INTRA16x16 = 0x0002; //FIXME h264 specific
    public static final int MB_TYPE_INTRA_PCM  = 0x0004; //FIXME h264 specific
    public static final int MB_TYPE_16x16      = 0x0008;
    public static final int MB_TYPE_16x8       = 0x0010;
    public static final int MB_TYPE_8x16       = 0x0020;
    public static final int MB_TYPE_8x8        = 0x0040;
    public static final int MB_TYPE_INTERLACED = 0x0080;
    public static final int MB_TYPE_DIRECT2    = 0x0100; //FIXME
    public static final int MB_TYPE_ACPRED     = 0x0200;
    public static final int MB_TYPE_GMC        = 0x0400; //FIXME mpeg4 specific
    public static final int MB_TYPE_SKIP       = 0x0800;
    public static final int MB_TYPE_P0L0       = 0x1000;
    public static final int MB_TYPE_P1L0       = 0x2000;
    public static final int MB_TYPE_P0L1       = 0x4000;
    public static final int MB_TYPE_P1L1       = 0x8000;
    public static final int MB_TYPE_L0         = (MB_TYPE_P0L0 | MB_TYPE_P1L0);
    public static final int MB_TYPE_L1         = (MB_TYPE_P0L1 | MB_TYPE_P1L1);
    public static final int MB_TYPE_L0L1       = (MB_TYPE_L0   | MB_TYPE_L1);
    public static final int MB_TYPE_QUANT      = 0x00010000;

    public static final int MB_TYPE_PAT        = 0x40000000;
    public static final int MB_TYPE_ZERO_MV    = 0x20000000;

    public static int[] getBType2mb_type() {
        return new int[] {
                            MB_TYPE_INTRA,
                            MB_TYPE_L1,
                            MB_TYPE_L1   | MB_TYPE_PAT,
                            MB_TYPE_L0,
                            MB_TYPE_L0   | MB_TYPE_PAT,
                            MB_TYPE_L0L1,
                            MB_TYPE_L0L1 | MB_TYPE_PAT,
            MB_TYPE_QUANT | MB_TYPE_INTRA,
            MB_TYPE_QUANT | MB_TYPE_L1   | MB_TYPE_PAT,
            MB_TYPE_QUANT | MB_TYPE_L0   | MB_TYPE_PAT,
            MB_TYPE_QUANT | MB_TYPE_L0L1 | MB_TYPE_PAT,
        };
    }
    
    public static int[] getPType2mb_type() {
        return new int[] {
                            MB_TYPE_INTRA,
                            MB_TYPE_L0 | MB_TYPE_PAT | MB_TYPE_ZERO_MV | MB_TYPE_16x16,
                            MB_TYPE_L0,
                            MB_TYPE_L0 | MB_TYPE_PAT,
            MB_TYPE_QUANT | MB_TYPE_INTRA,
            MB_TYPE_QUANT | MB_TYPE_L0 | MB_TYPE_PAT | MB_TYPE_ZERO_MV | MB_TYPE_16x16,
            MB_TYPE_QUANT | MB_TYPE_L0 | MB_TYPE_PAT,
        };
    }
}
