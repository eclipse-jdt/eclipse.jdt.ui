/*******************************************************************************
 * Copyright (c) 2018 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Angelo Zerr: initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.codemining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.source.ISourceViewerExtension5;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaCodeMiningReconciler;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesPropertyTester;

/**
 * Java code mining provider to show code minings by using {@link IJavaElement}:
 *
 * <ul>
 * <li>Show references</li>
 * <li>Show implementations</li>
 * </ul>
 *
 * @since 3.16
 */
public class JavaElementCodeMiningProvider extends AbstractCodeMiningProvider {

	private final boolean showAtLeastOne;

	private final boolean showReferences;

	private final boolean showReferencesOnTypes;

	private final boolean showReferencesOnFields;

	private final boolean showReferencesOnMethods;

	private final boolean showImplementations;

	private final boolean editorEnabled;

	public JavaElementCodeMiningProvider() {
		editorEnabled= JavaPreferencesPropertyTester.isEnabled(PreferenceConstants.EDITOR_CODEMINING_ENABLED);
		showAtLeastOne= editorEnabled && JavaPreferencesPropertyTester.isEnabled(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_CODEMINING_AT_LEAST_ONE);
		showReferences= editorEnabled && JavaPreferencesPropertyTester.isEnabled(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_REFERENCES);
		showReferencesOnTypes= editorEnabled && JavaPreferencesPropertyTester.isEnabled(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_REFERENCES_ON_TYPES);
		showReferencesOnFields= editorEnabled && JavaPreferencesPropertyTester.isEnabled(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_REFERENCES_ON_FIELDS);
		showReferencesOnMethods= editorEnabled && JavaPreferencesPropertyTester.isEnabled(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_REFERENCES_ON_METHODS);
		showImplementations= editorEnabled && JavaPreferencesPropertyTester.isEnabled(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_IMPLEMENTATIONS);
	}

	@Override
	public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
			IProgressMonitor monitor) {
		if (!editorEnabled) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		if (viewer instanceof ISourceViewerExtension5) {
			ISourceViewerExtension5 codeMiningViewer = (ISourceViewerExtension5)viewer;
			if (!JavaCodeMiningReconciler.isReconciled(codeMiningViewer)) {
				// the provider isn't able to return code minings for non-reconciled viewers
				return CompletableFuture.completedFuture(Collections.emptyList());
			}
		}
		return CompletableFuture.supplyAsync(() -> {
			monitor.isCanceled();
			ITextEditor textEditor= super.getAdapter(ITextEditor.class);
			ITypeRoot unit= EditorUtility.getEditorInputJavaElement(textEditor, true);
			if (unit == null) {
				return Collections.emptyList();
			}
			try {
				IJavaElement[] elements= unit.getChildren();
				List<ICodeMining> minings= new ArrayList<>(elements.length);
				collectMinings(unit, textEditor, unit.getChildren(), minings, viewer, monitor);
				// interrupt if editor was marked to be reconciled in the meantime
				if (viewer instanceof ISourceViewerExtension5) {
					ISourceViewerExtension5 codeMiningViewer= (ISourceViewerExtension5)viewer;
					if (!JavaCodeMiningReconciler.isReconciled(codeMiningViewer)) {
						monitor.setCanceled(true);
					}
				}
				monitor.isCanceled();
				return minings;
			} catch (JavaModelException e) {
				// Should never occur
			}
			return Collections.emptyList();
		});
	}

	/**
	 * Collect java code minings.
	 *
	 * @param unit the compilation unit
	 * @param textEditor the Java editor
	 * @param elements the java elements to track
	 * @param minings the current list of minings to update
	 * @param viewer the viewer
	 * @param monitor the monitor
	 * @throws JavaModelException thrown when java model error
	 */
	private void collectMinings(ITypeRoot unit, ITextEditor textEditor, IJavaElement[] elements,
			List<ICodeMining> minings, ITextViewer viewer, IProgressMonitor monitor) throws JavaModelException {

		// Only Java editor is supported, see bug 541811
		if(!(textEditor instanceof JavaEditor)) {
			return;
		}

		// Don't worth to loop if none of mining types are requested
		if (!showReferences && !showImplementations) {
			return;
		}

		for (IJavaElement element : elements) {
			if (monitor.isCanceled()) {
				return;
			}
			if (element.getElementType() == IJavaElement.TYPE) {
				collectMinings(unit, textEditor, ((IType) element).getChildren(), minings, viewer, monitor);
			} else if ((element.getElementType() != IJavaElement.METHOD)
					&& (element.getElementType() != IJavaElement.FIELD)) {
				continue;
			}
			if (showReferences) {
				try {
					if ((showReferencesOnTypes && (element.getElementType() == IJavaElement.TYPE)) // Show references on types
							|| (showReferencesOnMethods && (element.getElementType() == IJavaElement.METHOD)) // Show references on methods
							|| (showReferencesOnFields && (element.getElementType() == IJavaElement.FIELD)) // Show references on fields
					) {
						minings.add(new JavaReferenceCodeMining(element, (JavaEditor) textEditor, viewer.getDocument(),
								this, showAtLeastOne));
					}
				} catch (BadLocationException e) {
					// Should never occur
				}
			}
			if (showImplementations) {
				// support methods, classes, and interfaces
				boolean addMining= false;
				if (element instanceof IType) {
					IType type= (IType) element;
					if (type.isInterface() || type.isClass()) {
						addMining= true;
					}
				} else if (element instanceof IMethod) {
					addMining= true;
				}
				if (addMining) {
					try {
						minings.add(new JavaImplementationCodeMining(element, (JavaEditor) textEditor, viewer.getDocument(), this,
								showAtLeastOne));
					} catch (BadLocationException e) {
						// Should never occur
					}
				}
			}
		}
	}

}
