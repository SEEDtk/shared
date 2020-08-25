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
import org.theseed.locations.FLocation;
import org.theseed.proteins.DnaTranslator;
import org.theseed.proteins.LocationFixer;

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

    /**
     * Test the operon utilities.
     *
     * @throws IOException
     */
    public void testOperon() throws IOException {
        Genome myGto = new Genome(new File("src/test/resources/gto_test", "1313.7016.gto"));
        String myDna = myGto.getContig("1313.7016.con.0058").getSequence().substring(3900, 5920);
        DnaTranslator xlate = new DnaTranslator(11);
        List<String> prots = xlate.operonFrom(myDna);
        assertThat(prots, containsInAnyOrder(
                "MRMNGGFQMKEFYKKRFALTDGGARNLSKATLASFFVYCINMLPAILLMIFAQEVLENMGKSNGFYIVFSVLTLIAMYILLSIEYDKLYNTTYQESADLRIRTAENLSKLPLSYFSKHDISDISQTIMADIEGIEHAMSHSIPKVGGMVLFFPLISVMMPAGNVKMGLAVIIPSILNFIFIPLSKKYQVNGQNRYYDVLRKNSESFQENIEMQMEIKAYNLSKDIKDDLYKKMEDSERVHLKAEVTTILTLSISSIFSFISLAVVIFVGVNLIINKEINSLYLIGYLLAAMKIKDSLDASKEGLMEIFYLSPKIERLKEIQNQDLQEGDDYSLKKFDIDLKDVEFAYNKDAKVLNGVSFKAKQGEVTALVGASGCGKTTILKLISRLYDYDKGQILIDGKDIKEISTESLFDKVSIVFQDVVLFNQSVMENIRIGKQDASDEEVKRAAKLANCTDFIEKMDKGFDTVIGENGAELSGGERQRLSIARAFLKEAPILILDEITASLDVNNEKKIQESLNNLVKDKTVVIISHRMKSIENADKIVVLQNGRVESEGKHEELLQKSKIYKNLIEKTKMAEEFIY",
                "MHTGYLQLKTLMKLLSWIVEKL",
                "MKHQHLLTQITSLNCKKLLKIL"));
    }

    /**
     * Test the location fixer.
     */
    public void testLocationFixer() {
        String dna = "ccacccccgcctttgcgacgccggtaacgtgtgctactcatgctggaagactaggatttggcagcc";
        FLocation loc1a = new FLocation("contig1", 10, 24);
        FLocation loc2a = new FLocation("contig1", 19, 21);
        FLocation loc3a = new FLocation("contig1", 46, 51);
        FLocation loc4a = new FLocation("contig1", 58, 63);
        FLocation loc1b = new FLocation("contig1", 10, 24);
        FLocation loc2b = new FLocation("contig1", 19, 21);
        FLocation loc3b = new FLocation("contig1", 46, 51);
        FLocation loc4b = new FLocation("contig1", 58, 63);
        FLocation loc1c = new FLocation("contig1", 10, 24);
        FLocation loc2c = new FLocation("contig1", 19, 21);
        FLocation loc3c = new FLocation("contig1", 46, 51);
        FLocation loc4c = new FLocation("contig1", 58, 63);
        LocationFixer fixer = LocationFixer.Type.LONGEST.create(11);
        assertFalse(fixer.fix(loc1a, dna));
        assertThat(loc1a, equalTo(loc1b));
        assertTrue(fixer.fix(loc2a, dna));
        assertThat(loc2a.getLeft(), equalTo(13));
        assertThat(loc2a.getRight(), equalTo(27));
        assertTrue(fixer.fix(loc3a, dna));
        assertThat(loc3a.getLeft(), equalTo(31));
        assertThat(loc3a.getRight(), equalTo(54));
        assertFalse(fixer.fix(loc4a, dna));
        assertThat(loc4a, equalTo(loc4b));
        fixer = LocationFixer.Type.NEAREST.create(11);
        assertFalse(fixer.fix(loc1b, dna));
        assertThat(loc1b, equalTo(loc1a));
        assertTrue(fixer.fix(loc2b,  dna));
        assertThat(loc2b, equalTo(loc2a));
        assertTrue(fixer.fix(loc3b, dna));
        assertThat(loc3b.getLeft(), equalTo(40));
        assertThat(loc3b.getRight(), equalTo(54));
        assertFalse(fixer.fix(loc4b, dna));
        assertThat(loc4b, equalTo(loc4a));
        fixer = LocationFixer.Type.LIKELIEST.create(11);
        assertFalse(fixer.fix(loc1c, dna));
        assertThat(loc1c, equalTo(loc1b));
        assertTrue(fixer.fix(loc2c, dna));
        assertThat(loc2c.getLeft(), equalTo(13));
        assertThat(loc2c.getRight(), equalTo(27));
        assertTrue(fixer.fix(loc3c, dna));
        assertThat(loc3c.getLeft(), equalTo(40));
        assertThat(loc3c.getRight(), equalTo(54));
        assertFalse(fixer.fix(loc4c, dna));
        assertThat(loc4c, equalTo(loc4b));
        dna = "ccctagcccatgccccccatgccccccgtgcccccctagccctag";
        loc1a = new FLocation("contig1", 28, 36);
        assertTrue(fixer.fix(loc1a, dna));
        assertThat(loc1a.getLeft(), equalTo(10));
        assertThat(loc1a.getRight(), equalTo(39));
        fixer = LocationFixer.Type.BIASED.create(11);
        loc1a = new FLocation("contig1", 28, 36);
        assertTrue(fixer.fix(loc1a, dna));
        assertThat(loc1a.getLeft(), equalTo(19));
        assertThat(loc1a.getRight(), equalTo(39));
        dna = "acccgtgcccgtgccccccatgcccccctagccc";
        loc1a = new FLocation("contig1", 23, 28);
        loc1b = (FLocation) loc1a.clone();
        assertTrue(fixer.fix(loc1b, dna));
        assertThat(loc1b.getLeft(), equalTo(20));
        assertThat(loc1b.getRight(), equalTo(31));


    }

}
