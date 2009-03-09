/*******************************************************************************
 * Copyright (c) 2008 Mateusz Matela and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - [code manipulation] [dcr] toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=26070
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.StringConverter;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;

public class ToStringGenerationSettings extends CodeGenerationSettings {

	public static final String SETTINGS_SELECTED_TEMPLATE= "ToStringTemplateSelected"; //$NON-NLS-1$

	public static final String SETTINGS_STRINGSTYLE= "StringStyle"; //$NON-NLS-1$

	public static final String SETTINGS_SKIPNULLS= "SkipNull"; //$NON-NLS-1$

	public static final String SETTINGS_IGNOREDEFAULT= "IgnoreDefault"; //$NON-NLS-1$

	public static final String SETTINGS_LIMITELEMENTS= "LimitElements"; //$NON-NLS-1$

	public static final String SETTINGS_LIMITVALUE= "LimitValue"; //$NON-NLS-1$

	public static final String SETTINGS_TEMPLATE_NAMES= "ToStringTemplateNames"; //$NON-NLS-1$

	public static final String SETTINGS_TEMPLATES= "ToStringTemplates"; //$NON-NLS-1$	

	/** which template should be used to format the output of the toString() method? */
	public int stringFormatTemplateNumber;

	/**
	 * what is the template (redundancy - this field can be determined basing on
	 * <code>GenerateToStringDialog.getTemplates()</code> and
	 * <code>stringFormatTemplateNumber</code>, but this way it's more convenient)
	 */
	public String stringFormatTemplate;

	/** what style of code should the toString() method have? */
	public int toStringStyle;

	/** should the toString() method skip null values? */
	public boolean skipNulls;

	/** should the toString() method use its own way to show elements of an array? */
	public boolean customArrayToString;

	/**
	 * should the toString() limit maximum number of elements of arrays/Collections to be
	 * listed?
	 */
	public boolean limitElements;

	/** what is the maximum number of elements in array/Collection to show? */
	public int limitValue;

	/** should blocks be forced in if/for/while statements? */
	public boolean useBlocks;

	/** can generated code use jdk 1.5 API? **/
	public boolean is50orHigher;

	/** can generated code use jdk 1.6 API? **/
	public boolean is60orHigher;

	public ToStringGenerationSettings(IDialogSettings dialogSettings) {
		limitElements= asBoolean(dialogSettings.get(SETTINGS_LIMITELEMENTS), false);
		customArrayToString= asBoolean(dialogSettings.get(SETTINGS_IGNOREDEFAULT), true);
		toStringStyle= asInt(dialogSettings.get(SETTINGS_STRINGSTYLE), 0);
		limitValue= asInt(dialogSettings.get(SETTINGS_LIMITVALUE), 10);
		skipNulls= asBoolean(dialogSettings.get(SETTINGS_SKIPNULLS), false);
		stringFormatTemplateNumber= asInt(dialogSettings.get(SETTINGS_SELECTED_TEMPLATE), 0);
	}

	public ToStringGenerationSettings() {

	}

	public void writeDialogSettings(IDialogSettings dialogSettings) {
		dialogSettings.put(SETTINGS_LIMITELEMENTS, limitElements);
		dialogSettings.put(SETTINGS_IGNOREDEFAULT, customArrayToString);
		dialogSettings.put(SETTINGS_STRINGSTYLE, toStringStyle);
		dialogSettings.put(SETTINGS_LIMITVALUE, limitValue);
		dialogSettings.put(SETTINGS_SKIPNULLS, skipNulls);
		dialogSettings.put(SETTINGS_SELECTED_TEMPLATE, stringFormatTemplateNumber);
	}
	
	private boolean asBoolean(String string, boolean defaultValue) {
		if (string != null) {
			return StringConverter.asBoolean(string, defaultValue);
		}
		return defaultValue;
	}
	
	private static int asInt(String string, int defaultValue) {
		if (string != null) {
			return StringConverter.asInt(string, defaultValue);
		}
		return defaultValue;
	}
}