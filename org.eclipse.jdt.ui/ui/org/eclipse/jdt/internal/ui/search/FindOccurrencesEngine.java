/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.search.ui.NewSearchUI;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

public abstract class FindOccurrencesEngine {
	
	private IOccurrencesFinder fFinder;
	
	private static class FindOccurencesClassFileEngine extends FindOccurrencesEngine {
		private IClassFile fClassFile;
		
		public FindOccurencesClassFileEngine(IClassFile file, IOccurrencesFinder finder) {
			super(finder);
			fClassFile= file;
		}
		protected CompilationUnit createAST() {
			return JavaPlugin.getDefault().getASTProvider().getAST(fClassFile, ASTProvider.WAIT_YES, null);
		}
		protected IJavaElement getInput() {
			return fClassFile;
		}
		protected ISourceReference getSourceReference() {
			return fClassFile;
		}
	}

	private static class FindOccurencesCUEngine extends FindOccurrencesEngine {
		private ICompilationUnit fCUnit;
		
		public FindOccurencesCUEngine(ICompilationUnit unit, IOccurrencesFinder finder) {
			super(finder);
			fCUnit= unit;
		}
		protected CompilationUnit createAST() {
			return JavaPlugin.getDefault().getASTProvider().getAST(fCUnit, ASTProvider.WAIT_YES, null);
		}
		protected IJavaElement getInput() {
			return fCUnit;
		}
		protected ISourceReference getSourceReference() {
			return fCUnit;
		}
	}
	
	protected FindOccurrencesEngine(IOccurrencesFinder finder) {
		fFinder= finder;
	}
	
	public static FindOccurrencesEngine create(IJavaElement root, IOccurrencesFinder finder) {
		if (root == null || finder == null)
			return null;
		
		ICompilationUnit unit= (ICompilationUnit)root.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (unit != null)
			return new FindOccurencesCUEngine(unit, finder);
		IClassFile cf= (IClassFile)root.getAncestor(IJavaElement.CLASS_FILE);
		if (cf != null)
			return new FindOccurencesClassFileEngine(cf, finder);
		return null;
	}

	protected abstract CompilationUnit createAST();
	
	protected abstract IJavaElement getInput();
	
	protected abstract ISourceReference getSourceReference();
	
	protected IOccurrencesFinder getOccurrencesFinder() {
		return fFinder;
	}

	public String run(int offset, int length) throws JavaModelException {
		ISourceReference sr= getSourceReference();
		if (sr.getSourceRange() == null) {
			return SearchMessages.getString("FindOccurrencesEngine.noSource.text"); //$NON-NLS-1$ 
		}
		
		final CompilationUnit root= createAST();
		if (root == null) {
			return SearchMessages.getString("FindOccurrencesEngine.cannotParse.text"); //$NON-NLS-1$
		}
		String message= fFinder.initialize(root, offset, length);
		if (message != null)
			return message;
		
		final IDocument document= new Document(getSourceReference().getSource());
		
		performNewSearch(fFinder, document, getInput());
		return null;
	}
	
	private void performNewSearch(IOccurrencesFinder finder, IDocument document, IJavaElement element) {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInBackground(new OccurrencesSearchQuery(finder, document, element));
	}
}
