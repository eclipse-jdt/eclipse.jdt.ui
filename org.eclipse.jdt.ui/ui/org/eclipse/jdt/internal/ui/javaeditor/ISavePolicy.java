package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.jdt.core.ICompilationUnit;

public interface ISavePolicy {

	/**
	 *
	 */
	void preSave(ICompilationUnit unit);
	
	/**
	 * Returns the compilation unit in which the argument
	 * has been changed. If the argument is not changed, the
	 * returned result is <code>null</code>.
	 */
	ICompilationUnit postSave(ICompilationUnit unit);
}