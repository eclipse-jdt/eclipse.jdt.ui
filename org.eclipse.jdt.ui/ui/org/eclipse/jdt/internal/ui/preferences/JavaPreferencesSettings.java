package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;

public class JavaPreferencesSettings  {
	
	public static CodeGenerationSettings getCodeGenerationSettings() {
		CodeGenerationSettings res= new CodeGenerationSettings();
		res.createComments= CodeGenerationPreferencePage.doCreateComments();
		res.createNonJavadocComments= CodeGenerationPreferencePage.doNonJavaDocSeeComments();
		res.importOrder= ImportOrganizePreferencePage.getImportOrderPreference();
		res.importThreshold= ImportOrganizePreferencePage.getImportNumberThreshold();
		return res;
	}
	
}

