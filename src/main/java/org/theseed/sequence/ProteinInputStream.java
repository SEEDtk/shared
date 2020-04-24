/**
 *
 */
package org.theseed.sequence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * This is a DNA stream that is read from a file or I/O stream.
 *
 * @author Bruce Parrello
 *
 */
public class ProteinInputStream extends ProteinStream {

    // FIELDS
    /** input stream of sequences */
    private FastaInputStream inStream;

    /**
     * Create a DNA input stream from a normal input stream.
     *
     * @param proteinInput	stream of input containing DNA sequences in FASTA form
     */
    public ProteinInputStream(InputStream proteinInput) {
        this.inStream = new FastaInputStream(proteinInput);
    }

    /**
     * Create a DNA input stream from a FASTA file
     *
     * @param proteinInput	FASTA file containing DNA
     *
     * @throws IOException
     */
    public ProteinInputStream(File proteinFile) throws IOException {
        this.inStream = new FastaInputStream(proteinFile);
    }

    @Override
    public Iterator<Sequence> iterator() {
        return inStream.iterator();
    }

}
