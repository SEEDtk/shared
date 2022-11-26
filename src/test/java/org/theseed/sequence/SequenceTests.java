/**
 *
 */
package org.theseed.sequence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.theseed.genome.Contig;
import org.theseed.genome.Genome;

/**
 * @author Bruce Parrello
 *
 */
class SequenceTests {

    @Test
    void testReversing() throws IOException {
        File gtoFile = new File("data", "34004.7.gto");
        Genome gto = new Genome(gtoFile);
        File fastaFile = new File("data", "fasta.ser");
        List<Contig> contigs = new ArrayList<Contig>(gto.getContigs());
        try (FastaOutputStream fastaOut = new FastaOutputStream(fastaFile)) {
            // Create sequences and reverse them.
            List<Sequence> seqList = contigs.stream().map(x -> new Sequence(x.getId(), x.getDescription(), x.getSequence()))
                    .map(x -> x.reverse()).collect(Collectors.toList());
            // Write out the reversed sequences.
            fastaOut.write(seqList);
        }
        // Read the reversed sequences back in and validate them.
        try (FastaInputStream fastaIn = new FastaInputStream(fastaFile)) {
            var iter = contigs.iterator();
            int count = 0;
            for (Sequence seq : fastaIn) {
                count++;
                String message = String.format("Sequence %d: %s", count, seq.getLabel());
                assertThat(message, iter.hasNext());
                Contig contig = iter.next();
                assertThat(message, seq.getLabel(), equalTo(contig.getId()));
                assertThat(message, seq.getComment(), equalTo(contig.getDescription()));
                assertThat(message, seq.getSequence(), equalTo(Contig.reverse(contig.getSequence())));
            }
        }
    }

}
