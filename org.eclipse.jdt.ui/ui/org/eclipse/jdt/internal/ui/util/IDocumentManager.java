/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.jface.text.IDocument;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A document manager is responsible to connect a document managed by a document provider
 * to a corresponding annontation model to ensure the markers are updated whenever the
 * document gets changed.
 */
public interface IDocumentManager {

	/**
	 * Connects the document to its underlying document sharing mechanism.
	 * @exception CoreException if the textual representation or the annotation model
	 *	could not be created
	 */
	public void connect() throws CoreException;
	
	/**
	 * Returns the document managed by this manager. This method returns <code>
	 * null</code> if the document is disconnected.
	 * 
	 * @return the managed document
	 */
	public IDocument getDocument();

	/**
	 * Disconnects the document managed by this manager from its underlying
	 * document sharing mechanism. Additionally the document is also disconnected
	 * from the annotation model.
	 */
	public void disconnect();
	
	/**
	 * Informs this document manager about upcoming changes of the managed document.
	 */
	public void aboutToChange();
	
	/**
	 * Saves the managed document.
	 * 
	 * @param monitor a progress monitor to report progress and request cancelation
	 */
	public void save(IProgressMonitor pm) throws CoreException;
	
	/**
	 * Informs this document manager that the managed element has been changed.
	 */
	public void changed();	
}