/**********************************************************************
Copyright (c) 2000, 2003 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
	IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;


/**
 * Document that can also be used by a background reconciler.
 */
public class PartiallySynchronizedDocument extends Document {
	
	/*
	 * @see IDocumentExtension#startSequentialRewrite(boolean)
	 */
	synchronized public void startSequentialRewrite(boolean normalized) {
		super.startSequentialRewrite(normalized);
	}

	/*
	 * @see IDocumentExtension#stopSequentialRewrite()
	 */
	synchronized public void stopSequentialRewrite() {
		super.stopSequentialRewrite();
	}
	
	/*
	 * @see IDocument#get()
	 */
	synchronized public String get() {
		return super.get();
	}
	
	/*
	 * @see IDocument#get(int, int)
	 */
	synchronized public String get(int offset, int length) throws BadLocationException {
		return super.get(offset, length);
	}
	
	/*
	 * @see IDocument#getChar(int)
	 */
	synchronized public char getChar(int offset) throws BadLocationException {
		return super.getChar(offset);
	}
	
	/*
	 * @see IDocument#replace(int, int, String)
	 */
	synchronized public void replace(int offset, int length, String text) throws BadLocationException {
		super.replace(offset, length, text);
	}
	
	/*
	 * @see IDocument#set(String)
	 */
	synchronized public void set(String text) {
		super.set(text);
	}
	
	/*
	 * @see org.eclipse.jface.text.AbstractDocument#addPosition(java.lang.String, org.eclipse.jface.text.Position)
	 */
	synchronized public void addPosition(String category, Position position) throws BadLocationException, BadPositionCategoryException {
		super.addPosition(category, position);
	}
	
	/*
	 * @see org.eclipse.jface.text.AbstractDocument#removePosition(java.lang.String, org.eclipse.jface.text.Position)
	 */
	synchronized public void removePosition(String category, Position position) throws BadPositionCategoryException {
		super.removePosition(category, position);
	}
	
	/*
	 * @see org.eclipse.jface.text.AbstractDocument#getPositions(java.lang.String)
	 */
	synchronized public Position[] getPositions(String category) throws BadPositionCategoryException {
		return super.getPositions(category);
	}
}