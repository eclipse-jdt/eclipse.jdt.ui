/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.nls.changes;
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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

public class DeleteFileChange extends Change {

	private IPath fPath;
	private String fSource;
	private IChange fUndoChange;
	
	public DeleteFileChange(IFile file){
		Assert.isNotNull(file, "file"); //$NON-NLS-1$
		fPath= file.getFullPath().removeFirstSegments(ResourcesPlugin.getWorkspace().getRoot().getFullPath().segmentCount());
	}
	
	/*
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		try {
			if (!isActive())
				return;
			pm.beginTask(Messages.getString("deleteFile.deleting_resource"), 1); //$NON-NLS-1$
			IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
			Assert.isNotNull(file);
			Assert.isTrue(file.exists());
			Assert.isTrue(!file.isReadOnly());
			fSource= getSource(file);
			file.delete(true, false, pm);
		} catch (Exception e) {
			handleException(context, e);
			setActive(false);
		} finally {
			pm.done();
		}
	}
	
	private String getSource(IFile file) throws CoreException{
		InputStream in= file.getContents();
		BufferedReader br= new BufferedReader(new InputStreamReader(in));
		
		StringBuffer sb= new StringBuffer();
		try {
			int read= 0;
			while ((read= br.read()) != -1)
				sb.append((char) read);
		} catch (IOException e){
			//		
		} finally {
			try{
				br.close();	
			} catch (IOException e1){
				throw new JavaModelException(e1, IJavaModelStatusConstants.IO_EXCEPTION);
			}	
		}
		return sb.toString();
	}

	/*
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		if (!isActive())
			return new NullChange();
		else	
			return new CreateFileChange(fPath, fSource);
	}

	/*
	 * @see IChange#getName()
	 */
	public String getName() {
		return Messages.getString("deleteFile.Delete_File"); //$NON-NLS-1$
	}

	/*
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return null;
	}

}

