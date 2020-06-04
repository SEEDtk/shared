/**
 *
 */
package org.theseed.locations;

import org.theseed.proteins.DnaTranslator;

/**
 * This object represents an ORF location in a genome.  It is associated with the contig sequence,
 * and the genetic code is known.  The sequence and location stored is in the forward direction.
 *
 *
 * @author Bruce Parrello
 *
 */
public class OrfLocation {

    // FIELDS
    /** contig sequence relevant to this ORF */
    private String sequence;
    /** location of the ORF inside the sequence, excluding the stops */
    private FLocation orfLoc;
    /** original location used to create the ORF */
    private FLocation originalLoc;
    /** DNA translator for finding starts and stops */
    private DnaTranslator xlator;
    /** current position in the ORF, for iteration */
    private int iterPos;
    /** direction of this ORF */
    private char dir;

    /**
     * Construct an ORF location.
     *
     * @param sequence		contig sequence containing the ORF
     * @param loc			location containing the ORF
     * @param gc			genetic code of the containing genome
     * @param dir			direction of this ORF in the original contig
     */
    protected OrfLocation(String sequence, FLocation loc, int gc, char dir) {
        this.sequence = sequence;
        this.originalLoc = loc;
        this.xlator = new DnaTranslator(gc);
        this.dir = dir;
        // Now we need to find the extent of the ORF.  Go backward to the first STOP.
        int newLeft = loc.getLeft();
        while (newLeft > 0 && !this. xlator.isStop(sequence, newLeft)) newLeft -= 3;
        // Push forward past the stop found.
        newLeft += 3;
        // Now go forward to the next STOP.
        int limit = sequence.length() - 2;
        int newRight = loc.getRight() - 2;
        while (newRight <= limit && ! this.xlator.isStop(sequence, newRight)) newRight += 3;
        // Back up so we are inside the stop.
        newRight--;
        // Now create the ORF location.
        this.orfLoc = new FLocation(this.originalLoc.getContigId(), newLeft, newRight);
        // Finally, clear the iterator.
        this.iterPos = this.orfLoc.getBegin() - 3;
    }

    /**
     * @return a character array of the neighborhood around a specified position in the sequence.
     *
     * @param pos		position (1-based) in the sequence containing this ORF
     * @param left		number of nucleotides to include upstream
     * @param right		number of nucleotides to include downstream
     */
    public char[] getNeighborhood(int pos, int left, int right) {
        char[] retVal = new char[left + right + 1];
        int l = pos - 1 - left;
        int r = pos - 1 + right;
        for (int i = l; i < r; i++) {
            if (i < 0 || i >= this.sequence.length())
                retVal[i] = '-';
            else
                retVal[i] = this.sequence.charAt(i);
        }
        return retVal;
    }

    /**
     * @return a character array of the neighborhood around the current position in the ORF

     * @param left		number of nucleotides to include upstream
     * @param right		number of nucleotides to include downstream
     */
    public char[] getNeighborhood(int left, int right) {
        return this.getNeighborhood(this.iterPos, left, right);
    }

    /**
     * @return TRUE if the specified position is a potential start codon, else FALSE
     *
     * @param pos	position (1-based) in the sequence to check
     */
    public boolean isStart(int pos) {
        return this.xlator.isStart(this.sequence, pos);
    }

    /**
     * @return TRUE if the current position is a potential start codon, else FALSE
     *
     * @param pos	position (1-based) in the sequence to check
     */
    public boolean isStart() {
        return this.isStart(this.iterPos);
    }

    /**
     * @return the original start position from the location that created this object
     */
    public int originalBegin() {
        return this.originalLoc.getBegin();
    }

    /**
     * Reset this object's current position so we can begin iteration.
     */
    public void reset() {
        this.iterPos = this.orfLoc.getBegin() - 3;
    }

    /**
     * @return TRUE if we are not at the end of the ORF
     */
    public boolean hasNext() {
        return this.iterPos + 3 < this.orfLoc.getEnd();
    }

    /**
     * @return the next codon position in this ORF, or 0 if there is none
     */
    public int next() {
        this.iterPos += 3;
        int retVal = this.iterPos;
        if (retVal > this.orfLoc.getEnd()) retVal = 0;
        return retVal;
    }

    /**
     * @return the name of this ORF
     */
    public String toString() {
        // Compute the endpoint of the stop.  This depends on the direction.
        int stopLoc = orfStop();
        return String.format("%s%c%d", this.orfLoc.getContigId(), this.dir, stopLoc);
    }

    /**
     * @return a peg translation of the sequence starting at the specified position
     *
     * @param pos	position to start the translation
     */
    public String pegTranslate(int pos) {
        return this.xlator.pegTranslate(this.sequence, pos, this.orfLoc.getEnd() + 1 - pos);
    }

    /**
     * @return the stop location of this ORF on the original contig
     */
    public int orfStop() {
        int stopLoc = (this.dir == '-' ? this.sequence.length() - this.orfLoc.getEnd() - 2 : this.orfLoc.getEnd() + 3);
        return stopLoc;
    }

}

