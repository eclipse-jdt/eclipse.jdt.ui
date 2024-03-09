/*******************************************************************************
* Copyright (c) 2024 Jozef Tomek and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Jozef Tomek - initial API and implementation
*******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
public class JavadocStylingMessages extends NLS {

	private static final String BUNDLE_NAME= JavadocStylingMessages.class.getName();

	private JavadocStylingMessages() {
		// Do not instantiate
	}

	public static String JavadocStyling_styling_typeParamsReferencesColoring;
	public static String JavadocStyling_styling_typeParamsLevelsColoring;
	public static String JavadocStyling_styling_formatting;
	public static String JavadocStyling_styling_wrapping;

	public static String JavadocStyling_stylingMenu;
	public static String JavadocStyling_toggle;
	public static String JavadocStyling_disabledTooltip;
	public static String JavadocStyling_noEnhancements;
	public static String JavadocStyling_noEnhancementsTooltip;
	public static String JavadocStyling_colorPreferences_menu;
	public static String JavadocStyling_colorPreferences_typeParameterReference;
	public static String JavadocStyling_colorPreferences_typeParameterLevel;
	public static String JavadocStyling_colorPreferences_resetAll;
	public static String JavadocStyling_colorPreferences_noTypeParameters;
	public static String JavadocStyling_colorPreferences_unusedTypeParameter;
	public static String JavadocStyling_stylingTooltip_prefix;
	public static String JavadocStyling_stylingTooltip_preference_off;
	public static String JavadocStyling_stylingTooltip_preference_hover;
	public static String JavadocStyling_stylingTooltip_preference_always;

	public static String JavadocStyling_stylingPreview_watermark;
	public static String JavadocStyling_stylingPreview_typeParamsReferencesColoring;
	public static String JavadocStyling_stylingPreview_typeParamsLevelsColoring;
	public static String JavadocStyling_stylingPreview_formatting;
	public static String JavadocStyling_stylingPreview_wrapping;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavadocStylingMessages.class);
	}

}
