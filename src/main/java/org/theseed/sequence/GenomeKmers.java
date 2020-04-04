/**
 *
 */
package org.theseed.sequence;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

import org.theseed.genome.Contig;
import org.theseed.genome.Genome;

/**
 * This class tracks kmers for whole genomes.  In this case, we do not store the whole sequence; rather, we
 * keep the MD5 of the genome sequence along with the kmers.  The kmers are stored for both strands of each
 * contig.  (This is why the subclass does not have a sequence-retrieval method.)
 *
 *
 * @author Bruce Parrello
 *
 */
public class GenomeKmers extends SequenceKmers {

    // FIELDS

    /** current kmer size */
    private static int K = 24;

    /** ID of the genome represented */
    private String genomeId;

    /** name of the genome represented */
    private String genomeName;

    /**
     * Create a kmer object for a genome.  This can be used to determine genome
     * similarity.
     *
     * @param genome	genome to convert into kmers
     *
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public GenomeKmers(Genome genome) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // Store the genome MD5 as the identifying sequence.
        MD5Hex md5Computer = new MD5Hex();
        this.sequence = md5Computer.sequenceMD5(genome);
        // Save the genome ID and name.
        this.genomeId = genome.getId();
        this.genomeName = genome.getName();
        // Create the kmer hash.
        this.kmerSet = new HashSet<String>();
        // Process the contigs.
        for (Contig contig : genome.getContigs()) {
            String seq = contig.getSequence();
            int n = seq.length() - K;
            for (int i = 0; i < n; i++) {
                String plus = seq.substring(i, i + K);
                String minus = Contig.reverse(plus);
                if (plus.compareTo(minus) > 0) plus = minus;
                this.kmerSet.add(plus);
            }
        }
    }

    /**
     * Specify a new global genome kmer size.
     *
     * @param kSize	proposed new kmer size
     */
    public static void setKmerSize(int kSize) {
        K = kSize;
    }

    /**
     * @return the current genome kmer size
     */
    public static int kmerSize() {
        return K;
    }

    /**
     * @return the ID of the genome used to create this kmer set
     */
    public String getGenomeId() {
        return this.genomeId;
    }

    /**
     * @return the name of the genome used to create this kmer set
     */
    public String getGenomeName() {
        return this.genomeName;
    }

}
