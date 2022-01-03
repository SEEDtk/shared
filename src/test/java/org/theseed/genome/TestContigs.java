/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.theseed.sequence.Sequence;

/**
 * This will test the various hammer-related methods for contigs.
 *
 * @author Bruce Parrello
 *
 */
public class TestContigs {

    @Test
    public void testSequences() throws IOException {
        File gtoFile = new File("data", "1002870.3.gto");
        Genome genome = new Genome(gtoFile);
        List<Sequence> sequences = genome.getSequences();
        assertThat(sequences.size(), equalTo(genome.getContigCount()));
        for (Sequence seq : sequences) {
            String contigId = seq.getLabel();
            Contig contig = genome.getContig(contigId);
            assertThat(contigId, contig, not(nullValue()));
            assertThat(contigId, contig.getId(), equalTo(seq.getLabel()));
            assertThat(contigId, contig.getSequence(), equalTo(seq.getSequence()));
        }
    }

    @Test
    public void testCleaning() {
        String cleanDna = "tcccacgcttgcggTGTcaaccgccgcagcgacaagaggaggccgcgcgggatgctgacagaggaggagttcgtggaagcgagtgctctacaccgccgcgggtggtcgatctcggcgatcgcccgccacctcggccgcgaccgtaagacgatccgcgcctacctgcgcggggagcggaggccgggggtgcgtcgccgcaccgtccccgaccacttcgcccccttcgagccgtatgcgcgcgagcgcctcgccgaggacccgcacctgtggggctcggcgctctacgacgaggtggtggccctcggctacccgcgcagctacgtcaccttcgtccgcgagctccgccggcgcgggctgcggccgcgctgcgaggcctgcgccgtggtcaagggccgcccgacggtcgagatcgcccatcccccgggggaggagatccagtgggactggctcgagctgccgggcgctccctggggcggcgaggcgcacctgctggtcgggacgctctcccactcgggcacgtttcgggcggtcttcgccgagggcgaggatcaggcgcacctggtcggggcgctcgacggggtgctccgccgcctgggcgggacggcccgacgctggcgcgtcgaccggatggcgacggtctgcgatcgctcgacgggtcgtctccttccgagcttcgccgccgtcgcccgctactacggcgtctccgtcgacgtctgcgcgcgctaccgcgccaatcgcaagggcgcggtcgagtcgcgcaaccacttcctcgcccagcgcttctggcggacgctgcgggctcagacgccgcaggaggcgcagtccaagctcgaccgcttctcggagacgatcggcgaccggcgccgccgtggcgggcagagcgtggccgagctcgccgcggccgagcggctgctgccgctgccggtgctcccctacccggcgacgctcgaggtcgagcgggtcgtctccgccgcctgtctcgtctcctacgagggcaaccgctactcggtcccgccggggctgcacggacagcgggtgagcgtgcgccgccggctgggaagcgagcagatcgagctcgtctccgccgccggctcggtcgtcgcctcgcaccgcctcgccccggccggcgcgggggctctgcgacgacacgccgagcaccgggtcgcgctcgagcaggtcgtgctgcagagcctgacgagcggccgaccctgccggcgcaagcgaaaccggccgccgggcgaggcagcgctcgccgccgcggccgcgctggtcggcgcggagtccgacgtctcggtctcgctcgaggcctacgcccgctacgcggaggcatcgcgatgaccgccgccgagagccgctaccagaagctgcggcagcacctgcactacctgcgcctggaggccgccgccgaggcgctcccgggcgagctcgagcgcgcccgcaagcaaaagctctcgcacaccgccttcctcgagcggctgctccagatcgaggtcgaggcgaccgaggcccgccgcctcgccgggcggctgcgtttcgccagcctgcccgcgccctggaccttggacgacttcgacttcgacgcccagcccgcgctcgaccgaaagctcgtcgacgagctcgcatcgctgcgcttcgtcgaggaggccgccaacctcttgctcgtcggcccacccggggtgggcaagacgatgctcgcggtctgtctcgcccggcagacggtcgaggcgggctaccgcgtctactacacctccgccgccgacctcgccgcccgctgccaccgggccgcgctcgagggccgctgggcaaccgccatgcgcttctactgcggccccgcgctg";
        List<String> cleaned = Contig.cleanParts(cleanDna);
        assertThat(cleaned.size(), equalTo(1));
        assertThat(cleaned.get(0), equalTo(cleanDna.toLowerCase()));
        String dirtyDna = "xacgtacgtacgtacgtacgtxyzwtutututunnnnnnnancnbbgkkk";
        cleaned = Contig.cleanParts(dirtyDna);
        assertThat(cleaned, contains("acgtacgtacgtacgtacgt", "tutututu", "a", "c", "g"));
        dirtyDna = "ACGtnggggntttt";
        cleaned = Contig.cleanParts(dirtyDna);
        assertThat(cleaned, contains("acgt", "gggg", "tttt"));
    }
}
