package org.theseed.locations;

import java.util.HashMap;

/**
 * Enumeration describing the seven coding frames.  Note the special frame XX indicates an invalid result.
 *
 * @author Bruce Parrello
 *
 */
public enum Frame {
    M0("-1"), M2("-3"), M1("-2"), F0("0"), P1("+2"), P2("+3"), P0("+1"), XX("X");

    /** basic array of all frames */
    private static final Frame[] values = values();

    /** array of all good frames */
    public static final Frame[] all = new Frame[] { M0, M2, M1, F0, P1, P2, P0 };

    /** array of all good frames in report order */
    public static final Frame[] sorted = new Frame[] { M0, M1, M2, F0, P0, P1, P2 };

    /** array size for an array of frame data */
    public static final int nFrames = 7;

    /** reverse complement map */
    private static final Frame[] reverse = new Frame[] { P0, P2, P1, F0, M1, M2, M0 };

    /** modular conversion table for plus strand */
    public static final Frame[] plusFrames = new Frame[] { P0, P1, P2 };

    /** modular conversion table for minus strand */
    public static final Frame[] minusFrames = new Frame[] { M0, M1, M2 };

    /** conversion table for labels to frame objects (built on first use) */
    private static HashMap<String, Frame> labelMap = null;

    /** label of frame */
    private String label;

    private Frame(String label) {
        this.label = label;
    }

    /**
     * @return the idx
     */
    public int getIdx() {
        return this.ordinal();
    }

    /**
     * @return the label
     */
    public String toString() {
        return label;
    }

    /**
     * @return the opposite frame (will fail if frame is invalid)
     */
    public Frame rev() {
        return Frame.reverse[this.ordinal()];
    }

    /**
     * @return the frame for a given array index
     *
     * @param idx	array index whose frame object is needed
     */
    public static Frame idxFrame(int idx) {
        return values[idx];
    }

    /**
     * @return the frame corresponding to a label
     *
     * @param label	label whose frame object is desired
     */
    public static Frame frameOf(String label) {
        if (labelMap == null) {
            labelMap = new HashMap<String, Frame>();
            for (Frame frm : values) {
                labelMap.put(frm.label, frm);
            }
        }
        Frame retVal = labelMap.getOrDefault(label, Frame.XX);
        return retVal;
    }

}
