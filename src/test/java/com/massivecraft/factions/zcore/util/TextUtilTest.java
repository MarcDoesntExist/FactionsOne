package com.massivecraft.factions.zcore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for the Adventure parser and legacy string helpers.
 *
 * @author MarcDoesntExists
 */
class TextUtilTest {

    private final TextUtil text = new TextUtil();

    private static Component firstLeaf(Component component) {
        Component current = component;
        while (!current.children().isEmpty()) {
            current = current.children().get(0);
        }
        return current;
    }

    @Test
    void plainTextRoundTrips() {
        assertEquals("hello", TextUtil.plain(text.parse("hello")));
        assertEquals("", TextUtil.plain(text.parse(null)));
        assertEquals("", TextUtil.plain(null));
    }

    @Test
    void builtInTagsChangeColor() {
        Component red = text.parse("<red>hello");
        assertEquals(NamedTextColor.DARK_RED, firstLeaf(red).color());
        assertEquals("hello", TextUtil.plain(red));

        Component rose = text.parse("<rose>hello");
        assertEquals(NamedTextColor.RED, firstLeaf(rose).color());

        Component blue = text.parse("<blue>hello");
        assertEquals(NamedTextColor.BLUE, firstLeaf(blue).color());
    }

    @Test
    void decorationsStackOnTopOfColors() {
        Component component = text.parse("<rose>&lhello");
        Component leaf = firstLeaf(component);

        assertEquals(NamedTextColor.RED, leaf.color());
        assertEquals(TextDecoration.State.TRUE, leaf.decoration(TextDecoration.BOLD));
        assertEquals("hello", TextUtil.plain(component));
    }

    @Test
    void aNewColorResetsPreviousDecorations() {
        Component component = text.parse("<rose>&l<blue>hello");
        Component leaf = firstLeaf(component);

        assertEquals(NamedTextColor.BLUE, leaf.color());
        assertEquals(TextDecoration.State.NOT_SET, leaf.decoration(TextDecoration.BOLD));
    }

    @Test
    void legacyColorCodesAreParsed() {
        assertEquals(NamedTextColor.RED, firstLeaf(text.parse("&chello")).color());
        assertEquals(NamedTextColor.GREEN, firstLeaf(text.parse("\u00A7ahello")).color());
        assertEquals("hello", TextUtil.plain(text.parse("&chello")));
    }

    @Test
    void escapedCharactersStayLiteral() {
        assertEquals("a & b", TextUtil.plain(text.parse("a && b")));
        assertEquals("100%", TextUtil.plain(text.parse("100%%")));
    }

    @Test
    void unknownTagsStayVisible() {
        assertEquals("<notatag>", TextUtil.plain(text.parse("<notatag>")));
    }

    @Test
    void placeholdersAreFormatted() {
        assertEquals("Hello world", TextUtil.plain(text.parse("Hello %s", "world")));
        assertEquals("42", TextUtil.plain(text.parse("%d", 42)));
        assertEquals("5", TextUtil.plain(text.parse("%s", 5)));
    }

    @Test
    void componentArgumentsKeepTheirOwnStyle() {
        Component value = Component.text("colored", NamedTextColor.GOLD);
        Component result = text.parse("Value: %s", value);

        assertEquals("Value: colored", TextUtil.plain(result));
        assertTrue(result.children().stream().anyMatch(child -> NamedTextColor.GOLD.equals(child.color())));
    }

    @Test
    void tagReplacementUsesOnlyKnownTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("name", "Steve");
        tags.put("faction", "Rebels");

        assertEquals("Hello Steve of Rebels", TextUtil.replaceTags(
                "Hello <name> of <faction>", tags));
        assertEquals("Keep <unknown>", TextUtil.replaceTags("Keep <unknown>", tags));
    }

    @Test
    void legacyColorConversionHelpersWork() {
        assertEquals("\u00A7cRed", TextUtil.parseColorAmp("&cRed"));
        assertEquals("&", TextUtil.parseColorAmp("&&"));
        assertEquals("\u00A74", TextUtil.parseColorTags("<red>"));
        assertEquals("\u00A7c", TextUtil.parseColorTags("<rose>"));
        assertEquals("", TextUtil.parseColorTags("<empty>"));
        assertEquals("\u00A7c", TextUtil.parseColor("&c"));
    }

    @Test
    void configStylesAreParsedFromLegacyCodes() {
        assertEquals(NamedTextColor.RED, TextUtil.styleFromConfig("&c").color());
        assertEquals(NamedTextColor.GREEN, TextUtil.styleFromConfig("\u00A7a").color());
        assertTrue(TextUtil.styleFromConfig(null).isEmpty());
        assertTrue(TextUtil.styleFromConfig("").isEmpty());
    }

    @Test
    void stringHelpersHandleEdgeCases() {
        assertEquals("Faction", TextUtil.upperCaseFirst("faction"));
        assertEquals("", TextUtil.upperCaseFirst(""));
        assertNull(TextUtil.upperCaseFirst(null));
        assertEquals("ababab", TextUtil.repeat("ab", 3));
        assertEquals("", TextUtil.repeat("ab", 0));
        assertEquals("", TextUtil.repeat("ab", -1));
    }

    @Test
    void implodeHelpersJoinCollections() {
        assertEquals("a, b, c", TextUtil.implode(List.of("a", "b", "c"), ", "));
        assertEquals("", TextUtil.implode(List.of(), ", "));

        assertEquals("a, b and c", TextUtil.implodeCommaAnd(
                new ArrayList<>(List.of("a", "b", "c")), ", ", " and "));
        assertEquals("a and b", TextUtil.implodeCommaAnd(
                new ArrayList<>(List.of("a", "b")), ", ", " and "));
        assertEquals("a", TextUtil.implodeCommaAnd(
                new ArrayList<>(List.of("a")), ", ", " and "));
        assertEquals("", TextUtil.implodeCommaAnd(
                new ArrayList<>(), ", ", " and "));
    }

    @Test
    void timeDescriptionsUseTheLargestUnitsFirst() {
        assertEquals("just now", TextUtil.getTimeDeltaDescriptionRelNow(0));
        assertEquals("1 seconds from now", TextUtil.getTimeDeltaDescriptionRelNow(1000));
        assertEquals("1 minutes and 5 seconds from now", TextUtil.getTimeDeltaDescriptionRelNow(65_000));
        assertEquals("1 hours from now", TextUtil.getTimeDeltaDescriptionRelNow(3_600_000));
        assertEquals("1 seconds ago", TextUtil.getTimeDeltaDescriptionRelNow(-1000));
    }

    @Test
    void bestPrefixMatchPrefersTheShortestCandidate() {
        List<String> candidates = List.of("faction", "farm", "farmer");

        assertEquals("farm", TextUtil.getBestStartWithCI(candidates, "far"));
        assertEquals("farm", TextUtil.getBestStartWithCI(candidates, "FAR"));
        assertEquals("farmer", TextUtil.getBestStartWithCI(candidates, "farmer"));
        assertNull(TextUtil.getBestStartWithCI(candidates, "zzz"));
    }
}
