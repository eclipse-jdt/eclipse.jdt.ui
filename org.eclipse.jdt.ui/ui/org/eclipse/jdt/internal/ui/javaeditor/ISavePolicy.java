package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
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