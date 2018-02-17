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

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.swt.events.MouseEvent;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.OpenTypeHierarchyAction;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Java implementation code mining.
 *
 * @since 3.16
 */
public class JavaImplementationCodeMining extends AbstractJavaElementLineHeaderCodeMining {

	private final JavaEditor editor;

	private final boolean showImplementationsAtLeastOne;

	private Consumer<MouseEvent> action;

	public JavaImplementationCodeMining(IType element, JavaEditor editor, IDocument document, ICodeMiningProvider provider,
			boolean showImplementationsAtLeastOne) throws JavaModelException, BadLocationException {
		super(element, document, provider, null);
		this.editor= editor;
		this.showImplementationsAtLeastOne= showImplementationsAtLeastOne;
	}

	@SuppressWarnings("boxing")
	@Override
	protected CompletableFuture<Void> doResolve(ITextViewer viewer, IProgressMonitor monitor) {
		return CompletableFuture.runAsync(() -> {
			try {
				IJavaElement element= super.getElement();
				long implCount= countImplementations((IType) element, monitor);
				action= implCount > 0 ? e -> new OpenTypeHierarchyAction(editor).run(new StructuredSelection(element)) : null;
				if (implCount == 0 && showImplementationsAtLeastOne) {
					super.setLabel(""); //$NON-NLS-1$
				} else {
					super.setLabel(MessageFormat.format(JavaCodeMiningMessages.JavaImplementationCodeMining_label, implCount));
				}
			} catch (JavaModelException e) {
				// Should never occur
			}
		});
	}

	@Override
	public Consumer<MouseEvent> getAction() {
		return action;
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
