/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.jdt.bcoview.internal.messages"; //$NON-NLS-1$

	private static ResourceBundle resourceBundle;

	private Messages() {
		// do not init
	}

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static ResourceBundle getResourceBundle() {
		if (resourceBundle == null) {
			resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME);
		}
		return resourceBundle;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not found.
	 *
	 * @param key preference key
	 * @return translation
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = getResourceBundle();
		try {
			return bundle != null ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public static String BytecodeCompare_comparing;

	public static String BytecodeOutline_Title;
	public static String BytecodeOutline_Error;
	public static String BytecodeOutlineView_lvt_tooltip;
	public static String BytecodeOutlineView_stack_tooltip;
	public static String BytecodeOutlineView_lvt_header;
	public static String BytecodeOutlineView_stack_header;
	public static String BytecodeOutlineView_select_all_label;
	public static String BytecodeOutlineView_select_all_tooltip;
	public static String BytecodeOutlineView_select_all_description;
	public static String BytecodeOutlineView_copy_label;
	public static String BytecodeOutlineView_copy_tooltip;
	public static String BytecodeOutlineView_copy_description;
	public static String BytecodeOutlineView_find_replace_label;
	public static String BytecodeOutlineView_find_replace_tooltip;
	public static String BytecodeOutlineView_find_replace_description;
	public static String BytecodeOutlineView_find_replace_image;

	public static String BytecodeOutlineView_toggle_vertical_label;
	public static String BytecodeOutlineView_toggle_horizontal_label;
	public static String BytecodeOutlineView_toggle_automatic_label;

	public static String BytecodeReferenceView_empty_selection_text;

	public static String BCOPreferencePage_description;
	public static String BCOPreferencePage_defaultsGroup;
	public static String BCOPreferencePage_compareGroup;

	public static String BCOPreferencePage_showVariables;
	public static String BCOPreferencePage_showLineInfo;
	public static String BCOPreferencePage_showStackMap;
	public static String BCOPreferencePage_expandStackMap;
	public static String BCOPreferencePage_recalculateStackMap;
	public static String BCOPreferencePage_showRawBytecode;
	public static String BCOPreferencePage_showAnalyzer;
	public static String BCOPreferencePage_showAsmifierCode;
	public static String BCOPreferencePage_showOnlySelected;
	public static String BCOPreferencePage_linkViewToEditor;
	public static String BCOPreferencePage_linkRefViewToEditor;
	public static String BCOPreferencePage_showHexValues;

	public static String BCOPreferencePage_diffExpandStackMap;
	public static String BCOPreferencePage_diffShowStackMap;
	public static String BCOPreferencePage_diffShowLineInfo;
	public static String BCOPreferencePage_diffShowVariables;
	public static String BCOPreferencePage_diffShowAsmifierCode;

	public static String action_showVariables_text;
	public static String action_showVariables_toolTipText;
	public static String action_showVariables_image;

	public static String action_showLineInfo_text;
	public static String action_showLineInfo_toolTipText;
	public static String action_showLineInfo_image;

	public static String action_showStackmap_text;
	public static String action_showStackmap_toolTipText;
	public static String action_showStackmap_image;

	public static String action_expandStackmap_text;
	public static String action_expandStackmap_toolTipText;
	public static String action_expandStackmap_image;

	public static String action_showASMifierCode_text;
	public static String action_showASMifierCode_toolTipText;
	public static String action_showASMifierCode_image;

	public static String action_showAnalyzer_text;
	public static String action_showAnalyzer_toolTipText;
	public static String action_showAnalyzer_image;

	public static String action_showRawBytecode_text;
	public static String action_showRawBytecode_toolTipText;
	public static String action_showRawBytecode_image;

	public static String action_showOnlySelectedElement_text;
	public static String action_showOnlySelectedElement_toolTipText;
	public static String action_showOnlySelectedElement_image;

	public static String action_linkViewToEditor_text;
	public static String action_linkViewToEditor_toolTipText;
	public static String action_linkViewToEditor_image;

	public static String action_linkRefViewToEditor_text;
	public static String action_linkRefViewToEditor_toolTipText;
	public static String action_linkRefViewToEditor_image;

	public static String action_showHexValues_text;
	public static String action_showHexValues_toolTipText;
	public static String action_showHexValues_image;
}
