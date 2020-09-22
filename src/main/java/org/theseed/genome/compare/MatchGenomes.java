/**
 *
 */
package org.theseed.genome.compare;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.genome.GenomeDirectory;
import org.theseed.sequence.MD5Hex;

/**
 * This class helps to match genomes with sequence-identical genomes in another directory.  It scans the
 * target directory and computes the MD5s.
 *
 * @author Bruce Parrello
 *
 */
public class MatchGenomes {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(CompareORFs.class);
    /** MD5 computer */
    private MD5Hex md5Computer;


    /**
     * Construct a genome-matching object.
     *
     * @throws NoSuchAlgorithmException
     */
    public MatchGenomes() throws NoSuchAlgorithmException {
        this.md5Computer = new MD5Hex();
    }

    /**
     * @return a map of genome sequence MD5s to genome files
     *
     * @param genomeDir		directory of the genome files
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public Map<String, File> getMd5GenomeMap(File genomeDir) throws IOException {

        GenomeDirectory genomes = new GenomeDirectory(genomeDir);
        Map<String, File> retVal = new HashMap<String, File>();
        for (Genome refGenome : genomes) {
            log.info("Scanning {}.", refGenome);
            // Get the MD5 for all the contig sequences.
            String key = this.md5Computer.sequenceMD5(refGenome);
            // Map the file name to the MD5.
            retVal.put(key, genomes.currFile());
        }
        return retVal;
    }

    /**
     * @return the MD5 computation engine
     */
    protected MD5Hex getMd5Computer() {
        return this.md5Computer;
    }

}
