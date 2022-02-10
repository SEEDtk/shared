/**
 *
 */
package org.theseed.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestSetUtils {

    @Test
    public void testSetUtils() {
        Set<String> mySet = SetUtils.newFromArray(StringUtils.split("asd lysC lysA thrA thrB thrC thrD orgA repQ ysd yakR"));
        assertThat(mySet.size(), equalTo(11));
        assertThat(SetUtils.isMember(mySet, "asd"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "lysC"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "lysA"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrA"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrB"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrC"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrD"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "orgA"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "repQ"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "ysd"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "yakR"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "asdT"), equalTo(false));
        assertThat(SetUtils.isMember(mySet, "lysZ"), equalTo(false));
        assertThat(SetUtils.isMember(mySet, "lysY"), equalTo(false));
        assertThat(SetUtils.isMember(mySet, "thrX"), equalTo(false));
        assertThat(SetUtils.isMember(mySet, "thrW"), equalTo(false));
        assertThat(SetUtils.isMember(mySet, "thrV"), equalTo(false));
        mySet = null;
        assertThat(SetUtils.isMember(mySet, "asd"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "lysC"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "lysA"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrA"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrB"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrC"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrD"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "orgA"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "repQ"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "ysd"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "yakR"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "asdT"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "lysZ"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "lysY"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrX"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrW"), equalTo(true));
        assertThat(SetUtils.isMember(mySet, "thrV"), equalTo(true));
    }

    @Test
    public void testContainsAny() {
        Set<String> big = SetUtils.newFromArray(StringUtils.split("AB BC CD DE EF GH"));
        Set<String> small1 = SetUtils.newFromArray(StringUtils.split("EF IJ"));
        Set<String> small2 = SetUtils.newFromArray(StringUtils.split("KL MN"));
        Set<String> small3 = Collections.emptySet();
        assertThat(SetUtils.containsAny(big, small1), equalTo(true));
        assertThat(SetUtils.containsAny(big, small2), equalTo(false));
        assertThat(SetUtils.containsAny(big, small3), equalTo(false));
        assertThat(SetUtils.containsAny(small1, big), equalTo(true));
        assertThat(SetUtils.containsAny(small3, small1), equalTo(false));
    }

}
