/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.tagging;

import org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring;

public interface IReferenceUpdatingRefactoring extends IRefactoring{

	/**
	 * Checks if this refactoring object is capable of updating references to the renamed element.
	 */
	public boolean canEnableUpdateReferences();

	/**
	 * If <code>canUpdateReferences</code> returns <code>true</code>, then this method is used to
	 * inform the refactoring object whether references should be updated.
	 * This call can be ignored if  <code>canUpdateReferences</code> returns <code>false</code>.
	 */	
	public void setUpdateReferences(boolean update);

	/**
	 * If <code>canUpdateReferences</code> returns <code>true</code>, then this method is used to
	 * ask the refactoring object whether references should be updated.
	 * This call can be ignored if  <code>canUpdateReferences</code> returns <code>false</code>.
	 */		
	public boolean getUpdateReferences();

}

