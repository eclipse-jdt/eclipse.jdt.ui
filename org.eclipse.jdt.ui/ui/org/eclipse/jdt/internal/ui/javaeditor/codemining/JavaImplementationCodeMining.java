/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - [CodeMining] Provide Java References/Implementation CodeMinings - Bug 529127
 */
package org.eclipse.jdt.internal.ui.javaeditor.codemining;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Java implementation code mining.
 *
 * @since 3.16
 */
public class JavaImplementationCodeMining extends AbstractJavaElementLineHeaderCodeMining {

	private final boolean showImplementationsAtLeastOne;

	public JavaImplementationCodeMining(IType element, IDocument document, ICodeMiningProvider provider,
			boolean showImplementationsAtLeastOne) throws JavaModelException, BadLocationException {
		super(element, document, provider, null);
		this.showImplementationsAtLeastOne= showImplementationsAtLeastOne;
	}

	@Override
	protected CompletableFuture<Void> doResolve(ITextViewer viewer, IProgressMonitor monitor) {
		return CompletableFuture.runAsync(() -> {
			try {
				long implCount= countImplementations((IType) getElement(), monitor);
				if (implCount == 0 && showImplementationsAtLeastOne) {
					super.setLabel(""); //$NON-NLS-1$
				} else {
					super.setLabel(implCount + " " + (implCount > 1 ? "implementations" : "implementation")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			} catch (JavaModelException e) {
				// Should never occur
			}
		});
	}

	/**
	 * Return the count of implementation for the given java element type.
	 * 
	 * @param type the java element type.
	 * @param monitor the monitor
	 * @return the count of implementation for the given java element type.
	 * @throws JavaModelException throws when Java error
	 */
	private static long countImplementations(IType type, IProgressMonitor monitor) throws JavaModelException {
		IType[] results= type.newTypeHierarchy(monitor).getAllSubtypes(type);
		return Stream.of(results).filter(t -> t.getAncestor(IJavaElement.COMPILATION_UNIT) != null).count();
	}

}
