/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.reorg;

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
import org.eclipse.jdt.internal.core.refactoring.changes.DeleteFileChange;
import org.eclipse.jdt.internal.core.refactoring.changes.DeleteFolderChange;
import org.eclipse.jdt.internal.core.refactoring.changes.DeleteFromClasspathChange;
import org.eclipse.jdt.internal.core.refactoring.changes.DeleteProjectChange;
import org.eclipse.jdt.internal.core.refactoring.changes.DeleteSourceManipulationChange;

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

	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			if (fElements.isEmpty())
				return RefactoringStatus.createFatalErrorStatus("");
			
			if (hasProjects() && hasNonProjects())	
				return RefactoringStatus.createFatalErrorStatus("");
			
			if (! canDeleteAll())
				return RefactoringStatus.createFatalErrorStatus("");
						
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}
	
	/* non java-doc
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


	/* non java-doc
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
		if (elementPath == null)
			return false;

		for (Iterator iter= list.iterator(); iter.hasNext(); ){
			Object parent= iter.next();
			IPath parentPath= getPath(parent);
			if (parentPath == null)
				return false;
			
			//special case (2 packages are never parents of each other)
			if 	(element instanceof IPackageFragment && parent instanceof IPackageFragment)
				continue;
			
			if (! parentPath.equals(elementPath) && parentPath.isPrefixOf(elementPath))	
				return true;
		}
		return false;
	}
	
	private static IPath getPath(Object o) throws JavaModelException {
		if (o instanceof IJavaElement) {
			IJavaElement je= (IJavaElement)o;
			IResource res= je.getUnderlyingResource();
			if (res != null)
				return res.getFullPath();	
			res= je.getCorrespondingResource();
			if (res != null)
				return res.getFullPath();	
		}	

		if (o instanceof IResource)
			return ((IResource)o).getFullPath();
				
		return null;
	}
	
	private static Comparator createPathComparator(){
		return new Comparator(){
			public int compare(Object left, Object right) {
				return getPathLength(right) - getPathLength(left);
			}
			private int getPathLength(Object o){
				try{
					IPath path= getPath(o);
					if (path == null)
						return 0;
					return path.segmentCount();
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
			return new DeleteSourceManipulationChange((ISourceManipulation)o);
		
		if (o instanceof IJavaElement)
			return createDeleteChange(getResourceToDelete((IJavaElement)o));
		
		if (o instanceof IResource)
			return createDeleteChange((IResource)o);
		
		return new NullChange();				
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
			return new DeleteFileChange((IFile)res);
			
		if (res instanceof IFolder)	
			return createDeleteChange((IFolder)res, false);
			
		if (res instanceof IProject)
			return new DeleteProjectChange((IProject)res, deleteProjectContents((IProject)res));
			
		return new NullChange();
	}
	
	private IChange createDeleteChange(IFolder folder, boolean removeContentOnly) throws JavaModelException {
		if (!removeContentOnly)
			return new DeleteFolderChange(folder);
			
		IResource[] members = getMembers(folder);		
		if (members.length == 0)
			return new NullChange();
			
		CompositeChange composite= new CompositeChange("Delete resources", members.length);
		for (int i= 0; i < members.length; i++){
			composite.addChange(createDeleteChange(members[i]));
		}
		return composite;
	}

	private IChange createDeleteChange(IPackageFragmentRoot root) throws JavaModelException {
		CompositeChange composite= new CompositeChange("Delete package fragment root", 2);
		if (ReorgUtils.isClasspathDelete(root))
			composite.addChange(new DeleteFromClasspathChange(root));
		else {
			if (root.isArchive())
				composite.addChange(new DeleteFromClasspathChange(root));
			composite.addChange(createDeleteChange(getResourceToDelete(root)));
		}	
		return composite;
	}
	
	private static IResource[] getMembers(IFolder folder) throws JavaModelException {
		try{
			return folder.members();
		} catch (CoreException e){
			throw new JavaModelException(e);
		}
	}
	
	/**
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Delete";
	}
	
	private boolean hasNonProjects(){
		for (Iterator iter= fElements.iterator(); iter.hasNext(); ){
			if (! (iter.next() instanceof IJavaProject))
				return true;
		}
		return false;
	}
	
	private boolean hasProjects(){
		for (Iterator iter= fElements.iterator(); iter.hasNext(); ){
			if (iter.next() instanceof IJavaProject)
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

