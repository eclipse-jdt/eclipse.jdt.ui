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

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock;
import org.eclipse.ui.texteditor.spelling.IPreferenceStatusMonitor;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * Spelling preference block
 * 
 * @since 3.1
 */
public class SpellingPreferenceBlock implements ISpellingPreferenceBlock {
	
	private class NullStatusChangeListener implements IStatusChangeListener {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#statusChanged(org.eclipse.core.runtime.IStatus)
		 */
		public void statusChanged(IStatus status) {
		}
	}

	private class StatusChangeListenerAdapter implements IStatusChangeListener {
		
		private IPreferenceStatusMonitor fMonitor;
		
		public StatusChangeListenerAdapter(IPreferenceStatusMonitor monitor) {
			super();
			fMonitor= monitor;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#statusChanged(org.eclipse.core.runtime.IStatus)
		 */
		public void statusChanged(IStatus status) {
			fMonitor.statusChanged(status);
		}
	}

	private SpellingConfigurationBlock fBlock= new SpellingConfigurationBlock(new NullStatusChangeListener(), null);
	
	private Control fControl;

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public Control createControl(Composite parent) {
		fControl= fBlock.createContents(parent);
		WorkbenchHelp.setHelp(fControl, IJavaHelpContextIds.JAVA_EDITOR_PREFERENCE_PAGE);
		return fControl;
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#initialize(org.eclipse.ui.texteditor.spelling.IPreferenceStatusMonitor)
	 */
	public void initialize(IPreferenceStatusMonitor statusMonitor) {
		fBlock.fContext= new StatusChangeListenerAdapter(statusMonitor);
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#canPerformOk()
	 */
	public boolean canPerformOk() {
		return true;
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#performOk()
	 */
	public void performOk() {
		fBlock.performOk(true);
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#performDefaults()
	 */
	public void performDefaults() {
		fBlock.performDefaults();
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#performRevert()
	 */
	public void performRevert() {
		fBlock.performRevert();
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#dispose()
	 */
	public void dispose() {
		fBlock.dispose();
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		fBlock.setEnabled(enabled);
	}
}
