package org.eclipse.jdt.internal.core.refactoring.changes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

public class DeleteFileChange extends DeleteChange {

	private IPath fPath;
	
	public DeleteFileChange(IFile file){
		Assert.isNotNull(file, "file"); 
		fPath= ReorgUtils.getResourcePath(file);
	}
	
	private IFile getFile(){
		return ReorgUtils.getFile(fPath);
	}
	
	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Delete file " + fPath.lastSegment(); 
	}

	/**
	 * @see IChange#getCorrespondingJavaElement()
	 */
	public IJavaElement getCorrespondingJavaElement() {
		return JavaCore.create(getFile());
	}

	/**
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	protected void doDelete(IProgressMonitor pm) throws CoreException{
		IFile file= getFile();
		Assert.isNotNull(file);
		Assert.isTrue(file.exists());
		file.delete(true, false, pm);
	}
}

