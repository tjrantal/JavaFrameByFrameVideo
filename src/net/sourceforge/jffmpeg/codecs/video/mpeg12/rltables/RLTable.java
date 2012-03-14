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
package net.sourceforge.jffmpeg.codecs.video.mpeg12.rltables;

import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

/**
 * Manage RM Tables
 */
public class RLTable extends VLCTable {
    protected int n;
    protected int last;
    protected int[] table_run;
    protected int[] table_level;

    protected int table_levelLength;
    protected int table_runLength;
    
    protected int[][] max_level;
    protected int[][] index_run;    
    protected int[][] max_run;    

    public final int getLevel( int index ) {
        return table_level[ index ];
    }

    public final int getRun( int index ) {
        return table_run[ index ];
    }

    public final int[][] getMaxLevel() {
        return max_level;
    }

    public final int[][] getMaxRun() {
        return max_run;
    }

    protected void calculateStats() {
        /* compute max_level[], max_run[] and index_run[] */
        max_level = new int[2][n];
        index_run = new int[2][n];
        max_run   = new int[2][n];

        for( int c=0; c<2; c++) {
            int start;
            int end;
            if ( c == 0 ) {
                start = 0;
                end   = last;
            } else {
                start = last;
                end   = n;
            }
            
            for( int i = start; i < end; i++ ) {
                int run   = table_run[i];
                int level = table_level[i];
                if ( index_run[c][run] == n    ) index_run[c][run] = i;
                if ( level > max_level[c][run] ) max_level[c][run] = level;
                if ( run   > max_run[c][level] ) max_run[c][level] = run;
            }
        }
        
        /**
         * Manage table level and run look up
         */
        table_levelLength = table_level.length;
        table_runLength = table_run.length;
        
        int[] oldTable = table_level;
        table_level = new int[ table_levelLength * 2 ];
        for ( int i = 0; i < table_level.length; i++ ) {
            table_level[ i ] = ( i < table_levelLength ) ? oldTable[ i ] : 0;
        }
         
        oldTable = table_run;
        table_run = new int[ table_runLength * 2 ];
        for ( int i = 0; i < table_run.length; i++ ) {
            if ( i >= table_runLength ) {
                table_run[ i ] = 65;
            } else {
                table_run[ i ] = (i >= last ) ? oldTable[ i ] + 193 : oldTable[ i ] + 1;
            }
        }   
        
        createHighSpeedTable();
    }
}
