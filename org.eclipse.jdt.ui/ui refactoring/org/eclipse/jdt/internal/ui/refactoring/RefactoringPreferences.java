/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class RefactoringPreferences {
	
	public static final String FATAL_SEVERITY= "4";
	public static final String ERROR_SEVERITY= "3";
	public static final String WARNING_SEVERITY= "2";
	public static final String INFO_SEVERITY= "1";
	public static final String OK_SEVERITY= "0";
		
	public static final String PREF_ERROR_PAGE_SEVERITY_THRESHOLD= "Refactoring.ErrorPage.severityThreshold";
	public static final String PREF_JAVA_STYLE_GUIDE_CONFORM= "Refactoring.javaStyleGuideConform";

	static int getCheckPassedSeverity() {	
		String value= JavaPlugin.getDefault().getPreferenceStore().getString(PREF_ERROR_PAGE_SEVERITY_THRESHOLD);
		int threshold= RefactoringStatus.ERROR;
		try {
			threshold= Integer.valueOf(value).intValue() - 1;
		} catch (NumberFormatException e) {
		}
		return threshold;
	}
	
	static boolean getCodeIsJavaStyleGuideConform() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(PREF_JAVA_STYLE_GUIDE_CONFORM);
	}
}
