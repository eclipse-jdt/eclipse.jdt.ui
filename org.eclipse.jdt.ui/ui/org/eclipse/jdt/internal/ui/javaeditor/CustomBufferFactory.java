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

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferFactory;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IOpenable;


/**
 * Creates <code>IBuffer</code>s based on documents.
 */
public class CustomBufferFactory implements IBufferFactory {
	
	/*
	 * @see org.eclipse.jdt.core.IBufferFactory#createBuffer(org.eclipse.jdt.core.IOpenable)
	 */
	public IBuffer createBuffer(IOpenable owner) {
		if (owner instanceof ICompilationUnit) {
			ICompilationUnit unit= (ICompilationUnit) owner;
			ICompilationUnit original= (ICompilationUnit) unit.getOriginalElement();
			IResource resource= original.getResource();
			if (resource instanceof IFile) {
				return new DocumentAdapter(unit, (IFile) resource);
			}
				
		}
		return DocumentAdapter.NULL;
	}
}