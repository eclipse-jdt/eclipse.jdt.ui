/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class CompilationUnitChange extends TextFileChange {

	private ICompilationUnit fCUnit;

	/**
	 * Creates a new <code>CompilationUnitChange</code>.
	 * 
	 * @param name the change's name mainly used to render the change in the UI
	 * @param cunit the compilation unit this text change works on
	 */
	public CompilationUnitChange(String name, ICompilationUnit cunit) throws CoreException {
		super(name, getFile(cunit));
		Assert.isNotNull(cunit);
		fCUnit= cunit;
		setTextType("java"); //$NON-NLS-1$
	}
	
	private static IFile getFile(ICompilationUnit cunit) throws CoreException {
		cunit= JavaModelUtil.toOriginal(cunit);
		return (IFile) cunit.getResource();
	}
	
	/* non java-doc
	 * Method declared in IChange.
	 */
	public Object getModifiedLanguageElement(){
		return fCUnit;
	}
	
	/**
	 * Returns the compilation unit this change works on.
	 * 
	 * @return the compilation unit this change works on
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCUnit;
	}	
}

