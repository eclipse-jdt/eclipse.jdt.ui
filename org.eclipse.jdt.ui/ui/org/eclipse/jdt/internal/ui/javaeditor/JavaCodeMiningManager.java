/*******************************************************************************
 * Copyright (c) 2018 Angelo ZERR.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - [CodeMining] Update CodeMinings with IJavaReconcilingListener - Bug 530825
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * @since 3.14
 */
public class JavaCodeMiningManager implements IPropertyChangeListener {

	/** The editor */
	private JavaEditor fEditor;

	/** The source viewer */
	private JavaSourceViewer fSourceViewer;

	/** Java code mining reconciler */
	private JavaCodeMiningReconciler fReconciler;

	/** The preference store */
	private IPreferenceStore fPreferenceStore;

	/**
	 * Installs the Java code mining manager on the given editor infrastructure.
	 *
	 * @param editor          the Java editor
	 * @param sourceViewer    the source viewer
	 * @param preferenceStore the preference store
	 */
	public void install(JavaEditor editor, JavaSourceViewer sourceViewer, IPreferenceStore preferenceStore) {
		fEditor= editor;
		fSourceViewer= sourceViewer;
		fPreferenceStore= preferenceStore;

		fPreferenceStore.addPropertyChangeListener(this);

		if (isJavaCodeMiningEnabled()) {
			enable();
		}
	}

	/**
	 * Enable Java code mining manager.
	 */
	private void enable() {
		if (fEditor != null) {
			if (fReconciler != null) {
				fSourceViewer.updateCodeMinings();
			} else {
				fReconciler= new JavaCodeMiningReconciler();
				fReconciler.install(fEditor, fSourceViewer);
			}
		}
	}

	/**
	 * Uninstall Java code mining manager.
	 */
	public void uninstall() {
		disable();

		if (fPreferenceStore != null) {
			fPreferenceStore.removePropertyChangeListener(this);
			fPreferenceStore= null;
		}

		fEditor= null;
		fSourceViewer= null;
	}

	/**
	 * Disable Java code mining manager.
	 */
	private void disable() {
		if (fReconciler != null) {
			fReconciler.uninstall();
			fReconciler= null;
		}
	}

	/**
	 * @return <code>true</code> iff Java code mining is enabled in the preferences
	 */
	private boolean isJavaCodeMiningEnabled() {
		return fEditor != null && fEditor.isJavaCodeMiningEnabled();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		handlePropertyChangeEvent(event);
	}

	private void handlePropertyChangeEvent(PropertyChangeEvent event) {
		if (fPreferenceStore == null) {
			return; // Uninstalled during event notification
		}

		if (fEditor != null && fEditor.affectsJavaCodeMining(event)) {
			if (isJavaCodeMiningEnabled()) {
				enable();
			} else {
				disable();
			}
		}
	}

}
