/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestSsuRRna {

    private static final String RNA_STRING = "ttcttcacggagagtttgatcctggctcaggacgaacgctggcggcgcgcttaacacatgcaagtcgagcgagaaccgggacttcggtcctggggacagcggcgaacgggtgagtaacacgtgggtaatctgccctcgattctgggatagcccggggaaacccggattaataccggatagcctttcgagctccagggcccggaagaaaaggtagcttcggcctccgatcgaggatgagcccgcggtggattagcttgttggcggggtaacggcccaccaaggcgacgatccatagctggtctgagaggacgatcagccacactgggactgagacacggcccagactcctacgggaggcagcagtggggaatcttgcgcaatgggcgaaagcctgacgcagcgacgccgcgtgggggaagaaggccttcgggttgtaaacctctttcaggtgggacgaagccactcgggtgaatagcccagagggtgacggtaccaccagaagaagccccggctaactacgtgccagcagccgcggtaatacgtagggggcaagcgttgtccggatttattgggcgtaaagagcgtgtaggcggccaggtaggtcggttgtgaaaactggaggctcaaccttcagacgtcgaccgaaaccccctggctagagtccggaagaggagaatggaattcctggtgtagcggtgaaatgcgcagatatcaggaagaacacccgtggcgaaagcggttctctgggacggtactgacgctgagacgcgaaagcgtggggagcgaacaggattagataccctggtagtccacgccgtaaacgatgggtgctaggtgtggggggtgtcgactccccccgtgccgaagctaacgcattaagcaccccgcctggggagtacggccgcaaggctaaaactcaaaggaattgacgggggcccgcacaagcagcggagcatgtggtttaattcgacgcaacgcgaagaaccttaccaaggcttgacatgcacttgaaaagcgtagaaatacgttccctcttcggagcaagtgcacaggtggtgcatggctgtcgtcagctcgtgtcgtgagatgttgggttaagtcccgcaacgagcgcaacccctgtcctatgttgccagcgagtgatgtcggggactcataggagactgccggtgacaaatcggaggaaggtggggatgacgtcaagtcatcatgccccttatgtcttgggctacacacgtgctacattggccggtacaaagggctgcaaacctgcgagggtgagcgaatcccaaaaagccggtcccggttcggattggaggctgaaactcgcctccatgaaggcggagttgctagtaatcgcgaatcagcaacgtcgcggtgaatacgttcccgggccttgtacacaccgcccgtcacaccacgaaagtcggcaatacccgaagccggtgggctaacccgcaagggaggcagccgtcgaaggtagggtcgatgattggggtgaagtcgtaacaaggtagccgtagcggaagctgcggctggatcacctcctttcta";
    private static final String RNA_STRING_2 = "wgttcggtgacacacctaacggattaagcattccgcctggggagtacggtcgcaagattaaaactcaaaggaattgacgggggcccgcacaagcggtggagcatgtggtttaattcgaagcaacgcgcagaaccttaccaacccttgacatcgcaggacagcccgagagatcgggtcttctcgtaagagacctgtggacaggtgctgcatggctgtcgtcagctcgtgtcgtgagatgttcggttaagtccggcaacgagcgcaacccacacttccagttgccatcatttggttgggcactctggaagaactgccgatgataagtcggaggaaggtgtggatgacgtcaagtcctcatggcccttacgggttgggctacacacgtgctacaatggtggtgacagtgggttaatccccaaaagccatctcagttcggattggggtctgcaactcgaccccatgaagttggaatcgctagtaatcgcggaacagcatgccgcggtgaatacgttcccgggccttgtacacaccgcccgtcacaccatgggagttggttctacccgacggccgtgcgctaaccagcaatgggggcagcggaccacggtaggatcagcgactggggtgaagtcgtaacaaggtagccgtaggggaacctgcggctggatcacctcctttcta";

    @Test
    public void testGtos() throws IOException {
        File gFile = new File("data", "1002870.3.gto");
        Genome gto = new Genome(gFile);
        // We test twice because the get has a side effect.  The second time it will return the saved result.
        assertThat(gto.getSsuRRna(), equalTo(RNA_STRING));
        assertThat(gto.checkSsuRRna(), equalTo(true));
        assertThat(gto.getSsuRRna(), equalTo(RNA_STRING));
        gFile = new File("data", "gto.ser");
        gto.save(gFile);
        gto = new Genome(gFile);
        assertThat(gto.getSsuRRna(), equalTo(RNA_STRING));
        gFile = new File("data", "1262806.3.gto");
        gto = new Genome(gFile);
        assertThat(gto.getSsuRRna(), emptyString());
        gFile = new File("data", "34004.7.gto");
        gto = new Genome(gFile);
        String ssu = gto.getSsuRRna();
        assertThat(ssu.contains("nnnnn"), equalTo(false));
        assertThat(ssu, equalTo(RNA_STRING_2));
    }

}
