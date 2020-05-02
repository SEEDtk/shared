/**
 *
 */
package org.theseed.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.genome.Genome;
import org.theseed.proteins.DnaTranslator;

import junit.framework.TestCase;

/**
 * @author Bruce Parrello
 *
 */
public class TestDnaTranslate extends TestCase {

    /**
     * test protein translation
     */
    public void testDnaTranslator() {
        // Start with GC 4.
        DnaTranslator xlator = new DnaTranslator(4);
        String dna1 = "atggctataccaccggaggtgcactcgggcctgttgagcgccgggtgcggtccgggatca" +
                "ttgcttgttgccgcgcagcagtggcaagaacttagtgatcagtacgcactcgcatgcgcc" +
                "gagttgggccaattgttgggcgaggttcaggccagcagctggcagggaaccgccgccacc" +
                "cagtacgtggctgcccatggcccctatctggcctggcttgagcaaaccgcgatcaacagc" +
                "gccgtcaccgccgcacagcacgtagcggctgccgctgcctactgcagcgccctggccgcg" +
                "atgcccaccccagcagagctggccgccaaccacgccattcatggcgttctgatcgccacc" +
                "aacttcttcgggatcaacaccgttccgatcgcgctcaacgaagccgattatgtccgcatg" +
                "tggctgcaagccgccgacaccatggccgcctaccaggccgtcgccgatgcggccacggtg" +
                "gccgtaccgtccacccaaccggcgccaccgatccgcgcgcccggcggcgatgccgcagat" +
                "acctggctagacgtattgagttcaattggtcagctcatccgggatatcttggatttcatt" +
                "gccaacccgtacaagtattttctggagtttttcgagcaattcggcttcagcccggccgta" +
                "acggtcgtccttgcccttgttgccctgcagctgtacgactttctttggtatccctattac" +
                "gcctcgtacggcctgctcctgcttccgttcttcactcccaccttgagcgcgttgaccgcc" +
                "ctaagcgcgctgatccatttgctgaacctgcccccggctggactgcttcctatcgccgca" +
                "gcgctcggtcccggcgaccaatggggcgcaaacttggctgtggctgtcacgccggccacg" +
                "gcggccgtgcccggcggaagcccgcccaccagcaaccccgcgcccgccgctcccagctcg" +
                "aactcggttggcagcgcttcggctgcacccggcatcagctatgccgtgccaggcctggcg" +
                "ccacccggggttagctctggccctaaagccggcaccaaatcacctgacaccgccgccgac" +
                "acccttgcaaccgcgggcgcagcacgaccgggcctcgcccgagcccaccgaagaaagcgc" +
                "agcgaaagcggcgtcgggatacgcggttaccgcgacgaatttttggacgcgaccgccacg" +
                "gtggacgccgctacggatgtgcccgctcccgccaacgcggctggcagtcaaggtgccggc" +
                "actctcggctttgccggtaccgcaccgacaaccagcggcgccgcggccggaatggttcaa" +
                "ctgtcgtcgcacagcacaagcactacagtcccgttgctgcccactacctggacaaccgac" +
                "gccgaacaatga";
        String prot1 = "MAIPPEVHSGLLSAGCGPGSLLVAAQQWQELSDQYALACAELGQLLGEVQASSWQGTAAT" +
                "QYVAAHGPYLAWLEQTAINSAVTAAQHVAAAAAYCSALAAMPTPAELAANHAIHGVLIAT" +
                "NFFGINTVPIALNEADYVRMWLQAADTMAAYQAVADAATVAVPSTQPAPPIRAPGGDAAD" +
                "TWLDVLSSIGQLIRDILDFIANPYKYFLEFFEQFGFSPAVTVVLALVALQLYDFLWYPYY" +
                "ASYGLLLLPFFTPTLSALTALSALIHLLNLPPAGLLPIAAALGPGDQWGANLAVAVTPAT" +
                "AAVPGGSPPTSNPAPAAPSSNSVGSASAAPGISYAVPGLAPPGVSSGPKAGTKSPDTAAD" +
                "TLATAGAARPGLARAHRRKRSESGVGIRGYRDEFLDATATVDAATDVPAPANAAGSQGAG" +
                "TLGFAGTAPTTSGAAAGMVQLSSHSTSTTVPLLPTTWTTDAEQW";
        assertThat(xlator.pegTranslate(dna1, 1, dna1.length()), equalTo(prot1));
        assertThat(xlator.getGeneticCode(), equalTo(4));
        // Verify that we can switch to 11.
        xlator = new DnaTranslator(11);
        assertThat(xlator.getGeneticCode(), equalTo(11));
        String prot2 = StringUtils.chop(prot1) + "*";
        assertThat(xlator.pegTranslate(dna1, 1, dna1.length()), equalTo(prot2));
        assertTrue(xlator.isStart(dna1, 1));
        assertTrue(xlator.isStop(dna1, dna1.length() - 2));
        assertFalse(xlator.isStart(dna1, 2));
        assertFalse(xlator.isStop(dna1, 4));
        // Try a bad character.
        dna1 = "angcggatggcttggtcgaccgtgggggcgcacatcgggcagcgaccgggccaggccgca" +
                "taccagatgctggagacccgccgccgtggcagcgtgctgcgactcggcaatcccaagcgg" +
                "ggcatcgtcagccgccgccggtatcacaccctgaggggcgcccgaccaacccgcccgccg" +
                "ccgccgatgctcggctga";
        int len = dna1.length();
        prot1 = "XRMAWSTVGAHIGQRPGQAAYQMLETRRRGSVLRLGNPKRGIVSRRRYHTLRGARPTRPPPPMLG*";
        assertThat(xlator.pegTranslate(dna1, 1, len), equalTo(prot1));
        // Check the substring pathologies.
        dna1 = "abcd" + dna1;
        assertThat(xlator.pegTranslate(dna1, 5, len), equalTo(prot1));
        assertThat(xlator.pegTranslate(dna1, 5, len + 2), equalTo(prot1));
    }

    public void testOperon() throws IOException {
        Genome myGto = new Genome(new File("src/test/gto_test", "1313.7016.gto"));
        String myDna = myGto.getContig("1313.7016.con.0058").getSequence().substring(3900, 5920);
        DnaTranslator xlate = new DnaTranslator(11);
        List<String> prots = xlate.operonFrom(myDna);
        assertThat(prots, containsInAnyOrder(
                "MRMNGGFQMKEFYKKRFALTDGGARNLSKATLASFFVYCINMLPAILLMIFAQEVLENMGKSNGFYIVFSVLTLIAMYILLSIEYDKLYNTTYQESADLRIRTAENLSKLPLSYFSKHDISDISQTIMADIEGIEHAMSHSIPKVGGMVLFFPLISVMMPAGNVKMGLAVIIPSILNFIFIPLSKKYQVNGQNRYYDVLRKNSESFQENIEMQMEIKAYNLSKDIKDDLYKKMEDSERVHLKAEVTTILTLSISSIFSFISLAVVIFVGVNLIINKEINSLYLIGYLLAAMKIKDSLDASKEGLMEIFYLSPKIERLKEIQNQDLQEGDDYSLKKFDIDLKDVEFAYNKDAKVLNGVSFKAKQGEVTALVGASGCGKTTILKLISRLYDYDKGQILIDGKDIKEISTESLFDKVSIVFQDVVLFNQSVMENIRIGKQDASDEEVKRAAKLANCTDFIEKMDKGFDTVIGENGAELSGGERQRLSIARAFLKEAPILILDEITASLDVNNEKKIQESLNNLVKDKTVVIISHRMKSIENADKIVVLQNGRVESEGKHEELLQKSKIYKNLIEKTKMAEEFIY",
                "MHTGYLQLKTLMKLLSWIVEKL",
                "MKHQHLLTQITSLNCKKLLKIL"));
    }

}
