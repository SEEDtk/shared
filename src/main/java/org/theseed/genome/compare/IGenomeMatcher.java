/**
 *
 */
package org.theseed.genome.compare;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.theseed.genome.Genome;

/**
 * This interface is used for genome-to-genome matchers.  It provides methods to produce a map that can be used to identify
 * sequence-identical genomes, and a comparison method that counts good and bad correspondences.
 *
 * @author Bruce Parrello
 */
public interface IGenomeMatcher {

    /**
     * @return a map of genome sequence MD5s to genome files
     *
     * @param genomeDir		directory of the genome files
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public Map<String, File> getMd5GenomeMap(File genomeDir) throws IOException;

    /**
     * Compare the two genomes and store the statistics in this object.
     *
     * @param newGenome		new genome
     * @param compareGenome	original genome with different annotations
     *
     * @return TRUE if successful, FALSE if the genomes cannot be compared.
     *
     * @throws UnsupportedEncodingException
     */
    public boolean compare(Genome newGenome, Genome oldGenome) throws UnsupportedEncodingException;

    /**
     * @return the nubmer of good matches
     */
    public int getGood();

    /**
     * @return the number of bad matches
     */
    public int getBad();

    /**
     * @return the percent of features that were good
     */
    public double percent();


}
