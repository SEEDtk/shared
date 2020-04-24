/**
 *
 */
package org.theseed.sequence;

/**
 * This is the base class for sequence streams.
 *
 * @author Bruce Parrello
 *
 */
public interface SequenceStream extends Iterable<Sequence> {

    /**
     * @return TRUE if this is a protein stream, else FALSE (DNA stream)
     */
    public boolean isProtein();

}
