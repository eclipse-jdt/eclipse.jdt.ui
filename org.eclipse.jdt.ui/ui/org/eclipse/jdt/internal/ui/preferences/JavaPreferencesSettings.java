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

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class JavaPreferencesSettings  {
	
	public static CodeGenerationSettings getCodeGenerationSettings() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		
		CodeGenerationSettings res= new CodeGenerationSettings();
		res.createComments= store.getBoolean(PreferenceConstants.CODEGEN_ADD_COMMENTS);
		res.useKeywordThis= store.getBoolean(PreferenceConstants.CODEGEN_KEYWORD_THIS);
		res.importOrder= getImportOrderPreference(store);
		res.importThreshold= getImportNumberThreshold(store);
		res.tabWidth= CodeFormatterUtil.getTabWidth();
		return res;
	}

	public static int getImportNumberThreshold(IPreferenceStore prefs) {
		int threshold= prefs.getInt(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD);
		if (threshold < 0) {
			threshold= Integer.MAX_VALUE;
		}
		return threshold;
	}


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

