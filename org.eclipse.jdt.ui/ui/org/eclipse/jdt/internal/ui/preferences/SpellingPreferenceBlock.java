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

import org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock;
import org.eclipse.ui.texteditor.spelling.IPreferenceStatusMonitor;

import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * Spelling preference block
 * 
 * @since 3.1
 */
public class SpellingPreferenceBlock implements ISpellingPreferenceBlock {
	
	private class NullStatusChangeListener implements IStatusChangeListener {
		public void statusChanged(IStatus status) {
		}
	}

	private class StatusChangeListenerAdapter implements IStatusChangeListener {
		private IPreferenceStatusMonitor fMonitor;
		public StatusChangeListenerAdapter(IPreferenceStatusMonitor monitor) {
			super();
			fMonitor= monitor;
		}
		public void statusChanged(IStatus status) {
			fMonitor.statusChanged(status);
		}
	}

	private SpellingConfigurationBlock fBlock= new SpellingConfigurationBlock(new NullStatusChangeListener(), null);
	private Control fControl;

	public Control createControl(Composite parent) {
		fControl= fBlock.createContents(parent);
		return fControl;
	}

	public void initialize(IPreferenceStatusMonitor statusMonitor) {
		fBlock.fContext= new StatusChangeListenerAdapter(statusMonitor);
	}

	public boolean canPerformOk() {
		return true;
	}

	public void performOk() {
		fBlock.performOk(true);
	}

	public void performDefaults() {
		fBlock.performDefaults();
	}

	public void dispose() {
		fBlock.dispose();
	}

	public void setEnabled(boolean enabled) {
		fBlock.setEnabled(enabled);
	}
}
