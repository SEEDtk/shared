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
     * Create a DNA input stream with a specific genetic code from a FASTA file.
     *
     * @param dnaInput	FASTA file containing DNA
     * @param gc		the genetic code of the DNA
     *
     * @throws IOException
     */
    public DnaInputStream(File dnaFile, int gc) throws IOException {
        this.inStream = new FastaInputStream(dnaFile);
        this.setGeneticCode(gc);
    }

    /**
     * Create a DNA input stream with a specific genetic code from a normal input stream.
     *
     * @param dnaInput	stream of input containing DNA sequences in FASTA form
     * @param gc		the genetic code of the DNA
     */
    public DnaInputStream(InputStream dnaInput, int gc) {
        this.inStream = new FastaInputStream(dnaInput);
        this.setGeneticCode(gc);
    }

    @Override
    public Iterator<Sequence> iterator() {
        return inStream.iterator();
    }

}
