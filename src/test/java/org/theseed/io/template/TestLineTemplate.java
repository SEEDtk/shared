/**
 *
 */
package org.theseed.io.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.FieldInputStream;
import org.theseed.io.LineReader;
import org.theseed.io.template.output.TemplateHashWriter;
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
        TemplateHashWriter globals = new TemplateHashWriter();
        final String TEMPLATE = "The genome with identifier {{genome_id}} is called {{genome_name}} and has {{genome_length}} base pairs. " +
                "Its NCBI accession number is {{assembly_accession}} and it has {{contigs}} contigs with {{patric_cds}} known protein-coding regions. " +
                "{{$if:host_name}}Its organism is found in the species {{$list:host_name:and:, }}. {{$fi}}{{$if:disease}}The organism is known to cause {{$list:disease}}. {{$fi}}" +
                "{{$group:and}}It belongs to" +
                    "{{$clause:superkingdom}}the domain {{superkingdom}}" +
                    "{{$clause:species}}the species {{species}}" +
                    "{{$clause:genus}}the genus {{genus}}{{$end}}";
        try (var inStream = FieldInputStream.create(new File("data", "genomes10.tbl"));
                var testStream = new LineReader(new File("data", "genomes10.txt"))) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals);
            Iterator<String> testIter = testStream.iterator();
            int i = 1;
            for (var line : inStream) {
                String output = xlate.apply(line);
                assertThat(String.format("Line %d",  i), output, equalTo(testIter.next()));
                i++;
            }
        }
    }

    @Test
    void testProducts() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        final String TEMPLATE = "{{$if:type:fid}}{{$product:product:type}}{{$fi}}";
        try (var inStream = FieldInputStream.create(new File("data", "products.tbl"));
                var testStream = new LineReader(new File("data", "products.txt"))) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals);
            Iterator<String> testIter = testStream.iterator();
            int i = 1;
            for (var line : inStream) {
                String output = xlate.apply(line);
                String test = testIter.next();
                assertThat(String.format("Line %d", i), output, equalTo(test));
                i++;
            }
        }
    }

    @Test
    void testLocations() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        final String TEMPLATE = "Feature {{patric_id}} is on {{$strand:strand}} at location {{start}} to {{end}}.";
        try (var inStream = FieldInputStream.create(new File("data", "locs.txt"))) {
            int fidIdx = inStream.findField("patric_id");
            int valIdx = inStream.findField("value");
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals);
            for (var record : inStream) {
                String output = xlate.apply(record);
                String fid = record.get(fidIdx);
                String expected = record.get(valIdx);
                assertThat(fid, output, equalTo(expected));
            }
        }
    }

    @Test
    void testGroup() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        final String TEMPLATE = "Hello, we have a group{{$group:and:.}} with{{$clause:f1}}one {{f1}}"
                + "{{$clause:f2}}two {{f2}}{{$clause:f3}}three {{f3}}{{$end}}";
        try (var inStream = FieldInputStream.create(new File("data", "groups.tbl"))) {
            int i = 1;
            int testIdx = inStream.findField("expected");
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals);
            for (var line : inStream) {
                String output = xlate.apply(line);
                String test = line.get(testIdx);
                assertThat(String.format("Line %d", i), output, equalTo(test));
                i++;
            }
        }
    }

    @Test
    void testInclude() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        globals.write("dlits.txt", "key3", "key3 dlit text");
        globals.write("dlits.txt", "key3", "more key3 dlit text");
        globals.write("dlits.txt", "key3", "another key3 dlit text");
        globals.write("dlits.txt", "key2", "key2 dlit text");
        globals.write("dlits.txt", "key2", "more key2 dlit text");
        globals.write("dlits.txt", "key1", "key1 dlit text");
        globals.write("ilits.txt", "key1", "key1 ilit text");
        final String TEMPLATE = "This is an important test.  We need to know if {{datum}} works. " +
                "Also, we are testing include. {{$if:include(dlits.txt, key)}}" +
                "We have {{$list:include(dlits.txt, key):and}}" +
                "{{$else}}{{$if:list}}We do not have key, but we have {{$list:list}}" +
                "{{$fi}}{{$fi}}.";
        try (var inStream = FieldInputStream.create(new File("data", "includes.tbl"))) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals);
            int i = 1;
            int testIdx = inStream.findField("expected");
            for (var line : inStream) {
                String output = xlate.apply(line);
                String test = line.get(testIdx);
                log.info(output);
                assertThat(String.format("Line %d", i), output, equalTo(test));
                i++;
            }
        }
    }


}
