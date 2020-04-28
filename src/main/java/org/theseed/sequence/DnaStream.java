/**
 *
 */
package org.theseed.sequence;

import java.util.Iterator;

/**
 * This is a base class that provides a list of DNA sequences.
 *
 * @author Bruce Parrello
 *
 */
public abstract class DnaStream implements SequenceStream {

    // FIELDS
    private int geneticCode;

    @Override
    public abstract Iterator<Sequence> iterator();

    /**
     * Construct a DNA stream.
     */
    public DnaStream() {
        this.geneticCode = 11;
    }

    @Override
    public boolean isProtein() {
        return false;
    }

    /**
     * @return the genetic code of this stream's DNA
     */
    public int getGeneticCode() {
        return geneticCode;
    }

    /**
     * Specify the genetic code of this stream's DNA.
     *
     * @param geneticCode the genetic code to set
     */
    public void setGeneticCode(int geneticCode) {
        this.geneticCode = geneticCode;
    }

}
