/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;

import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


public class MonitoringNewNameQueries implements INewNameQueries {
	private INewNameQueries fDelegate;
	private ReorgExecutionLog fExecutionLog;
	public MonitoringNewNameQueries(INewNameQueries delegate, ReorgExecutionLog log) {
		fDelegate= delegate;
		fExecutionLog= log;
	}
	@Override
	public INewNameQuery createNewCompilationUnitNameQuery(final ICompilationUnit cu, final String initialSuggestedName) {
		return () -> {
			String result= fDelegate.createNewCompilationUnitNameQuery(cu, initialSuggestedName).getNewName();
			String newName= JavaModelUtil.getRenamedCUName(cu, result);
			fExecutionLog.setNewName(cu, newName);
			ResourceMapping mapping= JavaElementResourceMapping.create(cu);
			if (mapping != null) {
				fExecutionLog.setNewName(mapping, newName);
			}
			return result;
		};
	}
	@Override
	public INewNameQuery createNewPackageFragmentRootNameQuery(final IPackageFragmentRoot root, final String initialSuggestedName) {
		return () -> {
			String result= fDelegate.createNewPackageFragmentRootNameQuery(root, initialSuggestedName).getNewName();
			fExecutionLog.setNewName(root, result);
			ResourceMapping mapping= JavaElementResourceMapping.create(root);
			if (mapping != null) {
				fExecutionLog.setNewName(mapping, result);
			}
			return result;
		};
	}
	@Override
	public INewNameQuery createNewPackageNameQuery(final IPackageFragment pack, final String initialSuggestedName) {
		return () -> {
			String result= fDelegate.createNewPackageNameQuery(pack, initialSuggestedName).getNewName();
			fExecutionLog.setNewName(pack, result);
			ResourceMapping mapping= JavaElementResourceMapping.create(pack);
			if (mapping != null) {
				int index= result.lastIndexOf('.');
				String newFolderName= index == -1 ? result : result.substring(index + 1);
				fExecutionLog.setNewName(mapping, newFolderName);
			}
			return result;
		};
	}
	@Override
	public INewNameQuery createNewResourceNameQuery(final IResource res, final String initialSuggestedName) {
		return () -> {
			String result= fDelegate.createNewResourceNameQuery(res, initialSuggestedName).getNewName();
			fExecutionLog.setNewName(res, result);
			return result;
		};
	}
	@Override
	public INewNameQuery createNullQuery() {
		return fDelegate.createNullQuery();
	}
	@Override
	public INewNameQuery createStaticQuery(String newName) {
		return fDelegate.createStaticQuery(newName);
	}
}
