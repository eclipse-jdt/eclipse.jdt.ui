/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.tagging;



public interface ITextUpdatingRefactoring extends IRenameRefactoring{

	/**
	 * Performs a dynamic check whether this refactoring object is capable of updating references to the renamed element.
	 */
	public boolean canEnableTextUpdating();
	
	/**
	 * If <code>canEnableTextUpdating</code> returns <code>true</code>, then this method is used to
	 * ask the refactoring object whether references in JavaDoc comments should be updated.
	 * This call can be ignored if  <code>canEnableTextUpdating</code> returns <code>false</code>.
	 */		
	public boolean getUpdateJavaDoc();

	/**
	 * If <code>canEnableTextUpdating</code> returns <code>true</code>, then this method is used to
	 * ask the refactoring object whether references in regular (non JavaDoc) comments should be updated.
	 * This call can be ignored if  <code>canEnableTextUpdating</code> returns <code>false</code>.
	 */			
	public boolean getUpdateComments();
	
	/**
	 * If <code>canEnableTextUpdating</code> returns <code>true</code>, then this method is used to
	 * ask the refactoring object whether references in string literals should be updated.
	 * This call can be ignored if  <code>canEnableTextUpdating</code> returns <code>false</code>.
	 */		
	public boolean getUpdateStrings();
	
	/**
	 * If <code>canEnableTextUpdating</code> returns <code>true</code>, then this method is used to
	 * inform the refactoring object whether references in JavaDoc comments should be updated.
	 * This call can be ignored if  <code>canEnableTextUpdating</code> returns <code>false</code>.
	 */	
	public void setUpdateJavaDoc(boolean update);

	/**
	 * If <code>canEnableTextUpdating</code> returns <code>true</code>, then this method is used to
	 * inform the refactoring object whether references in regular (non JavaDoc)  comments should be updated.
	 * This call can be ignored if  <code>canEnableTextUpdating</code> returns <code>false</code>.
	 */		
	public void setUpdateComments(boolean update);
	
	/**
	 * If <code>canEnableTextUpdating</code> returns <code>true</code>, then this method is used to
	 * inform the refactoring object whether references in string literals should be updated.
	 * This call can be ignored if  <code>canEnableTextUpdating</code> returns <code>false</code>.
	 */	
	public void setUpdateStrings(boolean update);
}


