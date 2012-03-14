/*
 * This is a Java port of the a52dec audio codec,a free ATSC A-52 stream decoder.
 * Copyright (c) 2003 Jonathan Hueber.
 *
 * Copyright (C) 2000-2003 Michel Lespinasse <walken@zoy.org>
 * Copyright (C) 1999-2000 Aaron Holtzman <aholtzma@ess.engr.uvic.ca>
 *
 * a52dec is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * a52dec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.sourceforge.jffmpeg.codecs.audio.ac3;

import net.sourceforge.jffmpeg.GPLLicense;

/**
 *
 */
public class Quantizer implements GPLLicense {
    private double[] q1 = new double[ 256 ];
    private double[] q2 = new double[ 256 ];
    private double[] q3 = new double[ 256 ];
    private double[] q4 = new double[ 256 ];
    private int q1_ptr = 0;
    private int q2_ptr = 0;
    private int q3_ptr = 0;
    private int q4_ptr = 0;
    
    public double[] getQ1() {
        return q1;
    }
    
    public double[] getQ2() {
        return q2;
    }
    
    public double[] getQ3() {
        return q3;
    }
    
    public double[] getQ4() {
        return q4;
    }

    public int getQ1Pointer() {
        return q1_ptr;
    }
    
    public void setQ1Pointer( int q1_ptr ) {
        this.q1_ptr = q1_ptr;
    }

    public int getQ2Pointer() {
        return q2_ptr;
    }
    
    public void setQ2Pointer( int q2_ptr ) {
        this.q2_ptr = q2_ptr;
    }
    public int getQ3Pointer() {
        return q3_ptr;
    }
    
    public void setQ3Pointer( int q3_ptr ) {
        this.q3_ptr = q3_ptr;
    }
    public int getQ4Pointer() {
        return q4_ptr;
    }
    
    public void setQ4Pointer( int q4_ptr ) {
        this.q4_ptr = q4_ptr;
    }
}
