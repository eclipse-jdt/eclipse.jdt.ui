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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class CreateFileChange extends JDTChange {

	private IPath fPath;
	private String fSource;
	private String fName;
	private String fEncoding;
	
	public CreateFileChange(IPath path, String source, String encoding){
		Assert.isNotNull(path, "path"); //$NON-NLS-1$
		Assert.isNotNull(source, "source"); //$NON-NLS-1$
		fPath= path;
		fSource= source;
		fEncoding= encoding;
	}

	public CreateFileChange(IPath path, String source){
		this(path, source, ResourcesPlugin.getEncoding());
	}
	
	protected void setSource(String source){
		fSource= source;
	}

	protected void setPath(IPath path){
		fPath= path;
	}	
	
	protected IPath getPath(){
		return fPath;
	}	
	
	public RefactoringStatus isValid(IProgressMonitor pm) {
		return new RefactoringStatus();
	}
	
	public Change perform(IProgressMonitor pm)	throws CoreException {

		InputStream is= null;
		try {
			pm.beginTask(NLSChangesMessages.getString("createFile.creating_resource"), 2); //$NON-NLS-1$

			IFile file= getOldFile(new SubProgressMonitor(pm, 1));
			if (file.exists()){
				CompositeChange composite= new CompositeChange(getName());
				composite.add(new DeleteFileChange(file));
				composite.add(new CreateFileChange(fPath, fSource));
				return composite.perform(pm);
			} else {
				is= getInputStream(file);
				file.create(is, false, pm);
				return new DeleteFileChange(file);
			}				
		} finally {
			pm.done();
			try{
				if (is != null)
					is.close();
			} catch (IOException ioe) {
				throw new JavaModelException(ioe, IJavaModelStatusConstants.IO_EXCEPTION);
			}
		}
	}
	
	protected IFile getOldFile(IProgressMonitor pm) {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
		} finally{
			pm.done();
		}
	}

	private InputStream getInputStream(IFile file) {
		String encoding= fEncoding;
		if (encoding == null) {
			try {
				encoding= file.getCharset();
			} catch (CoreException e1) {
				// fall through. Take default encoding.
			}
		}
		if (encoding == null)
			return new ByteArrayInputStream(fSource.getBytes());
		try {
			return new ByteArrayInputStream(fSource.getBytes(encoding));
		} catch (UnsupportedEncodingException e) {
			return new ByteArrayInputStream(fSource.getBytes());
		}
	}
	
	public String getName() {
		if (fName == null)
			return NLSChangesMessages.getString("createFile.Create_file") + fPath.toString(); //$NON-NLS-1$
		else 
			return fName;
	}

	public Object getModifiedElement() {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
	}

	protected String getSource() {
		return fSource;
	}

	public void setName(String name) {
		fName = name;
	}
}

