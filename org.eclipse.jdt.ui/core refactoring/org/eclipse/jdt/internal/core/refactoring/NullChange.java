/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.refactoring.Change;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.ChangeContext;

public class NullChange extends Change {

	private String fName;
	
	public NullChange(String name){
		fName= name;
	}
	
	public NullChange(){
		this(null);
	}
		
	public void perform(ChangeContext context, IProgressMonitor pm) {
	}

	public IChange getUndoChange() {
		return new NullChange(fName);
	}
	
	public String getName(){
		return "NullChange (" + fName + ")"; 
	}
	
	public IJavaElement getCorrespondingJavaElement(){
		return null;
	}
}
