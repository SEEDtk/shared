/**
 *
 */
package org.theseed.sequence;

/**
 * This is a simple interface that allows use of a labeled sequence.
 *
 * @author Bruce Parrello
 *
 */
public interface ISequence {

    /**
     * @return the sequence string
     */
    public String getSequence();

    /**
     * @return the label
     */
    public String getLabel();

}
