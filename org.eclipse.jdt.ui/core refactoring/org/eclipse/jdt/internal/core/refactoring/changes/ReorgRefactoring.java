package org.eclipse.jdt.internal.core.refactoring.changes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
	private Map fRenamings;
	
	ReorgRefactoring(List elements){
		fElements= elements;
		Assert.isNotNull(elements);
	}
	
	public List getElementsToReorg(){
		return Collections.unmodifiableList(getElements());
	}
	
	IPackageFragment getDestinationForCusAndFiles(Object dest) throws JavaModelException{
		IPackageFragment result= ReorgUtils.getDestinationAsPackageFragement(dest);
		
		if (result != null && !result.isReadOnly())
			return result;

		return null;	
	}

	List getElements() {
		return fElements;
	}
	
	public void setRenamings(Map map){
		fRenamings= map;
	}
	
	Object getDestination() {
		return fDestination;
	}
	
	public void setDestination(Object destination) throws JavaModelException{
		if (destination instanceof IJavaProject) {
			IJavaProject p= (IJavaProject)destination;
			if (ReorgUtils.isPackageFragmentRoot(p)) 
				fDestination= ReorgUtils.getPackageFragmentRoot(p);
		}
		fDestination= destination;	
	}
	
	/**
	 * returns a Object -> Boolean map
	 * keys are elements that have to be renamed or replaced
	 * values: true means that the element  can be replaced
	 * false means otherwise
	 */
	public Map getElementsToRename() throws JavaModelException{
		Map result= new HashMap(getElements().size());
		for (Iterator iter= getElements().iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (isValidNewName(each, getElementName(each)) != null)
				result.put(each, new Boolean(canReplace(each, ReorgUtils.getName(each))));
		}
		return result;
	}
	
	private static boolean containsDuplicate(List list, Object value){
		return (list.indexOf(value) != list.lastIndexOf(value));
	}
	
	/**
	 * returns a Map (Object -> String) with error messages
	 * or an empty map if ok
	 */ 
	public Map checkRenamings(Map map) throws JavaModelException{
		Map result= new HashMap(map.size());
		List valueList= Arrays.asList(map.values().toArray());
		
		//dups
		for (Iterator iter= map.keySet().iterator(); iter.hasNext() ;){
			Object o= iter.next();
			Object value= map.get(o);
			if (value != null && containsDuplicate(valueList, value))
				result.put(o, "Duplicate name: " + (String)value);
		}
		
		//illegal names
		for (Iterator iter= map.keySet().iterator(); iter.hasNext() ;){
			Object o= iter.next();
			if (!result.containsKey(o)){
				String msg= isValidNewName(o, (String)map.get(o));
				if (msg != null)
					result.put(o, msg);
			}
		}
		return result;
	}
	
	//----------
	//XXX nasty
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
		
		//only resources are selected
		if (hasResources())	
			return (ancestor instanceof IContainer);
				
		return false;	
	}
	
	//-------
	
	/**
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public final RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			if (getElements().isEmpty())
				return RefactoringStatus.createFatalErrorStatus("");
			
			if (ReorgUtils.hasParentCollision(getElements()))
				return RefactoringStatus.createFatalErrorStatus("");
			
			if (hasCusOrFiles() && hasNonCusOrFiles())
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
	
	
	/**
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public final IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", getElements().size());
		try{
			if (fElements.size() == 1)
				return createChange(fElements.get(0));
			CompositeChange composite= new CompositeChange("reorganize elements", fElements.size());
			for (Iterator iter= getElements().iterator(); iter.hasNext();){
				composite.addChange(createChange(iter.next()));
			}
			return composite;
		} finally{
			pm.done();
		}
	}
	
	private IChange createChange(Object o) throws JavaModelException{
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
	public abstract boolean isValidDestination(Object dest) throws JavaModelException;
	//-------
	String getNewName(Object o){
		if (fRenamings == null)
			return null;
		if (fRenamings.containsKey(o))
			return (String)fRenamings.get(o);
		return null;	
	}
	
	public static String getElementName(Object o) {
		if (o instanceof IJavaElement)
			return ((IJavaElement)o).getElementName();
		if (o instanceof IResource) {
			return ((IResource)o).getName();
		}
		return o.toString();
	}
	
	
	///
	private boolean canReplace(Object o, String newName) throws JavaModelException{
		if (o instanceof IPackageFragment)
			return canReplace((IPackageFragment)o, newName);
		if (o instanceof ICompilationUnit)
			return canReplace((ICompilationUnit)o, newName);	
		if (o instanceof IResource)
			return canReplace((IResource)o, newName);	
		Assert.isTrue(false);
		return false;
	}
	
	private boolean canReplace(IPackageFragment original, String newName) throws JavaModelException{
		IPackageFragmentRoot root= getDestinationForPackages(fDestination);
		if (original.equals(root.getPackageFragment(newName)))
			return false;
		return true;
	}

	private boolean canReplace(ICompilationUnit original, String newName) throws JavaModelException{
		IPackageFragment fragment= getDestinationForCusAndFiles(fDestination);
		Object res= ReorgUtils.getResource(fragment, newName);
		if (original.equals(res))
			return false;
		ICompilationUnit cu= fragment.getCompilationUnit(newName);
		if (original.equals(cu))
			return false;
		return true;	
	}

	private boolean canReplace(IResource original, String newName){
		return false;
	}
	
	//
	static IPackageFragmentRoot getDestinationForPackages(Object dest) throws JavaModelException {
		IPackageFragmentRoot result= null;
		if (dest instanceof IPackageFragmentRoot) 
			result= (IPackageFragmentRoot)dest;
		
		if (dest instanceof IJavaProject)
			result= ReorgUtils.getDestinationAsPackageFragmentRoot((IJavaProject)dest);
		
		if (result != null && !result.isReadOnly())
			return result;
			
		return null;
	}
	
	static IContainer getDestinationForResources(Object dest) throws JavaModelException{
		if (dest instanceof IJavaElement) 
			dest= ((IJavaElement)dest).getCorrespondingResource();

		if (dest instanceof IContainer)
			return (IContainer)dest;
			
		return null;		
	}
	
	//-------
	boolean hasCusOrFiles(){
		for (Iterator iter= getElements().iterator(); iter.hasNext();){
			Object each= iter.next();
			if (each instanceof ICompilationUnit || each instanceof IFile)
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
		return getDestinationForCusAndFiles(dest) != null;
	}
	
	boolean canCopyResources(Object dest) throws JavaModelException{
		return getDestinationForResources(dest) != null;
	}
	
	boolean canCopyPackages(Object dest) throws JavaModelException{
		return getDestinationForPackages(dest) != null;
	}
	
	//---
	public String isValidNewName(Object o, String newName) throws JavaModelException{
		//XXX ???
		if (newName == null)
			return null;
		
		if (o instanceof ICompilationUnit)
			return isValidNewName(getDestinationForCusAndFiles(getDestination()), newName);
		
		if (o instanceof IPackageFragment)
			return isValidNewName(getDestinationForPackages(getDestination()), newName);
		
		if (o instanceof IResource)
			return isValidNewName(getDestinationForResources(getDestination()), newName);

		Assert.isTrue(false, "not expected to get here");	
		return "";
	}
	
	private String isValidNewName(IContainer c, String name){
		if (c == null)
			return null;
						
		if (c.findMember(name) != null)
			return "Resource " + name + " already exists in " + c.getName();
			
		if (!c.getFullPath().isValidSegment(name))
			return "Invalid resource name";
		return null;
		
	}
	
	private String isValidNewName(IPackageFragmentRoot root, String name) throws JavaModelException{
		if (root == null)
			return null;
			
		// the order is important here since getPackageFragment() throws an exception
		// if the name is invalid.
		IStatus status= JavaConventions.validatePackageName(name);
		if (! status.isOK())
			return status.getMessage();
			
		IPackageFragment pkg= root.getPackageFragment(name);
		if (pkg.exists() && pkg.hasChildren())
			return "Package " + name + " already exists in " + root.getElementName();
		
		return null;
		
	}
	private String isValidNewName(IPackageFragment pkg, String name) throws JavaModelException{
		if (pkg == null)
			return null;
			
		// the order is important here since getCompilationUnit() throws an exception
		// if the name is invalid.
		IStatus status= JavaConventions.validateCompilationUnitName(name);
		if (! status.isOK())
			return status.getMessage();
			
		if (pkg.getCompilationUnit(name).exists() || ReorgUtils.getResource(pkg, name) != null)
			return "Compilation unit " + name + " already exists in " + pkg.getElementName();
		return null;
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
		if (element instanceof IFile) {
			Object parent= ReorgUtils.getJavaParent(element);
			if (parent instanceof IJavaElement) {
				return !((IJavaElement)parent).isReadOnly();
			}
			return parent != null;
		};
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
	
}

