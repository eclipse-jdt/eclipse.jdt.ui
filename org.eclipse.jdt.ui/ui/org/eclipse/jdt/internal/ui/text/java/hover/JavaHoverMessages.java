/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import org.eclipse.osgi.util.NLS;


/**
 * Helper class to get NLSed messages.
 */
public final class JavaHoverMessages extends NLS {

	private static final String BUNDLE_NAME= JavaHoverMessages.class.getName();

	private JavaHoverMessages() {
		// Do not instantiate
	}

	public static String AbstractAnnotationHover_action_configureAnnotationPreferences;
	public static String AbstractAnnotationHover_message_singleQuickFix;
	public static String AbstractAnnotationHover_message_multipleQuickFix;
	public static String AbstractAnnotationHover_multifix_variable_description;

	public static String JavadocHover_back;
	public static String JavadocHover_back_toElement_toolTip;
	public static String JavadocHover_constantValue_hexValue;
	public static String JavadocHover_fallback_warning;
	public static String JavadocHover_forward;
	public static String JavadocHover_forward_toElement_toolTip;
	public static String JavadocHover_forward_toolTip;
	public static String JavadocHover_openDeclaration;
	public static String JavadocHover_showInJavadoc;
	public static String JavaSourceHover_skippedLines;
	public static String JavaSourceHover_skippedLinesSymbol;

	public static String JavaTextHover_createTextHover;

	public static String NoBreakpointAnnotation_addBreakpoint;

	public static String NLSStringHover_NLSStringHover_missingKeyWarning;
	public static String NLSStringHover_NLSStringHover_PropertiesFileCouldNotBeReadWarning;
	public static String NLSStringHover_NLSStringHover_PropertiesFileNotDetectedWarning;
	public static String NLSStringHover_open_in_properties_file;
	public static String ProblemHover_action_configureProblemSeverity;

	public static String ProblemHover_chooseSettingsTypeDialog_button_project;
	public static String ProblemHover_chooseSettingsTypeDialog_button_workspace;
	public static String ProblemHover_chooseSettingsTypeDialog_checkBox_dontShowAgain;
	public static String ProblemHover_chooseSettingsTypeDialog_message;
	public static String ProblemHover_chooseSettingsTypeDialog_title;
	public static String ProblemHover_chooseCompilerSettingsTypeDialog_message;
	public static String ProblemHover_chooseCompilerSettingsTypeDialog_title;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaHoverMessages.class);
	}
}
