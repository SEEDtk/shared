/**
 *
 */
package org.theseed.utils;

/**
 * This interface is used by enums that are to be selected in web forms.
 *
 * @author Bruce Parrello
 *
 */
public interface IDescribable {

    /**
     * @return a description of this enum value
     */
    public String getDescription();
}
