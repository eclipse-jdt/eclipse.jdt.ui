/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.reorg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

public abstract class ReorgRefactoring extends Refactoring {
	
	private final List fElements;
	private Object fDestination;
	private Set fExcludedElements;
	private static final String DEFAULT_PACKAGE= ""; //$NON-NLS-1$
	
	ReorgRefactoring(List elements){
		fElements= elements;
		Assert.isNotNull(elements);
		fExcludedElements= new HashSet(0);
	}
	
	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public final RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			if (getElements().isEmpty())
				return RefactoringStatus.createFatalErrorStatus("");
			
			if (hasParentCollision(getElements()))
				return RefactoringStatus.createFatalErrorStatus("");
			
			if (hasNonCusOrFiles() && (hasCus() || hasFiles()))
				return RefactoringStatus.createFatalErrorStatus("");
			
			if (hasPackages() && hasNonPackages())
				return RefactoringStatus.createFatalErrorStatus("");
				
			if (! canReorgAll())
				return RefactoringStatus.createFatalErrorStatus("");
						
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}
	
	public List getElementsToReorg(){
		return Collections.unmodifiableList(fElements);
	}
		
	public void setExcludedElements(Set excluded){
		Assert.isNotNull(excluded);
		Assert.isTrue(fElements.containsAll(excluded));
		fExcludedElements= excluded;
	}
	
	public void setDestination(Object destination) throws JavaModelException{
		if (destination instanceof IJavaProject) {
			IJavaProject p= (IJavaProject)destination;
			if (isPackageFragmentRoot(p)) 
				fDestination= getPackageFragmentRoot(p);
		}
		fDestination= destination;	
	}
	
	public Set getElementsThatExistInTarget() throws JavaModelException{
		Set result= new HashSet();
		for (Iterator iter= getElements().iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (causesNameConflict(each))
				result.add(each);
		}
		return result;		
	}
	
	public boolean canBeAncestor(Object ancestor){
		if (ancestor instanceof IJavaModel)
			return true;
		
		if (ancestor instanceof IJavaProject)
			return true;
			
		if (ancestor instanceof IPackageFragmentRoot)
			return !((IPackageFragmentRoot)ancestor).isReadOnly();
		
		//only packages are selected
		if (hasPackages())
			return false;
		
		if (ancestor instanceof IPackageFragment)
			return !((IPackageFragment)ancestor).isReadOnly();
		
		return (ancestor instanceof IContainer);
	}

	abstract boolean isValidDestinationForCusAndFiles(Object dest) throws JavaModelException;
	public boolean isValidDestination(Object dest) throws JavaModelException{
		if (dest instanceof IJavaProject){
			IJavaProject jp= (IJavaProject)dest;
			IPackageFragmentRoot root=getPackageFragmentRoot(jp); 
			if (root != null)
				return isValidDestination(root);
			else
				return isValidDestination(jp.getUnderlyingResource());	
		}	
		
		//only packages are selected
		if (hasPackages())	
			return canCopyPackages(dest);
		
		//only resources are selected
		if (hasResources() && ! hasNonResources())	
			return canCopyResources(dest);
			
		//return canMoveCusAndFiles(dest);
		return isValidDestinationForCusAndFiles(dest);
	}

	Object getDestination() {
		return fDestination;
	}
	
	List getElements() {
		return fElements;
	}
	
	//--- chnages
	
	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", fElements.size());
		try{
			CompositeChange composite= new CompositeChange("reorganize elements", fElements.size());
			for (Iterator iter= fElements.iterator(); iter.hasNext();){
				composite.addChange(createChange(iter.next()));
			}
			return composite;
		} finally{
			pm.done();
		}
	}
	
	private IChange createChange(Object o) throws JavaModelException{
		if (fExcludedElements.contains(o))
			return null;
		
		if (o instanceof IPackageFragment)
			return createChange((IPackageFragment)o);
		
		if (o instanceof ICompilationUnit)	
			return createChange((ICompilationUnit)o);
		
		if (o instanceof IResource)
			return createChange((IResource)o);
			
		Assert.isTrue(false, "not expected to get here");	
		return null;	
	}
	
	abstract IChange createChange(IPackageFragment pack) throws JavaModelException;
	abstract IChange createChange(ICompilationUnit cu) throws JavaModelException;
	abstract IChange createChange(IResource res) throws JavaModelException;
	
	/** returns IPackageFragment or IContainer
	 */
	static Object getDestinationForCusAndFiles(Object dest) throws JavaModelException{
		IPackageFragment result= getDestinationAsPackageFragment(dest);
		if (result != null){
			if  (result.isReadOnly())
				return null;	
			return result;	
		}	
		
		return getDestinationForResources(dest);	
	}
	
	static IPackageFragmentRoot getDestinationForPackages(Object dest) throws JavaModelException {
		IPackageFragmentRoot result= null;
		if (dest instanceof IPackageFragmentRoot) 
			result= (IPackageFragmentRoot)dest;
		else if (dest instanceof IJavaProject)
			result= getDestinationAsPackageFragmentRoot((IJavaProject)dest);
		
		if (result == null || result.isReadOnly())
			return null;
		return result;			
	}
	
	static IContainer getDestinationForResources(Object dest) throws JavaModelException{
		if (dest instanceof IJavaElement) 
			return getDestinationForResources(((IJavaElement)dest).getCorrespondingResource());

		if (dest instanceof IResource)
			return getDestinationForResources((IResource)dest);
			
		return null;		
	}
	
	private static IContainer getDestinationForResources(IResource dest){
		if (dest instanceof IContainer)
			return (IContainer)dest;
		return null;
	}
	
	//-------
	boolean hasCus(){
		for (Iterator iter= getElements().iterator(); iter.hasNext();){
			Object each= iter.next();
			if (each instanceof ICompilationUnit)
				return true;
		}
		return false;
	}
	
	boolean hasFiles(){
		for (Iterator iter= getElements().iterator(); iter.hasNext();){
			Object each= iter.next();
			if (each instanceof IFile)
				return true;
		}
		return false;
	}
	
	boolean hasPackages(){
		for (Iterator iter= getElements().iterator(); iter.hasNext();){
			Object each= iter.next();
			if (each instanceof IPackageFragment)
				return true;
		}
		return false;
	}
	
	boolean hasNonPackages(){
		for (Iterator iter= getElements().iterator(); iter.hasNext();){
			Object each= iter.next();
			if (! (each instanceof IPackageFragment))
				return true;
		}
		return false;
	}

	boolean hasResources(){
		for (Iterator iter= getElements().iterator(); iter.hasNext();){
			Object each= iter.next();
			if (each instanceof IResource)
				return true;
		}
		return false;
	}
	
	boolean hasNonResources(){
		for (Iterator iter= getElements().iterator(); iter.hasNext();){
			Object each= iter.next();
			if (! (each instanceof IResource))
				return true;
		}
		return false;
	}	
	
	boolean hasNonCusOrFiles(){
		for (Iterator iter= getElements().iterator(); iter.hasNext();){
			Object each= iter.next();
			if (! (each instanceof ICompilationUnit) && ! (each instanceof IFile))
				return true;
		}
		return false;
	}
	
	//-------
	//put here because they're used by both copy and move
	boolean canCopyCusAndFiles(Object dest) throws JavaModelException{
		Object destination= getDestinationForCusAndFiles(dest);
		if (destination instanceof IPackageFragment){
			if (destinationIsParent(getElements(), (IPackageFragment)destination))
				return false;
		}		
			
		return destination != null;
	}
	
	boolean canCopyResources(Object dest) throws JavaModelException{
		if (destinationIsParentForResources(getDestinationForResources(dest)))
			return false;
		
		return getDestinationForResources(dest) != null;
	}
	
	boolean canCopyPackages(Object dest) throws JavaModelException{
		if (destinationIsParent(getElements(), getDestinationForPackages(dest)))
			return false;
		
		return getDestinationForPackages(dest) != null;
	}
	
	//---
	/**
	 * Checks if <code>dest</code> isn't the parent of one of the elements given by the 
	 * list <code>elements</code>.
	 */
	private static boolean destinationIsParent(List elements, IJavaElement dest) throws JavaModelException{
		return destinationIsParent(elements, dest.getCorrespondingResource());
	}
	
	/**
	 * Checks if <code>dest</code> isn't the parent of one of the elements given by the 
	 * list <code>elements</code>.
	 */
	private static boolean destinationIsParent(List elements, IResource parent) {
		if (parent == null)
			return false;
			
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			IResource resource= convertToResource(iter.next());
			if (resource == null)
				return false;
			if (parent.equals(resource.getParent()))
				return true;	
		}
		return false;
	}
		
	/**
	 * Checks if <code>parent</code> isn't the parent of one of the elements given by the 
	 * list <code>elements</code>.
	 */
	private static boolean destinationIsParent(List elements, IPackageFragmentRoot parent) {
		if (parent == null)
			return false;
		for (Iterator iter= elements.iterator(); iter.hasNext(); ) {
			IPackageFragment pkg= (IPackageFragment)iter.next();
			if (parent.equals(pkg.getParent()))
				return true;
		}
		return false;
	}
	
	private boolean destinationIsParentForResources(IContainer dest){
		if (dest == null)
			return false;
		for (Iterator iter= getElements().iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (!(each instanceof IResource))
				continue;
				
			IResource resource= (IResource)each;
			if (dest.equals(resource.getParent()))
				return true;
		}
		return false;
	}
	
	
	/**
	 * Tries to convert the given object into an <code>IResource</code>. If the 
	 * object can't be converted into an <code>IResource</code> <code>null</code> 
	 * is returned.
	 */
	private static IResource convertToResource(Object o) {
		if (o instanceof IResource)
			return (IResource)o;
		if (o instanceof IAdaptable)
			return (IResource)((IAdaptable)o).getAdapter(IResource.class);
			
		return null;	
	}	
	//---
	
	private boolean causesNameConflict(Object o) throws JavaModelException{
		String newName= ReorgUtils.getName(o);
		
		if (o instanceof ICompilationUnit){
			Object dest= getDestinationForCusAndFiles(getDestination());
			if (dest instanceof IPackageFragment)
				return causesNameConflict((IPackageFragment)dest, newName);
			if (dest instanceof IContainer)	
				return causesNameConflict((IContainer)dest, newName);
			return true;	
		}	
		
		if (o instanceof IPackageFragment)
			return causesNameConflict(getDestinationForPackages(getDestination()), newName);
		
		if (o instanceof IResource)
			return causesNameConflict(getDestinationForResources(getDestination()), newName);

		Assert.isTrue(false, "not expected to get here");	
		return true;
	}
	
	private boolean causesNameConflict(IContainer c, String name){
		if (c == null)
			return false;
						
		if (c.findMember(name) != null)
			return true;
			
		return (!c.getFullPath().isValidSegment(name));
	}
	
	private boolean causesNameConflict(IPackageFragmentRoot root, String name) throws JavaModelException{
		if (root == null)
			return false;
			
		IPackageFragment pkg= root.getPackageFragment(name);
		return (pkg.exists() && pkg.hasChildren());
	}
	
	private boolean causesNameConflict(IPackageFragment pkg, String name) throws JavaModelException{
		if (pkg == null)
			return false;
			
		// the order is important here since getCompilationUnit() throws an exception
		// if the name is invalid.
		IStatus status= JavaConventions.validateCompilationUnitName(name);
		if (! status.isOK())
			return true;
			
		return (pkg.getCompilationUnit(name).exists() || getResource(pkg, name) != null);		
	}
		
	//
	private boolean canReorgAll(){
		for (Iterator iter= getElements().iterator(); iter.hasNext();){
			if (! canReorg(iter.next()))
				return false;
		}
		return true;
	}
	
	private static boolean canReorg(Object o){
		if (o instanceof IPackageFragment)
			return canReorg((IPackageFragment)o);
			
		if (o instanceof IResource)
			return canReorg((IResource)o);	
		
		if (o instanceof ICompilationUnit)
			return canReorg((ICompilationUnit)o);	
			
		return false;	
	}
	
	private static boolean canReorg(IPackageFragment pkg){
		if (pkg.isDefaultPackage())
			return false;
		try {
			IResource res= pkg.getUnderlyingResource();
			return  res != null && res.equals(pkg.getCorrespondingResource());
		} catch (JavaModelException e) {
			return false;
		}
	}
	
	private static boolean canReorg(IResource element){
		if (element instanceof IFolder)
			return true;
		
		//FIX ME???? read-only??
		if (! (element instanceof IFile))
			return false;
			
		Object parent= ReorgUtils.getJavaParent(element);
		if (parent == null)
			return false;
			
		if (parent instanceof IJavaElement) 
			return !((IJavaElement)parent).isReadOnly();
		return false;
	}
	
	private static boolean canReorg(ICompilationUnit cu){
		try {
			IResource res= cu.getUnderlyingResource();
			return res != null && res.equals(cu.getCorrespondingResource());
		} catch (JavaModelException e) {
			return false;
		}
	}
	
	//---
	/**
	 * Returns the actual destination for the given <code>dest</code> if the
	 * elements to be dropped are files or compilation units.
	 */
	private static IPackageFragment getDestinationAsPackageFragment(Object dest) throws JavaModelException {
		if (dest instanceof IPackageFragment)
			return (IPackageFragment)dest;
		
		if (dest instanceof IJavaProject)
			return getDestinationAsPackageFragment(getDestinationAsPackageFragmentRoot((IJavaProject)dest));
			
		if (dest instanceof IPackageFragmentRoot)
			return ((IPackageFragmentRoot)dest).getPackageFragment(DEFAULT_PACKAGE);
		
		return null;
	}	
	
	/**
	 * Returns the package fragment root to be used as a destination for the
	 * given project. If the project has more than one package fragment root
	 * that isn't an archive <code>null</code> is returned.
	 */
	private static IPackageFragmentRoot getDestinationAsPackageFragmentRoot(IJavaProject project) throws JavaModelException {
		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		IPackageFragmentRoot result= null;
		for (int i= 0; i < roots.length; i++) {
			if (! roots[i].isArchive()) {
				if (roots[i].getUnderlyingResource() instanceof IProject)
					return roots[i];	
				return null;
			}
		}
		return null;
	}
	
	static final IPackageFragmentRoot getPackageFragmentRoot(IJavaProject p) throws JavaModelException {
		IPackageFragmentRoot[] roots= p.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if (isProjectPackageFragmentRoot(roots[i]))
				return roots[i];
		}
		return null;
	}
	
	private static final boolean isProjectPackageFragmentRoot(IPackageFragmentRoot root) throws JavaModelException {
		return root.getUnderlyingResource() instanceof IProject;
	}
	
	private static final boolean isPackageFragmentRoot(IJavaProject p) throws JavaModelException {
		return getPackageFragmentRoot(p) != null;
	}
	
	private static boolean hasParentCollision(List elements) {
		int size= elements.size();
		List paths= new ArrayList(size);
		for (int i= 0; i < size; i++) {
			paths.add(getPath(elements.get(i)));
		}
		for (int i= 0; i < size; i++) {
			for (int j= 0; j < size; j++) {
				if (i != j) {
					IPath left= (IPath)paths.get(i);
					IPath right= (IPath)paths.get(j);
					if (left.isPrefixOf(right))
						return true;
				}
			}
		}
		return false;
	}
	
	private static IPath getPath(Object element) {
		String name= ReorgUtils.getName(element);
		if (name == null)
			return new Path(""); //$NON-NLS-1$
			
		Object parent= ReorgUtils.getJavaParent(element);
		if (parent == null) 
			return new Path(name);
		return getPath(parent).append(name);
	}
	
	static Object getResource(IPackageFragment fragment, String name) throws JavaModelException {
		Object[] children= fragment.getNonJavaResources();
		for (int i= 0; i < children.length; i++) {
			if (children[i] instanceof IResource) {
				IResource child= (IResource)children[i];
				if (child.getName().equals(name))
					return children[i];
			} else if (children[i] instanceof IStorage) {
				IStorage child= (IStorage)children[i];
				if (child.getName().equals(name))
					return children[i];
			}
		}
		return null;
	}
}

