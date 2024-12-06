/*******************************************************************************
 * Copyright (c) 2024 Broadcom Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alex Boyko (Broadcom Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.jdt.ui.PreferenceConstants;

public record SyntaxColorHighlighting(String label, String preferenceKey) {

	private static final String BOLD= PreferenceConstants.EDITOR_BOLD_SUFFIX;
	private static final String ITALIC= PreferenceConstants.EDITOR_ITALIC_SUFFIX;
	private static final String STRIKETHROUGH= PreferenceConstants.EDITOR_STRIKETHROUGH_SUFFIX;
	private static final String UNDERLINE= PreferenceConstants.EDITOR_UNDERLINE_SUFFIX;

	private static SyntaxColorHighlighting[] fgSyntaxColorHighlighting = null;

	public static SyntaxColorHighlighting[] getSyntaxColorHighlightings() {
		if (fgSyntaxColorHighlighting == null) {
			fgSyntaxColorHighlighting = new SyntaxColorHighlighting[] {
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_javaDocKeywords, PreferenceConstants.EDITOR_JAVADOC_KEYWORD_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_javaDocHtmlTags, PreferenceConstants.EDITOR_JAVADOC_TAG_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_javaDocLinks, PreferenceConstants.EDITOR_JAVADOC_LINKS_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_javaDocOthers, PreferenceConstants.EDITOR_JAVADOC_DEFAULT_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_multiLineComment, PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_singleLineComment, PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_javaCommentTaskTags, PreferenceConstants.EDITOR_TASK_TAG_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_keywords, PreferenceConstants.EDITOR_JAVA_KEYWORD_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_returnKeyword, PreferenceConstants.EDITOR_JAVA_KEYWORD_RETURN_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_operators, PreferenceConstants.EDITOR_JAVA_OPERATOR_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_brackets, PreferenceConstants.EDITOR_JAVA_BRACKET_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_strings, PreferenceConstants.EDITOR_STRING_COLOR),
					new SyntaxColorHighlighting(PreferencesMessages.JavaEditorPreferencePage_others, PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR)
			};
		}
		return fgSyntaxColorHighlighting;
	}

	public String getBoldPreferenceKey() {
		return preferenceKey + BOLD;
	}

	public String getItalicPreferenceKey() {
		return preferenceKey + ITALIC;
	}

	public String getStrikethroughPreferenceKey() {
		return preferenceKey + STRIKETHROUGH;
	}

	public String getUnderlinePreferenceKey() {
		return preferenceKey + UNDERLINE;
	}

}
