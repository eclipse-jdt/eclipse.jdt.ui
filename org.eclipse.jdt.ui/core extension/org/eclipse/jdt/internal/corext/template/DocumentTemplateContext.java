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
	/** The completion offset. */
	private final int fCompletionOffset;
	/** The completion length. */
	private final int fCompletionLength;

	/**
	 * Creates a document template context.
	 */
	protected DocumentTemplateContext(ContextType type, IDocument document,
		int completionOffset, int completionLength)
	{
		super(type);
		
		Assert.isNotNull(document);
		Assert.isTrue(completionOffset >= 0 && completionOffset <= document.getLength());
		Assert.isTrue(completionLength >= 0);
		
		fDocument= document;
		fCompletionOffset= completionOffset;
		fCompletionLength= completionLength;
	}
	
	public IDocument getDocument() {
		return fDocument;	
	}
	
	/**
	 * Returns the completion offset within the string of the context.
	 */
	public int getCompletionOffset() {
		return fCompletionOffset;	
	}
	
	/**
	 * Returns the completion length within the string of the context.
	 */
	public int getCompletionLength() {
		return fCompletionLength;
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
		return fCompletionOffset;		
	}
	
	/**
	 * Returns the end offset of the keyword.
	 */
	public int getEnd() {
		return fCompletionOffset + fCompletionLength;
	}
		
}
