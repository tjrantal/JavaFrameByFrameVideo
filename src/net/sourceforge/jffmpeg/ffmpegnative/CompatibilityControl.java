/*
 * Copyright (c) 2004 Guilhem Tardy (www.salyens.com)
 */

package net.sourceforge.jffmpeg.ffmpegnative;

/**
 * This interface is a Control for specifying the compatibility parameter
 * that enables interworking with decoders that only accept Mode A packets.
 */
public interface CompatibilityControl extends javax.media.Control {

    public boolean setCompatibility(boolean compatibility);
}
