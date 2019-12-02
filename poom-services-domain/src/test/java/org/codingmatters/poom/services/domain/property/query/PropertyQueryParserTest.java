package org.codingmatters.poom.services.domain.property.query;

import org.codingmatters.poom.services.domain.property.query.events.FilterEventError;
import org.codingmatters.poom.services.domain.property.query.validation.InvalidPropertyException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class PropertyQueryParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void givenExpressionsWithProperties__whenLHSPropertyValidatorIsDefined__thenValidatorIsCalledOnlyForLHSProperties() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 < r1 || l2 <r2) && l3 > r3").build();
        List<String> props = Collections.synchronizedList(new LinkedList<>());

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(s -> props.add(s) || true)
                .build(FilterEvents.noop()).parse(query);

        assertThat(props, contains("l1", "l2", "l3"));
    }

    @Test
    public void givenExpressionsWithProperties__whenLHSPropertyValidatorIsDefined_andOnePropertyIsInalid__thenParserExceptionRaised() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 < r1 || l2 <r2) && l3 > r3").build();

        thrown.expect(InvalidPropertyException.class);
        thrown.expectMessage("invalid left hand side properties : l2");

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(s -> ! "l2".equals(s))
                .build(FilterEvents.noop()).parse(query);
    }

    @Test
    public void givenExpressionsWithProperties__whenRHSPropertyValidatorIsDefined__thenValidatorIsCalledOnlyForRHSProperties() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 < r1 || l2 <r2) && l3 > r3").build();
        List<String> props = Collections.synchronizedList(new LinkedList<>());

        PropertyQueryParser.builder()
                .rightHandSidePropertyValidator(s -> props.add(s) || true)
                .build(FilterEvents.noop()).parse(query);

        assertThat(props, contains("r1", "r2", "r3"));
    }

    @Test
    public void givenExpressionsHaveLHS_andRHSProperties__whenRHSPropertyValidatorIsDefined_andOnePropertyIsInvalid__thenParserExceptionRaised() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 < r1 || l2 <r2) && l3 > r3").build();

        thrown.expect(InvalidPropertyException.class);
        thrown.expectMessage("invalid right hand side properties : r2");

        PropertyQueryParser.builder()
                .rightHandSidePropertyValidator(s -> ! "r2".equals(s))
                .build(FilterEvents.noop()).parse(query);
    }

    @Test
    public void givenEventsImplemented__whenParsing__thenExpressionIsParsed() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 > 1 || l2 >2) && l3 > 3 && ! l4 > 4").build();

        StackedFilterEvents<String> events = new StackedFilterEvents<String>() {
            @Override
            public Void graterThan(String left, Object right) throws FilterEventError {
                this.push(left + " > " + right);
                return null;
            }

            @Override
            public Void not() throws FilterEventError {
                String result = "not(" + this.pop() + ")";
                this.push(result);
                return null;
            }

            @Override
            public Void and() throws FilterEventError {
                String result = "and(";
                for (String s : this.reversedPopAll()) {
                    result += s + ", ";
                }
                result += ")";
                this.push(result);
                return null;
            }

            @Override
            public Void or() throws FilterEventError {
                String result = "or(";
                for (String s : this.reversedPopAll()) {
                    result += s + ", ";
                }
                result += ")";
                this.push(result);
                return null;
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(events.result(), is("and(and(or(l1 > 1, l2 > 2, ), l3 > 3, ), not(l4 > 4), )"));
    }

    @Test
    public void given__when__thenIsNull() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 == null").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String isNull(String property) throws FilterEventError {
                parsed.add(property + " is null");
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 is null"));
    }

    @Test
    public void given__when__thenIsNotNull() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 != null").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String isNotNull(String property) throws FilterEventError {
                parsed.add(property + " is not null");
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 is not null"));
    }

    @Test
    public void given__when__thenIsNotEqualTo() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 != 12").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String isNotEquals(String left, Object right) throws FilterEventError {
                parsed.add(left + " is not equal to " + right);
                return parsed.get(parsed.size() - 1);
            }

        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 is not equal to 12"));
    }

    @Test
    public void given__when__thenIsNotEqualToProperty() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 != l2").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String isNotEqualsProperty(String left, String right) throws FilterEventError {
                parsed.add(left + " is not equal to property " + right);
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 is not equal to property l2"));
    }
}