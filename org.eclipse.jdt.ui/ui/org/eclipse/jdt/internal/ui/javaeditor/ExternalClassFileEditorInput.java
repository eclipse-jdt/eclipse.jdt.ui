package org.eclipse.jdt.internal.ui.javaeditor;


/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
	
import org.eclipse.core.resources.IFile;

import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaCore;


/**
 * Editor input for .class files on the file system.
 */
public class ExternalClassFileEditorInput extends FileEditorInput implements IClassFileEditorInput {
	
	private IClassFile fClassFile;
	
	ExternalClassFileEditorInput(IFile file) {
		super(file);
		refresh();
	}
	
	/*
	 * @see IClassFileEditorInput#getClassFile()
	 */
	public IClassFile getClassFile() {
		return fClassFile;
	}
	
	/**
	 * Refreshs this input element. Workaround for non-updating class file elements.
	 */
	public void refresh() {
		Object element= JavaCore.create(getFile());
		if (element instanceof IClassFile)
			fClassFile= (IClassFile) element;
	}

	/*
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IClassFile.class)
			return fClassFile;
		return fClassFile.getAdapter(adapter);
	}
	
}