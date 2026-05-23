package my.edu.um.dissertation.observability.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pure-helper tests — no EMF runtime required. */
class EmfHelpersTest {

    @ParameterizedTest
    @CsvSource({
            "plain,         plain",
            "with space,    with_space",
            "dot.separated, dot_separated",
            "a/b-c.d,       a_b_c_d"
    })
    void identifierReplacesNonAlnumWithUnderscore(String raw, String expected) {
        assertEquals(expected, EmfHelpers.identifier(raw));
    }

    @Test
    void identifierTreatsNullAsEmptyString() {
        assertEquals("", EmfHelpers.identifier(null));
    }
}
