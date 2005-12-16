/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.ltk.core.refactoring.Refactoring;

import org.eclipse.jdt.internal.corext.refactoring.tagging.IDelegatingUpdating;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * 
 * This is a helper class to keep a consistent design between refactorings
 * capable of creating delegates.
 * 
 * @since 3.2
 * 
 */
public class DelegateUIHelper {

	public static Button generateLeaveDelegateCheckbox(Composite result, Refactoring ref, boolean plural) {

		final IDelegatingUpdating refactoring= (IDelegatingUpdating) ref.getAdapter(IDelegatingUpdating.class);
		if (refactoring == null || !refactoring.canEnableDelegatingUpdating())
			return null;

		final Button leaveDelegateCheckBox= createCheckbox(result, getLeaveDelegateCheckBoxTitle(plural), loadLeaveDelegateSetting(refactoring));
		refactoring.setDelegatingUpdating(leaveDelegateCheckBox.getSelection());
		leaveDelegateCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				refactoring.setDelegatingUpdating(leaveDelegateCheckBox.getSelection());
			}
		});
		return leaveDelegateCheckBox;

	}

	public static void saveLeaveDelegateSetting(Button checkbox) {
		saveBooleanSetting(DELEGATING_UPDATING, checkbox);
	}

	public static boolean loadLeaveDelegateSetting(final IDelegatingUpdating refactoring) {
		return getBooleanSetting(DELEGATING_UPDATING, refactoring.getDelegatingUpdating());
	}

	public static String getLeaveDelegateCheckBoxTitle(boolean plural) {
		return plural ? RefactoringMessages.DelegateCreator_leave_several_delegates : RefactoringMessages.DelegateCreator_leave_one_delegate;
	}

	// ************** Helper methods *******************

	private static final String DELEGATING_UPDATING= "delegatingUpdating"; //$NON-NLS-1$

	private DelegateUIHelper() {
		// no instances
	}
	
	private static Button createCheckbox(Composite parent, String title, boolean value) {
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(title);
		checkBox.setSelection(value);
		return checkBox;
	}

	private static boolean getBooleanSetting(String key, boolean defaultValue) {
		String update= JavaPlugin.getDefault().getDialogSettings().get(key);
		if (update != null)
			return Boolean.valueOf(update).booleanValue();
		else
			return defaultValue;
	}

	private static void saveBooleanSetting(String key, Button checkBox) {
		if (checkBox != null && !checkBox.isDisposed() && checkBox.getEnabled())
			JavaPlugin.getDefault().getDialogSettings().put(key, checkBox.getSelection());
	}

}
