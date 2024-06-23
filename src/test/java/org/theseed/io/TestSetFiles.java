/**
 *
 */
package org.theseed.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestSetFiles {

    @Test
    void testSetFile() {
        File testFile = new File("data", "test.ser");
        Set<String> set1 = Set.of("Apple","Cat","Dog","Fail safe","High Cliff","Zebra");
        SetFile.save(testFile, set1);
        Set<String> set2 = SetFile.load(testFile);
        assertThat(set1, equalTo(set2));
        set1 = Collections.emptySet();
        SetFile.save(testFile, set1);
        set2 = SetFile.load(testFile);
        assertThat(set1, equalTo(set2));

    }

}
