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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferFactory;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IOpenable;

import org.eclipse.jdt.ui.JavaUI;


/**
 * Creates <code>IBuffer</code>s based on documents.
 */
public class CustomBufferFactory implements IBufferFactory {
	
	private IDocument internalGetDocument(CompilationUnitDocumentProvider provider, IFileEditorInput input) throws CoreException {
		IDocument document= provider.getDocument(input);
		if (document != null)
			return document;
		return provider.createDocument(input);
	}
	
	/*
	 * @see org.eclipse.jdt.core.IBufferFactory#createBuffer(org.eclipse.jdt.core.IOpenable)
	 */
	public IBuffer createBuffer(IOpenable owner) {
		if (owner instanceof ICompilationUnit) {
			
			ICompilationUnit unit= (ICompilationUnit) owner;
			ICompilationUnit original= (ICompilationUnit) unit.getOriginalElement();
			IResource resource= original.getResource();
			if (resource instanceof IFile) {
				IFileEditorInput providerKey= new FileEditorInput((IFile) resource);
				
				IDocument document= null;
				IStatus status= null;
				
				CompilationUnitDocumentProvider provider= (CompilationUnitDocumentProvider) JavaUI.getDocumentProvider();
				
				try {
					document= internalGetDocument(provider, providerKey);
				} catch (CoreException x) {
					status= x.getStatus();
					document= new Document();
					provider.initializeDocument(document);
				}
				
				DocumentAdapter adapter= new DocumentAdapter(unit, document, new DefaultLineTracker(), provider, providerKey);
				adapter.setStatus(status);
				return adapter;
			}
				
		}
		return DocumentAdapter.NULL;
	}
}