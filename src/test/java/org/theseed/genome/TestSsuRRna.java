/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestSsuRRna {

    private static final String RNA_STRING = "ttcttcacggagagtttgatcctggctcaggacgaacgctggcggcgcgcttaacacatgcaagtcgagcgagaaccgggacttcggtcctggggacagcggcgaacgggtgagtaacacgtgggtaatctgccctcgattctgggatagcccggggaaacccggattaataccggatagcctttcgagctccagggcccggaagaaaaggtagcttcggcctccgatcgaggatgagcccgcggtggattagcttgttggcggggtaacggcccaccaaggcgacgatccatagctggtctgagaggacgatcagccacactgggactgagacacggcccagactcctacgggaggcagcagtggggaatcttgcgcaatgggcgaaagcctgacgcagcgacgccgcgtgggggaagaaggccttcgggttgtaaacctctttcaggtgggacgaagccactcgggtgaatagcccagagggtgacggtaccaccagaagaagccccggctaactacgtgccagcagccgcggtaatacgtagggggcaagcgttgtccggatttattgggcgtaaagagcgtgtaggcggccaggtaggtcggttgtgaaaactggaggctcaaccttcagacgtcgaccgaaaccccctggctagagtccggaagaggagaatggaattcctggtgtagcggtgaaatgcgcagatatcaggaagaacacccgtggcgaaagcggttctctgggacggtactgacgctgagacgcgaaagcgtggggagcgaacaggattagataccctggtagtccacgccgtaaacgatgggtgctaggtgtggggggtgtcgactccccccgtgccgaagctaacgcattaagcaccccgcctggggagtacggccgcaaggctaaaactcaaaggaattgacgggggcccgcacaagcagcggagcatgtggtttaattcgacgcaacgcgaagaaccttaccaaggcttgacatgcacttgaaaagcgtagaaatacgttccctcttcggagcaagtgcacaggtggtgcatggctgtcgtcagctcgtgtcgtgagatgttgggttaagtcccgcaacgagcgcaacccctgtcctatgttgccagcgagtgatgtcggggactcataggagactgccggtgacaaatcggaggaaggtggggatgacgtcaagtcatcatgccccttatgtcttgggctacacacgtgctacattggccggtacaaagggctgcaaacctgcgagggtgagcgaatcccaaaaagccggtcccggttcggattggaggctgaaactcgcctccatgaaggcggagttgctagtaatcgcgaatcagcaacgtcgcggtgaatacgttcccgggccttgtacacaccgcccgtcacaccacgaaagtcggcaatacccgaagccggtgggctaacccgcaagggaggcagccgtcgaaggtagggtcgatgattggggtgaagtcgtaacaaggtagccgtagcggaagctgcggctggatcacctcctttcta";

    @Test
    public void testGtos() throws IOException {
        File gFile = new File("data", "1002870.3.gto");
        Genome gto = new Genome(gFile);
        assertThat(gto.getSsuRRna(), equalTo(RNA_STRING));
        assertThat(gto.getSsuRRna(), equalTo(RNA_STRING));
        gFile = new File("data", "gto.ser");
        gto.save(gFile);
        gto = new Genome(gFile);
        assertThat(gto.getSsuRRna(), equalTo(RNA_STRING));
        gFile = new File("data", "1262806.3.gto");
        gto = new Genome(gFile);
        assertThat(gto.getSsuRRna(), emptyString());
    }

}
