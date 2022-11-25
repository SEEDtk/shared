/**
 *
 */
package org.theseed.shared;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.theseed.counters.Shuffler;
import org.theseed.utils.ProcessUtils;

/**
 * @author Bruce Parrello
 *
 */
public class TestProcessUtils {

    @Test
    public void testRunCommand() throws IOException, InterruptedException {
        // This test only works if the CLI is hooked up.
        String cliDir = System.getenv("CLI_PATH");
        if (cliDir != null) {
        	// The suffix is OS-dependent.
            String suffix = (SystemUtils.IS_OS_WINDOWS ? ".cmd" : "");
            // Test running a command.
            List<String> parms = new Shuffler<String>(5).add1("a").add1("bcd").add1("e@f/g");
            File p3echo = new File(cliDir, "p3-echo" + suffix);
            List<String> output = ProcessUtils.runProgram(p3echo, parms);
            assertThat(output, contains("id", "a", "bcd", "e@f/g"));
        }
    }

}
