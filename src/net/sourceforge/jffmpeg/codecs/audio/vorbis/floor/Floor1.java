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

package net.sourceforge.jffmpeg.codecs.audio.vorbis.floor;

import net.sourceforge.jffmpeg.codecs.audio.vorbis.VorbisDecoder;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.OggReader;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.CodeBook;

public class Floor1 extends Floor {
    private int   partitions;
    private int[] partitionclass;
    
    private int[]   class_dim     = new int[ 16 ];
    private int[]   class_subs    = new int[ 16 ];
    private int[]   class_books   = new int[ 16 ];
    private int[][] class_subbook = new int[ 16 ][ 16 ];

    private int mult;
    private int[] postlist = new int[ 32 * 9 ];

    public void unpack( OggReader oggRead ) {
        int maxclass = -1;

        /* Read partitions */
        partitions = (int)oggRead.getBits( 5 );
        partitionclass = new int[ partitions ];
        for ( int i = 0; i < partitions; i++ ) {
            partitionclass[ i ] = (int)oggRead.getBits(4);
            if (maxclass < partitionclass[ i ]) maxclass = partitionclass[ i ];
        }
        
        for ( int i = 0; i < maxclass + 1; i++ ) {
            class_dim[i]   = (int)oggRead.getBits( 3 ) + 1;
            class_subs[i]  = (int)oggRead.getBits( 2 );
            if ( class_subs[i] > 0 ) {
                class_books[i] = (int)oggRead.getBits( 8 );
            }
            for ( int j = 0; j < (1 << class_subs[i]); j++ ) {
                class_subbook[i][j] = (int)oggRead.getBits(8) - 1;
            }
        }

        mult = (int)oggRead.getBits( 2 ) + 1;

        /* Create the post list table */
        int rangebits = (int)oggRead.getBits( 4 );
        int count = 0;
        int k = 0;
        for ( int i = 0; i < partitions; i++ ) {
            count += class_dim[ partitionclass[ i ] ];
            for ( ; k < count; k++ ) {
                postlist[ k+2 ] = (int)oggRead.getBits( rangebits );
            }
        }
        postlist[ 0 ] = 0;
        postlist[ 1 ] = 1 << rangebits;             
    }

    private static final int ilog( long v ) {
       int ret=0;
       while( v > 0 ){
           ret++;
           v >>= 1;
       }
       return(ret);
    }

    /* Look */
    private int posts;
    private int quant_q;
    private int[] loneighbor;
    private int[] hineighbor;
    private int[] forward_index;

    private static void qsort( int[] pointer, int[] data ) {
        for ( int i = 0; i < pointer.length; i++ ) {
            for ( int j = i + 1; j < pointer.length; j++ ) {
                long a = data[ pointer[ i ] ] & 0xffffffffL;
                long b = data[ pointer[ j ] ] & 0xffffffffL;
                if ( a > b ) {
                    int t = pointer[ i ];
                    pointer[ i ] = pointer[ j ];
                    pointer[ j ] = t;
                }
            }
        }
    }

    public void look() {
//      System.out.println( "Floor1 look" );

      int lookn = postlist[1];

      /* we drop each position value in-between already decoded values,
         and use linear interpolation to predict each new value past the
         edges.  The positions are read in the order of the position
         list... we precompute the bounding positions in the lookup.  Of
         course, the neighbors can change (if a position is declined), but
         this is an initial mapping */

      int n = 0;
      for( int i = 0; i < partitions; i++ ) {
          n += class_dim[ partitionclass[i] ];
      }
      n += 2;
      posts = n;

      /* also store a sorted position index */
      int[] sortpointer = new int[ n ];
      forward_index = new int[ n ];
      int[] reverse_index = new int[ n ];
      int[] sorted_index = new int[ n ];
      for( int i = 0; i < n; i++ ) sortpointer[i]=i;
      qsort(sortpointer, postlist);

      /* points from sort order back to range number */
      for(int i = 0; i < n; i++) {
          forward_index[i]=sortpointer[i];
//	  System.out.println( "fi " + forward_index[ i ] );
      }
      /* points from range order to sorted position */
      for(int i = 0; i < n; i++) {
          reverse_index[ forward_index[i] ]=i;
      }
      /* we actually need the post values too */
      for(int i = 0; i < n; i++) {
          sorted_index[i] = postlist[forward_index[i]];
      }
      /* quantize values to multiplier spec */
      switch(mult){
          case 1: /* 1024 -> 256 */ {
              quant_q=256;
              break;
          }
          case 2: /* 1024 -> 128 */ {
              quant_q=128;
              break;
          }
          case 3: /* 1024 -> 86 */ {
              quant_q=86;
              break;
          }
          case 4: /* 1024 -> 64 */ {
              quant_q=64;
              break;
          }
      }

      loneighbor = new int[ n ];
      hineighbor = new int[ n ];
      /* discover our neighbors for decode where we don't use fit flags
         (that would push the neighbors outward) */
      for(int i = 0;i < n - 2; i++) {
          int lo = 0;
          int hi = 1;
          int lx = 0;
          int hx = lookn;
          int currentx = postlist[i+2];
          for(int j = 0; j < i+2; j++ ) {
              int x = postlist[j];
              if (x > lx && x < currentx) {
                  lo=j;
                  lx=x;
              }
              if (x < hx && x > currentx) {
                  hi=j;
                  hx=x;
              }
          }
          loneighbor[i]=lo;
          hineighbor[i]=hi;
// System.out.println( "neighbor " + i + " " + hi + " " + lo );
      }
  }
 
    public Object inverse1( OggReader oggRead, VorbisDecoder vorbis ) {
        /* unpack wrapped/predicted values from stream */
        if( oggRead.getBits(1) == 0 ) return null;

        int[] fit_value= new int[ posts ];

//        System.out.println( "Floor1 inverse1" );
        fit_value[0] = (int)oggRead.getBits( ilog(quant_q - 1) );
        fit_value[1] = (int)oggRead.getBits( ilog(quant_q - 1) );

        /* partition by partition */
        int i,j;
        for( i = 0, j = 2; i < partitions; i++ ) {
            int pclass   = partitionclass[ i ];
            int cdim     = class_dim[ pclass ];
            int csubbits = class_subs[ pclass ];
            int csub     = 1 << csubbits;
            int cval     = 0;
//	    System.out.println( "floor1" );

            /* decode the partition's first stage cascade value */
            if ( csubbits != 0 ) {
                CodeBook b = vorbis.getCodeBook( class_books[ pclass ] );
                cval = b.decode( oggRead );
            }

            for( int k = 0; k < cdim; k++ ) {
                int book = class_subbook[pclass][ cval & (csub-1) ];
                cval >>= csubbits;
                if( book >= 0 ) {
                    CodeBook b = vorbis.getCodeBook( book );
                    fit_value[j+k]= b.decode(oggRead);
                } else {
                    fit_value[j+k]=0;
                }
            }
            j += cdim;
       }
       /* unwrap positive values and reconsitute via linear interpolation */
       for( i = 2; i < posts; i++ ) {
           int predicted=render_point( postlist[loneighbor[i-2]],
                                       postlist[hineighbor[i-2]],
                                       fit_value[loneighbor[i-2]],
                                       fit_value[hineighbor[i-2]],
                                       postlist[i] );
           int hiroom = quant_q - predicted;
           int loroom = predicted;
           int room   = ( hiroom < loroom ? hiroom : loroom ) << 1;
           int val    = fit_value[i];

           if ( val != 0 ) {
               if ( val >= room ) {
                    if ( hiroom > loroom ) {
                        val = val - loroom;
                    } else {
                        val = -1 - ( val - hiroom );
                    }
                } else {
                    if ( (val & 1) != 0) {
                       val = -((val + 1) >> 1);
                    } else {
                        val>>=1;
                    }
                }
                fit_value[i]=val+predicted;
                fit_value[loneighbor[i-2]]&=0x7fff;
                fit_value[hineighbor[i-2]]&=0x7fff;
            } else {
                fit_value[i]=predicted|0x8000;
            }
//	   System.out.println( "fit_value[" + i + "]=" + fit_value[i] );
        }
        return fit_value;
    }

    private static int render_point( int x0, int x1, int y0, int y1, int x ) {
        y0 &= 0x7fff; /* mask off flag */
        y1 &= 0x7fff;
    
        int dy  = y1 - y0;
        int adx = x1 - x0;
        int ady = (dy > 0) ? dy : -dy;
        int err = ady * (x - x0);
    
        int off = err/adx;
        return (dy<0) ? (y0-off) : (y0+off);
    }

    public void inverse2( Object floor, float[] out, VorbisDecoder vorbis ) {
//        System.out.println( "Floor1 inverse2" );

        int n = vorbis.getBlockSize( vorbis.getW() ? 1 : 0 ) / 2;

        if ( floor != null ) {
            /* render the lines */
            int[] fit_value=(int[])floor;
            int hx = 0;
            int lx = 0;
            int ly= fit_value[0] * mult;
            for( int j = 1; j < posts; j++ ) {
                int current = forward_index[j];
                int hy = fit_value[ current ]&0x7fff;
                if( hy == fit_value[current] ) {
                    hy *= mult;
                    hx  = postlist[current];
                    render_line(lx,hx,ly,hy,out);
                    lx = hx;
                    ly = hy;
                }
            }
            for( int j = hx; j < n; j++ ) {
                out[j] *= FLOOR1_fromdB_LOOKUP[ly]; /* be certain */    
            }
        }
    }

    public static final float[] FLOOR1_fromdB_LOOKUP = new float[] {
  1.0649863e-07F, 1.1341951e-07F, 1.2079015e-07F, 1.2863978e-07F, 
  1.3699951e-07F, 1.4590251e-07F, 1.5538408e-07F, 1.6548181e-07F, 
  1.7623575e-07F, 1.8768855e-07F, 1.9988561e-07F, 2.128753e-07F, 
  2.2670913e-07F, 2.4144197e-07F, 2.5713223e-07F, 2.7384213e-07F, 
  2.9163793e-07F, 3.1059021e-07F, 3.3077411e-07F, 3.5226968e-07F, 
  3.7516214e-07F, 3.9954229e-07F, 4.2550680e-07F, 4.5315863e-07F, 
  4.8260743e-07F, 5.1396998e-07F, 5.4737065e-07F, 5.8294187e-07F, 
  6.2082472e-07F, 6.6116941e-07F, 7.0413592e-07F, 7.4989464e-07F, 
  7.9862701e-07F, 8.5052630e-07F, 9.0579828e-07F, 9.6466216e-07F, 
  1.0273513e-06F, 1.0941144e-06F, 1.1652161e-06F, 1.2409384e-06F, 
  1.3215816e-06F, 1.4074654e-06F, 1.4989305e-06F, 1.5963394e-06F, 
  1.7000785e-06F, 1.8105592e-06F, 1.9282195e-06F, 2.0535261e-06F, 
  2.1869758e-06F, 2.3290978e-06F, 2.4804557e-06F, 2.6416497e-06F, 
  2.8133190e-06F, 2.9961443e-06F, 3.1908506e-06F, 3.3982101e-06F, 
  3.6190449e-06F, 3.8542308e-06F, 4.1047004e-06F, 4.3714470e-06F, 
  4.6555282e-06F, 4.9580707e-06F, 5.2802740e-06F, 5.6234160e-06F, 
  5.9888572e-06F, 6.3780469e-06F, 6.7925283e-06F, 7.2339451e-06F, 
  7.7040476e-06F, 8.2047000e-06F, 8.7378876e-06F, 9.3057248e-06F, 
  9.9104632e-06F, 1.0554501e-05F, 1.1240392e-05F, 1.1970856e-05F, 
  1.2748789e-05F, 1.3577278e-05F, 1.4459606e-05F, 1.5399272e-05F, 
  1.6400004e-05F, 1.7465768e-05F, 1.8600792e-05F, 1.9809576e-05F, 
  2.1096914e-05F, 2.2467911e-05F, 2.3928002e-05F, 2.5482978e-05F, 
  2.7139006e-05F, 2.8902651e-05F, 3.0780908e-05F, 3.2781225e-05F, 
  3.4911534e-05F, 3.7180282e-05F, 3.9596466e-05F, 4.2169667e-05F, 
  4.4910090e-05F, 4.7828601e-05F, 5.0936773e-05F, 5.4246931e-05F, 
  5.7772202e-05F, 6.1526565e-05F, 6.5524908e-05F, 6.9783085e-05F, 
  7.4317983e-05F, 7.9147585e-05F, 8.4291040e-05F, 8.9768747e-05F, 
  9.5602426e-05F, 0.00010181521F, 0.00010843174F, 0.00011547824F, 
  0.00012298267F, 0.00013097477F, 0.00013948625F, 0.00014855085F, 
  0.00015820453F, 0.00016848555F, 0.00017943469F, 0.00019109536F, 
  0.00020351382F, 0.00021673929F, 0.00023082423F, 0.00024582449F, 
  0.00026179955F, 0.00027881276F, 0.00029693158F, 0.00031622787F, 
  0.00033677814F, 0.00035866388F, 0.00038197188F, 0.00040679456F, 
  0.00043323036F, 0.00046138411F, 0.00049136745F, 0.00052329927F, 
  0.00055730621F, 0.00059352311F, 0.00063209358F, 0.00067317058F, 
  0.00071691700F, 0.00076350630F, 0.00081312324F, 0.00086596457F, 
  0.00092223983F, 0.00098217216F, 0.0010459992F, 0.0011139742F, 
  0.0011863665F, 0.0012634633F, 0.0013455702F, 0.0014330129F, 
  0.0015261382F, 0.0016253153F, 0.0017309374F, 0.0018434235F, 
  0.0019632195F, 0.0020908006F, 0.0022266726F, 0.0023713743F, 
  0.0025254795F, 0.0026895994F, 0.0028643847F, 0.0030505286F, 
  0.0032487691F, 0.0034598925F, 0.0036847358F, 0.0039241906F, 
  0.0041792066F, 0.0044507950F, 0.0047400328F, 0.0050480668F, 
  0.0053761186F, 0.0057254891F, 0.0060975636F, 0.0064938176F, 
  0.0069158225F, 0.0073652516F, 0.0078438871F, 0.0083536271F, 
  0.0088964928F, 0.009474637F, 0.010090352F, 0.010746080F, 
  0.011444421F, 0.012188144F, 0.012980198F, 0.013823725F, 
  0.014722068F, 0.015678791F, 0.016697687F, 0.017782797F, 
  0.018938423F, 0.020169149F, 0.021479854F, 0.022875735F, 
  0.024362330F, 0.025945531F, 0.027631618F, 0.029427276F, 
  0.031339626F, 0.033376252F, 0.035545228F, 0.037855157F, 
  0.040315199F, 0.042935108F, 0.045725273F, 0.048696758F, 
  0.051861348F, 0.055231591F, 0.058820850F, 0.062643361F, 
  0.066714279F, 0.071049749F, 0.075666962F, 0.080584227F, 
  0.085821044F, 0.091398179F, 0.097337747F, 0.10366330F, 
  0.11039993F, 0.11757434F, 0.12521498F, 0.13335215F, 
  0.14201813F, 0.15124727F, 0.16107617F, 0.17154380F, 
  0.18269168F, 0.19456402F, 0.20720788F, 0.22067342F, 
  0.23501402F, 0.25028656F, 0.26655159F, 0.28387361F, 
  0.30232132F, 0.32196786F, 0.34289114F, 0.36517414F, 
  0.38890521F, 0.41417847F, 0.44109412F, 0.46975890F, 
  0.50028648F, 0.53279791F, 0.56742212F, 0.60429640F, 
  0.64356699F, 0.68538959F, 0.72993007F, 0.77736504F, 
  0.82788260F, 0.88168307F, 0.9389798F, 1.F, 
};

    private void render_line(int x0,int x1,int y0,int y1,float[] d){
        int dy=y1-y0;
        int adx=x1-x0;
        int ady=Math.abs(dy);
        int base=dy/adx;
        int sy=(dy<0?base-1:base+1);
        int x=x0;
        int y=y0;
        int err=0;

        ady -= Math.abs(base*adx);

        d[x]*=FLOOR1_fromdB_LOOKUP[y];
        while(++x<x1){
            err=err+ady;
            if(err>=adx){
                err-=adx;
                y+=sy;
            }else{
                y+=base;
            }
            d[x]*=FLOOR1_fromdB_LOOKUP[y];
        }
    }
}
