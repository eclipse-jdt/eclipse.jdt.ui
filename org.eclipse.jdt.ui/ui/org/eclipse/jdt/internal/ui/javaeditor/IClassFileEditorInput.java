package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.jdt.core.IClassFile;
import org.eclipse.ui.IEditorInput;


/**
 * Editor input for class files.
 */
public interface IClassFileEditorInput extends IEditorInput {
	
	/** 
	 * Returns the class file acting as input.
	 */
	public IClassFile getClassFile();	
}

