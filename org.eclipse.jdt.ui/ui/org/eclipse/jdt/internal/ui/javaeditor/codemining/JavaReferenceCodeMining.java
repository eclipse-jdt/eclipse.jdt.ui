/*******************************************************************************
 * Copyright (c) 2018, 2020 Angelo Zerr and others.
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
 * - IBM Corporation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.codemining;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;

import org.eclipse.ui.IEditorPart;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.search.ui.NewSearchUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.ui.actions.FindReferencesAction;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;

/**
 * Java reference code mining.
 *
 * @since 3.16
 */
public class JavaReferenceCodeMining extends AbstractJavaElementLineHeaderCodeMining {

	private final JavaEditor editor;

	private final boolean showReferencesAtLeastOne;

	private Consumer<MouseEvent> action;

	public JavaReferenceCodeMining(IJavaElement element, JavaEditor editor, IDocument document,
			ICodeMiningProvider provider, boolean showReferencesAtLeastOne)
			throws JavaModelException, BadLocationException {
		super(element, document, provider, null);
		this.editor= editor;
		this.showReferencesAtLeastOne= showReferencesAtLeastOne;
	}

	@SuppressWarnings("boxing")
	@Override
	protected CompletableFuture<Void> doResolve(ITextViewer viewer, IProgressMonitor monitor) {
		return CompletableFuture.runAsync(() -> {
			try {
				monitor.isCanceled();
				IJavaElement element= super.getElement();
				long refCount= countReferences(element, monitor);
				monitor.isCanceled();
				action= refCount > 0 ? e -> {
					if (refCount == 1 && ((e.stateMask & SWT.CTRL) == SWT.CTRL || (e.stateMask & SWT.COMMAND) == SWT.COMMAND)) {
						// Ctrl + Click is done, open the referenced element in the Java Editor
						try {
							SearchMatch match= getReferenceMatch(element, monitor);
							IJavaElement javaElement= (IJavaElement) match.getElement();
							IEditorPart part= EditorUtility.openInEditor(javaElement);
							if (part != null) {
								EditorUtility.revealInEditor(part, javaElement);
								if (part instanceof ITextEditor) {
									ITextEditor textEditor= (ITextEditor) part;
									textEditor.selectAndReveal(match.getOffset(), match.getLength());
								}
							}
						} catch (CoreException e1) {
							// Should never occur
						}
					} else {
						// Otherwise, launch references search
						new FindReferencesAction(editor).run(element);
					}
				} : null;
				if (refCount == 0 && showReferencesAtLeastOne) {
					super.setLabel(""); //$NON-NLS-1$
				} else {
					super.setLabel(MessageFormat.format(JavaCodeMiningMessages.JavaReferenceCodeMining_label, refCount));
				}
			} catch (CoreException e) {
				// Should never occur
			}
		});
	}

	@Override
	public Consumer<MouseEvent> getAction() {
		return action;
	}

	/**
	 * Return the number of references for the given java element.
	 *
	 * @param element the java element.
	 * @param monitor the monitor
	 * @return he number of references for the given java element.
	 * @throws JavaModelException throws when java error.
	 * @throws CoreException throws when java error.
	 */
	private static long countReferences(IJavaElement element, IProgressMonitor monitor)
			throws JavaModelException, CoreException {
		if (element == null) {
			return 0;
		}
		final AtomicLong count= new AtomicLong(0);
		SearchPattern pattern= SearchPattern.createPattern(element, IJavaSearchConstants.REFERENCES);
		if (pattern == null) {
			return 0;
		}
		SearchEngine engine= new SearchEngine();
		final boolean ignoreInaccurate= NewSearchUI.arePotentialMatchesIgnored();
		engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
				createSearchScope(element), new SearchRequestor() {

					@Override
					public void acceptSearchMatch(SearchMatch match) throws CoreException {
						if (match.getAccuracy() == SearchMatch.A_INACCURATE && ignoreInaccurate) {
							return;
						}
						Object o= match.getElement();
						if (o instanceof IJavaElement) {
							IJavaElement e= (IJavaElement)o;
							if (e.getAncestor(IJavaElement.COMPILATION_UNIT) != null
									|| e.getAncestor(IJavaElement.CLASS_FILE) != null) {
								count.incrementAndGet();
							}
						}
					}
				}, monitor);

		return count.get();
	}

	/**
	 * Return the single search match of references for the given java element.
	 *
	 * @param element the java element.
	 * @param monitor the monitor
	 * @return he number of references for the given java element.
	 * @throws JavaModelException throws when java error.
	 * @throws CoreException throws when java error.
	 */
	private SearchMatch getReferenceMatch(IJavaElement element, IProgressMonitor monitor)
			throws JavaModelException, CoreException {
		if (element == null) {
			return null;
		}
		final SearchMatch[] matches= new SearchMatch[1];
		SearchPattern pattern= SearchPattern.createPattern(element, IJavaSearchConstants.REFERENCES);
		if (pattern == null) {
			return null;
		}
		SearchEngine engine= new SearchEngine();
		engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
				createSourceSearchScope(), new SearchRequestor() {

					@Override
					public void acceptSearchMatch(final SearchMatch match) throws CoreException {
						Object o= match.getElement();
						if (o instanceof IJavaElement
								&& ((IJavaElement) o).getAncestor(IJavaElement.COMPILATION_UNIT) != null) {
							matches[0]= match;
						}
					}
				}, monitor);

		return matches[0];
	}

	/**
	 * Create Java workspace scope.
	 *
	 * @param element IJavaElement to search references for
	 *
	 * @return the Java workspace scope.
	 * @throws JavaModelException when java error.
	 */
	private static IJavaSearchScope createSearchScope(IJavaElement element) throws JavaModelException {
		JavaSearchScopeFactory factory= JavaSearchScopeFactory.getInstance();
		boolean isInsideJRE = factory.isInsideJRE(element);
		IJavaSearchScope scope= factory.createWorkspaceScope(isInsideJRE);
		return scope;
	}

	/**
	 * Create Java source search scope.
	 *
	 * @return the Java workspace scope.
	 * @throws JavaModelException when java error.
	 */
	private static IJavaSearchScope createSourceSearchScope() throws JavaModelException {
		IJavaProject[] projects= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
		return SearchEngine.createJavaSearchScope(projects, IJavaSearchScope.SOURCES);
	}
}
