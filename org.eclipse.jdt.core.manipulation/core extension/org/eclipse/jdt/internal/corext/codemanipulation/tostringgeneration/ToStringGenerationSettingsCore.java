/*******************************************************************************
 * Copyright (c) 2019 Mateusz Matela and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - [code manipulation] [dcr] toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=26070
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] finish toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=267710
 *     Red Hat Inc. - copied and modified to make ToStringGenerationSettingsCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;

public class ToStringGenerationSettingsCore extends CodeGenerationSettings {

	public static final String SETTINGS_SELECTED_TEMPLATE= "ToStringTemplateSelected"; //$NON-NLS-1$

	public static final String SETTINGS_STRINGSTYLE= "StringStyle"; //$NON-NLS-1$

	public static final String SETTINGS_SKIPNULLS= "SkipNull"; //$NON-NLS-1$

	public static final String SETTINGS_IGNOREDEFAULT= "IgnoreDefault"; //$NON-NLS-1$

	public static final String SETTINGS_LIMITELEMENTS= "LimitElements"; //$NON-NLS-1$

	public static final String SETTINGS_LIMITVALUE= "LimitValue"; //$NON-NLS-1$

	public static final String SETTINGS_TEMPLATE_NAMES= "ToStringTemplateNames"; //$NON-NLS-1$

	public static final String SETTINGS_TEMPLATES= "ToStringTemplates"; //$NON-NLS-1$

	public static final String SETTINGS_CUSTOMBUILDER_CLASS= "CustomBuilderClass"; //$NON-NLS-1$

	public static final String SETTINGS_CUSTOMBUILDER_LABEL= "CustomBuilderLabel"; //$NON-NLS-1$

	public static final String SETTINGS_CUSTOMBUILDER_APPENDMETHOD= "CustomBuilderAppendMethod"; //$NON-NLS-1$

	public static final String SETTINGS_CUSTOMBUILDER_RESULTMETHOD= "CustomBuilderResultMethod"; //$NON-NLS-1$

	public static final String SETTINGS_CUSTOMBUILDER_CHAINCALLS= "CustomBuilderChainCalls"; //$NON-NLS-1$

	/**
	 * Container for settings specific for custom toString() generator code style
	 */
	public static class CustomBuilderSettings {
		/**
		 * what class should be used as a custom toString() builder (this is a fully qualified and
		 * Parameterized name)
		 **/
		public String className;

		/** identifier for the local variable that holds the custom toString() builder in generated code **/
		public String variableName;

		/** name of a custom toString() builder's methods that should be called to append content **/
		public String appendMethod;

		/** name of a custom toString() builder method that should be called to retrieve result **/
		public String resultMethod;

		/** should custom toString() builder method calls be joined into chains? **/
		public boolean chainCalls;
	}

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

	/** settings specific for custom builder code style **/
	public CustomBuilderSettings customBuilderSettings;

	/**
	 * Returns a copy of customBuilderSettings. Changes made in the returned object will not affect
	 * this settings object.  To save changes made to the object, use:
	 *
	 * ToStringGenerationSettings#writeCustomBuilderSettings(ToStringGenerationSettings.CustomBuilderSettings)
	 *
	 * @return copy of custom builder settings object
	 */
	public CustomBuilderSettings getCustomBuilderSettings() {
		CustomBuilderSettings result= new CustomBuilderSettings();
		result.className= customBuilderSettings.className;
		result.variableName= customBuilderSettings.variableName;
		result.appendMethod= customBuilderSettings.appendMethod;
		result.resultMethod= customBuilderSettings.resultMethod;
		result.chainCalls= customBuilderSettings.chainCalls;
		return result;
	}

}
