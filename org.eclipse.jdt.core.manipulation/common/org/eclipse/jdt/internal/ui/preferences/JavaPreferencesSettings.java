/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Red Hat Inc. - moved to jdt.core.manipulation and modified
 *     Microsoft Corporation - add helper method to read preferences from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettingsConstants;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class JavaPreferencesSettings  {

	public static CodeGenerationSettings getCodeGenerationSettings(IJavaProject project) {
		CodeGenerationSettings res= new CodeGenerationSettings();
		res.createComments= Boolean.parseBoolean(JavaManipulation.getPreference(CodeGenerationSettingsConstants.CODEGEN_ADD_COMMENTS, project));
		res.useKeywordThis= Boolean.parseBoolean(JavaManipulation.getPreference(CodeGenerationSettingsConstants.CODEGEN_KEYWORD_THIS, project));
		res.overrideAnnotation= Boolean.parseBoolean(JavaManipulation.getPreference(CodeGenerationSettingsConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, project));
		res.importIgnoreLowercase= Boolean.parseBoolean(JavaManipulation.getPreference(CodeGenerationSettingsConstants.ORGIMPORTS_IGNORELOWERCASE, project));
		res.tabWidth= CodeFormatterUtil.getTabWidth(project);
		res.indentWidth= CodeFormatterUtil.getIndentWidth(project);
		return res;
	}

	public static CodeGenerationSettings getCodeGenerationSettings(ICompilationUnit cu) {
		CodeGenerationSettings res= new CodeGenerationSettings();
		res.createComments= Boolean.parseBoolean(getPreference(CodeGenerationSettingsConstants.CODEGEN_ADD_COMMENTS, cu));
		res.useKeywordThis= Boolean.parseBoolean(getPreference(CodeGenerationSettingsConstants.CODEGEN_KEYWORD_THIS, cu));
		res.overrideAnnotation= Boolean.parseBoolean(getPreference(CodeGenerationSettingsConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, cu));
		res.importIgnoreLowercase= Boolean.parseBoolean(getPreference(CodeGenerationSettingsConstants.ORGIMPORTS_IGNORELOWERCASE, cu));
		res.tabWidth= CodeFormatterUtil.getTabWidth(cu);
		res.indentWidth= CodeFormatterUtil.getIndentWidth(cu);
		return res;
	}

	private static String getPreference(String key, ICompilationUnit cu) {
		if (cu != null) {
			String val= cu.getCustomOptions().getOrDefault(key, null);
			if (val != null) {
				return val;
			}
			return JavaManipulation.getPreference(key, cu.getJavaProject());
		}
		return JavaManipulation.getPreference(key, null);
	}

	private JavaPreferencesSettings() {
	}

}

