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

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.ide.IDE;

import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.ui.JavaUI;

public abstract class FindOccurrencesEngine {
	
	/** @deprecated TODO: remove switch for 3.0 */
	private boolean USE_NEW_SEARCH= true;
	
	private IOccurrencesFinder fFinder;
	
	private static class FindOccurencesClassFileEngine extends FindOccurrencesEngine {
		private IClassFile fClassFile;
		
		public FindOccurencesClassFileEngine(IClassFile file, IOccurrencesFinder finder) {
			super(finder);
			fClassFile= file;
		}
		protected CompilationUnit createAST() {
			return JavaPlugin.getDefault().getASTProvider().getAST(fClassFile, true, null);
		}
		protected IJavaElement getInput() {
			return fClassFile;
		}
		protected IResource getMarkerOwner() {
			return fClassFile.getJavaProject().getProject();
		}
		protected void addSpecialAttributes(Map attributes) throws JavaModelException {
			attributes.put(IDE.EDITOR_ID_ATTR, JavaUI.ID_CF_EDITOR);
			JavaCore.addJavaElementMarkerAttributes(attributes, fClassFile.getType());
			attributes.put(IJavaSearchUIConstants.ATT_JE_HANDLE_ID, fClassFile.getType().getHandleIdentifier());
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
			return JavaPlugin.getDefault().getASTProvider().getAST(fCUnit, true, null);
		}
		protected IJavaElement getInput() {
			return fCUnit;
		}
		protected IResource getMarkerOwner() throws JavaModelException {
			ICompilationUnit original= WorkingCopyUtil.getOriginal(fCUnit);
			return original.getUnderlyingResource();
		}
		protected void addSpecialAttributes(Map attributes) {
			// do nothing
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
	
	/** @deprecated */
	protected abstract IResource getMarkerOwner() throws JavaModelException;
	
	/** @deprecated */
	protected abstract void addSpecialAttributes(Map attributes) throws JavaModelException;
		
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
		
		if (USE_NEW_SEARCH) {
			performNewSearch(fFinder, document, getInput());
			return null;
		}
		
		final IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				ISearchResultView view= startSearch();
				fFinder.perform();
				IResource file= getMarkerOwner();				
				IMarker[] markers= fFinder.createMarkers(file, document);
				for (int i = 0; i < markers.length; i++) {
					IMarker marker= markers[i];
					Map attributes= marker.getAttributes();
					addSpecialAttributes(attributes);
					marker.setAttributes(attributes);
					addMatch(view, file, marker);
				}
				searchFinished(view);
			}
		};
		run(runnable);
		return null;
	}

	private ISearchResultView startSearch() {
		SearchUI.activateSearchResultView();
		ISearchResultView view= SearchUI.getSearchResultView();
		IJavaElement element= getInput();
		if (view != null) {
			fFinder.searchStarted(view, element.getElementName());
		}
		return view;
	}
	
	private void addMatch(final ISearchResultView view, IResource file, IMarker marker) {
		if (view != null)
			view.addMatch("", getGroupByKey(marker), file, marker); //$NON-NLS-1$
	}
	
	private void searchFinished(final ISearchResultView view) {
		if (view != null) 
			view.searchFinished();
	}

	private Object getGroupByKey(IMarker marker) {
		try {
			return marker.getAttribute(IMarker.LINE_NUMBER);
		} catch (CoreException e) {
		}
		return marker;
	}
	
	private void run(final IWorkspaceRunnable runnable) {
		BusyIndicator.showWhile(null,
			new Runnable() {
				public void run() {
					try {
						JavaCore.run(runnable, null);
					} catch (CoreException e) {
						JavaPlugin.log(e);
					}
				}
			}
		);
	}
	
	private void performNewSearch(IOccurrencesFinder finder, IDocument document, IJavaElement element) {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQuery(new OccurrencesSearchQuery(finder, document, element));
	}
}
