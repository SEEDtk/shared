/**
 *
 */
package org.theseed.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.theseed.test.Matchers.*;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestSetUtils {

    @Test
    public void testSetUtils() {
        Set<String> mySet = SetUtils.newFromArray(StringUtils.split("asd lysC lysA thrA thrB thrC thrD orgA repQ ysd yakR"));
        assertThat(mySet.size(), equalTo(11));
        assertThat(SetUtils.isMember(mySet, "asd"), isTrue());
        assertThat(SetUtils.isMember(mySet, "lysC"), isTrue());
        assertThat(SetUtils.isMember(mySet, "lysA"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrA"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrB"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrC"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrD"), isTrue());
        assertThat(SetUtils.isMember(mySet, "orgA"), isTrue());
        assertThat(SetUtils.isMember(mySet, "repQ"), isTrue());
        assertThat(SetUtils.isMember(mySet, "ysd"), isTrue());
        assertThat(SetUtils.isMember(mySet, "yakR"), isTrue());
        assertThat(SetUtils.isMember(mySet, "asdT"), isFalse());
        assertThat(SetUtils.isMember(mySet, "lysZ"), isFalse());
        assertThat(SetUtils.isMember(mySet, "lysY"), isFalse());
        assertThat(SetUtils.isMember(mySet, "thrX"), isFalse());
        assertThat(SetUtils.isMember(mySet, "thrW"), isFalse());
        assertThat(SetUtils.isMember(mySet, "thrV"), isFalse());
        mySet = null;
        assertThat(SetUtils.isMember(mySet, "asd"), isTrue());
        assertThat(SetUtils.isMember(mySet, "lysC"), isTrue());
        assertThat(SetUtils.isMember(mySet, "lysA"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrA"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrB"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrC"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrD"), isTrue());
        assertThat(SetUtils.isMember(mySet, "orgA"), isTrue());
        assertThat(SetUtils.isMember(mySet, "repQ"), isTrue());
        assertThat(SetUtils.isMember(mySet, "ysd"), isTrue());
        assertThat(SetUtils.isMember(mySet, "yakR"), isTrue());
        assertThat(SetUtils.isMember(mySet, "asdT"), isTrue());
        assertThat(SetUtils.isMember(mySet, "lysZ"), isTrue());
        assertThat(SetUtils.isMember(mySet, "lysY"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrX"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrW"), isTrue());
        assertThat(SetUtils.isMember(mySet, "thrV"), isTrue());
    }

    @Test
    public void testContainsAny() {
        Set<String> big = SetUtils.newFromArray(StringUtils.split("AB BC CD DE EF GH"));
        Set<String> small1 = SetUtils.newFromArray(StringUtils.split("EF IJ"));
        Set<String> small2 = SetUtils.newFromArray(StringUtils.split("KL MN"));
        Set<String> small3 = Collections.emptySet();
        assertThat(SetUtils.containsAny(big, small1), isTrue());
        assertThat(SetUtils.containsAny(big, small2), isFalse());
        assertThat(SetUtils.containsAny(big, small3), isFalse());
        assertThat(SetUtils.containsAny(small1, big), isTrue());
        assertThat(SetUtils.containsAny(small3, small1), isFalse());
    }

}