/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

public class CreateCopyOfCompilationUnitChange extends CreateTextFileChange {

	private ICompilationUnit fOldCu;
	private INewNameQuery fNameQuery;
	
	public CreateCopyOfCompilationUnitChange(IPath path, String source, ICompilationUnit oldCu, INewNameQuery nameQuery) {
		super(path, source, null, "java"); //$NON-NLS-1$
		fOldCu= oldCu;
		fNameQuery= nameQuery;
		setEncoding(oldCu);
	}
	
	public Change perform(IProgressMonitor pm) throws CoreException {
		ICompilationUnit unit= fOldCu;
		ResourceMapping mapping= JavaElementResourceMapping.create(unit);
		final Change result= super.perform(pm);
		markAsExecuted(unit, mapping);
		return result;
	}
	
	private void setEncoding(ICompilationUnit cunit) {
		IResource resource= cunit.getResource();
		// no file so the encoding is taken from the target
		if (!(resource instanceof IFile))
			return;
		IFile file= (IFile)resource;
		try {
			String encoding= file.getCharset(false);
			if (encoding != null) {
				setEncoding(encoding, true);
			} else {
				encoding= file.getCharset(true);
				if (encoding != null) {
					setEncoding(encoding, false);
				}
			}
		} catch (CoreException e) {
			// do nothing. Take encoding from target
		}
	}

	protected IFile getOldFile(IProgressMonitor pm) {
		pm.beginTask("", 10); //$NON-NLS-1$
		String oldSource= super.getSource();
		IPath oldPath= super.getPath();
		String newTypeName= fNameQuery.getNewName();
		try {
			String newSource= getCopiedFileSource(new SubProgressMonitor(pm, 9), fOldCu, newTypeName);
			setSource(newSource);
			setPath(constructNewPath(newTypeName));
			return super.getOldFile(new SubProgressMonitor(pm, 1));
		} catch (CoreException e) {
			setSource(oldSource);
			setPath(oldPath);
			return super.getOldFile(pm);
		}
	}

	private IPath constructNewPath(String newTypeName) {
		String newCUName= JavaModelUtil.getRenamedCUName(fOldCu, newTypeName);
		return ResourceUtil.getResource(fOldCu).getParent().getFullPath().append(newCUName);
	}

	private static String getCopiedFileSource(IProgressMonitor pm, ICompilationUnit cu, String newTypeName) throws CoreException {
		ICompilationUnit wc= cu.getPrimary().getWorkingCopy(null);
		try {
			TextChangeManager manager= createChangeManager(pm, wc, newTypeName);
			String result= manager.get(wc).getPreviewContent(new NullProgressMonitor());
			return result;
		} finally {
			wc.discardWorkingCopy();
		}
	}
	
	private static TextChangeManager createChangeManager(IProgressMonitor pm, ICompilationUnit wc, String newName) throws CoreException {
		TextChangeManager manager= new TextChangeManager();
		SearchResultGroup refs= getReferences(wc, pm);
		if (refs == null)
			return manager;
		if (refs.getCompilationUnit() == null)	
			return manager;
				
		String name= RefactoringCoreMessages.CopyRefactoring_update_ref; 
		SearchMatch[] results= refs.getSearchResults();
		for (int j= 0; j < results.length; j++){
			SearchMatch searchResult= results[j];
			if (searchResult.getAccuracy() == SearchMatch.A_INACCURATE)
				continue;
			String oldName= wc.findPrimaryType().getElementName();
			int length= oldName.length();
			int offset= searchResult.getOffset() + searchResult.getLength() - length; // may be qualified
			TextChangeCompatibility.addTextEdit(manager.get(wc), name, new ReplaceEdit(offset, length, newName));
		}
		return manager;
	}
	
	private static SearchResultGroup getReferences(ICompilationUnit wc, IProgressMonitor pm) throws JavaModelException{
		pm.subTask(RefactoringCoreMessages.CopyRefactoring_searching); 
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[]{wc});
		if (wc.findPrimaryType() == null)
			return null;
		SearchPattern pattern= createSearchPattern(wc.findPrimaryType());
		SearchResultGroup[] groups= RefactoringSearchEngine.search(pattern, scope, pm, new ICompilationUnit[]{wc},
				new RefactoringStatus()); //status cannot get an error by construction: search scope is only the CU. 
//		Assert.isTrue(groups.length <= 1); //just 1 file or none, but inaccurate matches can play bad here (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=106127)
		for (int index= 0; index < groups.length; index++) {
			SearchResultGroup group= groups[index];
			if (group.getCompilationUnit().equals(wc))
				return group;
		}
		return null;
	}
	
	private static SearchPattern createSearchPattern(IType type) throws JavaModelException{
		SearchPattern pattern= SearchPattern.createPattern(type, IJavaSearchConstants.ALL_OCCURRENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		IMethod[] constructors= JavaElementUtil.getAllConstructors(type);
		if (constructors.length == 0)
			return pattern;
		SearchPattern constructorDeclarationPattern= RefactoringSearchEngine.createOrPattern(constructors, IJavaSearchConstants.DECLARATIONS);
		return SearchPattern.createOrPattern(pattern, constructorDeclarationPattern);
	}
	
	private void markAsExecuted(ICompilationUnit unit, ResourceMapping mapping) {
		ReorgExecutionLog log= (ReorgExecutionLog) getAdapter(ReorgExecutionLog.class);
		if (log != null) {
			log.markAsProcessed(unit);
			log.markAsProcessed(mapping);
		}
	}

	private String getPathLabel(IResource resource) {
		final StringBuffer buffer= new StringBuffer(resource.getProject().getName());
		final String path= resource.getParent().getProjectRelativePath().toString();
		if (path.length() > 0) {
			buffer.append('/');
			buffer.append(path);
		}
		return buffer.toString();
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.CreateCopyOfCompilationUnitChange_create_copy, new String[] { fOldCu.getElementName(), getPathLabel(fOldCu.getResource())});
	}
}
