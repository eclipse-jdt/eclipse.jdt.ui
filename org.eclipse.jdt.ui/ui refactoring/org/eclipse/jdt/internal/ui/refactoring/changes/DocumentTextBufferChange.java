/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.changes;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.text.AbstractTextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBuffer;

import org.eclipse.jdt.internal.ui.util.IDocumentManager;

public class DocumentTextBufferChange extends AbstractTextBufferChange {
	
	private IDocumentManager fDocumentManager;
	private ITextBuffer fTextBuffer;
	private int fConnectCount;
	private boolean fConnected;
		
	public DocumentTextBufferChange(String name, IJavaElement element, IDocumentManager manager) {
		super(name, element);
		fDocumentManager= manager;
		Assert.isNotNull(fDocumentManager);
	}
	
	private DocumentTextBufferChange(String name, IJavaElement element, IDocumentManager manager, List modifications, boolean isUndo) {
		super(name, element, modifications, isUndo);
		fDocumentManager= manager;
		Assert.isNotNull(fDocumentManager);
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void aboutToPerform() {
		try {
			connectTextBuffer();
		} catch (CoreException e) {
			return;
		}
		fConnected= true;
		super.aboutToPerform();
	}
	 
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void performed() {
		if (fConnected) {
			fDocumentManager.changed();
			disconnectTextBuffer();
			fConnected= false;
		}
		super.performed();
	}
	 
	//---- Text Buffer management ------------------------------------------
	
	public void connectTextBuffer() throws CoreException {
		if (fConnectCount == 0) {
			fDocumentManager.connect();
			fTextBuffer= new TextBuffer(fDocumentManager.getDocument());
		}
		fConnectCount++;	
	}
	 
	public void disconnectTextBuffer() {
		fConnectCount--;
		if (fConnectCount == 0) {
			fDocumentManager.disconnect();
			fTextBuffer= null;
		}
	}
	
	protected ITextBuffer getTextBuffer() {
		return fTextBuffer;
	}
	
	protected void saveTextBuffer(IProgressMonitor pm) throws CoreException {
		fDocumentManager.aboutToChange();
		fDocumentManager.save(pm);
	}
	
	protected IChange createChange(List modifications, boolean isUndo) {
		return new DocumentTextBufferChange(getName(), 
			getCorrespondingJavaElement(), fDocumentManager, modifications, isUndo);
	}
	
	protected ITextBuffer createTextBuffer(String content) {
		return new TextBuffer(new Document(content));
	}	
}