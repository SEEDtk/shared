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
public class DnaInputStream extends DnaStream {

    // FIELDS
    /** input stream of sequences */
    private FastaInputStream inStream;

    /**
     * Create a DNA input stream from a normal input stream.
     *
     * @param dnaInput	stream of input containing DNA sequences in FASTA form
     */
    public DnaInputStream(InputStream dnaInput) {
        this.inStream = new FastaInputStream(dnaInput);
    }

    /**
     * Create a DNA input stream from a FASTA file
     *
     * @param dnaInput	FASTA file containing DNA
     *
     * @throws IOException
     */
    public DnaInputStream(File dnaFile) throws IOException {
        this.inStream = new FastaInputStream(dnaFile);
    }

    @Override
    public Iterator<Sequence> iterator() {
        return inStream.iterator();
    }

}
