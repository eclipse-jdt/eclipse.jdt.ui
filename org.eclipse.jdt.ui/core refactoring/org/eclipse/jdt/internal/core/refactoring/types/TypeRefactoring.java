/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.types;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.RenameResourceChange;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

/*
 * non java-doc
 * not API
 */
abstract class TypeRefactoring extends Refactoring{

	private IType fType;
		
	public TypeRefactoring(IJavaSearchScope scope, IType type){
		super(scope);
		Assert.isNotNull(type);
		Assert.isTrue(type.exists(), RefactoringCoreMessages.getString("TypeRefactoring.assert.must_exist")); //$NON-NLS-1$
		fType= type;
	}
	
	public TypeRefactoring(IType type) {
		super();
		fType= type;
	}	
	
	public final IType getType(){
		return fType;
	}
	
	protected static boolean typeNameExistsInPackage(IPackageFragment pack, String name) throws JavaModelException{
		Assert.isTrue(pack.exists(), RefactoringCoreMessages.getString("TypeRefactoring.assert.must_exist_1")); //$NON-NLS-1$
		Assert.isTrue(!pack.isReadOnly(), RefactoringCoreMessages.getString("TypeRefactoring.assert.read-only")); //$NON-NLS-1$
		/*
		 * ICompilationUnit.getType expects simple name
		 */  
		if (name.indexOf(".") != -1) //$NON-NLS-1$
			name= name.substring(0, name.indexOf(".")); //$NON-NLS-1$
		ICompilationUnit[] cus= pack.getCompilationUnits();
		for (int i= 0; i < cus.length; i++){
			if (cus[i].getType(name).exists())
				return true;
		}
		return false;
	}
}
