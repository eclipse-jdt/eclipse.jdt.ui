package org.eclipse.jdt.internal.core.refactoring.changes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

public class DeleteRefactoring extends Refactoring {
	
	private List fElements;
	private boolean fDeleteProjectContents;
	private boolean fCheckIfUsed;
	
	public DeleteRefactoring(List elements){
		Assert.isNotNull(elements);
		fElements= elements;
	}
	
	public void setDeleteProjectContents(boolean delete){
		fDeleteProjectContents= delete;
	}
	
	public List getElementsToDelete(){
		return fElements;
	}
	
	public void setCheckIfUsed(boolean check){
		fCheckIfUsed= check;
	}

	/**
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			RefactoringStatus result= new RefactoringStatus();	
			result.merge(checkReadOnlyStatus());
			if (!fCheckIfUsed)
				return result;
			//XX to be implemented
			return result;
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkReadOnlyStatus() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= fElements.iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (Checks.isReadOnly(each))
				result.addError("Selected element " + ReorgUtils.getName(each) + "(or one or its sub-elements) is marked as read-only.");
		}
		return result;
	}

	/**
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			if (fElements.isEmpty())
				return RefactoringStatus.createFatalErrorStatus("");
			
			if (hasProjectsAndNonProjects())	
				return RefactoringStatus.createFatalErrorStatus("");
			
			if (! canDeleteAll())
				return RefactoringStatus.createFatalErrorStatus("");
						
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}

	/**
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			prepareElementList();
			CompositeChange composite= new CompositeChange();
			for (Iterator iter= fElements.iterator(); iter.hasNext() ;){
				composite.addChange(createDeleteChange(iter.next()));
			}
			if (composite.getChildren() == null)
				return new NullChange();
			else
				return composite;
		} finally{
			pm.done();
		}	
	}
	
	private void prepareElementList() throws JavaModelException {
		//XXX n^2 algorithm
		Collections.sort(fElements, createPathComparator());
		fElements= removeSubElements(fElements);
	}
	
	private static List removeSubElements(List list) throws JavaModelException {
		List newList= new ArrayList(list.size());
		for (Iterator iter= list.iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (!hasParentOnList(each, list))
				newList.add(each);
		}
		return newList;
	}
	
	private static boolean hasParentOnList(Object element, List list) throws JavaModelException {
		IPath elementPath= getPath(element);
		//FIX ME ???
		if (elementPath == null)
			return false;

		for (Iterator iter= list.iterator(); iter.hasNext(); ){
			Object each= iter.next();
			IPath parentPath= getPath(each);
			if (parentPath == null)
				return false;
			if (! parentPath.equals(elementPath) && parentPath.isPrefixOf(elementPath))	
				return true;
		}
		return false;
	}
	
	private static IPath getPath(Object o) throws JavaModelException {
		Object o1= o;
		if (o1 instanceof IJavaElement) 
			o1= ((IJavaElement)o1).getUnderlyingResource();

		if (o1 instanceof IResource)
			return ((IResource)o1).getFullPath();
				
		return null;
	}
	
	private Comparator createPathComparator(){
		return new Comparator(){
			public int compare(Object left, Object right) {
				return getPathLength(right) - getPathLength(left);
			}
			private int getPathLength(final Object o){
				try{
					return getPath(o).segmentCount();
				} catch (JavaModelException e){
					return 0;
				}	
			}
		};
	}
	
	private boolean deleteProjectContents(IProject project){
		return fDeleteProjectContents;
	}
	
	private IChange createDeleteChange(Object o) throws JavaModelException {
		//the sequence is important here
		
		if (o instanceof IPackageFragmentRoot)
			return createDeleteChange((IPackageFragmentRoot)o);
		
		if (o instanceof ISourceManipulation)
			return createDeleteChange((ISourceManipulation)o);
		
		if (o instanceof IJavaElement)
			return createDeleteChange(getResourceToDelete((IJavaElement)o));
		
		if (o instanceof IResource)
			return createDeleteChange((IResource)o);
		
		Assert.isTrue(false);	
		return null;	
					
	}
	
	private static IResource getResourceToDelete(IJavaElement element) throws JavaModelException {
		if (!element.exists())
			return null;
		return element.getCorrespondingResource();
	}
	
	private IChange createDeleteChange(IResource res) throws JavaModelException {
		if (res == null)
			return null;
		if (!res.exists())	
			return null;
		if (res instanceof IFile)
			return createDeleteChange((IFile)res);
		if (res instanceof IFolder)	
			return createDeleteChange((IFolder)res);
		if (res instanceof IProject)
			return createDeleteChange((IProject)res);	

		Assert.isTrue(false);	
		return null;
	}
	
	private IChange createDeleteChange(ISourceManipulation sm){
		return new DeleteSourceManipulationChange(sm);
	}
	
	private IChange createDeleteChange(IFolder folder) throws JavaModelException {
		return createDeleteChange(folder, false);
	}
	
	private IChange createDeleteChange(IFolder folder, boolean removeContentOnly) throws JavaModelException {
		if (!removeContentOnly)
			return new DeleteFolderChange(folder);
			
		IResource[] members = getMembers(folder);		
		if (members.length == 0)
			return null;
			
		CompositeChange composite= new CompositeChange("Delete resources", members.length);
		for (int i= 0; i < members.length; i++){
			composite.addChange(createDeleteChange(members[i]));
		}
		return composite;
	}

	private IChange createDeleteChange(IFile file){
		return new DeleteFileChange(file);
	}
	
	private IChange createDeleteChange(IProject project){
		return new DeleteProjectChange(project, deleteProjectContents(project));
	}
	
	private IChange createDeleteChange(IPackageFragmentRoot root) throws JavaModelException {
		CompositeChange composite= new CompositeChange("Delete package fragment root", 2);
		//FIX ME: ??
		//if (isClasspathDelete(root)) {
			composite.addChange(new DeleteFromClasspathChange(root));
		//}
		composite.addChange(createDeleteChange(getResourceToDelete(root)));
		if (composite.getChildren().length == 1)
			return composite.getChildren()[0];
		return composite;
	}
	
	private IResource[] getMembers(IFolder folder) throws JavaModelException {
		IResource[] members;
		try{
			members= folder.members();
		} catch (CoreException e){
			throw new JavaModelException(e);
		}
		return members;
	}
	
	/**
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Delete";
	}
	
	private boolean hasProjectsAndNonProjects(){
		boolean hasProject= false;
		boolean hasNonProject= false;
		for (Iterator iter= fElements.iterator(); iter.hasNext(); ){
			if (iter.next() instanceof IJavaProject)
				hasProject= true;
			else
				hasNonProject= true;	

			if (hasProject && hasNonProject)
				return true;	
		}
		return false;
	}
	
	private boolean canDeleteAll(){
		for (Iterator iter= fElements.iterator(); iter.hasNext();){
			if (! canDelete(iter.next()))
				return false;
		}
		return true;
	}
	
	private static boolean canDelete(Object o){
		try {
			if (o instanceof IPackageFragmentRoot && Checks.isClasspathDelete((IPackageFragmentRoot)o)) {
				return true;
			}
		} catch (JavaModelException e) {
			// we can't delete.
			return false;
		}
		
		if (isDefaultPackage(o))
			return false;
		
		if (o instanceof IJavaElement)	
			return canDelete((IJavaElement)o);
					
		if (o instanceof IFile)
			return canDelete((IFile)o);

		return (o instanceof IResource);
	}
	
	private static boolean isDefaultPackage(Object o){
		return (o instanceof IPackageFragment) && ((IPackageFragment)o).isDefaultPackage();
	}
	
	private static boolean canDelete(IJavaElement element){
		try {
			IResource res= element.getCorrespondingResource();
			if (res == null)
				return false;
			if (!res.getProject().equals(element.getJavaProject().getProject()))
				return false;
		} catch (JavaModelException e) {
			return false;
		}
		IJavaElement parent= element.getParent();
		return parent == null || !parent.isReadOnly();
	}
	
	private static boolean canDelete(IFile file){
		Object parent= ReorgUtils.getJavaParent(file);
		if (parent instanceof IJavaElement) {
			return !((IJavaElement)parent).isReadOnly();
		}
		return parent != null;
	}
	
}

