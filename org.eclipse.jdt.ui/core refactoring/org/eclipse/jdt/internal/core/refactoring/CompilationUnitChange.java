/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.refactoring.Change;

public abstract class CompilationUnitChange extends Change {

	private String fParentHandle;
	private String fSource;
	private String fName;
		
	public CompilationUnitChange(IPackageFragment parent, String source, String name){
		Assert.isNotNull(parent, "parent");
		Assert.isNotNull(source, "source");
		Assert.isNotNull(name, "name");
		fParentHandle= parent.getHandleIdentifier();
		fSource= source;
		fName= name;
	}
	
	public CompilationUnitChange(IPackageFragment parent, String name){
		this(parent, "", name);
	}
	
	public final void setSource(String source){
		fSource= source;
	}
	
	public final String getSource(){
		return fSource;
	}
	
	public final String getCUName(){
		return fName;
	}
	
	public final IPackageFragment getPackage(){
		IPackageFragment parent= (IPackageFragment)JavaCore.create(fParentHandle);
		Assert.isNotNull(parent);
		Assert.isTrue(parent.exists());
		return parent;
	}
	
	public String getPackageName(){
		String packageName= getPackage().getElementName();
		if ("".equals(packageName))
			packageName= "(default package)";		
		return packageName;	
	}
	
	public String toString(){
		return getName();
	}
}