/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestAliasAnalyze {

	@Test
	void testAliasPairs() {
		List<Map.Entry<String, String>> retVal;
		retVal = Feature.analyzeAlias("gi|123456");
		assertThat(retVal.size(), equalTo(1));
		var entry = retVal.get(0);
		assertThat(entry.getKey(), equalTo("gi"));
		assertThat(entry.getValue(), equalTo("123456"));
		retVal = Feature.analyzeAlias("dnaK");
		assertThat(retVal.size(), equalTo(1));
		entry = retVal.get(0);
		assertThat(entry.getKey(), equalTo("gene_name"));
		assertThat(entry.getValue(), equalTo("dnaK"));
		retVal = Feature.analyzeAlias("b0001");
		assertThat(retVal.size(), equalTo(1));
		entry = retVal.get(0);
		assertThat(entry.getKey(), equalTo("LocusTag"));
		assertThat(entry.getValue(), equalTo("b0001"));
		retVal = Feature.analyzeAlias("protein_id:ABC");
		assertThat(retVal.size(), equalTo(1));
		entry = retVal.get(0);
		assertThat(entry.getKey(), equalTo("protein_id"));
		assertThat(entry.getValue(), equalTo("ABC"));
		retVal = Feature.analyzeAlias("gene_id:1234567");
		assertThat(retVal.size(), equalTo(1));
		entry = retVal.get(0);
		assertThat(entry.getKey(), equalTo("GeneID"));
		assertThat(entry.getValue(), equalTo("1234567"));
		retVal = Feature.analyzeAlias("Swiss-Prot/UniProtKB:frog");
		assertThat(retVal.size(), equalTo(2));
		entry = retVal.get(0);
		assertThat(entry.getKey(), equalTo("SwissProt"));
		assertThat(entry.getValue(), equalTo("frog"));
		entry = retVal.get(1);
		assertThat(entry.getKey(), equalTo("UniProt"));
		assertThat(entry.getValue(), equalTo("frog"));
		retVal = Feature.analyzeAlias("GENE:1bcD");
		assertThat(retVal.size(), equalTo(1));
		entry = retVal.get(0);
		assertThat(entry.getKey(), equalTo("gene_name"));
		assertThat(entry.getValue(), equalTo("1bcD"));
		retVal = Feature.analyzeAlias("Uniprot/SWISSPROT|5221");
		assertThat(retVal.size(), equalTo(2));
		entry = retVal.get(0);
		assertThat(entry.getKey(), equalTo("UniProt"));
		assertThat(entry.getValue(), equalTo("5221"));
		entry = retVal.get(1);
		assertThat(entry.getKey(), equalTo("SwissProt"));
		assertThat(entry.getValue(), equalTo("5221"));
	}

	@Test
	void testAliasConversion() throws IOException {
		File gtoFile = new File("data", "511145.12.gto");
		Genome genome = new Genome(gtoFile);
		Feature feat = genome.getFeature("fig|511145.12.peg.314");
		var aliasPairs = feat.getAliasMap();
		assertThat(aliasPairs.size(), equalTo(3));
		assertThat(aliasPairs.get("LocusTag"), contains("b0306"));
		assertThat(aliasPairs.get("GeneID"), contains("948438"));
		assertThat(aliasPairs.get("gene_name"), contains("ykgE"));
		feat = genome.getFeature("fig|511145.12.peg.4460");
		aliasPairs = feat.getAliasMap();
		assertThat(aliasPairs.size(), equalTo(3));
		assertThat(aliasPairs.get("gene_name"), contains("fimG"));
		assertThat(aliasPairs.get("LocusTag"), contains("b4319"));
		assertThat(aliasPairs.get("GeneID"), contains("948846"));
		feat = genome.getFeature("fig|511145.12.peg.313");
		aliasPairs = feat.getAliasMap();
		assertThat(aliasPairs.size(), equalTo(0));
		File saveFile = new File("data", "temp.ser");
		genome.save(saveFile);
		Genome genome2 = new Genome(saveFile);
		feat = genome2.getFeature("fig|511145.12.peg.4460");
		aliasPairs = feat.getAliasMap();
		assertThat(aliasPairs.size(), equalTo(3));
		assertThat(aliasPairs.get("gene_name"), contains("fimG"));
		assertThat(aliasPairs.get("LocusTag"), contains("b4319"));
		assertThat(aliasPairs.get("GeneID"), contains("948846"));
	}

}
