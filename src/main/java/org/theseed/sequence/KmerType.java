/**
 *
 */
package org.theseed.sequence;

/**
 * This is a simple enum that specifies a kmer type-- DNA, RNA, or Protein. It can be used to
 * generate a kmer object of the appropriate type.
 *
 * @author Bruce Parrello
 *
 */
public enum KmerType {
	/** DNA sequence */
	DNA {
		@Override
		public SequenceKmers createKmers(String seq, int kSize) {
			return new DnaKmers(seq, kSize);
		}

		@Override
		public int getKmerSize() {
			return 21;
		}
	},
	/** RNA sequence */
	RNA {
		@Override
		public SequenceKmers createKmers(String seq, int kSize) {
			return new RnaKmers(seq, kSize);
		}

		@Override
		public int getKmerSize() {
			return 21;
		}
	},
	/** amino acid (protein) sequence */
	PROT {
		@Override
		public SequenceKmers createKmers(String seq, int kSize) {
			return new ProteinKmers(seq, kSize);
		}

		@Override
		public int getKmerSize() {
			return 8;
		}
	};

	/**
	 * Create a kmer object of this type.
	 *
	 * @param seq		input sequence string
	 * @param kSize		kmer size to use
	 *
	 * @return a kmer object of this type for the specified sequence
	 */
	public abstract SequenceKmers createKmers(String seq, int kSize);

	/**
	 * @return the default kmer size for this sequence type
	 */
	public abstract int getKmerSize();

}
