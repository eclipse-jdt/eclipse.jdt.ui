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
 * Adapts IDocument to IBuffer.
 */
class DocumentAdapter implements IBuffer, IDocumentListener {
	
	private IOpenable fOwner;
	private IDocument fDocument;
	private Object fProviderKey;
	private CompilationUnitDocumentProvider fProvider;
	
	private List fBufferListeners= new ArrayList(3);
	
	
	DocumentAdapter(IOpenable owner, IDocument document, CompilationUnitDocumentProvider provider, Object providerKey) {
		
		Assert.isNotNull(owner);
		Assert.isNotNull(document);
		
		fOwner= owner;
		fDocument= document;
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
			fDocument.replace(fDocument.getLength(), 0, text);
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
			fDocument.replace(position, length, text);
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
		fDocument.set(contents);
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
