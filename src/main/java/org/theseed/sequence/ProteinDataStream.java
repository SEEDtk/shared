/**
 *
 */
package org.theseed.sequence;

import java.util.Collection;
import java.util.Iterator;

/**
 * This is a protein stream created from an in-memory list.
 *
 * @author Bruce Parrello
 *
 */
public class ProteinDataStream extends ProteinStream {

    // FIELDS
    private Collection<Sequence> sequences;

    /**
     * Create a protein stream from an in-memory list.
     *
     * @param sequences		collection of sequences to use
     */
    public ProteinDataStream(Collection<Sequence> sequences) {
        this.sequences = sequences;
    }

    @Override
    public Iterator<Sequence> iterator() {
        return sequences.iterator();
    }

}
