/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.browsing;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

/**
 * Contains a list of package fragments with the same name
 * but residing in different source folders of a unique Java project.
 */
class LogicalPackage {

	private Set fPackages;
	private String fName;
	private IJavaProject fJavaProject;
	
	public LogicalPackage(IPackageFragment fragment){
		Assert.isNotNull(fragment);
		fPackages= new HashSet();
		fJavaProject= fragment.getJavaProject();
		Assert.isNotNull(fJavaProject);
		add(fragment);
		fName= fragment.getElementName();
	}
	
	public IJavaProject getJavaProject(){
		return fJavaProject;	
	}
	
	public IPackageFragment[] getFragments(){
		return (IPackageFragment[]) fPackages.toArray(new IPackageFragment[fPackages.size()]);
	}
	
	public void add(IPackageFragment fragment){
		Assert.isTrue(fragment != null && fJavaProject.equals(fragment.getJavaProject()));
		fPackages.add(fragment);
	}
	
	public void remove(IPackageFragment fragment){
		fPackages.remove(fragment);	
	}
	
	public boolean contains(IPackageFragment fragment){
		return fPackages.contains(fragment);	
	}
	
	public String getElementName(){
		return fName;
	}
	
	public int size(){
		return fPackages.size();	
	}
	
	/**
	 * Returns true if the given fragment has the same name and
	 * resides inside the same project as the other fragments in
	 * the CompoundElement.
	 * 
	 * @param fragment
	 * @return boolean
	 */
	public boolean belongs(IPackageFragment fragment) {
		
		if(fragment==null)
			return false;
		
		if(fJavaProject.equals(fragment.getJavaProject())){
			return fName.equals(fragment.getElementName());
		}
	
		return false;
	}
}
