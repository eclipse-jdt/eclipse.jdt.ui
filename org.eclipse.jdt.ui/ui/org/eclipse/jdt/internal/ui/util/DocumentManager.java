/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;


import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
  
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Helper class to deal with documents and annotations.
 */
public class DocumentManager implements IDocumentManager {
	
	private IFileEditorInput fInput;
	private IDocumentProvider fDocumentProvider;
	private int fConnectCount= 0;
	private IDocument fDocument;
	private IAnnotationModel fAnnotationModel;

	/**
	 * Creates a new DocumentManager using the Java Plugin's compilation
	 * unit document provider.
	 */
	public DocumentManager(ICompilationUnit co) throws JavaModelException {
		this(getUnderlyingResource(co), JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
	}
	
	private static IFile getUnderlyingResource(ICompilationUnit cu) throws JavaModelException {
		if (cu.isWorkingCopy()) {
			cu= (ICompilationUnit) cu.getOriginalElement();
		}
		return (IFile) cu.getUnderlyingResource();
	}	
	 
	/**
	 * Creates a new DocumentManager for the given compilation unit and document provider.
	 */
	public DocumentManager(ICompilationUnit co, IDocumentProvider provider)  throws JavaModelException {
		this(getUnderlyingResource(co), provider);
	}
	
	/**
	 * Creates a new DocumentManager for the given file and document provider.
	 */
	public DocumentManager(IFile file, IDocumentProvider provider) {
		fInput= new FileEditorInput(file);
		Assert.isNotNull(fInput);
		fDocumentProvider= provider;
		Assert.isNotNull(fDocumentProvider);
	}
		
	/**
	 * Connects the document to the annotation model. The document is shared via 
	 * the document provider passed to the constructor.
	 */
	public void connect() throws CoreException {
		if (fConnectCount == 0) {
			fDocumentProvider.connect(fInput);
			fDocument= fDocumentProvider.getDocument(fInput);
			fAnnotationModel= fDocumentProvider.getAnnotationModel(fInput);
			fAnnotationModel.connect(fDocument);
		}
		fConnectCount++;
	}
	
	/**
	 * Returns the document managed by this manager. This method returns <code>
	 * null</code> if the document is disconnected.
	 */
	public IDocument getDocument() {
		return fDocument;
	}
	
	/**
	 * Disconnects the document managed by this manager from its document
	 * provider and disconnects the annotation model.
	 */
	public void disconnect() {
		Assert.isTrue(fConnectCount > 0);
		fConnectCount--;
		if(fConnectCount == 0) {
			fAnnotationModel.disconnect(fDocument);			
			fDocumentProvider.disconnect(fInput);
			fDocument= null;
			fAnnotationModel= null;
		}
	} 
	
	/**
	 * The client is about to change the document.
	 */
	public void aboutToChange() {
		if (fDocument == null)
			return;
		
		fDocumentProvider.aboutToChange(fInput);
	}
	 
	/**
	 * Save the document managed by this object.
	 */
	public void save(IProgressMonitor pm) throws CoreException {
		if (fDocument == null)
			return;
			
		fDocumentProvider.saveDocument(pm, fInput, fDocument, false);	
	}	 
	
	/**
	 * The client has changed the document.
	 */
	public void changed() {
		if (fDocument == null)
			return;
			
		fDocumentProvider.changed(fInput);
	} 	
}