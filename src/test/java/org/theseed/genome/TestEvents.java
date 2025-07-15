/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;

/**
 * @author Bruce Parrello
 *
 */
class TestEvents {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TestEvents.class);

    @Test
    void testGenomeEvents() throws IOException {
        File gFile = new File("data", "test.good.gto");
        Genome genome = new Genome(gFile);
        var events = genome.getEvents();
        assertThat(events.size(), equalTo(21));
        var eventCheck = events.stream().filter(x -> x.getId().contentEquals("71050484-2E1B-11ED-9683-9F2E743C8BC2")).findAny();
        assertThat(eventCheck.isPresent(), equalTo(true));
        var event = eventCheck.get();
        assertThat(event.getToolName(), equalTo("find_special_proteins selenoprotein"));
        assertThat(event.getHostname(), equalTo("bio-compute-04.cels.anl.gov"));
        assertThat(event.getParameters(), contains("-tmpdir", "/tmp/tmp_dir_BiqTfYAB"));
        assertThat(event.getExecuteTime(), closeTo(1662493091.35017, 0.001));
        // Create a command processor.
        var processor = new TestProcessor();
        var parms = new String[] { "--verbose", "--key", "A#", "directory" };
        processor.parseCommand(parms);
        double timeStamp = ((double) System.currentTimeMillis()) / 1000.0;
        event = new AnalysisEvent("shared.test test", processor);
        genome.addEvent(event);
        double timeStamp2 = ((double) System.currentTimeMillis()) / 1000.0;
        String newEventId = event.getId();
        String host = event.getHostname();
        assertThat(event.getParameters(), contains(parms));
        assertThat(event.getToolName(), equalTo("shared.test test"));
        assertThat(event.getExecuteTime(), closeTo(timeStamp, timeStamp2 - timeStamp));
        timeStamp = event.getExecuteTime();
        eventCheck = events.stream().filter(x -> x.getId().contentEquals(newEventId)).findAny();
        assertThat(eventCheck.isPresent(), equalTo(true));
        File testFile = new File("data", "genome.ser");
        genome.save(testFile);
        Genome genome2 = new Genome(testFile);
        events = genome2.getEvents();
        assertThat(events.size(), equalTo(22));
        eventCheck = events.stream().filter(x -> x.getId().contentEquals(newEventId)).findAny();
        assertThat(eventCheck.isPresent(), equalTo(true));
        event = eventCheck.get();
        assertThat(event.getHostname(), equalTo(host));
        assertThat(event.getExecuteTime(), closeTo(timeStamp, 0.001));
        assertThat(event.getParameters(), contains(parms));
        assertThat(event.getToolName(), equalTo("shared.test test"));
    }

    public static class TestProcessor extends BaseProcessor {

        @Option(name = "--key", usage = "musical key of the test")
        private String keyString;

        @Argument(index = 0, metaVar = "dir", usage = "input directory", required = true)
        private File inDir;

        @Override
        protected void setDefaults() {
        }

        @Override
        protected void validateParms() throws IOException, ParseFailureException {
        }

        @Override
        protected void runCommand() throws Exception {
        }

    }
}
