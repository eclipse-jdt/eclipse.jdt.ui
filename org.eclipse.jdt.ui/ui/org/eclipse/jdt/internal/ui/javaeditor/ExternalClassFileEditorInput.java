package org.eclipse.jdt.internal.ui.javaeditor;


/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
	
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.jdt.core.IClassFile;


/**
 * Editor input for .class files on the file system.
 */
public class ExternalClassFileEditorInput extends FileEditorInput implements IClassFileEditorInput {
	
	private IClassFile fClassFile;
	
	ExternalClassFileEditorInput(IFile file, IClassFile classFile) {
		super(file);
		fClassFile= classFile;
	}
	
	/*
	 * @see IClassFileEditorInput#getClassFile()
	 */
	public IClassFile getClassFile() {
		return fClassFile;
	}
}