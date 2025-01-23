/**
 *
 */
package org.theseed.shared;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Iterator;
import org.theseed.proteins.RoleSet;

/**
 * @author Bruce Parrello
 *
 */
public class TestRoleSets {

    @Test
    public void testEmptySets() {
        RoleSet emptySet = RoleSet.fromString("");
        assertThat(emptySet.size(), equalTo(0));
        assertThat(emptySet, sameInstance(RoleSet.NO_ROLES));
        RoleSet single = RoleSet.fromString("abc");
        assertThat(emptySet, not(equalTo(single)));
        assertThat(emptySet.toString(), equalTo(""));
        Iterator<String> emptyIter = emptySet.iterator();
        assertThat(emptyIter.hasNext(), equalTo(false));
        assertThat(emptySet.isEmpty(), equalTo(true));
    }

    @Test
    public void testRoleSetMethods() {
        RoleSet set1 = RoleSet.fromString("Bbbb,Aaaa");
        RoleSet set2 = RoleSet.fromString("Cccc");
        RoleSet set3 = RoleSet.fromString("");
        RoleSet set4 = RoleSet.fromString("Bbbb,Cccc");
        RoleSet set5 = RoleSet.fromString("Bbbb,Cccc");
        RoleSet set6 = RoleSet.fromString("Cccc,Bbbb");
        assertThat(set1, not(equalTo(set4)));
        assertThat(set4, equalTo(set5));
        assertThat(set2, not(equalTo(set1)));
        assertThat(set1.toString(), equalTo("Bbbb,Aaaa"));
        assertThat(set2.toString(), equalTo("Cccc"));
        assertThat(set1.compareTo(set2), lessThan(0));
        assertThat(set2.compareTo(set3), lessThan(0));
        assertThat(set1.compareTo(set4), lessThan(0));
        assertThat(set2.compareTo(set1), greaterThan(0));
        assertThat(set3.isEmpty(), equalTo(true));
        assertThat(set1.isEmpty(), equalTo(false));
        assertThat(set2.isEmpty(), equalTo(false));
        Iterator<String> iter = set4.iterator();
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo("Bbbb"));
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo("Cccc"));
        assertThat(iter.hasNext(), equalTo(false));
        assertThat(set1.size(), equalTo(2));
        assertThat(set2.size(), equalTo(1));
        RoleSet[] roleArray = new RoleSet[] { set5, set4, set3, set1, set2 };
        assertThat(RoleSet.min(roleArray), equalTo("Aaaa"));
        assertThat(set1.contains(set2), equalTo(false));
        assertThat(set4.contains(set2), equalTo(true));
        assertThat(set4.contains(set1), equalTo(false));
        assertThat(set1.contains(set3), equalTo(true));
        assertThat(set3.contains(set1), equalTo(false));
        assertThat(set5.contains(set6), equalTo(true));
    }
}
