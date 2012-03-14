/*
 * Copyright (c) 2004 Guilhem Tardy (www.salyens.com)
 */

package net.sourceforge.jffmpeg.ffmpegnative;

/**
 * This interface is a Control for specifying the cpuActive parameter
 * that automatically throttles down the frame rate when it can't keep up.
 */
public interface CpuLoadMgtControl extends javax.media.Control {

    public boolean setCpuLoadMgt(boolean cpuLoadMgt);
}
