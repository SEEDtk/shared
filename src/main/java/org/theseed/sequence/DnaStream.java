/**
 *
 */
package org.theseed.sequence;

import java.util.Iterator;

/**
 * This is a base class that provides a list of DNA sequences.
 *
 * @author Bruce Parrello
 *
 */
public abstract class DnaStream implements SequenceStream {

    @Override
    public abstract Iterator<Sequence> iterator();

    @Override
    public boolean isProtein() {
        return false;
    }

}
