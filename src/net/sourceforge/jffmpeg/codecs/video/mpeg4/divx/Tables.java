/*
 * Java port of ffmpeg DIVX decoder.
 * Copyright (c) 2004 Jonathan Hueber.
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
 * 1a39e335700bec46ae31a38e2156a898
 */
package net.sourceforge.jffmpeg.codecs.video.mpeg4.divx;

public class Tables {
    public static final int[] ff_mpeg4_default_intra_matrix = new int[] {
          8, 17, 18, 19, 21, 23, 25, 27,
         17, 18, 19, 21, 23, 25, 27, 28,
         20, 21, 22, 23, 24, 26, 28, 30,
         21, 22, 23, 24, 26, 28, 30, 32,
         22, 23, 24, 26, 28, 30, 32, 35,
         23, 24, 26, 28, 30, 32, 35, 38,
         25, 26, 28, 30, 32, 35, 38, 41,
         27, 28, 30, 32, 35, 38, 41, 45, 
    };

    public static final int[] ff_mpeg4_default_non_intra_matrix = new int[] {
         16, 17, 18, 19, 20, 21, 22, 23,
         17, 18, 19, 20, 21, 22, 23, 24,
         18, 19, 20, 21, 22, 23, 24, 25,
         19, 20, 21, 22, 23, 24, 26, 27,
         20, 21, 22, 23, 25, 26, 27, 28,
         21, 22, 23, 24, 26, 27, 28, 30,
         22, 23, 24, 26, 27, 28, 30, 31,
         23, 24, 25, 27, 28, 30, 31, 33,
    };
}

