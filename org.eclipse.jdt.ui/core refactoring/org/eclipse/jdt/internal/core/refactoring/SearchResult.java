/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Represents a search result - exactly as found by SearchEngine
 */
public final class SearchResult {
	private IResource fResource;
	private IJavaElement fEnclosingElement;
	private int fStart, fEnd;
	private int fAccuracy;
	
	private boolean fIsQualified;

	public SearchResult(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy){
		this(resource, start, end, enclosingElement, accuracy, false);
	}
	
	/**
	 * @see IJavaSearchResultCollector#accept
	 */
	public SearchResult(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy, boolean qualified){
		fResource= resource;
		fStart= start;
		fEnd= end;
		fEnclosingElement= enclosingElement;
		fAccuracy= accuracy;
		fIsQualified= qualified;
	}

	public boolean isQualified(){
		return fIsQualified;
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
		return "\n<Search Result" 
			 + "\n\tstart:" + fStart
			 + "\n\tend:" + fEnd
			 + "\n\tresource:" + fResource.getFullPath()
			 + "\n\tjavaElement:" + fEnclosingElement + "(instanceof " + fEnclosingElement.getClass() + ")"
			 + getElementSourceRange()
			 + "\n\tAccuracy:" + fAccuracy
			 + "\n\tqualified:" + fIsQualified + "/>";
	}
	
	//just for debugging
	private String getElementSourceRange(){
		try{
			if (fEnclosingElement instanceof ISourceReference)
			    return ((ISourceReference)fEnclosingElement).getSourceRange().toString();
			else return "";
		} catch (JavaModelException e){
			return "<Exception>";
		}	
	}
}