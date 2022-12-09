/**
 *
 */
package org.theseed.sequence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Contig;
import org.theseed.genome.Genome;
import org.theseed.genome.GenomeDirectory;

/**
 * @author Bruce Parrello
 *
 */
class TestDiscriminators {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TestDiscriminators.class);

    @Test
    void testProtDiscrimDb() throws IOException {
        // Build a protein discriminator database.
        ProteinDiscriminatingKmerDb db = new ProteinDiscriminatingKmerDb(8);
        File genomeDir = new File("data", "kmer_test");
        GenomeDirectory genomes = new GenomeDirectory(genomeDir);
        for (Genome genome : genomes) {
            db.addGenome(genome);
            var counts = db.getGroupCounts();
            log.info("{} groups in database.", counts.size());
        }
        db.finalize();
        var counts = db.getGroupCounts();
        log.info("{} groups in final database with {} total kmers.", counts.size(), counts.getTotal());
        // Now loop back through the genomes getting the counts.
        for (Genome genome : genomes) {
            int gc = genome.getGeneticCode();
            for (Contig contig : genome.getContigs()) {
                counts = db.countHits(contig.getSequence(), gc);
                var found = counts.getBestEntry();
                if (found != null && found.getCount() > 10) {
                    assertThat(contig.getId(), found.getKey(), equalTo(genome.getId()));
                }
            }
        }
    }

    @Test
    void testDnaDiscrimDb() throws IOException {
        // Build a DNA discriminator database.
        DnaDiscriminatingKmerDb db = new DnaDiscriminatingKmerDb(20);
        File genomeDir = new File("data", "gto_test");
        GenomeDirectory genomes = new GenomeDirectory(genomeDir);
        for (Genome genome : genomes)
            db.addGenome(genome);
        db.finalize();
        // Now loop back through the genomes getting the counts.
        for (Genome genome : genomes) {
            for (Contig contig : genome.getContigs()) {
                var counts = db.countHits(contig.getSequence());
                assertThat(genome.toString(), counts.size(), not(greaterThan(1)));
                if (counts.size() > 0)
                    assertThat(contig.getId(), counts.getBestEntry().getKey(), equalTo(genome.getId()));
            }
        }

    }

}
