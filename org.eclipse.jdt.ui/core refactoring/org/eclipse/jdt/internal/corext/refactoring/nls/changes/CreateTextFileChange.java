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
package org.eclipse.jdt.internal.corext.refactoring.nls.changes;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSUtil;

public class CreateTextFileChange extends CreateFileChange {
	
	private final String fTextType;
	
	public CreateTextFileChange(IPath path, String source, String encoding, String textType) {
		super(path, source, encoding);
		fTextType= textType;
	}
	
	public String getTextType() {
		return fTextType;
	}
	
	public String getCurrentContent() throws JavaModelException {
		IFile file= getOldFile(new NullProgressMonitor());
		if (! file.exists())
			return ""; //$NON-NLS-1$
		try{
			String c= NLSUtil.readString(file.getContents());	
			return (c == null) ? "": c; //$NON-NLS-1$
		} catch (CoreException e){
			throw new JavaModelException(e, IJavaModelStatusConstants.CORE_EXCEPTION);
		}	
	}
	
	public String getPreview() {
		return getSource();
	}
}

