package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.BufferChangedEvent;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Adapts <code>IDocument</code> to <code>IBuffer</code>. Uses the
 * same algorithm as the text widget to determine the buffer's line delimiter. 
 * All text inserted into the buffer is converted to this line delimiter.
 * This class is <code>public</code> for test purposes only.
 */
public class DocumentAdapter implements IBuffer, IDocumentListener {
	
	private IOpenable fOwner;
	private IDocument fDocument;
	private Object fProviderKey;
	private CompilationUnitDocumentProvider fProvider;
	private String fLineDelimiter;
	private ILineTracker fLineTracker;
	
	private List fBufferListeners= new ArrayList(3);
	
	/**
	 * This method is <code>public</code> for test purposes only.
	 */
	public DocumentAdapter(IOpenable owner, IDocument document, ILineTracker lineTracker, CompilationUnitDocumentProvider provider, Object providerKey) {
		
		Assert.isNotNull(document);
		Assert.isNotNull(lineTracker);
		
		fOwner= owner;
		fDocument= document;
		fLineTracker= lineTracker;
		fProvider= provider;
		fProviderKey= providerKey;
		
		fDocument.addPrenotifiedDocumentListener(this);
	}
	
	/**
	 * Returns the adapted document.
	 * 
	 * @return the adapted document
	 */
	public IDocument getDocument() {
		return fDocument;
	}
	
	/**
	 * Returns the line delimiter of this buffer. As a document has a set of
	 * valid line delimiters, this set must be reduced to size 1.
	 */
	protected String getLineDelimiter() {
		
		if (fLineDelimiter == null) {
			
			try {
				fLineDelimiter= fDocument.getLineDelimiter(0);
			} catch (BadLocationException x) {
			}
			
			if (fLineDelimiter == null) {
				/*
				 * Follow up fix for: 1GF5UU0: ITPJUI:WIN2000 - "Organize Imports" in java editor inserts lines in wrong format
				 * The line delimiter must always be a legal document line delimiter.
				 */
				String sysLineDelimiter= System.getProperty("line.separator"); //$NON-NLS-1$
				String[] delimiters= fDocument.getLegalLineDelimiters();
				Assert.isTrue(delimiters.length > 0);
				for (int i= 0; i < delimiters.length; i++) {
					if (delimiters[i].equals(sysLineDelimiter)) {
						fLineDelimiter= sysLineDelimiter;
						break;
					}
				}
				
				if (fLineDelimiter == null) {
					// system line delimiter is not a legal document line delimiter
					fLineDelimiter= delimiters[0];
				}
			}
		}
		
		return fLineDelimiter;
	}	
	
	/**
	 * Converts the given string to the line delimiter of this buffer.
	 * This method is <code>public</code> for test purposes only.
	 */
	public String normalize(String text) {
		fLineTracker.set(text);
		
		int lines= fLineTracker.getNumberOfLines();
		if (lines <= 1)
			return text;
			
		StringBuffer buffer= new StringBuffer(text);
		
		try {
			IRegion previous= fLineTracker.getLineInformation(0);
			for (int i= 1; i < lines; i++) {
				int lastLineEnd= previous.getOffset() + previous.getLength();
				int lineStart= fLineTracker.getLineInformation(i).getOffset();
				fLineTracker.replace(lastLineEnd,  lineStart - lastLineEnd, getLineDelimiter());
				buffer.replace(lastLineEnd, lineStart, getLineDelimiter());
				previous= fLineTracker.getLineInformation(i);
			}
			
			// last line
			String delimiter= fLineTracker.getLineDelimiter(lines -1);
			if (delimiter != null && delimiter.length() > 0)
				buffer.replace(previous.getOffset() + previous.getLength(), buffer.length(), getLineDelimiter());
				
			return buffer.toString();
		} catch (BadLocationException x) {
		}
		
		return text;
	}
	
	/*
	 * @see IBuffer#addBufferChangedListener(IBufferChangedListener)
	 */
	public void addBufferChangedListener(IBufferChangedListener listener) {
		Assert.isNotNull(listener);
		if (!fBufferListeners.contains(listener))
			fBufferListeners.add(listener);
	}
	
	/*
	 * @see IBuffer#removeBufferChangedListener(IBufferChangedListener)
	 */
	public void removeBufferChangedListener(IBufferChangedListener listener) {
		Assert.isNotNull(listener);
		fBufferListeners.remove(listener);
	}
	
	/*
	 * @see IBuffer#append(char[])
	 */
	public void append(char[] text) {
		append(new String(text));
	}
	
	/*
	 * @see IBuffer#append(String) 
	 */
	public void append(String text) {
		try {
			fDocument.replace(fDocument.getLength(), 0, normalize(text));
		} catch (BadLocationException x) {
			// cannot happen
		}
	}
	
	/*
	 * @see IBuffer#close()
	 */
	public void close() {
		if (fDocument != null) {
			// already closed
			return;
		}
		
		IDocument d= fDocument;
		fDocument= null;
		d.removePrenotifiedDocumentListener(this);
		
		fireBufferChanged(new BufferChangedEvent(this, 0, 0, null));
		fBufferListeners.clear();
		fBufferListeners= null;
	}
	
	/*
	 * @see IBuffer#getChar(int)
	 */
	public char getChar(int position) {
		try {
			return fDocument.getChar(position);
		} catch (BadLocationException x) {
			throw new ArrayIndexOutOfBoundsException();
		}
	}
	
	/*
	 *  @see IBuffer#getCharacters()
	 */
	public char[] getCharacters() {
		String content= getContents();
		return content == null ? null : content.toCharArray();
	}
	
	/*
	 * @see IBuffer#getContents()
	 */
	public String getContents() {
		return fDocument.get();
	}
	
	/*
	 * @see IBuffer#getLength()
	 */
	public int getLength() {
		return fDocument.getLength();
	}
	
	/*
	 * @see IBuffer#getOwner()
	 */
	public IOpenable getOwner() {
		return (IOpenable) fOwner;
	}
	
	/*
	 * @see IBuffer#getText(int, int)
	 */
	public String getText(int offset, int length) {
		try {
			return fDocument.get(offset, length);
		} catch (BadLocationException x) {
			throw new ArrayIndexOutOfBoundsException();
		}
	}
	
	/*
	 * @see IBuffer#getUnderlyingResource()
	 */
	public IResource getUnderlyingResource() {
		return fProvider != null ? fProvider.getUnderlyingResource(fProviderKey) : null;
	}
	
	/*
	 * @see IBuffer#hasUnsavedChanges()
	 */
	public boolean hasUnsavedChanges() {
		return fProvider != null ? fProvider.canSaveDocument(fProviderKey) : false;
	}
	
	/*
	 * @see IBuffer#isClosed()
	 */
	public boolean isClosed() {
		return fDocument == null;
	}
	
	/*
	 * @see IBuffer#isReadOnly()
	 */
	public boolean isReadOnly() {
		IResource resource= getUnderlyingResource();
		return resource == null ? true : resource.isReadOnly();
	}
	
	/*
	 * @see IBuffer#replace(int, int, char[])
	 */
	public void replace(int position, int length, char[] text) {
		replace(position, length, new String(text));
	}
	
	/*
	 * @see IBuffer#replace(int, int, String)
	 */
	public void replace(int position, int length, String text) {
		try {
			fDocument.replace(position, length, normalize(text));
		} catch (BadLocationException x) {
			throw new ArrayIndexOutOfBoundsException();
		}
	}
	
	/*
	 * @see IBuffer#save(IProgressMonitor, boolean)
	 */
	public void save(IProgressMonitor progress, boolean force) throws JavaModelException {
		if (fProvider != null) {
			try {
				fProvider.saveDocumentContent(progress, fProviderKey, fDocument, force);
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}
	}
	
	/*
	 * @see IBuffer#setContents(char[])
	 */
	public void setContents(char[] contents) {
		setContents(new String(contents));
	}
	
	/*
	 * @see IBuffer#setContents(String)
	 */
	public void setContents(String contents) {
		fDocument.set(normalize(contents));
	}
	
	/*
	 * @see IDocumentListener#documentAboutToBeChanged(DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {
		// there is nothing to do here
	}

	/*
	 * @see IDocumentListener#documentChanged(DocumentEvent)
	 */
	public void documentChanged(DocumentEvent event) {
		fireBufferChanged(new BufferChangedEvent(this, event.getOffset(), event.getLength(), event.getText()));
	}
	
	private void fireBufferChanged(BufferChangedEvent event) {
		if (fBufferListeners != null && fBufferListeners.size() > 0) {
			Iterator e= new ArrayList(fBufferListeners).iterator();
			while (e.hasNext())
				((IBufferChangedListener) e.next()).bufferChanged(event);
		}
	}
}
