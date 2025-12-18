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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension5;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener;

/**
 * @since 3.14
 */
public class JavaCodeMiningReconciler implements IJavaReconcilingListener {

	/**
	 * Maps Java editors to futures representing their associated {@link ITypeRoot}.
	 * <p>
	 * The future is completed when a reconciled {@link ITypeRoot} becomes available for the editor,
	 * or cancelled/replaced when a new reconciliation cycle starts.
	 */
	private static final Map<ITextEditor, CompletableFuture<ITypeRoot>> typeRootFutureByEditor= new ConcurrentHashMap<>();

	/** The Java editor this Java code mining reconciler is installed on */
	private JavaEditor fEditor;

	/** The source viewer this Java code mining reconciler is installed on */
	private ISourceViewerExtension5 fSourceViewer;


	@Override
	public void reconciled(CompilationUnit ast, boolean forced, IProgressMonitor progressMonitor) {
		if (ast == null) {
			return;
		}
		final JavaEditor editor= fEditor; // take a copy as this can be null-ed in the meantime
		final ISourceViewerExtension5 sourceViewer= fSourceViewer;
		if (editor != null && sourceViewer != null) {
			sourceViewer.updateCodeMinings();
			CompletableFuture<ITypeRoot> future= typeRootFutureByEditor.get(editor);
			if (future != null && !future.isDone() && ast.getTypeRoot() != null) {
				future.complete(ast.getTypeRoot());
			}
		}
	}

	public static CompletableFuture<ITypeRoot> getFuture(ITextEditor editor) {
		return typeRootFutureByEditor.computeIfAbsent(editor, JavaCodeMiningReconciler::typeRootFor);
	}

	private static CompletableFuture<ITypeRoot> typeRootFor(ITextEditor editor) {
		ITypeRoot unit= EditorUtility.getEditorInputJavaElement(editor, true);
		CompletableFuture<ITypeRoot> future= new CompletableFuture<>();
		future.complete(unit);
		return future;
	}

	@Override
	public void aboutToBeReconciled() {
		if (fEditor == null) {
			return;
		}
		typeRootFutureByEditor.compute(fEditor, (editor, existingFuture) -> {
			if (existingFuture != null) {
				existingFuture.cancel(false);
			}
			return new CompletableFuture<ITypeRoot>();
		});
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
		CompletableFuture<ITypeRoot> future= typeRootFutureByEditor.remove(fEditor);
		if (future != null) {
			future.cancel(false);
		}
		if (fEditor instanceof CompilationUnitEditor) {
			((CompilationUnitEditor) fEditor).removeReconcileListener(this);
		}
		fEditor= null;
		fSourceViewer= null;
	}
}
