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

package net.sourceforge.jffmpeg.codecs.audio.vorbis;

public class CodeBook {
    private int    dim;
    private int    entries;
    private int[] lengthList;

    private int mapType;

    private long q_min;
    private long q_delta;
    private int  q_quant;
    private long q_sequencep;
    private int  quantvals;
    private long[] quantlist = new long[ quantvals ];

    /* Calculated */
    private float[] valuelist;

    public int getDim() {
        return dim;
    }

    private final int decode_packed_entry_number(OggReader oggRead){
        int  read = dec_maxlength;
        int lo,hi;
        long lok = oggRead.showBits(dec_firsttablen);  //TODO handle EOF exc
//	System.out.println( "lok " +lok + " " + dec_firsttablen + " " + oggRead.showBits(dec_firsttablen*2));
        int entry = dec_firsttable[(int)lok];
        if( (entry & 0x80000000L) != 0 ){
            lo = (int)(( entry >> 15 ) &0x7fff);
            hi = (int)(used_entries - (entry & 0x7fff));
//System.out.println( lo + " " + hi );
        } else {
            oggRead.skipBits( dec_codelengths[entry-1] );
//System.out.println( "adv " + dec_codelengths[entry-1] );
            return entry - 1;
        }

        
        lok = oggRead.showBits(read);               //TODO handle EOF exc

//        while(lok<0 && read>1)
//           lok = oggpack_look(b, --read);
//        if(lok<0)return -1;

        /* bisect search for the codeword in the ordered list */
        {
            int testword = bitreverse(lok);

            while(hi-lo>1) {
                int p=(hi-lo)>>1;
                int test=(codelist[lo+p] > testword) ? 1 : 0;    
                lo+=p&(test-1);
                hi-=p&(-test);
//		System.out.println( "lo " + lo );
            }

            if( dec_codelengths[lo] <= read ) {
                oggRead.skipBits( dec_codelengths[lo] );
                return(lo);
            }
        }
        throw new Error( "Unrecognised Code" );
    }

    /* Read an array of floats */
    public void decodev_set(float[] out, int offset, OggReader read, int n) {
        for( int i = 0; i < n; ) {
            int entry = decode_packed_entry_number(read);
//	    System.out.println( "decodec_set " + entry );
            int t = entry * dim;
            for ( int j = 0; j < dim; j++ ) {
                out[ offset + (i++) ]=valuelist[ t + j ];
//System.out.println( (int)(valuelist[t+j]*1000) );
            }
        }
    }

    public void decodev_add(float[] out, int offset, OggReader read, int n) {
        for( int i = 0; i < n; ) {
            int entry = decode_packed_entry_number( read );
	    System.out.println( "decodev_add " + entry );
            int t = entry * dim;
            for ( int j = 0; j < dim; j++ ) {
                out[ offset + (i++) ] += valuelist[ t + j ];
            }
        }
    }

    public void decodevs_add(float[] out, int offset, OggReader read, int n) {
        int step = n / dim;
        long[] entry = new long[ step ];
        int[] t = new int[ step ];

        for ( int i = 0; i < step; i++ ) {
            entry[i] = decode_packed_entry_number(read);
            t[i] = (int)(entry[i] * dim);
//	    System.out.println( "decodecs_add " + entry[i] );
        }

        int o = 0;
        for( int i = 0; i < dim; i++, o+=step ) {
            for ( int j = 0; j < step; j++ ) {
                out[ offset + j ] += valuelist[ t[j] + i ];
            }
        }
    }

    public void decodevv_add( float[][] out, int offset, int ch, OggReader read, int n ) {
        int chptr = 0;
        for( int i = offset / ch; i < (offset + n) / ch; ) {
            long entry = decode_packed_entry_number(read);
//	    System.out.println( "decodevv_add " + entry );
            int t = (int)entry * dim;
            for ( int j = 0; j < dim; j++ ) {
                out[chptr++][i]+= valuelist[ t + j ];
//System.out.println( (int)(valuelist[t+j]*1000) );            
                if ( chptr == ch ) {
                    chptr = 0;
                    i++;
                }
            }
        }
    }


    public int decode( OggReader read ) {
        int packed_entry = decode_packed_entry_number(read);
//        System.out.println( "decode " + packed_entry );
        return dec_index[ packed_entry ];
    }

    private static final int _ilog( long v ) {
       int ret=0;
       while( v > 0 ){
           ret++;
           v >>= 1;
       }
       return(ret);
    }

    public void unpack( OggReader oggRead ) {
//System.out.println( "vorbis_staticbook_unpack" );
        /* Check header */
        if ( oggRead.getBits(24) != 0x564342 ) {
            throw new Error( "Bad padding" );
        }
        dim     = (int)oggRead.getBits(16);
        entries = (int)oggRead.getBits(24);
        lengthList = new int[ entries ];

        if ( oggRead.getBits(1) == 0 ) {
            /* Unordered */
            if ( oggRead.getBits(1) == 1 ) {
                for( int i = 0; i < entries; i++ ) {
                    /* Use tagging */
                    if( oggRead.getBits(1) != 0 ) {
                        int num = (int)oggRead.getBits( 5 );
                        lengthList[i] = num + 1;
                    } else {
                        lengthList[i] = 0;
                    }
//	System.out.println( "a " + lengthList[i] );
                }
            } else {
                for( int i = 0; i < entries; i++ ) {
                    /* No tagging */
                    int num = (int)oggRead.getBits( 5 );
                    lengthList[i] = num + 1;
//	System.out.println( "b " + lengthList[i] );
                }
            }
        } else {
            /* Ordered */
            int length = (int)oggRead.getBits(5);
            for( int i = 0; i < entries; ) {
                int num = (int)oggRead.getBits( _ilog(entries-i) );
                for( int j = 0; j < num && i < entries; j++,i++) {
                    lengthList[i] = length;
//         System.out.println( "c " + lengthList[i] );
                }
                length++;
            }
        }
//	System.out.println( "ListLength " + lengthList.length );

        /* Do we have a mapping to unpack? */
        mapType = (int)oggRead.getBits(4);
        switch( mapType ) {
            case 0: {
                /* no mapping */
                break;
            }
            case 1: 
            case 2: {
                /* implicitly populated value mapping */
                /* explicitly populated value mapping */

                q_min       = oggRead.getBits(32);
                q_delta     = oggRead.getBits(32);
                q_quant     = (int)(oggRead.getBits(4) + 1);
                q_sequencep = oggRead.getBits(1);
                quantvals=0;
                if ( mapType == 1 ) {
                    quantvals = (int)Math.floor( Math.pow( (double)entries, ((double)1)/((double)dim) ) );
                } else {
                    quantvals = entries * dim;
                }
//		System.out.println( "quantvals " + quantvals );
                /* quantized values */
                quantlist = new long[ quantvals ];
                for( int i = 0; i < quantvals; i++ ) {
                    quantlist[i] = oggRead.getBits( q_quant );
                }
                break;
            }
            default: {
                throw new Error( "Illegal mapType" );
            }
        }
    }

/* given a list of word lengths, generate a list of codewords.  Works
 *  for length ordered or unordered, always assigns the lowest valued
 *  codewords first.  Extended to handle unused entries (length 0) 
 */
  private int[] _make_words(int[] l,int n,int sparsecount){
  int count=0;
  int[] marker = new int[33];
  int[] r = new int[(sparsecount > 0?sparsecount:n)];

  for(int i=0;i<n;i++){
    int length=l[i];
    if(length>0){
      int entry=marker[length];
      
      /* when we claim a node for an entry, we also claim the nodes
	 below it (pruning off the imagined tree that may have dangled
	 from it) as well as blocking the use of any nodes directly
	 above for leaves */
      
      /* update ourself */
      if(length<32 && (entry>>length) != 0){
	/* error condition; the lengths must specify an overpopulated tree */
	return null;
      }
      r[count++]=entry;

      /* Look to see if the next shorter marker points to the node
	 above. if so, update it and repeat.  */
      {
	for(int j=length;j>0;j--){
	  
	  if((marker[j]&1) != 0){
	    /* have to jump branches */
	    if(j==1)
	      marker[1]++;
	    else
	      marker[j]=marker[j-1]<<1;
	    break; /* invariant says next upper marker would already
		      have been moved if it was on the same path */
	  }
	  marker[j]++;
	}
      }
      
      /* prune the tree; the implicit invariant says all the longer
	 markers were dangling from our just-taken node.  Dangle them
	 from our *new* node. */
      for(int j=length+1;j<33;j++)
	if((marker[j]>>1) == entry){
	  entry=marker[j];
	  marker[j]=marker[j-1]<<1;
	}else
	  break;
    }else
      if(sparsecount==0)count++;
  }
    
  /* bitreverse the words because our bitwise packer/unpacker is LSb
     endian */
  int i = 0;
  for(i=0,count=0;i<n;i++){
    int temp=0;
    for(int j=0;j<l[i];j++){
      temp<<=1;
      temp|=(r[count]>>j)&1;
    }

//      System.out.println( "mkwrd " + temp);
    if(sparsecount != 0){
      if(l[i] != 0)
	r[count++]=temp;
    }else
      r[count++]=temp;
  }

  return(r);
}


    private static final int bitreverse(long x) {
        x = ((x>>16)&0x0000ffffL) | ((x<<16)&0xffff0000L);
        x = ((x>> 8)&0x00ff00ffL) | ((x<< 8)&0xff00ff00L);
        x = ((x>> 4)&0x0f0f0f0fL) | ((x<< 4)&0xf0f0f0f0L);
        x = ((x>> 2)&0x33333333L) | ((x<< 2)&0xccccccccL);
        x = ((x>> 1)&0x55555555L) | ((x<< 1)&0xaaaaaaaaL);
        return (int)x;
    }

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

    private int used_entries;
    private int dec_maxlength;
    private int dec_firsttablen;
    private int[] dec_firsttable;
    private int[] dec_codelengths;
    private int[] dec_index;
    private int[] codelist;

    public void initDecode() {
//        System.out.println( "vorbis_book_init_decode" );
        int i = 0;
        int j = 0;
        int n=0,tabn;

        /* count actually used entries */
        for( i = 0; i < entries; i++ ) {
            if( lengthList[i]>0 ) {
                n++;
            }
//            System.out.println( "LengthList " + lengthList[i] );
        }

        used_entries = n;

        /* two different remappings go on here.  

           First, we collapse the likely sparse codebook down only to
           actually represented values/words.  This collapsing needs to be
           indexed as map-valueless books are used to encode original entry
           positions as integers.

           Second, we reorder all vectors, including the entry index above,
           by sorted bitreversed codeword to allow treeless decode. */

        /* perform sort */
        int[] codes=_make_words(lengthList,entries,used_entries);
        int[] codep= new int[ n ];
    
        if(codes==null) throw new Error("Error creating words");

        for(i=0;i<n;i++){
//	     System.out.println( "BitReverse " + codes[ i ] );
             codes[i]=bitreverse(codes[i]);
             codep[i]=i;
        }

        qsort(codep,codes);

        int[] sortindex=new int [ n ];
        codelist=new int[ n ];
        /* the index is a reverse index */
        for(i=0;i<n;i++){
            int position=codep[i];
            sortindex[position]=i;
//	    System.out.println( "sortindex["+position+"]="+i);
        }

        for(i=0;i<n;i++) {
            codelist[sortindex[i]]=codes[i];
        }

        valuelist=_book_unquantize(n,sortindex);
        dec_index=new int[n];

        for(n=0,i=0;i<entries;i++) {
            if(lengthList[i]>0) {
                dec_index[sortindex[n++]]=i;
            }
        }
  
        dec_codelengths=new int[n];
        for(n=0,i=0;i<entries;i++) {
            if(lengthList[i]>0) {
                dec_codelengths[sortindex[n++]]=lengthList[i];
            }
        }

        dec_firsttablen=_ilog(used_entries)-4; /* this is magic */
        if(dec_firsttablen<5) dec_firsttablen=5;
        if(dec_firsttablen>8) dec_firsttablen=8;

        tabn=1<<dec_firsttablen;
        dec_firsttable=new int[ tabn ];
        dec_maxlength=0;

        for(i=0;i<n;i++) {
            if(dec_maxlength < dec_codelengths[i]) {
                dec_maxlength = dec_codelengths[i];
            }
            if(dec_codelengths[i]<=dec_firsttablen) {
                int orig=bitreverse( codelist[i] );
                for( j=0;j<(1<<(dec_firsttablen-dec_codelengths[i]));j++) {
                    dec_firsttable[orig|(j<<dec_codelengths[i])]=i+1;
                }
            }
        }

        /* now fill in 'unused' entries in the firsttable with hi/lo search
           hints for the non-direct-hits */
        long mask=0xfffffffeL<<(31-dec_firsttablen);
        int lo=0,hi=0;

        for( i = 0; i < tabn; i++ ) {
            long word=((long)i)<<(32-dec_firsttablen);
            if(dec_firsttable[bitreverse(word)]==0) {
                while((lo+1)<n && (codelist[lo+1]&0xffffffffL)<=word)lo++;
                while(    hi<n && word>=(((long)codelist[hi])&mask&0xffffffffL))hi++;
                /* we only actually have 15 bits per hint to play with here.
                In order to overflow gracefully (nothing breaks, efficiency
                just drops), encode as the difference from the extremes. */
                int loval=lo;
                int hival=n-hi;

                if(loval>0x7fff)loval=0x7fff;
                if(hival>0x7fff)hival=0x7fff;
                dec_firsttable[bitreverse(word)]=
                    (int)(0x80000000L | (loval<<15) | hival);
//		System.out.println( "dec_firsttable " + dec_firsttable[bitreverse(word)] );
            }
        }
    }


    /* 32 bit float (not IEEE; nonnormalized mantissa +
       biased exponent) : neeeeeee eeemmmmm mmmmmmmm mmmmmmmm 
       Why not IEEE?  It's just not that important here. */
    private static final int VQ_FEXP = 10;
    private static final int VQ_FMAN = 21;
    private static final int VQ_FEXP_BIAS = 768;

    private static float _float32_unpack( long val ){
        double  mant=val&0x1fffff;
        boolean sign= (val&0x80000000L) != 0;
        long    exp = val & 0x7fe00000L;
        exp = exp >>>VQ_FMAN;
//	System.out.println( "float32_unpack " + val + " " + ((int)mant) + " " + (sign?1:0) + " " + exp+ " " + (val&0x7fe00000L) );
        if (sign) mant= -mant;
        return (float)(mant * Math.pow( 2, exp-(VQ_FMAN-1)-VQ_FEXP_BIAS) );
    }

    private static final float fabs( float f ) {
        return (f > 0) ? f : -f;
    }

    /* we need to deal with two map types: in map type 1, the values are
       generated algorithmically (each column of the vector counts through
       the values in the quant vector). in map type 2, all the values came
       in in an explicit list.  Both value lists must be unpacked */
    private float[] _book_unquantize( int n, int[] sparsemap ) {
        int j,k,count=0;
        float[] r = null;
        if(mapType==1 || mapType==2){
            int quantvals;
            float mindel=_float32_unpack(q_min);
            float delta=_float32_unpack(q_delta);
            r= new float[ n * dim ];

            /* maptype 1 and 2 both use a quantized value vector, but
               different sizes */
            switch(mapType){
                case 1: {
                    /* most of the time, entries%dimensions == 0, but we need to be
                       well defined.  We define that the possible vales at each
                       scalar is values == entries/dim.  If entries%dim != 0, we'll
                       have 'too few' values (values*dim<entries), which means that
                       we'll have 'left over' entries; left over entries use zeroed
                       values (and are wasted).  So don't generate codebooks like
                       that */
                    quantvals = (int)Math.floor( Math.pow( (double)entries, ((double)1)/((double)dim) ) );

                    for(j=0;j<entries;j++) {
	                if((sparsemap != null && (lengthList[j] != 0)) || sparsemap == null ){
                            float last=0.f;
                            int indexdiv=1;
                            for(k=0;k<dim;k++){
                                int index= (j/indexdiv)%quantvals;
                                float val=quantlist[index];
//System.out.println( "a" + (int)(val * 1000) + " " + index );
                                val=fabs(val)*delta+mindel+last;
//System.out.println( "d" + (int)(delta * 1000) + " " + (int)(mindel * 1000) );
                                if(q_sequencep != 0)last=val;	  
//System.out.println( (int)(val * 1000) );
                                if(sparsemap != null) {
                                    r[sparsemap[count]*dim+k]=val;
// System.out.println( "sparsemap " + sparsemap[count] );
                                } else {
                                    r[count*dim+k]=val;
                                }
                                indexdiv*=quantvals;
                            }
                        count++;
                        }
                    }
                    break;
                }
                case 2: {
                    for(j=0;j<entries;j++){
                            if((sparsemap != null && lengthList[j] != 0) || sparsemap == null ){
	                    float last=0.f;
	  
	                    for(k=0;k<dim;k++) {
                                float val=quantlist[j*dim+k];
//System.out.println( "a" + (int)(val * 1000) + " " + (j*dim+k));

                                val=fabs(val)*delta+mindel+last;
//System.out.println( "d" + (int)(delta * 1000) + " " + (int)(mindel * 1000) );
                                if(q_sequencep != 0)last=val;
//System.out.println( (int)(val * 1000) );
                                if(sparsemap != null)
                                    r[sparsemap[count]*dim+k]=val;
                                else
                                    r[count*dim+k]=val;
                            }
                            count++;
                        }
                    }
                    break;
                }
            }
        }
        return(r);
    }
}

