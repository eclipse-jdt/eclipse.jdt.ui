/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class JavaPreferencesSettings  {
	
	
	public static CodeGenerationSettings getCodeGenerationSettings(IJavaProject project) {
		CodeGenerationSettings res= new CodeGenerationSettings();
		res.createComments= Boolean.valueOf(PreferenceConstants.getPreference(PreferenceConstants.CODEGEN_ADD_COMMENTS, project)).booleanValue();
		res.useKeywordThis= Boolean.valueOf(PreferenceConstants.getPreference(PreferenceConstants.CODEGEN_KEYWORD_THIS, project)).booleanValue();
		res.importOrder= getImportOrderPreference(project);
		res.importThreshold= getImportNumberThreshold(project);
		res.importIgnoreLowercase= Boolean.valueOf(PreferenceConstants.getPreference(PreferenceConstants.ORGIMPORTS_IGNORELOWERCASE, project)).booleanValue();
		res.tabWidth= CodeFormatterUtil.getTabWidth(project);
		return res;
	}
	
	/**
	 * @deprecated Use getCodeGenerationSettings(IJavaProject) instead
	 */
	public static CodeGenerationSettings getCodeGenerationSettings() {
		return getCodeGenerationSettings(null);
	}

	public static int getImportNumberThreshold(IJavaProject project) {
		String thresholdStr= PreferenceConstants.getPreference(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD, project);
		try {
			int threshold= Integer.parseInt(thresholdStr);
			if (threshold < 0) {
				threshold= Integer.MAX_VALUE;
			}
			return threshold;
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	public static String[] getImportOrderPreference(IJavaProject project) {
		String str= PreferenceConstants.getPreference(PreferenceConstants.ORGIMPORTS_IMPORTORDER, project);
		if (str != null) {
			return unpackList(str, ";"); //$NON-NLS-1$
		}
		return new String[0];
	}
	
	/**
	 * @deprecated Use getImportNumberThreshold(IJavaProject) instead
	 */
	public static int getImportNumberThreshold(IPreferenceStore prefs) {
		int threshold= prefs.getInt(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD);
		if (threshold < 0) {
			threshold= Integer.MAX_VALUE;
		}
		return threshold;
	}

	/**
	 * @deprecated Use getImportOrderPreference(IJavaProject) instead
	 */
	public static String[] getImportOrderPreference(IPreferenceStore prefs) {
		String str= prefs.getString(PreferenceConstants.ORGIMPORTS_IMPORTORDER);
		if (str != null) {
			return unpackList(str, ";"); //$NON-NLS-1$
		}
		return new String[0];
	}
		
	private static String[] unpackList(String str, String separator) {
		StringTokenizer tok= new StringTokenizer(str, separator); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < nTokens; i++) {
			res[i]= tok.nextToken().trim();
		}
		return res;
	}
	
		
}

