/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.search.ui.IGroupByKeyComputer;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public abstract class FindOccurrencesEngine {
	
	public static final String IS_WRITEACCESS= "writeAccess"; //$NON-NLS-1$
	public static final String IS_VARIABLE= "variable"; //$NON-NLS-1$
	
	private static class SearchGroupByKeyComputer implements IGroupByKeyComputer {
		public Object computeGroupByKey(IMarker marker) {
			return marker; 
		}
	}
	
	private static class FindOccurencesClassFileEngine extends FindOccurrencesEngine {
		private IClassFile fClassFile;
		
		public FindOccurencesClassFileEngine(IClassFile file) {
			fClassFile= file;
		}
		protected CompilationUnit createAST() {
			return AST.parseCompilationUnit(fClassFile, true);
		}
		protected IJavaElement getInput() {
			return fClassFile;
		}
		protected IResource getMarkerOwner() {
			return fClassFile.getJavaProject().getProject();
		}
		protected void addSpecialAttributes(Map attributes) throws JavaModelException {
			attributes.put(IWorkbenchPage.EDITOR_ID_ATTR, JavaUI.ID_CF_EDITOR);
			JavaCore.addJavaElementMarkerAttributes(attributes, fClassFile.getType());
			attributes.put(IJavaSearchUIConstants.ATT_JE_HANDLE_ID, fClassFile.getType().getHandleIdentifier());
		}
		protected ISourceReference getSourceReference() {
			return fClassFile;
		}
	}

	private static class FindOccurencesCUEngine extends FindOccurrencesEngine {
		private ICompilationUnit fCUnit;
		
		public FindOccurencesCUEngine(ICompilationUnit unit) {
			fCUnit= unit;
		}
		protected CompilationUnit createAST() {
			return AST.parseCompilationUnit(fCUnit, true);
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
	
	public static FindOccurrencesEngine create(IJavaElement root) {
		ICompilationUnit unit= (ICompilationUnit)root.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (unit != null)
			return new FindOccurencesCUEngine(unit);
		IClassFile cf= (IClassFile)root.getAncestor(IJavaElement.CLASS_FILE);
		if (cf != null)
			return new FindOccurencesClassFileEngine(cf);
		return null;
	}

	protected abstract CompilationUnit createAST();
	
	protected abstract IJavaElement getInput();
	
	protected abstract ISourceReference getSourceReference();
	
	protected abstract IResource getMarkerOwner() throws JavaModelException;
	
	protected abstract void addSpecialAttributes(Map attributes) throws JavaModelException;

	/**
	 * Finds occurrences in this engines input.
	 * 
	 * @param offset the offset of the current selection
	 * @param length the lenght of the current selection
	 * 
	 * @return the matches as ASTNode list or <code>null</code> if there was an error
	 * @throws JavaModelException
	 */
	public List findOccurrences(int offset, int length) throws JavaModelException {
		ISourceReference sr= getSourceReference();
		if (sr.getSourceRange() == null) {
			return null; 
		}
		
		final CompilationUnit root= createAST();
		if (root == null) {
			return null;
		}
		final Name name= getNameNode(root, offset, length);
		if (name == null) 
			return null;
		
		final IBinding target;
		if (name.getParent() instanceof ClassInstanceCreation)
			target= ((ClassInstanceCreation)name.getParent()).resolveConstructorBinding();
		else
			target= name.resolveBinding();
		
		if (target == null)
			return null;
		
		OccurrencesFinder finder= new OccurrencesFinder(target);
		root.accept(finder);
		return finder.getUsages();
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
		final Name name= getNameNode(root, offset, length);
		if (name == null) 
			return SearchMessages.getString("FindOccurrencesEngine.noJavaElement.text"); //$NON-NLS-1$
		
		final IBinding target= name.resolveBinding();
		final IDocument document= new Document(getSourceReference().getSource());
		
		if (target == null)
			return null;
		
		final IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IJavaElement element= getInput();
				ISearchResultView view= startSearch(element.getElementName(), name);
				OccurrencesFinder finder= new OccurrencesFinder(target);
				root.accept(finder);
				List matches= finder.getUsages();
				List writeMatches= finder.getWriteUsages();
				IResource file= getMarkerOwner();				
				for (Iterator each= matches.iterator(); each.hasNext();) {
					ASTNode node= (ASTNode) each.next();
					addMatch(
						view, 
						file, 
						createMarker(file, document, node, writeMatches.contains(node), target instanceof IVariableBinding)
					);
				}
				searchFinished(view);
			}
		};
		run(runnable);
		return null;
	}

	public Name getNameNode(CompilationUnit root, int offset, int length) {
		ASTNode node= NodeFinder.perform(root, offset, length);
		if (node instanceof Name)
			return (Name)node;
		return null;
	}
	
	private ISearchResultView startSearch(String fileName, final Name name) {
		SearchUI.activateSearchResultView();
		ISearchResultView view= SearchUI.getSearchResultView();
		String elementName= ASTNodes.asString(name);
		
		if (view != null) 
			view.searchStarted(
				null,
				getSingularLabel(elementName, fileName),
				getPluralLabel(elementName, fileName),
				JavaPluginImages.DESC_OBJS_SEARCH_REF,
				"org.eclipse.jdt.ui.JavaFileSearch", //$NON-NLS-1$
				new OccurrencesInFileLabelProvider(),
				new GotoMarkerAction(), 
				new SearchGroupByKeyComputer(),
				null
			);	
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
	
	private IMarker createMarker(IResource file, IDocument document, ASTNode element, boolean writeAccess, boolean isVariable) throws CoreException {
		Map attributes= new HashMap(10);
		IMarker marker= file.createMarker(SearchUI.SEARCH_MARKER);

		int startPosition= element.getStartPosition();
		MarkerUtilities.setCharStart(attributes, startPosition);
		MarkerUtilities.setCharEnd(attributes, startPosition + element.getLength());
		addSpecialAttributes(attributes);
		
		if(writeAccess)
			attributes.put(IS_WRITEACCESS, new Boolean(true));

		if(isVariable)
			attributes.put(IS_VARIABLE, new Boolean(true));
			
		try {
			int line= document.getLineOfOffset(startPosition);
			MarkerUtilities.setLineNumber(attributes, line);
			IRegion region= document.getLineInformation(line);
			String lineContents= document.get(region.getOffset(), region.getLength());
			MarkerUtilities.setMessage(attributes, lineContents.trim());
		} catch (BadLocationException e) {
		}
		marker.setAttributes(attributes);
		return marker;
	}

	private String getPluralLabel(String nodeContents, String elementName) {
		String[] args= new String[] {nodeContents, "{0}", elementName}; //$NON-NLS-1$
		return SearchMessages.getFormattedString("JavaSearchInFile.pluralPostfix", args); //$NON-NLS-1$
	}
	
	private String getSingularLabel(String nodeContents, String elementName) {
		String[] args= new String[] {nodeContents, elementName}; //$NON-NLS-1$
		return SearchMessages.getFormattedString("JavaSearchInFile.singularPostfix", args); //$NON-NLS-1$
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
}
