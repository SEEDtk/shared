/**
 *
 */
package org.theseed.genome.core;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.theseed.sequence.FastaInputStream;
import org.theseed.sequence.Sequence;

/**
 * This class manages a list of sequences loaded from a FASTA file.  The sequences are sorted by length and accessible
 * by ID.  In addition to finding a specific sequence, we want to find other sequences that are similar in size.  A
 * blacklist is provided to prevent this last operation from finding sequences we wish to exclude.
 *
 * @author Bruce Parrello
 *
 */
public class PegList {

    // FIELDS
    /** map of feature IDs to sequences */
    private Map<String, Sequence> idMap;
    /** sequences sorted by length */
    private NavigableSet<Sequence> seqList;
    /** blacklist */
    private Set<Sequence> blackList;


    /**
     * Subclass for comparing sequences by length.
     */
    public static class SeqSorter implements Comparator<Sequence> {

        @Override
        public int compare(Sequence arg0, Sequence arg1) {
            int retVal = arg0.length() - arg1.length();
            if (retVal == 0) {
                retVal = arg0.getLabel().compareTo(arg1.getLabel());
            }
            return retVal;
        }

    }

    /**
     * Construct a peg list from a FASTA file.
     *
     * @param fastaFile		file containing the peg sequences
     * @throws IOException
     */
    public PegList(File fastaFile) throws IOException {
        FastaInputStream pegFile = new FastaInputStream(fastaFile);
        // Create the two containers.
        this.idMap = new HashMap<String, Sequence>();
        this.seqList = new TreeSet<Sequence>(new SeqSorter());
        this.blackList = new HashSet<Sequence>();
        // Loop through the sequences, adding them to the containers.
        for (Sequence seq : pegFile) {
            this.idMap.put(seq.getLabel(), seq);
            this.seqList.add(seq);
        }
        pegFile.close();
    }

    /**
     * @return the sequence with the specified ID, or NULL if no such sequence exists
     *
     * @param id	sequence ID
     */
    public Sequence get(String id) {
        Sequence retVal = this.idMap.get(id);
        return retVal;
    }

    /**
     * Add a sequence to the search blacklist.
     *
     * @param seq	sequence to add
     */
    public void suppress(Sequence seq) {
        this.blackList.add(seq);
    }
    /**
     * Find the sequences close in length to the specified sequence.
     *
     * @param seq		target sequence
     * @param count		number of sequences to return
     * @param buffer	collection into which the sequences found should be placed
     */
    public void findClose(Sequence seq, int count, Collection<Sequence> buffer) {
        int remaining = count;
        // Move one sequence in each direction.  Note that the comparator falls back to an ID compare, so
        // sequences the same size but with a different ID will be found.
        Sequence bigger = this.getBigger(seq);
        Sequence smaller = this.getSmaller(seq);
        // Our distance from this determines which sequence we pick.
        int targetLength = seq.length();
        // Loop through the sequences, adding one each time.
        while ((bigger != null || smaller != null) && remaining > 0) {
            // Pick the closer item.
            Sequence pick;
            int bigDist = this.distance(bigger, targetLength);
            int smallDist = this.distance(smaller, targetLength);
            if (bigDist < smallDist) {
                pick = bigger;
                bigger = this.getBigger(bigger);
            } else {
                pick = smaller;
                smaller = this.getSmaller(smaller);
            }
            buffer.add(pick);
            remaining--;
        }
    }

    /**
     * @return the difference between the target length and the length of the specified sequence
     *
     * @param seq		sequence whose distance is desired
     * @param targetLength	baseline length
     */
    protected int distance(Sequence seq, int targetLength) {
        int retVal = Integer.MAX_VALUE;
        if (seq != null) retVal = Math.abs(seq.length() - targetLength);
        return retVal;
    }

    /**
     * Get the next eligible longer sequence.
     *
     * @param seq	current sequence
     * @return the next longer sequence that is not in the blacklist
     */
    protected Sequence getBigger(Sequence seq) {
        Sequence retVal = seq;
        boolean done = false;
        while (retVal != null && ! done) {
            retVal = this.seqList.higher(retVal);
            done = ! this.blackList.contains(retVal);
        }
        return retVal;
    }

    /**
     * Get the next eligible shorter sequence.
     *
     * @param seq	current sequence
     * @return the next shorter sequence that is not in the blacklist
     */
    protected Sequence getSmaller(Sequence seq) {
        Sequence retVal = seq;
        boolean done = false;
        while (retVal != null && ! done) {
            retVal = this.seqList.lower(retVal);
            done = ! this.blackList.contains(retVal);
        }
        return retVal;
    }


}
