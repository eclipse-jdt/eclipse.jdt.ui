/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;

abstract class PackageReorgChange extends Change {

	private String fPackageHandle;
	private String fDestinationHandle;
	private INewNameQuery fNameQuery;
	
	PackageReorgChange(IPackageFragment pack, IPackageFragmentRoot dest, INewNameQuery nameQuery){
		fPackageHandle= pack.getHandleIdentifier();
		fDestinationHandle= dest.getHandleIdentifier();
		fNameQuery= nameQuery;
	}
	
	abstract void doPerform(IProgressMonitor pm) throws JavaModelException;
	
	/* non java-doc
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm) throws ChangeAbortException, CoreException {
		pm.beginTask(getName(), 1);
		try{
			if (!isActive())
				return;
			doPerform(pm);
		}catch (Exception e) {
			handleException(context, e);
			setActive(false);	
		} finally{
			pm.done();
		}
	}
	
	/* non java-doc
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return getPackage();
	}
	
	IPackageFragmentRoot getDestination(){
		return (IPackageFragmentRoot)JavaCore.create(fDestinationHandle);
	}
	
	IPackageFragment getPackage(){
		return (IPackageFragment)JavaCore.create(fPackageHandle);
	}

	String getNewName() {
		if (fNameQuery == null)
			return null;
		return fNameQuery.getNewName();
	}

	public boolean isUndoable() {
		return false;
	}

	public IChange getUndoChange() {
		return new NullChange();
	}
}

