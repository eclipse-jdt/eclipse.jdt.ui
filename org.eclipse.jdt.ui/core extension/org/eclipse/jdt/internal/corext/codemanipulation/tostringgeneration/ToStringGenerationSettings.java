/*******************************************************************************
 * Copyright (c) 2008, 2019 Mateusz Matela and others.
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
 *     Red Hat Inc. - modified to split logic into ToStringGenerationSettingsCore in jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.StringConverter;

public class ToStringGenerationSettings extends ToStringGenerationSettingsCore {

	private IDialogSettings dialogSettings;

	public ToStringGenerationSettings(IDialogSettings dialogSettings) {
		this.dialogSettings= dialogSettings;
		limitElements= asBoolean(dialogSettings.get(SETTINGS_LIMITELEMENTS), false);
		customArrayToString= asBoolean(dialogSettings.get(SETTINGS_IGNOREDEFAULT), true);
		toStringStyle= asInt(dialogSettings.get(SETTINGS_STRINGSTYLE), 0);
		limitValue= asInt(dialogSettings.get(SETTINGS_LIMITVALUE), 10);
		skipNulls= asBoolean(dialogSettings.get(SETTINGS_SKIPNULLS), false);
		stringFormatTemplateNumber= asInt(dialogSettings.get(SETTINGS_SELECTED_TEMPLATE), 0);
		customBuilderSettings= new CustomBuilderSettings();
		customBuilderSettings.className= asString(dialogSettings.get(SETTINGS_CUSTOMBUILDER_CLASS), ""); //$NON-NLS-1$
		customBuilderSettings.variableName= asString(dialogSettings.get(SETTINGS_CUSTOMBUILDER_LABEL), "builder"); //$NON-NLS-1$
		customBuilderSettings.appendMethod= asString(dialogSettings.get(SETTINGS_CUSTOMBUILDER_APPENDMETHOD), "append"); //$NON-NLS-1$
		customBuilderSettings.resultMethod= asString(dialogSettings.get(SETTINGS_CUSTOMBUILDER_RESULTMETHOD), "toString"); //$NON-NLS-1$
		customBuilderSettings.chainCalls= asBoolean(dialogSettings.get(SETTINGS_CUSTOMBUILDER_CHAINCALLS), false);
	}

	public ToStringGenerationSettings() {

	}

	public void writeDialogSettings() {
		dialogSettings.put(SETTINGS_LIMITELEMENTS, limitElements);
		dialogSettings.put(SETTINGS_IGNOREDEFAULT, customArrayToString);
		dialogSettings.put(SETTINGS_STRINGSTYLE, toStringStyle);
		dialogSettings.put(SETTINGS_LIMITVALUE, limitValue);
		dialogSettings.put(SETTINGS_SKIPNULLS, skipNulls);
		dialogSettings.put(SETTINGS_SELECTED_TEMPLATE, stringFormatTemplateNumber);
	}


	/**
	 * Writes given custom builder settings object to the underlying dialog settings.
	 *
	 * @param customBuilderSettings1 settings to save
	 */
	public void writeCustomBuilderSettings(CustomBuilderSettings customBuilderSettings1) {
		dialogSettings.put(SETTINGS_CUSTOMBUILDER_CLASS, customBuilderSettings1.className);
		dialogSettings.put(SETTINGS_CUSTOMBUILDER_LABEL, customBuilderSettings1.variableName);
		dialogSettings.put(SETTINGS_CUSTOMBUILDER_APPENDMETHOD, customBuilderSettings1.appendMethod);
		dialogSettings.put(SETTINGS_CUSTOMBUILDER_RESULTMETHOD, customBuilderSettings1.resultMethod);
		dialogSettings.put(SETTINGS_CUSTOMBUILDER_CHAINCALLS, customBuilderSettings1.chainCalls);
		customBuilderSettings= customBuilderSettings1;
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

	private static String asString(String string, String defaultValue) {
		if (string != null) {
			return string;
		}
		return defaultValue;
	}
}
