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
	 *
	 */
	void postSave(ICompilationUnit unit);
}