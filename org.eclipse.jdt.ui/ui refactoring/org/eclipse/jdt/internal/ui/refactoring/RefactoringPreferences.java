/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class RefactoringPreferences {
	
	public static final String FATAL_SEVERITY= "4"; //$NON-NLS-1$
	public static final String ERROR_SEVERITY= "3"; //$NON-NLS-1$
	public static final String WARNING_SEVERITY= "2"; //$NON-NLS-1$
	public static final String INFO_SEVERITY= "1"; //$NON-NLS-1$
	public static final String OK_SEVERITY= "0"; //$NON-NLS-1$
		
	public static final String PREF_ERROR_PAGE_SEVERITY_THRESHOLD= "Refactoring.ErrorPage.severityThreshold"; //$NON-NLS-1$
	public static final String PREF_JAVA_STYLE_GUIDE_CONFORM= "Refactoring.javaStyleGuideConform"; //$NON-NLS-1$
	public static final String PREF_SAVE_ALL_EDITORS= "Refactoring.savealleditors"; //$NON-NLS-1$

	static public int getCheckPassedSeverity() {	
		String value= JavaPlugin.getDefault().getPreferenceStore().getString(PREF_ERROR_PAGE_SEVERITY_THRESHOLD);
		int threshold= RefactoringStatus.ERROR;
		try {
			threshold= Integer.valueOf(value).intValue() - 1;
		} catch (NumberFormatException e) {
		}
		return threshold;
	}
	
	static public boolean getCodeIsJavaStyleGuideConform() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(PREF_JAVA_STYLE_GUIDE_CONFORM);
	}
	
	static public boolean getSaveAllEditors() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(PREF_SAVE_ALL_EDITORS);
	}
	
	static public void setSaveAllEditors(boolean value) {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PREF_SAVE_ALL_EDITORS, value);
	}
}
