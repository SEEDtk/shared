/**
 *
 */
package org.theseed.proteins;

import org.theseed.locations.FLocation;

/**
 * This class fixes locations according to various algorithms.  Currently, it only works for forward locations.
 * The caller provides a location and a sequence string, and the location will be expanded to a start and stop
 * according to the active DNA translation code.  Currently, the algorithms supported only affect the choice
 * of start code.  (We don't expect that to change, but we can forsee more complicated start selection rules
 * than we have now.)  Only forward locations are supported.
 *
 * @author Bruce Parrello
 *
 */
public abstract class LocationFixer extends DnaTranslator {

    /**
     * Back up to the codon inside the previous stop in the specified sequence.
     *
     * @param loc		location indicating the frame and the rightmost possible result
     * @param sequence	sequence containing the location
     *
     * @return the location inside the nearest stop to the left (start of the ORF)
     */
    protected int backToStop(FLocation loc, String sequence) {
        int retVal = loc.getLeft();
        while (retVal > 0 && ! this.isStop(sequence, retVal)) retVal -= 3;
        // Push past the stop.  Note an implied stop before the start of the contig is assumed.
        retVal += 3;
        return retVal;
    }

    /**
     * Construct a location-fixing DNA translator
     *
     * @param gc	relevant genetic code
     */
    protected LocationFixer(int gc) {
        super(gc);
    }

    /**
     * Fix the indicated location so that it has a start and stop.
     *
     * @param loc		location to be extended
     * @param sequence	sequence in which the location occurs
     *
     * @return TRUE if successful, FALSE if no such location can be found
     */
    public boolean fix(FLocation loc, String sequence) {
        boolean retVal = false;
        // Find the stop at the end.
        int newRight = this.findStop(loc, sequence);
        if (newRight >= 1) {
            // We found a stop.
            int newLeft = this.findStart(loc, sequence);
            if (newLeft >= 1) {
                loc.setLeft(newLeft);
                loc.setRight(newRight);
                retVal = true;
            }
        }
        return retVal;
    }


    /**
     * Find the appropriate start codon for the specified location.
     *
     * @param loc		location to extend
     * @param sequence	sequence in which the location occurs
     *
     * @return the position of the new start codon, or 0 if none was found
     */
    protected abstract int findStart(FLocation loc, String sequence);

    /**
     * This method finds the stop location for a fix operation.
     *
     * @param loc		location to be extended
     * @param sequence	sequence in which the location occurs
     *
     * @return the position at the end of the stop found, or 0 if no stop was found
     */
    protected int findStop(FLocation loc, String sequence) {
        int retVal = loc.getRight() - 2;
        int limit = sequence.length() - 2;
        while (retVal <= limit && ! this.isStop(sequence, retVal)) retVal += 3;
        if (retVal > limit) retVal = 0; else retVal += 2;
        return retVal;
    }

    /**
     * Fixing algorithm types.
     */
    public static enum Type {
        /** find longest protein */
        LONGEST,
        /** find nearest protein */
        NEAREST,
        /** find likeliest protein */
        LIKELIEST;

        /**
         * Create a location fixer using this algorithm.
         *
         * @param gc	target genetic code
         *
         * @return a location fixer that uses the appropriate algorithm
         */
        public LocationFixer create(int gc) {
            LocationFixer retVal = null;
            switch (this) {
            case LONGEST :
                retVal = new LocationFixer.Long(gc);
                break;
            case NEAREST :
                retVal = new LocationFixer.Standard(gc);
                break;
            case LIKELIEST :
                retVal = new LocationFixer.Smart(gc);
            }
            return retVal;
        }
    }

    /**
     * Location fixer biased toward longer proteins.
     */
    public static class Long extends LocationFixer {

        protected Long(int gc) {
            super(gc);
        }

        @Override
        protected int findStart(FLocation loc, String sequence) {
            // Run backward to the beginning of this ORF.
            int retVal = backToStop(loc, sequence);
            // Search forward for a start.  We don't allow anything inside the old location, but if
            // the old location was positioned on a start that is ok.
            while (retVal <= loc.getLeft() && ! this.isStart(sequence, retVal)) retVal += 3;
            // Return 0 if no start was found.
            if (retVal > loc.getLeft()) retVal = 0;
            return retVal;
        }

    }

    /**
     * Location fixer biased toward ATG starts.
     */
    public static class Smart extends LocationFixer {

        public Smart(int gc) {
            super(gc);
        }

        @Override
        protected int findStart(FLocation loc, String sequence) {
            // Run backward to the beginning of this ORF.
            int orfStart = backToStop(loc, sequence);
            // Find the first ATG in the ORF.
            int retVal = orfStart;
            while (retVal <= loc.getLeft() && ! sequence.substring(retVal - 1, retVal + 2).contentEquals("atg"))
                retVal += 3;
            // We didn't find an ATG.  Try again looking for any start.
            if (retVal > loc.getLeft()) {
                retVal -= 3;
                while (retVal >= orfStart && ! this.isStart(sequence, retVal)) retVal -= 3;
                // Return a 0 if no start was found.
                if (retVal < orfStart) retVal = 0;
            }
            return retVal;
        }

    }

    /**
     * Location fixer biased toward shorter proteins.
     */
    public static class Standard extends LocationFixer {

        protected Standard(int gc) {
            super(gc);
        }

        @Override
        protected int findStart(FLocation loc, String sequence) {
            // Find a start before we find a stop.
            int retVal = loc.getLeft();
            while (retVal > 0 && ! this.isStop(sequence, retVal) && ! this.isStart(sequence, retVal)) retVal -= 3;
            // If we failed, return 0.
            if (retVal <= 0 || this.isStop(sequence, retVal))
                retVal = 0;
            return retVal;
        }

    }

}
