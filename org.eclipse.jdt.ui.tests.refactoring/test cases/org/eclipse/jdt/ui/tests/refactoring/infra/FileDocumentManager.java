/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring.infra;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.util.IDocumentManager;

public class FileDocumentManager implements IDocumentManager{

	private ICompilationUnit fCU;
	private IDocument fDocument;
	
	public FileDocumentManager(ICompilationUnit cu) throws JavaModelException {
		fCU= cu;
	}
	
	public void save(IProgressMonitor pm) throws CoreException {
		if (fDocument == null)
			return;
		String newSource= fDocument.get();
		fCU.getBuffer().setContents(newSource);
		fCU.save(pm, true);		
	}

	public void connect(){
		if (fDocument != null)
			return;
		try{
			fDocument= new Document(fCU.getSource());
		} catch (JavaModelException e){
			e.printStackTrace();
		}	
	}
	
	public void disconnect() {
		fDocument= null;
	}

	public IDocument getDocument() {
		return fDocument;
	}
	
	public void changed(){
		//??
	}

	public void aboutToChange(){
		//??
	}
}