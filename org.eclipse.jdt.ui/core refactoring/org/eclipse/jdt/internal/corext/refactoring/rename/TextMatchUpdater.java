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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating2;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;

/**
 * @todo work in progress
 */
class TextMatchUpdater {
	
	private static final String TEXT_EDIT_LABEL= RefactoringCoreMessages.getString("TextMatchUpdater.update"); //$NON-NLS-1$
	
	private IJavaSearchScope fScope;
	private TextChangeManager fManager;
	private SearchResultGroup[] fReferences;

	private RefactoringScanner2 fScanner;
	private String fNewName;
	private int fCurrentNameLength;
	
	private TextMatchUpdater(TextChangeManager manager, IJavaSearchScope scope, ITextUpdating2 processor, SearchResultGroup[] references){
		fManager= manager;
		fScope= scope;
		fReferences= references;

		fNewName= processor.getNewElementName();
		fCurrentNameLength= processor.getCurrentElementName().length();
		fScanner= new RefactoringScanner2();
		fScanner.setPattern(processor.getCurrentElementName());
	}

	static void perform(IProgressMonitor pm, IJavaSearchScope scope, ITextUpdating2 processor, TextChangeManager manager, SearchResultGroup[] references) throws JavaModelException{
		new TextMatchUpdater(manager, scope, processor, references).updateTextMatches(pm);
	}

	private void updateTextMatches(IProgressMonitor pm) throws JavaModelException {	
		try{
			IProject[] projectsInScope= getProjectsInScope();
			
			pm.beginTask("", projectsInScope.length); //$NON-NLS-1$
			
			for (int i =0 ; i < projectsInScope.length; i++){
				if (pm.isCanceled())
					throw new OperationCanceledException();
				addTextMatches(projectsInScope[i], new SubProgressMonitor(pm, 1));
			}
		} finally{
			pm.done();
		}		
	}

	private IProject[] getProjectsInScope() {
		IPath[] enclosingProjects= fScope.enclosingProjectsAndJars();
		Set enclosingProjectSet= new HashSet();
		enclosingProjectSet.addAll(Arrays.asList(enclosingProjects));
		
		ArrayList projectsInScope= new ArrayList();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i =0 ; i < projects.length; i++){
			if (enclosingProjectSet.contains(projects[i].getFullPath()))
				projectsInScope.add(projects[i]);
		}
		
		return (IProject[]) projectsInScope.toArray(new IProject[projectsInScope.size()]);
	}

	private void addTextMatches(IResource resource, IProgressMonitor pm) throws JavaModelException{
		try{
			String task= RefactoringCoreMessages.getString("TextMatchUpdater.searching") + resource.getFullPath(); //$NON-NLS-1$
			if (resource instanceof IFile){
				IJavaElement element= JavaCore.create(resource);
				// don't start pm task (flickering label updates; finally {pm.done()} is enough)
				if (!(element instanceof ICompilationUnit))
					return;
				if (! element.exists())
					return;
				if (! fScope.encloses(element))
					return;
				addCuTextMatches((ICompilationUnit) element);
				
			} else if (resource instanceof IContainer){
				IResource[] members= ((IContainer) resource).members();
				pm.beginTask(task, members.length); //$NON-NLS-1$
				pm.subTask(task);
				for (int i = 0; i < members.length; i++) {
					if (pm.isCanceled())
						throw new OperationCanceledException();
					
					addTextMatches(members[i], new SubProgressMonitor(pm, 1));
				}	
			}
		} catch (JavaModelException e){
			throw e;	
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} finally{
			pm.done();
		}	
	}
	
	private void addCuTextMatches(ICompilationUnit cu) throws JavaModelException{
		fScanner.scan(cu);
		Set matches= fScanner.getMatches(); //Set of Integer (start position)
		if (matches.size() == 0)
			return;

		removeReferences(cu, matches);
		if (matches.size() != 0)
			addTextUpdates(cu, matches); 
	}
	
	private void removeReferences(ICompilationUnit cu, Set matches) {
		for (int i= 0; i < fReferences.length; i++) {
			SearchResultGroup group= fReferences[i];
			if (cu.equals(group.getCompilationUnit())) {
				removeReferences(matches, group);
			}
		}
	}

	private void removeReferences(Set matches, SearchResultGroup group) {
		SearchResult[] searchResults= group.getSearchResults();
		for (int r= 0; r < searchResults.length; r++) {
			//int start= searchResults[r].getStart(); // doesn't work for pack.ReferencedType
			int unqualifiedStart= searchResults[r].getEnd() - fCurrentNameLength;
			matches.remove(new Integer(unqualifiedStart));
		}
	}

	private void addTextUpdates(ICompilationUnit cu, Set matches) {
		for (Iterator resultIter= matches.iterator(); resultIter.hasNext();){
			int matchStart= ((Integer) resultIter.next()).intValue();
			ReplaceEdit edit= new ReplaceEdit(matchStart, fCurrentNameLength, fNewName);
			TextChangeCompatibility.addTextEdit(fManager.get(cu), TEXT_EDIT_LABEL, edit);
		}
	}
}
