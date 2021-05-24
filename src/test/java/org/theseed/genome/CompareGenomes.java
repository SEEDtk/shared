/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;

/**
 * This is a utility class for verifying two genomes are identical.
 *
 * @author Bruce Parrello
 *
 */
public class CompareGenomes {

    /**
     * Verify two genomes are identical.
     *
     * @param gto	first genome
     * @param gto2	second genome
     * @param full	if TRUE, non-core attributes will be tested
     */
    public static void test(Genome gto, Genome gto2, boolean full) {
        assertThat(gto2.getId(), equalTo(gto.getId()));
        assertThat(gto2.getName(), equalTo(gto.getName()));
        assertThat(gto2.getDomain(), equalTo(gto.getDomain()));
        assertThat(ArrayUtils.toObject(gto2.getLineage()), arrayContaining(ArrayUtils.toObject(gto.getLineage())));
        assertThat(gto2.getGeneticCode(), equalTo(gto.getGeneticCode()));
        assertThat(gto2.getTaxonomyId(), equalTo(gto.getTaxonomyId()));
        assertThat(gto2.getFeatureCount(), equalTo(gto.getFeatureCount()));
        assertThat(gto2.getContigCount(), equalTo(gto.getContigCount()));
        Collection<Feature> fids = gto.getFeatures();
        for (Feature fid : fids) {
            Feature diskFid = gto2.getFeature(fid.getId());
            assertThat(diskFid.getFunction(), equalTo(fid.getFunction()));
            assertThat(diskFid.getLocation(), equalTo(fid.getLocation()));
            assertThat(diskFid.getPlfam(), equalTo(fid.getPlfam()));
            assertThat(diskFid.getType(), equalTo(fid.getType()));
            assertThat(diskFid.getProteinTranslation(), equalTo(fid.getProteinTranslation()));
            if (full) {
                Collection<GoTerm> fidGoTerms = fid.getGoTerms();
                assertThat(diskFid.getGoTerms().size(), equalTo(fidGoTerms.size()));
                for (GoTerm diskGoTerm : diskFid.getGoTerms()) {
                    assertThat(fidGoTerms, hasItem(diskGoTerm));
                }
            }
            Collection<Annotation> fidAnnotations = fid.getAnnotations();
            assertThat(diskFid.getAnnotations().size(), equalTo(fidAnnotations.size()));
            for (Annotation diskAnnotation : diskFid.getAnnotations()) {
                assertThat(fidAnnotations, hasItem(diskAnnotation));
            }
            Collection<String> fidAliases = fid.getAliases();
            assertThat(diskFid.getAliases().size(), equalTo(fidAliases.size()));
            for (String diskAlias : diskFid.getAliases()) {
                assertThat(fidAliases, hasItem(diskAlias));
            }
        }
        Collection<Contig> contigs = gto.getContigs();
        for (Contig contig : contigs) {
            Contig diskContig = gto2.getContig(contig.getId());
            assertThat(diskContig.length(), equalTo(contig.length()));
            assertThat(diskContig.getSequence(), equalTo(contig.getSequence()));
        }
    }


}
