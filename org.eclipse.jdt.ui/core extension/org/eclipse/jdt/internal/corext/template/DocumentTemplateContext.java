/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A typical text based document template context.
 */
public abstract class DocumentTemplateContext extends TemplateContext {

	/** The text of the document. */
	private final IDocument fDocument;
	/** The completion position. */
	private final int fCompletionPosition;

	/**
	 * Creates a document template context.
	 */
	protected DocumentTemplateContext(ContextType type, IDocument document, int completionPosition) {
		super(type);
		
		Assert.isNotNull(document);
		Assert.isTrue(completionPosition >= 0 && completionPosition <= document.getLength());
		
		fDocument= document;
		fCompletionPosition= completionPosition;
	}
	
	public IDocument getDocument() {
		return fDocument;	
	}
	
	/**
	 * Returns the string of the context.
	 */
//	public String getString() {
//		return fDocument.get();
//	}
	
	/**
	 * Returns the completion position within the string of the context.
	 */
	public int getCompletionPosition() {
		return fCompletionPosition;	
	}
	
	/**
	 * Returns the keyword which triggered template insertion.
	 */
	public String getKey() {
		int offset= getStart();
		int length= getEnd() - offset;
		try {
			return fDocument.get(offset, length);
		} catch (BadLocationException e) {
			return ""; //$NON-NLS-1$	
		}
	}

	/**
	 * Returns the beginning offset of the keyword.
	 */
	public int getStart() {
		return fCompletionPosition;		
	}
	
	/**
	 * Returns the end offset of the keyword.
	 */
	public int getEnd() {
		return fCompletionPosition;
	}
		
}
