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
package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Represents a search result - exactly as found by SearchEngine
 */
public class SearchResult {
	private final IResource fResource;
	private final IJavaElement fEnclosingElement;
	private final int fStart, fEnd;
	private final int fAccuracy;
	
	/**
	 * @see org.eclipse.jdt.core.search.IJavaSearchResultCollector#accept(IResource, int, int, IJavaElement, int)
	 */
	public SearchResult(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy){
		fResource= resource;
		fStart= start;
		fEnd= end;
		fEnclosingElement= enclosingElement;
		fAccuracy= accuracy;
	}

	public int getStart(){
		return fStart;	
	}
	
	public int getEnd(){
		return fEnd;	
	}
	
	public IResource getResource(){
		return fResource;
	}
	
	public IJavaElement getEnclosingElement(){
		return fEnclosingElement;
	}
	
	public int getAccuracy(){
		return fAccuracy;
	}
	
	/* non java-doc
	 * for debugging only
	 */
	public String toString(){
		return "<Search Result"  //$NON-NLS-1$
			 + "\n\tstart: " + fStart + "; end: " + fEnd //$NON-NLS-1$ //$NON-NLS-2$
			 + "\n\tresource: " + fResource.getFullPath() //$NON-NLS-1$
			 + "\n\tjavaElement: " + "(instanceof " + fEnclosingElement.getClass() + ")" //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$
			 + "\n" + fEnclosingElement + getElementSourceRange() //$NON-NLS-1$
			 + "\n\tAccuracy:" + fAccuracy //$NON-NLS-1$
			 + "/>"; //$NON-NLS-1$
	}
	
	//just for debugging
	private String getElementSourceRange(){
		try{
			if (fEnclosingElement instanceof ISourceReference)
			    return ((ISourceReference)fEnclosingElement).getSourceRange().toString();
			else return ""; //$NON-NLS-1$
		} catch (JavaModelException e){
			return "<Exception>"; //$NON-NLS-1$
		}	
	}
	
	public ICompilationUnit getCompilationUnit(){
		if (fEnclosingElement != null){
			if (fEnclosingElement instanceof ICompilationUnit)
				return (ICompilationUnit)fEnclosingElement;
			ICompilationUnit cu= (ICompilationUnit) fEnclosingElement.getAncestor(IJavaElement.COMPILATION_UNIT);
			if (cu != null)
				return cu;
		}
		
		IJavaElement jElement= JavaCore.create(getResource());
		if (jElement != null && jElement.exists() && jElement.getElementType() == IJavaElement.COMPILATION_UNIT)
			return (ICompilationUnit)jElement;
		return null;
	}
}
