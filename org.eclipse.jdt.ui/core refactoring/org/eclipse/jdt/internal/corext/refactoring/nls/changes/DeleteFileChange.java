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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.jdt.internal.corext.util.IOCloser;

public class DeleteFileChange extends JDTChange {

	private IPath fPath;
	private String fSource;
	
	public DeleteFileChange(IFile file){
		Assert.isNotNull(file, "file"); //$NON-NLS-1$
		fPath= file.getFullPath().removeFirstSegments(ResourcesPlugin.getWorkspace().getRoot().getFullPath().segmentCount());
	}
	
	/*
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(NLSChangesMessages.getString("deleteFile.deleting_resource"), 1); //$NON-NLS-1$
			IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
			Assert.isNotNull(file);
			Assert.isTrue(file.exists());
			Assert.isTrue(!file.isReadOnly());
			fSource= getSource(file);
			file.delete(true, false, pm);
			return new CreateFileChange(fPath, fSource);
		} finally {
			pm.done();
		}
	}
	
	private String getSource(IFile file) throws CoreException {
		InputStream in= file.getContents();
		// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19319
		StringBuffer sb= new StringBuffer();
		BufferedReader br= null;
		try {
			br= new BufferedReader(new InputStreamReader(in, ResourcesPlugin.getEncoding()));	
			int read= 0;
			while ((read= br.read()) != -1)
				sb.append((char) read);
		} catch (IOException e){
				throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
		} finally {
			try{
				IOCloser.rethrows(br, in);
			} catch (IOException e){
				throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
			}	
		}
		return sb.toString();
	}

	/*
	 * @see IChange#getName()
	 */
	public String getName() {
		return NLSChangesMessages.getString("deleteFile.Delete_File"); //$NON-NLS-1$
	}

	/*
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedElement() {
		return null;
	}

}

