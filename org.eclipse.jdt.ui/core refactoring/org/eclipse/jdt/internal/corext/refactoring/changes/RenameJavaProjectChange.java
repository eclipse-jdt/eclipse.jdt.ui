package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;


public class RenameJavaProjectChange extends AbstractJavaElementRenameChange {

	private boolean fUpdateReferences;
	
	public RenameJavaProjectChange(IJavaProject project, String newName, boolean updateReferences) throws JavaModelException {
		this(project.getCorrespondingResource().getFullPath(), project.getElementName(), newName);
		Assert.isTrue(!project.isReadOnly(), RefactoringCoreMessages.getString("RenameJavaProjectChange.assert.read_only"));  //$NON-NLS-1$
		
		fUpdateReferences= updateReferences;
	}
	
	private RenameJavaProjectChange(IPath resourcePath, String oldName, String newName) {
		super(resourcePath, oldName, newName);
	}

	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("RenameJavaProjectChange.rename", //$NON-NLS-1$
			 new String[]{getOldName(), getNewName()});
	}

	public RefactoringStatus aboutToPerform(ChangeContext context, IProgressMonitor pm) {
		RefactoringStatus result= super.aboutToPerform(context, pm);

		if (context.getUnsavedFiles().length == 0)
			return result;
			
		if (! getJavaProject().exists()) 
			return result;
			
		try {
			IPackageFragmentRoot[] roots= getJavaProject().getPackageFragmentRoots();
			if (roots.length == 0)
				return result;
			
			pm.beginTask("", roots.length); //$NON-NLS-1$
			for (int i= 0; i < roots.length; i++) {
				result.merge(checkIfModifiable(roots[i], context, new SubProgressMonitor(pm, 1)));
			}
		} catch (JavaModelException e) {
			handleJavaModelException(e, result);
		} finally{
			pm.done();
			return result;	
		}
	}
	
	/* non java-doc
	 * @see AbstractRenameChange#doRename(IProgressMonitor)
	 */
	protected void doRename(IProgressMonitor pm) throws Exception {
		try{
			pm.beginTask(getName(), 2);
			modifyClassPaths(new SubProgressMonitor(pm, 1));
			IProjectDescription description = getProject().getDescription();
			description.setName(createNewPath().segment(0));
			getProject().move(description, true, new SubProgressMonitor(pm, 1));
		} finally{
			pm.done();
		}	
	}

	/* non java-doc
	 * @see AbstractRenameChange#createUndoChange()
	 */
	protected IChange createUndoChange() throws JavaModelException {
		return new RenameJavaProjectChange(createNewPath(), getNewName(), getOldName());
	}

	private IProject getProject() {
		return getJavaProject().getProject();
	}

	private IJavaProject getJavaProject() {
		return  (IJavaProject)getModifiedLanguageElement();
	}
	
	private void modifyClassPaths(IProgressMonitor pm) throws JavaModelException{
		IProject[] referencing=getReferencingProjects();
		pm.beginTask(RefactoringCoreMessages.getString("RenameJavaProjectChange.update"), referencing.length);	 //$NON-NLS-1$
		for (int i= 0; i < referencing.length; i++) {
			IJavaProject jp= JavaCore.create(referencing[i]);
			if (jp != null){
				modifyClassPath(jp, new SubProgressMonitor(pm, 1));
			}	else{
				pm.worked(1);
			}	
		}
		pm.done();		
	}
	
	private void modifyClassPath(IJavaProject referencingProject, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1); //$NON-NLS-1$
		IClasspathEntry[] oldEntries= referencingProject.getRawClasspath();
		IClasspathEntry[] newEntries= new IClasspathEntry[oldEntries.length];
		for (int i= 0; i < newEntries.length; i++) {
			if (isOurEntry(oldEntries[i]))
				newEntries[i]= createModifiedEntry(oldEntries[i]);
			else
				newEntries[i]= oldEntries[i];	
		}
		referencingProject.setRawClasspath(newEntries, pm);
		pm.done();
	}
	
	private boolean isOurEntry(IClasspathEntry cpe){
		if (cpe.getEntryKind() != IClasspathEntry.CPE_PROJECT)
			return false;
		if (! cpe.getPath().equals(getResourcePath()))
			return false;
		return true;	
	}
	
	private IClasspathEntry createModifiedEntry(IClasspathEntry cpe){
		return JavaCore.newProjectEntry(createNewPath());
	}
	
	private IProject[] getReferencingProjects() {
		return  getProject().getReferencingProjects();
	}
	
	private IPath createNewPath(){
		return getResourcePath().removeLastSegments(1).append(getNewName());
	}
}