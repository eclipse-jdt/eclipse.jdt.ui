package org.eclipse.jdt.internal.ui.preferences;

import java.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;

public class JavaPreferencesSettings  {
	
	public static CodeGenerationSettings getCodeGenerationSettings() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		
		CodeGenerationSettings res= new CodeGenerationSettings();
		res.createFileComments= store.getBoolean(PreferenceConstants.CODEGEN__FILE_COMMENTS);
		res.createComments= store.getBoolean(PreferenceConstants.CODEGEN__JAVADOC_STUBS);
		res.createNonJavadocComments= store.getBoolean(PreferenceConstants.CODEGEN__NON_JAVADOC_COMMENTS);
		res.importOrder= getImportOrderPreference(store);
		res.importThreshold= getImportNumberThreshold(store);
		res.fieldPrefixes= getGetterStetterPrefixes(store);
		res.fieldSuffixes= getGetterStetterSuffixes(store);
		res.tabWidth= JavaCore.getPlugin().getPluginPreferences().getInt(JavaCore.FORMATTER_TAB_SIZE);
		return res;
	}

	private static int getImportNumberThreshold(IPreferenceStore prefs) {
		int threshold= prefs.getInt(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD);
		if (threshold < 0) {
			threshold= Integer.MAX_VALUE;
		}
		return threshold;
	}


	private static String[] getImportOrderPreference(IPreferenceStore prefs) {
		String str= prefs.getString(PreferenceConstants.ORGIMPORTS_IMPORTORDER);
		if (str != null) {
			return unpackList(str, ";");
		}
		return new String[0];
	}
	
	private static String[] getGetterStetterPrefixes(IPreferenceStore prefs) {
		if (prefs.getBoolean(PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX)) {
			String str= prefs.getString(PreferenceConstants.CODEGEN_GETTERSETTER_PREFIX);
			if (str != null) {
				return unpackList(str, ",");
			}
		}
		return new String[0];
	}

	private static String[] getGetterStetterSuffixes(IPreferenceStore prefs) {
		if (prefs.getBoolean(PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX)) {
			String str= prefs.getString(PreferenceConstants.CODEGEN_GETTERSETTER_SUFFIX);
			if (str != null) {
				return unpackList(str, ",");
			}
		}
		return new String[0];
	}	
	
	private static String[] unpackList(String str, String separator) {
		StringTokenizer tok= new StringTokenizer(str, separator); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < nTokens; i++) {
			res[i]= tok.nextToken();
		}
		return res;
	}
	
	private static int getTabSize() {
		return JavaCore.getPlugin().getPluginPreferences().getInt(JavaCore.FORMATTER_TAB_SIZE);
	}	
		
}

