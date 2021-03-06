package org.jabref.migrations;

import java.util.prefs.Preferences;

import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreferencesMigrationsTest {

    private JabRefPreferences prefs;
    private Preferences mainPrefsNode;

    private final String[] oldStylePatterns = new String[] {"\\bibtexkey",
                                                            "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
    private final String[] newStylePatterns = new String[] {"[bibtexkey]",
                                                            "[bibtexkey] - [fulltitle]"};

    @BeforeEach
    public void setUp() {
        prefs = mock(JabRefPreferences.class);
        mainPrefsNode = mock(Preferences.class);

    }

    @Test
    public void testOldStyleBibtexkeyPattern0() {
        when(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(oldStylePatterns[0]);
        when(mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(oldStylePatterns[0]);
        when(prefs.hasKey(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(true);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(prefs, mainPrefsNode);

        verify(prefs).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
        verify(mainPrefsNode).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);

    }

    @Test
    public void testOldStyleBibtexkeyPattern1() {

        when(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(oldStylePatterns[1]);
        when(mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(oldStylePatterns[1]);
        when(prefs.hasKey(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(true);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(prefs, mainPrefsNode);

        verify(prefs).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[1]);
        verify(mainPrefsNode).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[1]);

    }

    @Test
    public void testArbitraryBibtexkeyPattern() {
        String arbitraryPattern = "[anyUserPrividedString]";

        when(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(arbitraryPattern);
        when(mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(arbitraryPattern);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(prefs, mainPrefsNode);

        verify(prefs, never()).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);
        verify(mainPrefsNode, never()).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);

    }

    @Test
    public void testPreviewStyle() {
        String oldPreviewStyle = "<font face=\"sans-serif\">"
                                 + "<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>"
                                 + "\\end{bibtexkey}</b><br>__NEWLINE__"
                                 + "\\begin{author} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\author}<BR>\\end{author}__NEWLINE__"
                                 + "\\begin{editor} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\editor} "
                                 + "<i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}__NEWLINE__"
                                 + "\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                                 + "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                                 + "\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                                 // Include the booktitle field for @inproceedings, @proceedings, etc.
                                 + "\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}__NEWLINE__"
                                 + "\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                                 + "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                                 + "\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                                 + "\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}"
                                 + "\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}__NEWLINE__"
                                 + "\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__"
                                 + "\\begin{review}<BR><BR><b>Review: </b> \\format[HTMLChars]{\\review} \\end{review}"
                                 + "</dd>__NEWLINE__<p></p></font>";

        String newPreviewStyle = "<font face=\"sans-serif\">"
                                 + "<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>"
                                 + "\\end{bibtexkey}</b><br>__NEWLINE__"
                                 + "\\begin{author} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\author}<BR>\\end{author}__NEWLINE__"
                                 + "\\begin{editor} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\editor} "
                                 + "<i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}__NEWLINE__"
                                 + "\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                                 + "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                                 + "\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                                 // Include the booktitle field for @inproceedings, @proceedings, etc.
                                 + "\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}__NEWLINE__"
                                 + "\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                                 + "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                                 + "\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                                 + "\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}"
                                 + "\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}__NEWLINE__"
                                 + "\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__"
                                 + "\\begin{comment}<BR><BR><b>Comment: </b> \\format[HTMLChars]{\\comment} \\end{comment}"
                                 + "</dd>__NEWLINE__<p></p></font>";

        prefs.setPreviewStyle(oldPreviewStyle);
        when(prefs.getPreviewStyle()).thenReturn(oldPreviewStyle);

        PreferencesMigrations.upgradePreviewStyleFromReviewToComment(prefs);

        verify(prefs).setPreviewStyle(newPreviewStyle);

    }
}
