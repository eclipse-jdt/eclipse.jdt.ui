/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.nls.changes;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSUtil;

public class CreateTextFileChange extends CreateFileChange {
	
	private final boolean fIsJavaFile;
	
	public CreateTextFileChange(IPath path, String source, boolean isJava){
		super(path, source);
		fIsJavaFile= isJava;
	}

	public CreateTextFileChange(IPath path, String source, String encoding, boolean isJava){
		super(path, source, encoding);
		fIsJavaFile= isJava;
	}
	
	public boolean isJavaFile(){
		return fIsJavaFile;
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
	
	public String getPreview() throws JavaModelException {
		return getSource();
	}
}

