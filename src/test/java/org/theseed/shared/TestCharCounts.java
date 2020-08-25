/**
 *
 */
package org.theseed.shared;

import junit.framework.TestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.theseed.io.Shuffler;
import org.theseed.sequence.CharCounter;
import org.theseed.sequence.FastaInputStream;
import org.theseed.sequence.Sequence;

/**
 * @author Bruce Parrello
 *
 */
public class TestCharCounts extends TestCase {

    public void testCharCounts() throws IOException {
        CharCounter counters = new CharCounter();
        File seqFile = new File("src/test/resources", "alignCounts.fa");
        List<Sequence> seqList;
        try (FastaInputStream seqStream = new FastaInputStream(seqFile)) {
            seqList = new Shuffler<Sequence>(seqStream);
        }
        CharCounter.prepare(seqList);
        for (int i = 0; i < seqList.size(); i++) {
            Sequence seqI = seqList.get(i);
            assertThat(seqI.getLabel(), seqI.getSequence(), matchesPattern("[A-Z\\-]+"));
        }
        List<CharCounter.Count> counts = counters.countSequences(seqList, 0);
        assertThat(counts.get(0).getTarget(), equalTo('A'));
        assertThat(counts.get(0).getCount(), equalTo(3));
        assertThat(counts.get(1).getTarget(), equalTo('-'));
        assertThat(counts.get(1).getCount(), equalTo(1));
        assertThat(counts.get(2).getTarget(), equalTo('G'));
        assertThat(counts.get(2).getCount(), equalTo(1));
        assertThat(counts.size(), equalTo(3));
        assertThat(counters.getMostCommon(), equalTo('A'));
        counts = counters.countSequences(seqList, 1);
        assertThat(counts.get(0).getTarget(), equalTo('-'));
        assertThat(counts.get(0).getCount(), equalTo(2));
        assertThat(counts.get(1).getTarget(), equalTo('A'));
        assertThat(counts.get(1).getCount(), equalTo(1));
        assertThat(counts.get(2).getTarget(), equalTo('C'));
        assertThat(counts.get(2).getCount(), equalTo(1));
        assertThat(counts.get(3).getTarget(), equalTo('T'));
        assertThat(counts.get(3).getCount(), equalTo(1));
        assertThat(counts.size(), equalTo(4));
        assertThat(counters.getMostCommon(), equalTo('A'));
        counts = counters.countSequences(seqList, 2);
        assertThat(counts.get(0).getTarget(), equalTo('C'));
        assertThat(counts.get(0).getCount(), equalTo(3));
        assertThat(counts.get(1).getTarget(), equalTo('A'));
        assertThat(counts.get(1).getCount(), equalTo(2));
        assertThat(counts.size(), equalTo(2));
        assertThat(counters.getMostCommon(), equalTo('C'));
        counts = counters.countSequences(seqList, 6);
        assertThat(counts.get(0).getTarget(), equalTo('-'));
        assertThat(counts.get(0).getCount(), equalTo(3));
        assertThat(counts.get(1).getTarget(), equalTo('G'));
        assertThat(counts.get(1).getCount(), equalTo(2));
        assertThat(counts.size(), equalTo(2));
        assertThat(counters.getMostCommon(), equalTo('G'));
        counts = counters.countSequences(seqList, 20);
        assertThat(counters.getMostCommon(), equalTo(' '));
    }
}
