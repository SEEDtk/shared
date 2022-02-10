/**
 *
 */
package org.theseed.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.theseed.genome.Genome;
import org.theseed.sequence.DnaKmers;
import org.theseed.sequence.GenomeKmers;
import org.theseed.sequence.ProteinKmers;
import org.theseed.sequence.SequenceKmers;

import org.junit.jupiter.api.Test;
/**
 * @author Bruce Parrello
 *
 */
public class KmerTests {

    /**
     * test protein kmers
     */
    @Test
    public void testProtKmers() {
        ProteinKmers.setKmerSize(10);
        String myProt1 = "MGMLVPLISKISDLSEEAKACVAACSSVEELDEVRGRYIGRAGALTALLA"; // 50 AA
        String myProt2 = "MDINLFKEELEELAKKAKHMLNETASKNDLEQVKVSLLGKKGLLTLQSAA";
        String myProt3 = "MDINLFKEELKHMLNETASKKGLLTLQSA"; // 30 AA
        ProteinKmers kmer1 = new ProteinKmers(myProt1);
        ProteinKmers kmer2 = new ProteinKmers(myProt2);
        ProteinKmers kmer3 = new ProteinKmers(myProt3);
        assertThat("Kmer1 has wrong protein.", kmer1.getProtein(), equalTo(myProt1));
        assertThat("Kmer1 has wrong count.", kmer1.size(), equalTo(41));
        assertThat("Kmer1/kmer3 wrong similarity.", kmer2.similarity(kmer3), equalTo(3));
        assertThat("Similarity not commutative.", kmer3.similarity(kmer2), equalTo(3));
        assertThat("Kmer1 too close to kmer2.", kmer1.distance(kmer2), equalTo(1.0));
        assertThat("Kmer1 too close to kmer3.", kmer2.distance(kmer3), closeTo(0.95, 0.005));
        for (String kmer : kmer3) {
            assertThat(kmer.length(), equalTo(10));
            assertThat(myProt3, containsString(kmer));
        }
        int[] hash1 = kmer1.hashSet(200);
        int[] hash2 = kmer2.hashSet(200);
        int[] hash3 = kmer3.hashSet(200);
        assertThat(hash1.length, lessThanOrEqualTo(200));
        assertThat(hash2.length, lessThanOrEqualTo(200));
        assertThat(hash3.length, lessThanOrEqualTo(200));
        validateSignature("hash1", hash1);
        validateSignature("hash2", hash2);
        validateSignature("hash3", hash3);
        assertThat(SequenceKmers.signatureDistance(hash1, hash2), closeTo(1.000, 0.05));
        assertThat(SequenceKmers.signatureDistance(hash2, hash3), closeTo(0.950, 0.05));
    }

    /**
     * test DNA kmers
     */
    @Test
    public void testDnaKmers() {
        DnaKmers.setKmerSize(12);
        String myDna1 = "ATGTTTGTTTTTCTTGTTTTATTGCCACTAGTCTCTATAACACTGCTGAC"; // 50 bp
        String myDna2 = "atgtttgtaatcgttgttttattgccagtagtagtagtcagcagtgttaa";
        String myDna2r = "TTAACACTGCTGACTACTACTACTGGCAATAAAACAACGATTACAAACAT"; // rev of 2
        String myDna3 = "ATGTTTGTTTTTCTTGTTTTATTGCCACTA"; // 30 bp
        DnaKmers kmer1 = new DnaKmers(myDna1);
        DnaKmers kmer2 = new DnaKmers(myDna2);
        DnaKmers kmer2r = new DnaKmers(myDna2r);
        DnaKmers kmer3 = new DnaKmers(myDna3);
        assertThat(myDna1.toLowerCase(), equalTo(kmer1.getDna()));
        assertThat(myDna2.toLowerCase(), equalTo(kmer2.getDna()));
        assertThat(myDna3.toLowerCase(), equalTo(kmer3.getDna()));
        assertThat(kmer1.equals(kmer2), equalTo(false));
        assertThat(kmer2.equals(kmer1), equalTo(false));
        assertThat(kmer2.equals(kmer2r), equalTo(true));
        assertThat(kmer2r.equals(kmer2), equalTo(true));
        assertThat(kmer2.hashCode(), equalTo(kmer2r.hashCode()));
        assertThat(kmer2.distance(kmer2r), equalTo(0.0));
        assertThat(kmer1.similarity(kmer2), equalTo(10));
        assertThat(kmer1.similarity(kmer2r), equalTo(10));
        assertThat(kmer1.similarity(kmer3), equalTo(38));
        assertThat(kmer3.similarity(kmer1), equalTo(38));
        assertThat(kmer1.distance(kmer2), closeTo(0.932, 0.001));
        assertThat(kmer1.distance(kmer3), closeTo(0.513, 0.001));
    }


    /**
     * test genome kmers
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    public void testGenomeKmers() throws IOException, NoSuchAlgorithmException {
        GenomeKmers.setKmerSize(24);
        Genome g = new Genome(new File("data/gto_test", "1005394.4.gto"));
        GenomeKmers kmer1 = new GenomeKmers(g);
        g = new Genome(new File("data/gto_test", "1313.7001.gto"));
        GenomeKmers kmer2 = new GenomeKmers(g);
        g = new Genome(new File("data/gto_test", "1313.7002.gto"));
        GenomeKmers kmer3 = new GenomeKmers(g);
        g = null;
        assertThat(kmer1.equals(kmer2), equalTo(false));
        assertThat(kmer2.equals(kmer3), equalTo(false));
        assertThat(kmer1.getGenomeId(), equalTo("1005394.4"));
        assertThat(kmer2.getGenomeId(), equalTo("1313.7001"));
        assertThat(kmer3.getGenomeName(), equalTo("Streptococcus pneumoniae P210824-213"));
        assertThat(kmer1.similarity(kmer2), equalTo(220));
        assertThat(kmer2.similarity(kmer3), equalTo(1554743));
        assertThat(kmer3.similarity(kmer2), equalTo(1554743));
        assertThat(kmer1.distance(kmer2), closeTo(1.000, 0.001));
        assertThat(kmer2.distance(kmer3), closeTo(0.386, 0.001));
        assertThat(kmer2.distance(kmer3), equalTo(kmer3.distance(kmer2)));
        int[] hash1 = kmer1.hashSet(2000);
        int[] hash2 = kmer2.hashSet(2000);
        int[] hash3 = kmer3.hashSet(2000);
        assertThat(hash1.length, lessThanOrEqualTo(2000));
        assertThat(hash2.length, lessThanOrEqualTo(2000));
        assertThat(hash3.length, lessThanOrEqualTo(2000));
        validateSignature("hash1", hash1);
        validateSignature("hash2", hash2);
        validateSignature("hash3", hash3);
        assertThat(SequenceKmers.signatureDistance(hash1, hash2), closeTo(1.000, 0.05));
        assertThat(SequenceKmers.signatureDistance(hash2, hash3), closeTo(0.386, 0.05));
    }

    /**
     * test signature distances
     */
    @Test
    public void testSignatures() {
        int[] hash1 = new int[] { 1, 3, 5, 7, 9};
        int[] hash2 = new int[] { 2, 3, 4, 6, 7, 8, 9};
        assertThat(SequenceKmers.signatureDistance(hash1, hash2), closeTo(0.666, 0.001));
        hash1 = new int[] { 3, 4, 5, 6, 7 };
        assertThat(SequenceKmers.signatureDistance(hash1, hash2), closeTo(0.500, 0.001));
        hash1 = new int[] { 0, 1, 2, 9, 10 };
        assertThat(SequenceKmers.signatureDistance(hash1, hash2), closeTo(0.800, 0.001));
    }

    /**
     * Verify that a signature array is sorted and all its elements are in range.
     */
    @Test
    private void validateSignature(String name, int[] hash) {
        for (int i = 1; i < hash.length; i++)
            assertThat(name + "[" + i + "]", hash[i-1], lessThan(hash[i]));
    }

    /**
     * Test randomness of hashing
     */
    @Test
    public void testHashAlgorithm() {
        int[] hashes = new int[] {	SequenceKmers.hashKmer("ABCDEFGH"), SequenceKmers.hashKmer("ABCDEFHG"), SequenceKmers.hashKmer("ACDEFGHB"),
                                    SequenceKmers.hashKmer("BCDEFGHA"), SequenceKmers.hashKmer("CDEFGHAB"), SequenceKmers.hashKmer("DEFGHABC") };
        int ups = 0;
        int downs = 0;
        for (int i = 1; i < hashes.length; i++) {
            assertThat(hashes[i-1] == hashes[i], equalTo(false));
            if (hashes[i-1] < hashes[i])
                ups++;
            else
                downs++;
        }
        assertThat(ups > 0 && downs > 0, equalTo(true));
    }

}
