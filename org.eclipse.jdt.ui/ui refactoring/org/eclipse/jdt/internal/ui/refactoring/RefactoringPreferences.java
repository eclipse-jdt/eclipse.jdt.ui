/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class RefactoringPreferences {

	public static final String PREF_ERROR_PAGE_SEVERITY_THRESHOLD= PreferenceConstants.REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD;
	public static final String PREF_SAVE_ALL_EDITORS= PreferenceConstants.REFACTOR_SAVE_ALL_EDITORS;
	
	public static int getCheckPassedSeverity() {
		String value= JavaPlugin.getDefault().getPreferenceStore().getString(PREF_ERROR_PAGE_SEVERITY_THRESHOLD);
		try {
			return Integer.valueOf(value).intValue() - 1;
		} catch (NumberFormatException e) {
			return RefactoringStatus.ERROR;
		}
	}
	
	public static int getStopSeverity() {
		switch (getCheckPassedSeverity()) {
			case RefactoringStatus.OK:
				return RefactoringStatus.INFO;
			case RefactoringStatus.INFO:
				return RefactoringStatus.WARNING;
			case RefactoringStatus.WARNING:
				return RefactoringStatus.ERROR;
		}
		return RefactoringStatus.FATAL;
	}
	
	public static boolean getSaveAllEditors() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(PREF_SAVE_ALL_EDITORS);
	}
	
	public static void setSaveAllEditors(boolean save) {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(RefactoringPreferences.PREF_SAVE_ALL_EDITORS, save);
	}	
}
