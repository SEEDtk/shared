/**
 *
 */
package org.theseed.subsystems.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.theseed.basic.ParseFailureException;

/**
 * @author Bruce Parrello
 *
 */
class TestSubsystemRules {

    @Test
    void testCompiler() throws ParseFailureException {
        // First, we have to set up a name space for the role IDs.
        Map<String, SubsystemRule> nameSpace = Map.of(
                "1.3",   new SubsystemPrimitiveRule("MethCoaMuta"),
                "1.3.N", new SubsystemPrimitiveRule("MethCoaMutaN"),
                "1.3.C", new SubsystemPrimitiveRule("MethCoaMutaC"),
                "1.3l",  new SubsystemPrimitiveRule("MethCoaMutaL"),
                "1.3s1(a)", new SubsystemPrimitiveRule("MethCoaMutaLs"),
                "mcl1",  new SubsystemPrimitiveRule("MalyCoaLyas"),
                "6", new SubsystemPrimitiveRule("MalyCoaLyas")
                );
        String rule = "1.3 or (1.3.N and 1.3.C) or 2 of {1.3l, 1.3s1(a), 6}";
        List<String> tokens = RuleCompiler.tokenize(rule);
        assertThat(tokens, contains("1.3", "or", "(", "1.3.N", "and", "1.3.C", ")", "or", "2", "of", "{", "1.3l",
                ",", "1.3s1(a)", ",", "6", "}"));
        SubsystemRule compiled = RuleCompiler.parseRule(rule, nameSpace);
        Set<String> roles = Set.of("FakeRule1", "MethCoaMuta", "MethCoaMutaC");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MethCoaMutaN", "FakeRule2");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("MethCoaMutaLs");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MethCoaMutaLs", "FakeRule2");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MethCoaMutaLs", "FakeRule2", "MethCoaMutaC");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MethCoaMutaLs", "FakeRule2", "MalyCoaLyas");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MethCoaMutaL", "FakeRule2");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MethCoaLyas", "FakeRule2");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MalyCoaLyas", "MethCoaMutaLs");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MethCoaMutaLs", "FakeRule2", "MethCoaMutaC", "MethCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(true));
        String rule2 = "(" + rule + ")";
        SubsystemRule compiled2 = RuleCompiler.parseRule(rule2, nameSpace);
        assertThat(compiled, equalTo(compiled2));
        rule = "(mcl1 and not (1 of {1.3, (1.3.N)}) or (1.3s1(a) and not 1.3.C)";
        compiled = RuleCompiler.parseRule(rule, nameSpace);
        roles = Set.of("FakeRule1", "MethCoaMuta", "MethCoaMutaC");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MethCoaMutaN", "MalyCoaLyas");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("MethCoaMutaLs");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MethCoaMutaLs", "MethCoaMutaC");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MethCoaMutaLs", "FakeRule2", "MethCoaMutaC");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MethCoaMutaLs", "FakeRule2");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MethCoaMutaL", "FakeRule2");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MalyCoaLyas", "FakeRule2");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MalyCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MethCoaMutaN", "FakeRule2", "MethCoaMutaC", "MalyCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(false));
        rule = "1 of { 1.3, 1.3.C, 1.3.N and 1.3s1(a) } and mcl1";
        compiled = RuleCompiler.parseRule(rule, nameSpace);
        roles = Set.of("FakeRule1", "MethCoaMutaN", "FakeRule2", "MethCoaMutaC", "MalyCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MethCoaMutaN", "FakeRule2", "MalyCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MethCoaMutaN", "MethCoaMutaLs", "MalyCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MalyCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MalyCoaLyas", "MethCoaMuta");
        assertThat(compiled.check(roles), equalTo(true));
        rule = "2 of {1 of { 1.3, 1.3.C, 1.3.N and 1.3s1(a) }, mcl1}";
        compiled = RuleCompiler.parseRule(rule, nameSpace);
        roles = Set.of("FakeRule1", "MethCoaMutaN", "FakeRule2", "MethCoaMutaC", "MalyCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MethCoaMutaN", "FakeRule2", "MalyCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MethCoaMutaN", "MethCoaMutaLs", "MalyCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(true));
        roles = Set.of("FakeRule1", "MalyCoaLyas", "MethCoaMutaL");
        assertThat(compiled.check(roles), equalTo(false));
        roles = Set.of("FakeRule1", "MalyCoaLyas", "MethCoaMuta");
        assertThat(compiled.check(roles), equalTo(true));
    }

}
