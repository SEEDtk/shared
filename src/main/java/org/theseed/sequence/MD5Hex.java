package org.theseed.sequence;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.theseed.genome.Contig;
import org.theseed.genome.Genome;

/**
 * This class produces hexadecimal MD5 checksums suitable for identify DNA and protein
 * sequences.
 *
 * @author Bruce Parrello
 *
 */
public class MD5Hex {

    protected MessageDigest md;

    public MD5Hex() throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance("MD5");
    }

    /**
     * Produce the checksum of a single string.
     *
     * @param input		incoming string to convert
     *
     * @return the hexadecimal MD5 checksum
     *
     * @throws UnsupportedEncodingException
     */
    public String checksum(String input) throws UnsupportedEncodingException {
        byte[] bytes = md.digest(input.getBytes("UTF-8"));
        return hexOf(bytes);
    }

    /**
     * Produce the checksum of a protein or DNA string.  In this case, the
     * string is converted to uppercase before processing.
     *
     * @param sequence	incoming sequence to convert
     *
     * @return the hexadecimal MD5 checksum
     * @throws UnsupportedEncodingException
     */
    public String sequenceMD5(String sequence) throws UnsupportedEncodingException {
        return checksum(sequence.toUpperCase());
    }

    /**
     * Convert a byte-string MD5 to its hex representation.
     *
     * @param bytes		bytes to convert
     *
     * @return the checksum in hexadecimal
     */
    private String hexOf(byte[] bytes) {
        StringBuffer retVal = new StringBuffer(32);
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString((int) 0x00FF & bytes[i]);
            // Add the leading 0 if we need it.
            if (hex.length() == 1) retVal.append("0");
            retVal.append(hex);
        }
        return retVal.toString();
    }

    /**
     * Compute the MD5 of the contigs in a genome.  The genome must have its contig
     * sequences in memory.
     *
     * @param genome	genome whose MD5 is desired
     *
     * @return the MD5 of the genome's DNA
     * @throws UnsupportedEncodingException
     */
    public String sequenceMD5(Genome genome) throws UnsupportedEncodingException {
        return sequenceMD5(genome.getContigs());
    }

    /**
     * Compute the MD5 of a collection of contigs.
     *
     * @param contigs	collection of contigs to process
     *
     * @return the MD5 of the DNA in the contigs
     * @throws UnsupportedEncodingException
     */
    protected String sequenceMD5(Collection<Contig> contigs) throws UnsupportedEncodingException {
        // Loop through the contigs, accumulating the MD5s in lexical order.
        SortedSet<String> md5s = new TreeSet<String>();
        for (Contig contig : contigs) {
            String md5 = this.sequenceMD5(contig.getSequence());
            md5s.add(md5);
        }
        // Join the MD5s with commas and return the checksum.
        return this.checksum(md5s.stream().collect(Collectors.joining(",")));
    }

}
