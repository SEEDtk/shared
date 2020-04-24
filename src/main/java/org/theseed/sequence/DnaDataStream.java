/**
 *
 */
package org.theseed.sequence;

import java.util.Collection;
import java.util.Iterator;

/**
 * This is a DNA stream created from an in-memory list.
 *
 * @author Bruce Parrello
 *
 */
public class DnaDataStream extends DnaStream {

    // FIELDS
    private Collection<Sequence> sequences;

    /**
     * Create a DNA stream from an in-memory list.
     *
     * @param sequences		collection of sequences to use
     */
    public DnaDataStream(Collection<Sequence> sequences) {
        this.sequences = sequences;
    }

    @Override
    public Iterator<Sequence> iterator() {
        return sequences.iterator();
    }

}
