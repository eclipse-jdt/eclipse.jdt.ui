/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

public class HistoryElement extends PlatformObject implements IPersistableElement {
	
	private static final String FACTORY_ID= "org.eclipse.jdt.internal.ui.historyElement";  //$NON-NLS-1$
	
	private static final String TAG_HISTORY_STAMP= "historyStamp"; //$NON-NLS-1$
	
	private long fHistoryStamp; 
	private IFile fFile;
	
	public HistoryElement(IFile file) {
		fHistoryStamp= System.currentTimeMillis();
		setFile(file);
	}
	
	public HistoryElement(IMemento memento) {
		restoreMemento(memento);
		if (isValid())
			initializeCompilationUnit();
	}
	
	private void initializeCompilationUnit() {
		/*
		IJavaElement element= JavaCore.create(fFile);
		if (element != null && element.exists() && element.getElementType() == IJavaElement.COMPILATION_UNIT)
			fCorrespondingCUnit= (ICompilationUnit)element;
		*/
	}

	public boolean isValid() {
		return fFile != null;
	}

	public void setFile(IFile newFile) {
		Assert.isNotNull(newFile);
		fFile= newFile;
		initializeCompilationUnit();
	}
	
	public IFile getFile() {
		return fFile;
	}
	
	public IAdaptable getModelElement() {
		IJavaElement element= JavaCore.create(fFile);
		if (element != null && element.exists() && element.getElementType() == IJavaElement.COMPILATION_UNIT)
			return (ICompilationUnit)element;
		return fFile;
	}
	
	public long getHistoryStamp() {
		return fHistoryStamp;
	}
	
	public String getFactoryId() {
		return FACTORY_ID;
	}

	public void saveState(IMemento memento) {
		memento.putString(TAG_HISTORY_STAMP, Long.toString(fHistoryStamp));
		Mementos.saveItem(memento.createChild(Mementos.TAG_ITEM), fFile);
	}
	
	public void update() {
		fHistoryStamp= System.currentTimeMillis();
	}
	
	public boolean equals(Object obj) {
		if (!getClass().getName().equals(obj.getClass().getName()))
			return false;
		HistoryElement other= (HistoryElement)obj;
		return fFile.equals(other.fFile);
	}
	
	public int hashCode() {
		return fFile.hashCode();
	}
	
	private void restoreMemento(IMemento memento) {
		String s= memento.getString(TAG_HISTORY_STAMP);
		if (s == null)
			return;
		fHistoryStamp= new Long(s).longValue();
		fFile= (IFile)Mementos.restoreItem(memento.getChild(Mementos.TAG_ITEM));
	}
}
