/**
 *
 */
package org.theseed.io.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.FieldInputStream;
import org.theseed.utils.ParseFailureException;

/**
 * @author Bruce Parrello
 *
 */
class TestLineTemplate {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TestLineTemplate.class);


    @Test
    void testPattern() {
        String test1 = "abc{{def}}{{ghi}}jklmn{{op}}";
        Matcher m = LineTemplate.VARIABLE.matcher(test1);
        assertThat(m.matches(), equalTo(true));
        assertThat(m.group(1), equalTo("abc"));
        assertThat(m.group(2), equalTo("def"));
        assertThat(m.group(3), equalTo("{{ghi}}jklmn{{op}}"));
        m = LineTemplate.VARIABLE.matcher(m.group(3));
        assertThat(m.matches(), equalTo(true));
        assertThat(m.group(1).isEmpty(), equalTo(true));
        assertThat(m.group(2), equalTo("ghi"));
        assertThat(m.group(3), equalTo("jklmn{{op}}"));
        m = LineTemplate.VARIABLE.matcher(m.group(3));
        assertThat(m.matches(), equalTo(true));
        assertThat(m.group(1), equalTo("jklmn"));
        assertThat(m.group(2), equalTo("op"));
        assertThat(m.group(3).isEmpty(), equalTo(true));
        m = LineTemplate.VARIABLE.matcher("abd{{def}}ghi");
        assertThat(m.matches(), equalTo(true));
        assertThat(m.group(1), equalTo("abd"));
        assertThat(m.group(2), equalTo("def"));
        m = LineTemplate.VARIABLE.matcher(m.group(3));
        assertThat(m.matches(), equalTo(false));
    }

    @Test
    void testTemplates() throws IOException, ParseFailureException {
        final String TEMPLATE = "The genome with identifier {{genome_id}} is called {{genome_name}} and has {{genome_length}} base pairs. " +
                "Its NCBI accession number is {{assembly_accession}} and it has {{contigs}} contigs with {{patric_cds}} known protein-coding regions. " +
                "{{$if:host_name}}Its organism is found in the species {{$list:and:host_name:, }}. {{$fi}}{{$if:disease}}The organism is known to cause {{$list:and:disease:::}}. {{$fi}}" +
                "It belongs to the domain {{superkingdom}}{{$if:species}}, the species {{species}}{{$fi}}{{$if:genus}}, the genus {{genus}}{{$fi}}{{$if:family}}, the family {{family}}{{$fi}}, and its NCBI taxonomic identifier is {{taxon_id}}. " +
                "It is believed to be of {{genome_quality}} quality. ";
        File inFile = new File("data", "genomes10.tbl");
        try (FieldInputStream inStream = FieldInputStream.create(inFile)) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE);
            for (var line : inStream) {
                String output = xlate.apply(line);
                log.info(output);
            }
        }
    }

}
