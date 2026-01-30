/**
 *
 */
package org.theseed.locations;

import org.apache.commons.lang3.Strings;
import org.theseed.proteins.CodonSet;
import org.theseed.proteins.DnaTranslator;

/**
 * This object represents a location on a contig as a forward location associated with a DNA sequence.
 * Methods are provided to traverse the sequence and search for specific codons.
 *
 * @author Bruce Parrello
 *
 */
public abstract class SequenceLocation {

    // FIELDS
    /** relevant DNA sequence */
    private final String sequence;
    /** location in the sequence */
    private final FLocation loc;
    /** DNA translator */
    private final DnaTranslator xlator;
    /** codon set for iteration */
    private CodonSet codons;
    /** current position in the sequence, for iteration */
    private int iterPos;

    /**
     * Construct a new sequence location.
     *
     * @param sequence	DNA sequence of interest
     * @param loc		location in the DNA sequence
     * @param gc		genetic code for DNA translation
     */
    protected SequenceLocation(String sequence, FLocation loc, int gc) {
        this.sequence = sequence;
        this.loc = loc;
        this.codons = null;
        this.iterPos = 0;
        this.xlator = new DnaTranslator(gc);
    }

    /**
     * @return the position (1-based) of the first codon in this region belonging to the specified codon set,
     * 			or 0 if there is none
     *
     * @param codons	set of codons to find
     * @param frame		frame to search (1, 2, or 3)
     */
    public int first(CodonSet codons, int frame) {
        this.codons = codons;
        // Get the first position in this frame.
        this.iterPos = loc.getLeft() + frame - 1;
        // Search for a codon we want.
        this.findCodon();
        return this.iterPos;
    }

    /**
     * Search for the next codon in the current codon set.
     *
     * @param limit		last possible position where a codon can be found
     */
    protected void findCodon() {
        // Compute the last legal frame.
        int limit = loc.getRight() - 2;
        // Search for a codon we want.
        while (this.iterPos <= limit && ! this.codons.contains(this.sequence, this.iterPos))
            this.iterPos += 3;
        if (this.iterPos > limit) this.iterPos = 0;
    }

    /**
     * @return the position (1-based) of the next codon in this region belonging to the specified codon set,
     * 			or 0 if there is none
     */
    public int next() {
        if (this.codons == null)
            throw new RuntimeException("Cannot call next() before first().");
        // Only proceed if we have not already hit the end.
        if (this.iterPos > 0) {
            this.iterPos += 3;
            this.findCodon();
        }
        return this.iterPos;
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
        int r = pos + right;
        for (int i = l; i < r; i++) {
            int idx = i - l;
            if (i < 0 || i >= this.sequence.length())
                retVal[idx] = '-';
            else
                retVal[idx] = this.sequence.charAt(i);
        }
        return retVal;
    }

    /**
     * @return a character array of the neighborhood around the current position in the sequence

     * @param left		number of nucleotides to include upstream
     * @param right		number of nucleotides to include downstream
     */
    public char[] getNeighborhood(int left, int right) {
        return this.getNeighborhood(this.iterPos, left, right);
    }

    /**
     * @return a peg translation of the sequence starting at the specified position
     *
     * @param pos	position in the full sequence to start the translation
     */
    public String pegTranslate(int pos) {
        String retVal = this.xlator.pegTranslate(this.sequence, pos, this.loc.getEnd() + 1 - pos);
        // Note we chop off a stop codon if it is at the end.
        return Strings.CS.removeEnd(retVal, "*");
    }

    /**
     * @return the contig ID of this location
     */
    public String getContigId() {
        return this.loc.getContigId();
    }

    /**
     * @return the length of this location's containing sequence
     */
    public int getSeqLen() {
        return this.sequence.length();
    }

    /**
     * @return the current position in the sequence
     */
    public int getPosition() {
        return this.iterPos;
    }

    /**
     * @return the current position in the original sequence
     */
    public abstract int getRealPosition();

    /**
     * Set the current position to the specified position relative to the start.
     *
     * @param pos	position to set, with 1 being the first position in the location
     *
     * @return the position in the sequence (1-based) that was set
     */
    public int setRelativePosition(int pos) {
        this.iterPos = this.loc.getLeft() + pos - 1;
        return this.iterPos;
    }

    /**
     * @return the end position in the sequence of this location
     */
    public int getEndPosition() {
        return this.loc.getEnd();
    }

    /**
     * @return the real genome location corresponding to the specified left and right positions in this sequence location
     *
     * @param left		position (1-based) of the left edge
     * @param right		position (1-based) of the right edge
     */
    public abstract Location realLocation(int left, int right);

    /**
     * @return a string identifying the current position in this sequence location as it appears in the genome
     */
    public abstract String positionString();

    /**
     * @return a string identifying the last position in this sequence location as it appears in the genome
     */
    public abstract String endString();

    /**
     * @return the beginning of the specified location relative to this sequence
     *
     * @loc		location of interest
     */
    public abstract int getBegin(Location loc);

    /**
     * @return TRUE if the specified position contains a codon from the specified set
     *
     * @param codons	codon set of interest
     * @param pos		position (1-based) to check
     */
    public boolean isCodon(CodonSet codons, int pos) {
        return codons.contains(this.sequence, pos);
    }


}
