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

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener;

/**
 * @since 3.14
 */
public class JavaCodeMiningReconciler implements IJavaReconcilingListener {

	/** The Java editor this Java code mining reconciler is installed on */
	private JavaEditor fEditor;

	/** The source viewer this Java code mining reconciler is installed on */
	private ISourceViewer fSourceViewer;

	@Override
	public void reconciled(CompilationUnit ast, boolean forced, IProgressMonitor progressMonitor) {
		updateCodeMinings();
	}

	@Override
	public void aboutToBeReconciled() {
		// Do nothing
	}

	/**
	 * Install this reconciler on the given editor.
	 *
	 * @param editor the editor
	 * @param sourceViewer the source viewer
	 */
	public void install(JavaEditor editor, ISourceViewer sourceViewer) {
		fEditor= editor;
		fSourceViewer= sourceViewer;

		if (fEditor instanceof CompilationUnitEditor) {
			((CompilationUnitEditor) fEditor).addReconcileListener(this);
		}
		updateCodeMinings();
	}

	/**
	 * Uninstall this reconciler from the editor.
	 */
	public void uninstall() {
		if (fEditor instanceof CompilationUnitEditor) {
			((CompilationUnitEditor) fEditor).removeReconcileListener(this);
		}
		fEditor= null;
		fSourceViewer= null;
	}

	/**
	 * Update Java code mining in the Java editor.
	 */
	void updateCodeMinings() {
		if (fSourceViewer instanceof JavaSourceViewer) {
			((JavaSourceViewer) fSourceViewer).doUpdateCodeMinings();
		}
	}

}
