/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.jface.text.IDocument;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IDocumentManager {

	/**
	 * Connects the document to its underlying document sharing mechanism.
	 */
	public void connect() throws CoreException;
	
	/**
	 * Returns the document managed by this manager. This method returns <code>
	 * null</code> if the document is disconnected.
	 */
	public IDocument getDocument();

	/**
	 * Disconnects the document managed by this manager from its underlying
	 * document sharing mechanism.
	 */
	public void disconnect();
	
	/**
	 * The client is about to change the document managed by this document manager.
	 */
	public void aboutToChange();
	
	/**
	 * Save the document managed by this object.
	 */
	public void save(IProgressMonitor pm) throws CoreException;
	
	/**
	 * The client has changed the document managed by this manager.
	 */
	public void changed();	
}