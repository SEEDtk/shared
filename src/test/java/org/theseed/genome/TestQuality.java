/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * @author Bruce Parrello
 *
 */
class TestQuality {

    @Test
    void test() throws IOException {
        Genome gto = new Genome(new File("data", "test.good.gto"));
        JsonObject quality = gto.getQuality();
        assertThat(quality.get("genome_quality"), equalTo("Good"));
        assertThat(quality.get("genome_status"), equalTo("WGS"));
        quality.put("ref_genome", "1234.56");
        File tempFile = new File("data", "test.good.ser");
        // Save and load to check the modification.
        gto.save(tempFile);
        gto = new Genome(tempFile);
        quality = gto.getQuality();
        assertThat(quality.get("genome_quality"), equalTo("Good"));
        assertThat(quality.get("genome_status"), equalTo("WGS"));
        assertThat(quality.get("ref_genome"), equalTo("1234.56"));
        // Now try an empty genome.
        gto = new Genome("5432.10");
        quality = gto.getQuality();
        quality.put("ref_genome", "1234.56");
        gto.save(tempFile);
        gto = new Genome(tempFile);
        quality = gto.getQuality();
        assertThat(quality.get("ref_genome"), equalTo("1234.56"));
    }

}
