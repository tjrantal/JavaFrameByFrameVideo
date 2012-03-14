/*
 * Copyright (c) 2004 Guilhem Tardy (www.salyens.com)
 */

package net.sourceforge.jffmpeg.ffmpegnative;

import javax.media.Owned;
import java.awt.Component;

public class CompatibilityAdapter implements CompatibilityControl, Owned {
    NativeEncoder owner;

    public CompatibilityAdapter(NativeEncoder owner) {
        this.owner=owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

    public boolean setCompatibility(boolean compatibility) {
        owner.compatibility = compatibility;
        owner.set_compatibility(owner.peer, owner.compatibility);
        owner.set_rtpPayloadSize(owner.peer, (owner.compatibility ? owner.targetPacketSize : owner.targetPacketSize - 128));
        owner.resetRequired = true;
        return owner.compatibility;
    }

    public Component getControlComponent() {
        return null;
    }
}
