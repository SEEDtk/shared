/**
 *
 */
package org.theseed.sequence;

import java.util.ArrayList;
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
     * Create a DNA stream from an in-memory list with a specified genetic code.
     *
     * @param sequences		collection of sequences to use
     * @param gc			genetic code of the DNA in the sequeneces
     */
    public DnaDataStream(Collection<Sequence> sequences, int gc) {
        this.sequences = sequences;
        this.setGeneticCode(gc);
    }

    /**
     * Create an empty DNA data stream for batches of a specific size.
     *
     * @param batchSize		number of sequences per batch
     * @param gc			genetic code of the DNA in the sequeneces
     */
    public DnaDataStream(int batchSize, int gc) {
        this.sequences = new ArrayList<Sequence>(batchSize);
    }

    @Override
    public Iterator<Sequence> iterator() {
        return sequences.iterator();
    }

    /**
     * Clear the data stream.
     */
    public void clear() {
        this.sequences.clear();
    }

    /**
     * Add a sequence to the stream.
     *
     * @param seq	sequence to add
     */
    public DnaDataStream add(Sequence seq) {
        this.sequences.add(seq);
        return this;
    }

    /**
     * @return the number of sequences in the stream
     */
    public int size() {
        return this.sequences.size();
    }

}
