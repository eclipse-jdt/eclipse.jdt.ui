/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.nls.changes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

public class CreateFileChange extends Change {

	private IPath fPath;
	private String fSource;
	private IChange fUndoChange;
	private String fName;
	
	public CreateFileChange(IPath path, String source){
		Assert.isNotNull(path, "path"); //$NON-NLS-1$
		Assert.isNotNull(source, "source"); //$NON-NLS-1$
		fPath= path;
		fSource= source;
	}
	
	/*
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public void perform(ChangeContext context, IProgressMonitor pm)	throws JavaModelException, ChangeAbortException {

		//FIXME must ask DB to do the exception hadnling dance correctly
		
		InputStream is= null;
		try {
			pm.beginTask(Messages.getString("createFile.creating_resource"), 1); //$NON-NLS-1$

			if (!isActive()){
				fUndoChange= new NullChange();	
			} else{
				ResourcesPlugin.getWorkspace();
				is= getInputStream();
				IFile file= getOldFile();
				if (file.exists()){
					CompositeChange composite= new CompositeChange();
					composite.addChange(new DeleteFileChange(file));
					composite.addChange(new CreateFileChange(fPath, fSource));
					composite.perform(context, pm);
					fUndoChange= composite.getUndoChange();
				} else {
					file.create(is, false, pm);
					fUndoChange= new DeleteFileChange(file);
				}				
			}	
		} catch (Exception e) {
			handleException(context, e);
			fUndoChange= new NullChange();
			setActive(false);
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
	
	protected IFile getOldFile(){
		return ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
	}

	private InputStream getInputStream(){
		return new ByteArrayInputStream(fSource.getBytes());
	}
	
	/*
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		return fUndoChange;
	}

	/*
	 * @see IChange#getName()
	 */
	public String getName() {
		if (fName == null)
			return Messages.getString("createFile.Create_file") + fPath.toString(); //$NON-NLS-1$
		else 
			return fName;
	}

	/*
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return null;
	}

	/*
	 * Gets the source
	 * @return Returns a String
	 */
	protected String getSource() {
		return fSource;
	}

	/*
	 * Sets the name
	 * @param name The name to set
	 */
	public void setName(String name) {
		fName = name;
	}
}

