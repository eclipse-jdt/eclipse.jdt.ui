/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;


public class MonitoringNewNameQueries implements INewNameQueries {
	private INewNameQueries fDelegate;
	private ReorgExecutionLog fExecutionLog;
	public MonitoringNewNameQueries(INewNameQueries delegate, ReorgExecutionLog log) {
		fDelegate= delegate;
		fExecutionLog= log;
	}
	public INewNameQuery createNewCompilationUnitNameQuery(final ICompilationUnit cu, final String initialSuggestedName) {
		return new INewNameQuery() {
			public String getNewName() {
				String result= fDelegate.createNewCompilationUnitNameQuery(cu, initialSuggestedName).getNewName();
				final String newName= result + ".java"; //$NON-NLS-1$
				fExecutionLog.setNewName(cu, newName); 
				if (cu.getResource() != null) {
					fExecutionLog.setNewName(cu.getResource(), newName);
				}
				return result;
			}
		};
	}
	public INewNameQuery createNewPackageFragmentRootNameQuery(final IPackageFragmentRoot root, final String initialSuggestedName) {
		return new INewNameQuery() {
			public String getNewName() {
				String result= fDelegate.createNewPackageFragmentRootNameQuery(root, initialSuggestedName).getNewName();
				fExecutionLog.setNewName(root, result);
				if (root.getResource() != null) {
					fExecutionLog.setNewName(root.getResource(), result);
				}
				return result;
			}
		};
	}
	public INewNameQuery createNewPackageNameQuery(final IPackageFragment pack, final String initialSuggestedName) {
		return new INewNameQuery() {
			public String getNewName() {
				String result= fDelegate.createNewPackageNameQuery(pack, initialSuggestedName).getNewName();
				fExecutionLog.setNewName(pack, result);
				return result;
			}
		};
	}
	public INewNameQuery createNewResourceNameQuery(final IResource res, final String initialSuggestedName) {
		return new INewNameQuery() {
			public String getNewName() {
				String result= fDelegate.createNewResourceNameQuery(res, initialSuggestedName).getNewName();
				fExecutionLog.setNewName(res, result);
				return result;
			}
		};
	}
	public INewNameQuery createNullQuery() {
		return fDelegate.createNullQuery();
	}
	public INewNameQuery createStaticQuery(String newName) {
		return fDelegate.createStaticQuery(newName);
	}
}