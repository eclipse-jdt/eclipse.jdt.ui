package org.eclipse.jdt.internal.core.refactoring.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

class AddToClasspathChange extends Change {
	
	private String fProjectHandle;
	private int fEntryKind;
	private int fContentKind;
	private IPath fPath;
	private IPath fSourceAttachmentPath;
	private IPath fSourceAttachmentRootPath;
	
	AddToClasspathChange(IJavaProject project, int entryKind, int contentKind, IPath path, IPath sourceAttachmentPath, IPath sourceAttachmentRootPath){
		fProjectHandle= project.getHandleIdentifier();
		fEntryKind= entryKind;
		fContentKind= contentKind;
		fPath=path;
		fSourceAttachmentPath= sourceAttachmentPath;
		fSourceAttachmentRootPath= sourceAttachmentRootPath;
	}
	
	/**
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		pm.beginTask(getName(), 1);
		try{
			if (!isActive())
				return;
			getJavaProject().setRawClasspath(getNewClasspathEntries(), new SubProgressMonitor(pm, 1));
		}finally{
			pm.done();
		}		
	}
	
	private IClasspathEntry[] getNewClasspathEntries() throws JavaModelException{
		IClasspathEntry[] entries= getJavaProject().getRawClasspath();
		List cp= new ArrayList(entries.length + 1);
		cp.addAll(Arrays.asList(entries));
		cp.add(createNewClasspathEntry());
		return (IClasspathEntry[])cp.toArray(new IClasspathEntry[cp.size()]);
	}
	
	private IClasspathEntry createNewClasspathEntry(){
		switch(fEntryKind){
			case IClasspathEntry.CPE_LIBRARY:
				return JavaCore.newLibraryEntry(fPath, fSourceAttachmentPath, fSourceAttachmentRootPath);
			case IClasspathEntry.CPE_PROJECT:
				return JavaCore.newProjectEntry(fPath);
			case IClasspathEntry.CPE_SOURCE:
				return JavaCore.newSourceEntry(fPath);
			case IClasspathEntry.CPE_VARIABLE:
				return JavaCore.newVariableEntry(fPath, fSourceAttachmentPath, fSourceAttachmentRootPath);	
		}
		Assert.isTrue(false, "not expected: " + fEntryKind);
		return null;
	}
	
	private IJavaProject getJavaProject(){
		return (IJavaProject)JavaCore.create(fProjectHandle);
	}

	/**
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		if (!isActive())
			return new NullChange();

		IPath classpathEntryPath= JavaCore.getResolvedClasspathEntry(createNewClasspathEntry()).getPath();
		return new DeleteFromClasspathChange(classpathEntryPath, getJavaProject());
	}

	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Add entry to classpath";
	}

	/**
	 * @see IChange#getCorrespondingJavaElement()
	 */
	public IJavaElement getCorrespondingJavaElement() {
		return getJavaProject();
	}
}

