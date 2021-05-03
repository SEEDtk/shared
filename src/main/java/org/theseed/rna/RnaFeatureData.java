/**
 *
 */
package org.theseed.rna;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.theseed.genome.Feature;
import org.theseed.locations.Location;

/**
 * This object contains key data fields for a feature in the RNA database.  It serves as the header
 * for a row of expression data, each column being a sample.
 *
 *
 */
public class RnaFeatureData implements Comparable<RnaFeatureData>, Serializable {

    // FIELDS
    /** serialization version ID */
    private static final long serialVersionUID = 3941353134302649701L;
    /** feature ID */
    private String id;
    /** functional assignment */
    private String function;
    /** genome location of the feature */
    private Location location;
    /** gene name */
    private String gene;
    /** Blattner number */
    private String bNumber;
    /** atomic regulon number */
    private int atomicRegulon;
    /** operon name */
    private String operon;
    /** array of iModulon names */
    private String[] iModulons;
    /** default expression baseline */
    private double baseLine;
    /** B-number match pattern */
    private static final Pattern B_NUMBER = Pattern.compile("b\\d+");
    /** gene name match pattern */
    private static final Pattern GENE_NAME = Pattern.compile("[a-z]{3}(?:[A-Z])?");
    /** default array used to represent no iModulons */
    private static final String[] NO_MODULONS = new String[0];

    /**
     * Construct a feature-data object from a feature.
     *
     * @param feat	incoming feature
     */
    public RnaFeatureData(Feature feat) {
        this.id = feat.getId();
        this.function = feat.getPegFunction();
        this.location = feat.getLocation();
        // Get the feature's gene name and blatner number.
        this.gene = "";
        this.bNumber = "";
        for (String alias : feat.getAliases()) {
            if (B_NUMBER.matcher(alias).matches())
                this.bNumber = alias;
            else if (GENE_NAME.matcher(alias).matches())
                this.gene = alias;
        }
        // Denote there is no iModulon and we are not in an atomic regulon.
        this.atomicRegulon = 0;
        this.iModulons = NO_MODULONS;
        this.operon = "";
        this.baseLine = Double.NaN;

    }

    /**
     * This object sorts by location.
     */
    @Override
    public int compareTo(RnaFeatureData o) {
        int retVal = this.location.compareTo(o.location);
        if (retVal == 0)
            retVal = this.id.compareTo(o.id);
        return retVal;
    }

    /**
     * @return the feature ID
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return the function of this feature
     */
    public String getFunction() {
        return this.function;
    }

    /**
     * @return the location of this feature
     */
    public Location getLocation() {
        return this.location;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RnaFeatureData)) {
            return false;
        }
        RnaFeatureData other = (RnaFeatureData) obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    /** serialization method for FeatureData */
    private void writeObject(ObjectOutputStream os) throws IOException {
        os.writeUTF(this.id);
        os.writeUTF(this.function);
        os.writeUTF(this.location.toString());
        os.writeUTF(this.gene);
        os.writeUTF(this.bNumber);
        os.writeInt(this.atomicRegulon);
        os.writeUTF(this.operon);
        os.writeUTF(StringUtils.join(this.iModulons, ','));
        os.writeDouble(this.baseLine);
    }

    /** deserialization method for FeatureData */
    private void readObject(ObjectInputStream is) throws IOException {
        this.id = is.readUTF();
        this.function = is.readUTF();
        String locString = is.readUTF();
        this.location = Location.fromString(locString);
        this.gene = is.readUTF();
        this.bNumber = is.readUTF();
        this.atomicRegulon = is.readInt();
        this.operon = is.readUTF();
        this.setiModulons(is.readUTF());
        this.setBaseLine(is.readDouble());
    }

    /**
     * @return the gene name for this feature
     */
    public String getGene() {
        return this.gene;
    }

    /**
     * @return the Blatner number for this feature
     */
    public String getBNumber() {
        return this.bNumber;
    }

    /**
     * @return the atomic regulon number, or 0 if this feature is not in a regulon
     */
    public int getAtomicRegulon() {
        return this.atomicRegulon;
    }

    /**
     * @param atomicRegulon 	specify this feature's atomic regulon number
     */
    public void setAtomicRegulon(int atomicRegulon) {
        this.atomicRegulon = atomicRegulon;
    }

    /**
     * @return the array of iModulons containing this feature
     */
    public String[] getiModulons() {
        return this.iModulons;
    }

    /**
     * Specify the iModulons containing this feature.
     *
     * @param iModString	a string containing a comma-delimited list of modulon names
     */
    public void setiModulons(String iModString) {
        this.iModulons = StringUtils.split(iModString, ',');
    }

    /**
     * @return the baseline expression level for this feature
     */
    public double getBaseLine() {
        return this.baseLine;
    }

    /**
     * Specify the baseline expression level for this feature.
     *
     * @param baseLine 	the baseline to set
     */
    public void setBaseLine(double baseLine) {
        this.baseLine = baseLine;
    }

    /**
     * @return the operon name
     */
    public String getOperon() {
        return this.operon;
    }

    /**
     * Specify the operon name.
     *
     * @param operon 	the operon name to set
     */
    public void setOperon(String operon) {
        this.operon = operon;
    }

}
