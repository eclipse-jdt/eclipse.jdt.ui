/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.*;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.internal.corext.codemanipulation.TextBuffer;

import org.eclipse.compare.*;

/**
 * Makes the IStreamContentAccessor and ITypedElement protocols
 * available to for a TextBuffer.
 */
class JavaTextBufferNode implements ITypedElement, IStreamContentAccessor {
	
	private TextBuffer fBuffer;
	private String fName;
	
	JavaTextBufferNode(TextBuffer buffer, String name) {
		fBuffer= buffer;
	}
	
	public String getName() {
		return fName;
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

