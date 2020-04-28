/**
 *
 */
package org.theseed.shared;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.theseed.genome.Contig;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.sequence.DnaDataStream;
import org.theseed.sequence.DnaInputStream;
import org.theseed.sequence.DnaStream;
import org.theseed.sequence.ProteinDataStream;
import org.theseed.sequence.ProteinInputStream;
import org.theseed.sequence.ProteinStream;
import org.theseed.sequence.Sequence;

import junit.framework.TestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test the DNA and protein streams
 *
 * @author parrello
 *
 */
public class StreamTest extends TestCase {

    public void testDnaStreams() throws IOException {
        Genome gto = new Genome(new File("src/test", "360106.5.gto"));
        File testFile = new File("src/test", "fasta.ser");
        gto.saveDna(testFile);
        DnaStream dnaStream = new DnaInputStream(testFile, 4);
        assertThat(dnaStream.getGeneticCode(), equalTo(4));
        int count = 0;
        List<Sequence> seqs = new ArrayList<Sequence>(gto.getContigCount());
        for (Sequence contigSeq : dnaStream) {
            String contigId = contigSeq.getLabel();
            seqs.add(contigSeq);
            Contig contig = gto.getContig(contigId);
            assertThat(contigId, contig, not(nullValue()));
            assertThat(contigId, contigSeq.getComment(), equalTo(contig.getDescription()));
            assertThat(contigId, contigSeq.getSequence(), equalTo(contig.getSequence()));
            count++;
        }
        assertThat(count, equalTo(gto.getContigCount()));
        count = 0;
        dnaStream = new DnaDataStream(seqs, 1);
        for (Sequence contigSeq : dnaStream) {
            assertThat(contigSeq, equalTo(seqs.get(count)));
            count++;
        }
        assertThat(count, equalTo(gto.getContigCount()));
        assertThat(dnaStream.getGeneticCode(), equalTo(1));
        DnaDataStream batchStream = new DnaDataStream(10, 11);
        assertThat(batchStream.size(), equalTo(0));
        assertThat(batchStream.getGeneticCode(), equalTo(11));
        for (Sequence contigSeq : seqs)
            batchStream.add(contigSeq);
        assertThat(seqs.size(), equalTo(gto.getContigCount()));
        int i = 0;
        for (Sequence contigSeq : batchStream) {
            assertThat(Integer.toString(i), contigSeq, equalTo(seqs.get(i)));
            i++;
        }
        batchStream.clear();
        assertThat(batchStream.size(), equalTo(0));
        batchStream.setGeneticCode(5);
        assertThat(batchStream.getGeneticCode(), equalTo(5));
    }

    public void testProteinStreams() throws IOException {
        Genome gto = new Genome(new File("src/test", "360106.5.gto"));
        File testFile = new File("src/test", "fasta.ser");
        gto.savePegs(testFile);
        ProteinStream protStream = new ProteinInputStream(testFile);
        int count = 0;
        int pegCount = gto.getPegs().size();
        List<Sequence> seqs = new ArrayList<Sequence>(pegCount);
        for (Sequence pegSeq : protStream) {
            String pegId = pegSeq.getLabel();
            Feature peg = gto.getFeature(pegId);
            seqs.add(pegSeq);
            assertThat(pegId, peg, not(nullValue()));
            assertThat(pegSeq.getComment(), equalTo(peg.getFunction()));
            assertThat(pegSeq.getSequence(), equalTo(peg.getProteinTranslation()));
            count++;
        }
        assertThat(count, equalTo(pegCount));
        count = 0;
        protStream = new ProteinDataStream(seqs);
        for (Sequence pegSeq : protStream) {
            assertThat(pegSeq, equalTo(seqs.get(count)));
            count++;
        }
        assertThat(count, equalTo(pegCount));
        ProteinDataStream batchStream = new ProteinDataStream(10);
        assertThat(batchStream.size(), equalTo(0));
        for (Sequence protSeq : seqs)
            batchStream.add(protSeq);
        assertThat(seqs.size(), equalTo(pegCount));
        int i = 0;
        for (Sequence pegSeq : batchStream) {
            assertThat(Integer.toString(i), pegSeq, equalTo(seqs.get(i)));
            i++;
        }
        batchStream.clear();
        assertThat(batchStream.size(), equalTo(0));
    }

}
