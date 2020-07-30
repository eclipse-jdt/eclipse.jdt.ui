/*******************************************************************************
 * Copyright (c) 2019 Angelo Zerr and others.
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
 * - Red Hat Inc. - add Method implementation support
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.codemining;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.FindDeclarationsInHierarchyAction;
import org.eclipse.jdt.ui.actions.OpenTypeHierarchyAction;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
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

	public JavaImplementationCodeMining(IJavaElement element, JavaEditor editor, IDocument document, ICodeMiningProvider provider,
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
				if (element instanceof IType) {
					// for a type, count types implementing this type and show type hierarchy
					final long typeImplCount= countTypeImplementations((IType) element, monitor);
					action= typeImplCount > 0 ? e -> {
						if (typeImplCount == 1 && (e.stateMask & SWT.CTRL) == SWT.CTRL) {
							// Ctrl + Click is done, open the referenced element in the Java Editor
							try {
								IType javaElement= getTypeImplementation((IType)element, monitor);
								showJavaElementInEditor(javaElement);
							} catch (CoreException e1) {
								// Should never occur
							}
						} else {
							// Otherwise, launch open type hierarchy
							new OpenTypeHierarchyAction(editor).run(new StructuredSelection(element));
						}
					} : null;
					if (typeImplCount == 0 && showImplementationsAtLeastOne) {
						super.setLabel(""); //$NON-NLS-1$
					} else {
						super.setLabel(MessageFormat.format(JavaCodeMiningMessages.JavaImplementationCodeMining_label, typeImplCount));
					}
				} else if (element instanceof IMethod) {
					// for a method, count declarations in hierarchy and show search->declarations->hierarchy
					final long methodImplCount= countMethodImplementations((IMethod) element, monitor);
					action= methodImplCount > 0 ? e -> {
						if (methodImplCount == 1 && (e.stateMask & SWT.CTRL) == SWT.CTRL) {
							// Ctrl + Click is done, open the referenced element in the Java Editor
							try {
								IMethod javaElement= getMethodImplementation((IMethod)element, monitor);
								showJavaElementInEditor(javaElement);
							} catch (CoreException e1) {
								// Should never occur
							}
						} else {
							// Otherwise, launch find declarations in type hierarchy
							new FindDeclarationsInHierarchyAction(editor, true).run(element);
						}
					} : null;
					if (methodImplCount == 0 && showImplementationsAtLeastOne) {
						super.setLabel(""); //$NON-NLS-1$
					} else {
						super.setLabel(MessageFormat.format(JavaCodeMiningMessages.JavaImplementationCodeMining_label, methodImplCount));
					}
				}
			} catch (CoreException e1) {
				// Should never occur
			}
		});
	}

	@Override
	public Consumer<MouseEvent> getAction() {
		return action;
	}

	/**
	 * Show Java Element in editor
	 *
	 * @param javaElement element to show
	 * @throws JavaModelException if failure occurs in reveal
	 * @throws PartInitException if failure occurs in opening editor
	 */
	private static void showJavaElementInEditor(IJavaElement javaElement) throws JavaModelException, PartInitException {
		IEditorPart part= EditorUtility.openInEditor(javaElement);
		if (part != null) {
			EditorUtility.revealInEditor(part, javaElement);
			if (part instanceof ITextEditor) {
				ITextEditor textEditor= (ITextEditor) part;
				textEditor.selectAndReveal(((ISourceReference)javaElement).getNameRange().getOffset(),
						((ISourceReference)javaElement).getNameRange().getLength());
			}
		}
	}

	/**
	 * Return the count of implementation for the given java element type.
	 *
	 * @param type the java element type.
	 * @param monitor the monitor
	 * @return the count of implementation for the given java element type.
	 * @throws JavaModelException throws when Java error
	 */
	private static long countTypeImplementations(IType type, IProgressMonitor monitor) throws JavaModelException {
		IType[] results= type.newTypeHierarchy(monitor).getAllSubtypes(type);
		return Stream.of(results).filter(t -> t.getAncestor(IJavaElement.COMPILATION_UNIT) != null).count();
	}

	/**
	 * Return the first implementation for the given java element type.
	 *
	 * @param type the java element type.
	 * @param monitor the monitor
	 * @return the first type implementation.
	 * @throws JavaModelException throws when Java error
	 */
	private static IType getTypeImplementation(IType type, IProgressMonitor monitor) throws JavaModelException {
		IType[] results= type.newTypeHierarchy(monitor).getAllSubtypes(type);
		return Stream.of(results).filter(t -> t.getAncestor(IJavaElement.COMPILATION_UNIT) != null).findFirst().get();
	}

	/**
	 * Return the count of implementation for the java element method.
	 *
	 * @param method the java element method.
	 * @param monitor the monitor
	 * @return the count of implementation for the given java element method.
	 * @throws CoreException throws when java error
	 */
	private static long countMethodImplementations(IMethod method, IProgressMonitor monitor) throws CoreException {
		if (method == null) {
			return 0;
		}
		IType type= method.getDeclaringType();
		IType[] results= type.newTypeHierarchy(monitor).getAllSubtypes(type);
		List<IType> list= Stream.of(results).filter(t -> t.getAncestor(IJavaElement.COMPILATION_UNIT) != null).collect(Collectors.toList());
		long count= list.stream().filter(t -> t.getMethod(method.getElementName(), method.getParameterTypes()).exists()).count();
		return count;
	}

	/**
	 * Return the implementation of a java element method.
	 *
	 * @param method the java element method.
	 * @param monitor the monitor
	 * @return the implementation for the given java element method.
	 * @throws CoreException throws when java error
	 */
	private static IMethod getMethodImplementation(IMethod method, IProgressMonitor monitor) throws CoreException {
		IType type= method.getDeclaringType();
		IType[] results= type.newTypeHierarchy(monitor).getAllSubtypes(type);
		List<IType> list= Stream.of(results).filter(t -> t.getAncestor(IJavaElement.COMPILATION_UNIT) != null).collect(Collectors.toList());
		Optional<IType> foundType= list.stream().filter(t -> t.getMethod(method.getElementName(), method.getParameterTypes()).exists()).findFirst();
		return foundType.get().getMethod(method.getElementName(), method.getParameterTypes());
	}

}
