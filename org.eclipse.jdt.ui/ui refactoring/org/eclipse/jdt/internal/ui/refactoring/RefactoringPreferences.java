/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.ui.preferences.RefactoringPreferencePage;

public class RefactoringPreferences {
	
	static public int getCheckPassedSeverity() {
		return RefactoringPreferencePage.getCheckPassedSeverity();
	}
	
	/*
	static public boolean getCodeIsJavaStyleGuideConform() {
		return RefactoringPreferencePage.getCodeIsJavaStyleGuideConform();
	}
	*/
	
	static public boolean getSaveAllEditors() {
		return RefactoringPreferencePage.getSaveAllEditors();
	}
	
	static public void setSaveAllEditors(boolean value) {
		RefactoringPreferencePage.setSaveAllEditors(value);
	}
}
