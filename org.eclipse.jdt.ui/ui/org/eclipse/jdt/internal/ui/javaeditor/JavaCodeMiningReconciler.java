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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension5;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener;

/**
 * @since 3.14
 */
public class JavaCodeMiningReconciler implements IJavaReconcilingListener {

	/**
	 * Stores the set of viewers for which source is reconciled and requests
	 * for references can be performed.
	 */
	private static final Set<ISourceViewerExtension5> reconciledViewers= new HashSet<>();

	/** The Java editor this Java code mining reconciler is installed on */
	private JavaEditor fEditor;

	/** The source viewer this Java code mining reconciler is installed on */
	private ISourceViewerExtension5 fSourceViewer;


	@Override
	public void reconciled(CompilationUnit ast, boolean forced, IProgressMonitor progressMonitor) {
		final ISourceViewerExtension5 sourceViewer= fSourceViewer; // take a copy as this can be null-ed in the meantime
		if (sourceViewer != null) {
			reconciledViewers.add(sourceViewer);
			sourceViewer.updateCodeMinings();
		}
	}

	@Override
	public void aboutToBeReconciled() {
		// interrupt code minings if modification occurs
		reconciledViewers.remove(fSourceViewer);
	}

	/**
	 * Install this reconciler on the given editor.
	 *
	 * @param editor the editor
	 * @param sourceViewer the source viewer
	 */
	public void install(JavaEditor editor, ISourceViewer sourceViewer) {
		fEditor= editor;
		if (sourceViewer instanceof ISourceViewerExtension5) {
			fSourceViewer= (ISourceViewerExtension5)sourceViewer;
		} else {
			uninstall();
			return;
		}

		if (fEditor instanceof CompilationUnitEditor) {
			((CompilationUnitEditor) fEditor).addReconcileListener(this);
		}
		fSourceViewer.updateCodeMinings();
	}

	/**
	 * Uninstall this reconciler from the editor.
	 */
	public void uninstall() {
		reconciledViewers.remove(fSourceViewer);
		if (fEditor instanceof CompilationUnitEditor) {
			((CompilationUnitEditor) fEditor).removeReconcileListener(this);
		}
		fEditor= null;
		fSourceViewer= null;
	}

	public static boolean isReconciled(ISourceViewerExtension5 viewer) {
		return reconciledViewers.contains(viewer);
	}

}
