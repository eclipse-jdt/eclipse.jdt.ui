/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.types;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.RenameResourceChange;
import org.eclipse.jdt.internal.core.util.HackFinder;

/*
 * non java-doc
 * not API
 */
abstract class TypeRefactoring extends Refactoring{

	private IType fType;
		
	public TypeRefactoring(IJavaSearchScope scope, IType type){
		super(scope);
		Assert.isNotNull(type);
		Assert.isTrue(type.exists(), "type must exist");
		fType= type;
	}
	
	public TypeRefactoring(IType type) {
		super();
		fType= type;
	}	
	
	public final IType getType(){
		return fType;
	}
		
	protected static IPath getNewFilePath(IType type, String newName) throws JavaModelException{
		  return RenameResourceChange.renamedResourcePath(getResource(type).getFullPath(), newName);
	}
	
	protected static boolean resourceExists(IPath resourcePath){
		return ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath) != null;
	}
	
	protected static boolean typeNameExistsInPackage(IPackageFragment pack, String name) throws JavaModelException{
		Assert.isTrue(pack.exists(), "package must exist");
		Assert.isTrue(!pack.isReadOnly(), "package must not be read-only");
		HackFinder.fixMeSoon("ICompilationUnit feature - assert fails if arg for getType has '.'");
		if (name.indexOf(".") != -1)
			name= name.substring(0, name.indexOf("."));
		ICompilationUnit[] cus= pack.getCompilationUnits();
		for (int i= 0; i < cus.length; i++){
			if (cus[i].getType(name).exists())
				return true;
		}
		return false;
	}
}
