/*******************************************************************************
 * Copyright (c) 2017 Angelo Zerr and others.
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

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.source.ISourceViewerExtension5;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaCodeMiningReconciler;

/**
 * Java code mining provider to show method parameters code minings.
 *
 * @since 3.14
 *
 */
public class JavaMethodParameterCodeMiningProvider extends AbstractCodeMiningProvider {

	@Override
	public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer, IProgressMonitor monitor) {
		if (viewer instanceof ISourceViewerExtension5) {
			ISourceViewerExtension5 codeMiningViewer= (ISourceViewerExtension5)viewer;
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
				return null;
			}
			try {
				IJavaElement[] elements= unit.getChildren();
				List<ICodeMining> minings= new ArrayList<>(elements.length);
				collectLineContentCodeMinings(unit, minings);
				if (viewer instanceof ISourceViewerExtension5) {
					ISourceViewerExtension5 codeMiningViewer= (ISourceViewerExtension5)viewer;
					if (!JavaCodeMiningReconciler.isReconciled(codeMiningViewer)) {
						// the provider isn't able to return code minings for non-reconciled viewers
						monitor.setCanceled(true);
					}
				}
				monitor.isCanceled();
				return minings;
			} catch (JavaModelException e) {
				// TODO: what should we done when there are some errors?
			}
			return null;
		});
	}

	private void collectLineContentCodeMinings(ITypeRoot unit, List<ICodeMining> minings) {
		CompilationUnit cu= getCompilationUnitNode(unit, true);
		CalleeJavaMethodParameterVisitor visitor= new CalleeJavaMethodParameterVisitor(minings, this);
		cu.accept(visitor);
	}

	static CompilationUnit getCompilationUnitNode(ITypeRoot typeRoot, boolean resolveBindings) {
		try {
			if (typeRoot.exists() && typeRoot.getBuffer() != null) {
				ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
				parser.setSource(typeRoot);
				parser.setResolveBindings(resolveBindings);
				return (CompilationUnit) parser.createAST(null);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

}
