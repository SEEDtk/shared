/**
 *
 */
package org.theseed.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.theseed.test.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.theseed.io.ParmDescriptor;
import org.theseed.io.ParmFile;
import org.theseed.io.Shuffler;
import org.theseed.utils.MultiParms;

/**
 * @author Bruce Parrello
 *
 */
public class TestParmFile {

    @Test
    public void testPattern() {
        Matcher m = ParmFile.LINE_PATTERN.matcher("--prefer SCORE\t# optimization preference");
        assertThat(m.matches(), isTrue());
        assertThat(m.group(1), nullValue());
        assertThat(m.group(2), equalTo("prefer"));
        assertThat(m.group(3), equalTo("SCORE"));
        assertThat(m.group(4), equalTo("optimization preference"));
        ParmDescriptor desc = new ParmDescriptor(m, new Shuffler<String>(2).add1("## comment 1").add1("## comment 2"));
        String[] parts = StringUtils.split(desc.toString(), System.getProperty("line.separator"));
        assertThat(parts[0], equalTo("## comment 1"));
        assertThat(parts[1], equalTo("## comment 2"));
        assertThat(parts[2], equalTo("--prefer SCORE\t# optimization preference"));
        m = ParmFile.LINE_PATTERN.matcher("--prefer SCORE");
        assertThat(m.matches(), isTrue());
        assertThat(m.group(1), nullValue());
        assertThat(m.group(2), equalTo("prefer"));
        assertThat(m.group(3), equalTo("SCORE"));
        assertThat(m.group(4), nullValue());
        desc = new ParmDescriptor(m, Collections.emptyList());
        assertThat(desc.toString(), equalTo("--prefer SCORE"));
        m = ParmFile.LINE_PATTERN.matcher("--balanced 2, 3, 4\t# number of hidden layers (overrides widths)");
        assertThat(m.matches(), isTrue());
        assertThat(m.group(1), nullValue());
        assertThat(m.group(2), equalTo("balanced"));
        assertThat(m.group(3), equalTo("2, 3, 4"));
        assertThat(m.group(4), equalTo("number of hidden layers (overrides widths)"));
        desc = new ParmDescriptor(m, Collections.emptyList());
        assertThat(desc.toString(), equalTo("--balanced 2, 3, 4\t# number of hidden layers (overrides widths)"));
        m = ParmFile.LINE_PATTERN.matcher("# --raw\t# suppress input normalization");
        assertThat(m.matches(), isTrue());
        assertThat(m.group(1), equalTo("# "));
        assertThat(m.group(2), equalTo("raw"));
        assertThat(m.group(3), nullValue());
        assertThat(m.group(4), equalTo("suppress input normalization"));
        desc = new ParmDescriptor(m, Collections.emptyList());
        assertThat(desc.toString(), equalTo("# --raw \t# suppress input normalization"));
        m = ParmFile.LINE_PATTERN.matcher(desc.toString());
        assertThat(m.matches(), isTrue());
        assertThat(m.group(1), equalTo("# "));
        assertThat(m.group(2), equalTo("raw"));
        assertThat(m.group(3), nullValue());
        assertThat(m.group(4), equalTo("suppress input normalization"));
        m = ParmFile.LINE_PATTERN.matcher("# --meta \t# comma-delimited list of meta-data columns");
        assertThat(m.matches(), isTrue());
        assertThat(m.group(1), equalTo("# "));
        assertThat(m.group(2), equalTo("meta"));
        assertThat(m.group(3), nullValue());
        assertThat(m.group(4), equalTo("comma-delimited list of meta-data columns"));
    }

    @Test
    public void testParmFile() throws IOException {
        // Load the parameters from the parm file.
        File parmFile = new File("data", "parms.prm");
        ParmFile parms = new ParmFile(parmFile);
        ParmDescriptor data = parms.get("method");
        assertThat(data.getName(), equalTo("method"));
        assertThat(data.isCommented(), isFalse());
        assertThat(data.getValue(), equalTo("EPOCH"));
        assertThat(data.getDescription(), equalTo("training set processing method"));
        assertThat(parms.get("frog"), nullValue());
        File testFile = new File("data", "parms.ser");
        parms.save(testFile);
        ParmFile parms2 = new ParmFile(testFile);
        assertThat(parms.size(), equalTo(parms2.size()));
        for (ParmDescriptor desc : parms2) {
            data = parms.get(desc.getName());
            assertThat(desc.isCommented(), equalTo(data.isCommented()));
            assertThat(desc.getValue(), equalTo(data.getValue()));
            assertThat(desc.getDescription(), equalTo(data.getDescription()));
            if (data.getLineComments().length == 0)
                assertThat(desc.getLineComments().length, equalTo(0));
            else
                assertThat(desc.getLineComments(), arrayContaining(data.getLineComments()));
        }
        MultiParms mParms = new MultiParms(testFile);
        List<String> parmStrings = mParms.next();
        assertThat(mParms.toString(), equalTo("--balanced 2"));
        int parmIdx = parmStrings.indexOf("--prefer");
        assertThat(parmIdx, greaterThanOrEqualTo(0));
        assertThat(parmStrings.get(parmIdx+1), equalTo("SCORE"));
        assertThat(parmStrings.indexOf("--raw"), equalTo(-1));
        parmIdx = parmStrings.indexOf("--meta");
        assertThat(parmStrings.get(parmIdx+1), equalTo("sample_id,density"));
        parmStrings = mParms.next();
        assertThat(mParms.toString(), equalTo("--balanced 3"));
    }

}
