/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;

public class SimpleCountingCollector extends SearchResultCollector {
	
	private int fFound;

	public SimpleCountingCollector(IProgressMonitor pm) {
		super(pm);
	}
	
	/**
	 * @see IJavaSearchResultCollector#accept
	 */
	public void accept(IResource res, int start, int end, IJavaElement element, int accuracy) {
		//should i accept potential matches too?
		fFound++; //anything will do
	}
	
	public void accept(IResource res, int start, int end, IJavaElement element, int accuracy, boolean qualified) {
		//should i accept potential matches too?
		fFound++; //anything will do		
	}

	public int found() {
		return fFound;
	}

}


