/**
 *
 */
package org.theseed.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.theseed.test.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import org.theseed.proteins.Function;
import org.theseed.proteins.FunctionMap;
import org.theseed.magic.MagicMap;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;

import junit.framework.TestCase;

/**
 * @author Bruce Parrello
 *
 */
public class TestRoles extends TestCase {

    /**
     * Test the Role object
     */
    public void testRole() {
        // We need to verify that the normalization works.  We create
        // equivalent roles and insure their checksums are equal.
        String rDesc = "(R)-2-hydroxyacid dehydrogenase, similar to L-sulfolactate dehydrogenase (EC 1.1.1.272)";
        Role rObj1 = new Role("2HydrDehySimiLSulf", rDesc);
        assertEquals("Role ID not stored properly.", "2HydrDehySimiLSulf", rObj1.getId());
        Role rObj2 = new Role("2HydrDehySimiLSulf2", "(R)-2-hydroxyacid dehydrogenase, similar to L-sulfolactate dehydrogenase");
        assertEquals("EC number affects checksum.", rObj1.getChecksum(), rObj2.getChecksum());
        assertEquals("Equal checksums is not equal roles.", rObj1, rObj2);
        assertEquals("Equal checksums does not compare-0 roles.", 0, rObj1.compareTo(rObj2));
        assertEquals("Equal roles have different hashcodes.", rObj1.hashCode(), rObj2.hashCode());
        Role rObj3 = new Role("2HydrDehySimiLSulf3", "(r)-2-hydroxyacid dehydrogenase, similar to L-sulfolactate dehydrogenase");
        assertEquals("Role checksums are case sensitive.", rObj1, rObj3);
        Role rObj4 = new Role("2HydrDehySimiLSulf4", "(R)-2-hydroxyacid dehydrogenase (EC 1.1.1.272), similar to L-sulfolactate dehydrogenase");
        assertEquals("Role checksums affected by EC position.", rObj1, rObj4);
        Role rObj5 = new Role("2HydrDehySimiLSulf5", "(R)-2-hydroxyacid dehydrogenase similar to L-sulfolactate dehydrogenase");
        assertTrue(rObj5.matches("(r)-2-hydroxyacid dehydrogenase, similar to L-sulfolactate dehydrogenase"));
        assertTrue(rObj5.matches("(R)-2-hydroxyacid dehydrogenase similar to L-sulfolactate Dehydrogenase"));
        assertFalse(rObj5.matches("(R)-3-hydroxyacid dehydrogenase similar to L-sulfolactate Dehydrogenase"));
        assertFalse(rObj4.matches("Phenylalanyl tRNA synthetase alpha chain"));
        assertEquals("Role checksum affected by comma.", rObj1, rObj5);
    }


    /**
     * Test role IDs.
     * @throws IOException
     */
    public void testRoleMagic() throws IOException {
        File inFile = new File("data", "words.txt");
        Scanner roleScanner = new Scanner(inFile);
        roleScanner.useDelimiter("\t|\r\n|\n");
        while (roleScanner.hasNext()) {
            String condensed = roleScanner.next();
            String full = roleScanner.next();
            assertEquals("String did not condense.", condensed, MagicMap.condense(full));
        }
        roleScanner.close();
        // Test registration
        RoleMap magicTable = new RoleMap();
        inFile = new File("data", "roles.txt");
        roleScanner = new Scanner(inFile);
        roleScanner.useDelimiter("\t|\r\n|\n");
        while (roleScanner.hasNext()) {
            String roleId = roleScanner.next();
            roleScanner.next();
            assertNull("Wrong ID found", magicTable.getItem(roleId));
            String roleDesc = roleScanner.next();
            Role newRole = new Role(roleId, roleDesc);
            magicTable.register(newRole);
            assertEquals("Registered ID did not read back.", roleDesc, magicTable.getName(roleId));
        }
        roleScanner.close();
        assertTrue("PheS not found.", magicTable.containsKey("PhenTrnaSyntAlph"));
        assertFalse("Known bad key found.", magicTable.containsKey("PhenTrnaSyntGamm"));
        String modifiedRole = "3-keto-L-gulonate-6-phosphate decarboxylase UlaK putative (L-ascorbate utilization protein D) (EC 4.1.1.85)";
        Role newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Wrong ID assigned for modified role.", "3KetoLGulo6PhosDeca6", newRole.getId());
        assertSame("Modified role did not read back.", newRole, magicTable.getItem("3KetoLGulo6PhosDeca6"));
        modifiedRole = "Unique (new) role string without numbers";
        newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Wrong ID assigned for unique role.", "UniqRoleStriWith", newRole.getId());
        assertSame("Unique role did not read back.", newRole, magicTable.getItem("UniqRoleStriWith"));
        Role findRole = magicTable.findOrInsert(modifiedRole);
        assertSame("Unique role was re-inserted.", newRole, findRole);
        modifiedRole = "Unique (old) role string without numbers";
        findRole = magicTable.findOrInsert(modifiedRole);
        assertTrue("Parenthetical did not change role ID.", findRole != newRole);
        assertEquals("Wrong ID assigned for parenthetical role.", "UniqRoleStriWith2", findRole.getId());
        assertSame("Parenthetical role did not read back.", findRole, magicTable.getItem("UniqRoleStriWith2"));
        modifiedRole = "Unique (newer) role string without numbers";
        newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Wrong ID assigned for newer role.", "UniqRoleStriWith3", newRole.getId());
        assertSame("Parenthetical role did not read back.", newRole, magicTable.getItem("UniqRoleStriWith3"));
        modifiedRole = "Unique role string 12345 with numbers";
        newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Name not stored in role.", modifiedRole, newRole.getName());
        assertEquals("Wrong ID assigned for numbered role.", "UniqRoleStri1234n1", newRole.getId());
        modifiedRole = "Unique role string 12345 with more numbers";
        newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Wrong ID assigned for second numbered role.", "UniqRoleStri1234n2", newRole.getId());
        // Test save and load.
        File saveFile = new File("data", "roles.ser");
        magicTable.save(saveFile);
        RoleMap newTable = RoleMap.load(saveFile);
        for (Role oldRole : magicTable.objectValues()) {
            newRole = newTable.getItem(oldRole.getId());
            assertNotNull("Could not find role in loaded table.", newRole);
            assertEquals("Loaded table has wrong role name.", newRole.getName(), oldRole.getName());
            assertEquals("Loaded role has wrong checksum.", newRole, oldRole);
        }
        // Test on-the-fly registration.
        RoleMap goodRoles = new RoleMap();
        goodRoles.register("Role 1", "Role 2", "Role 3");
        assertNotNull("Role 1 not found", goodRoles.containsName("role 1"));
        assertNotNull("Role 2 not found", goodRoles.containsName("role 2"));
        assertNotNull("Role 3 not found", goodRoles.containsName("role 3"));
    }

    /**
     * test role aliases
     */
    public void testAliases() {
        RoleMap roleMap = RoleMap.load(new File("data", "ssuRoles.txt"));
        Role primeRole = roleMap.getByName("SSU ribosomal protein S13e (S15p)");
        assertThat(primeRole, notNullValue());
        assertThat(primeRole.getName(), equalTo("SSU ribosomal protein S13e (S15p)"));
        Role otherRole = roleMap.getByName("SSU ribosomal protein S15p (S13e)");
        assertThat(otherRole, notNullValue());
        assertThat(otherRole.getName(), equalTo("SSU ribosomal protein S15p (S13e)"));
        primeRole = roleMap.findOrInsert("SSU ribosomal protein S13e (S15p)");
        assertThat(primeRole, notNullValue());
        assertThat(primeRole.getName(), equalTo("SSU ribosomal protein S13e (S15p)"));
        otherRole = roleMap.findOrInsert("SSU ribosomal protein S15p (S13e)");
        assertThat(otherRole, notNullValue());
        assertThat(otherRole.getName(), equalTo("SSU ribosomal protein S15p (S13e)"));
    }

    public void testFunctions() {
        FunctionMap map1 = new FunctionMap();
        Function fun1 = map1.findOrInsert("2,5-diamino-6-ribosylamino-pyrimidinone 5-phosphate reductase, fungal/archaeal (EC 1.1.1.302) / 2,5-diamino-6-ribitylamino-pyrimidinone 5-phosphate deaminase, fungal (EC 3.5.4.-)");
        Function fun2 = map1.findOrInsert("2,5-diamino-6-ribosylamino-pyrimidinone 5-phosphate reductase, fungal/archaeal (EC 1.1.1.302) / 2,5-diamino-6-ribitylamino-pyrimidinone 5-phosphate deaminase, fungal");
        assertThat(fun1, equalTo(fun2));
        Function fun3 = map1.findOrInsert("23S rRNA (adenine(2503)-C(2))-methyltransferase @ tRNA (adenine(37)-C(2))-methyltransferase (EC 2.1.1.192) ## RlmN");
        assertThat(fun3, not(equalTo(fun1)));
        Function fun4 = map1.findOrInsert("23S rRNA (adenine(2503)-C(2))-methyltransferase @ tRNA (adenine(37)-C(2))-methyltransferase (EC 2.1.1.192)");
        assertThat(fun4, equalTo(fun3));
        Function fun5 = map1.findOrInsert("23S rRNA (cytidine(1920)-2'-O)-methyltransferase (EC 2.1.1.226) @ 16S rRNA (cytidine(1409)-2'-O)-methyltransferase (EC 2.1.1.227)");
        Function fun6 = map1.findOrInsert("23S rRNA (cytidine(1920)-2'-O)-methyltransferase  @ 16S rRNA (cytidine(1409)-2'-O)-methyltransferase");
        assertThat(fun5, equalTo(fun6));
        Function fun7 = map1.findOrInsert("2-oxoglutarate/2-oxoacid ferredoxin oxidoreductase, gamma subunit (EC 1.2.7.-) / 2-oxoglutarate/2-oxoacid ferredoxin oxidoreductase, alpha subunit (EC 1.2.7.-)");
        Function fun8 = map1.findOrInsert("2-oxoglutarate/2-oxoacid ferredoxin oxidoreductase, gamma subunit (EC 1.2.7.-) / 2-oxoglutarate/2-oxoacid ferredoxin oxidoreductase, alpha subunit");
        Function fun9 = map1.findOrInsert("2-oxoglutarate/2-oxoacid ferredoxin oxidoreductase, gamma subunit / 2-oxoglutarate/2-oxoacid ferredoxin oxidoreductase, alpha subunit");
        assertThat(fun7, not(equalTo(fun5)));
        assertThat(fun7, equalTo(fun8));
        assertThat(fun8, equalTo(fun9));
        Function fun10 = map1.findOrInsert("3-ketoacyl-CoA thiolase (EC 2.3.1.16) @ Acetyl-CoA acetyltransferase (EC 2.3.1.9)");
        Function fun11 = map1.findOrInsert("3-ketoacyl-CoA thiolase @ Acetyl-CoA acetyltransferase (EC 2.3.1.9)");
        assertThat(fun10, equalTo(fun11));
        assertThat(fun10.matches("3-ketoacyl-CoA thiolase (EC 2.3.1.16) @ Acetyl-CoA acetyltransferase (EC 2.3.1.9) ## comment"), isTrue());
        assertThat(fun10.matches("3-ketoacyl-CoA thiolase @ Acetyl-CoA acetyltransferase "), isTrue());
        assertThat(fun11.matches("3-ketoacyl-CoA thiolase (EC 2.3.1.16) @ Acetyl-CoA acetyltransferase (EC 2.3.1.9) ## comment"), isTrue());
        assertThat(fun11.matches("3-ketoacyl-CoA thiolase @ Acetyl-CoA acetyltransferase "), isTrue());
        assertThat(fun11.matches("3-ketoacyl-CoA thiolase"), isFalse());
        assertThat(fun11.matches(""), isFalse());
    }
}
