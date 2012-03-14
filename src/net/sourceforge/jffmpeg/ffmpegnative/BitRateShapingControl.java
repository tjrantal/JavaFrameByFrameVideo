/*
 * Copyright (c) 2004 Guilhem Tardy (www.salyens.com)
 */

package net.sourceforge.jffmpeg.ffmpegnative;

/**
 * This interface is a Control for specifying the shapingActive parameter
 * that ensures an upper bound on the transmitted bit rate.
 */
public interface BitRateShapingControl extends javax.media.Control {

    public boolean setBitRateShaping(boolean bitRateShaping);
}
