/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.*;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;

import org.eclipse.compare.*;

/**
 * Implements the IStreamContentAccessor and ITypedElement protocols
 * for a TextBuffer.
 */
class JavaTextBufferNode implements ITypedElement, IStreamContentAccessor {
	
	private TextBuffer fBuffer;
	private boolean fInEditor;
	
	JavaTextBufferNode(TextBuffer buffer, boolean inEditor) {
		fBuffer= buffer;
		fInEditor= inEditor;
	}
	
	public String getName() {
		if (fInEditor)
			return "Editor Buffer";
		return "Workspace File";
	}
	
	public String getType() {
		return "java";
	}
	
	public Image getImage() {
		return null;
	}
	
	public InputStream getContents() {
		return new ByteArrayInputStream(fBuffer.getContent().getBytes());
	}
}

