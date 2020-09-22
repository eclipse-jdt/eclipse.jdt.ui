/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.ui.texteditor.spelling.IPreferenceStatusMonitor;
import org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock;

import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * Spelling preference block
 *
 * @since 3.1
 */
public class SpellingPreferenceBlock implements ISpellingPreferenceBlock {

	private static class NullStatusChangeListener implements IStatusChangeListener {

		/*
		 * @see org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#statusChanged(org.eclipse.core.runtime.IStatus)
		 */
		@Override
		public void statusChanged(IStatus status) {
		}
	}

	private static class StatusChangeListenerAdapter implements IStatusChangeListener {

		private IPreferenceStatusMonitor fMonitor;

		private IStatus fStatus;

		public StatusChangeListenerAdapter(IPreferenceStatusMonitor monitor) {
			super();
			fMonitor= monitor;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#statusChanged(org.eclipse.core.runtime.IStatus)
		 */
		@Override
		public void statusChanged(IStatus status) {
			fStatus= status;
			fMonitor.statusChanged(status);
		}

		public IStatus getStatus() {
			return fStatus;
		}
	}

	private SpellingConfigurationBlock fBlock= new SpellingConfigurationBlock(new NullStatusChangeListener(), null, null);

	private SpellingPreferenceBlock.StatusChangeListenerAdapter fStatusMonitor;

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Control createControl(Composite parent) {
		return fBlock.createContents(parent);
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#initialize(org.eclipse.ui.texteditor.spelling.IPreferenceStatusMonitor)
	 */
	@Override
	public void initialize(IPreferenceStatusMonitor statusMonitor) {
		fStatusMonitor= new StatusChangeListenerAdapter(statusMonitor);
		fBlock.fContext= fStatusMonitor;
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#canPerformOk()
	 */
	@Override
	public boolean canPerformOk() {
		return fStatusMonitor == null || fStatusMonitor.getStatus() == null || !fStatusMonitor.getStatus().matches(IStatus.ERROR);
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#performOk()
	 */
	@Override
	public void performOk() {
		fBlock.performOk();
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#performDefaults()
	 */
	@Override
	public void performDefaults() {
		fBlock.performDefaults();
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#performRevert()
	 */
	@Override
	public void performRevert() {
		fBlock.performRevert();
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#dispose()
	 */
	@Override
	public void dispose() {
		fBlock.dispose();
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingPreferenceBlock#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enabled) {
		fBlock.setEnabled(enabled);
	}
}
