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
package org.eclipse.jdt.ui.tests.refactoring.ccp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IConfirmQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgQueries;

public class MockReorgQueries implements IReorgQueries, INewNameQueries {
	private final List<Integer> fQueriesRun= new ArrayList<>();

	@Override
	public IConfirmQuery createYesNoQuery(String queryTitle, boolean allowCancel, int queryID) {
		run(queryID);
		return yesQuery;
	}

	@Override
	public IConfirmQuery createYesYesToAllNoNoToAllQuery(String queryTitle, boolean allowCancel, int queryID) {
		run(queryID);
		return yesQuery;
	}

	private void run(int queryID) {
		fQueriesRun.add(queryID);
	}

	//List<Integer>
	public List<Integer> getRunQueryIDs() {
		return fQueriesRun;
	}

	private final IConfirmQuery yesQuery= new IConfirmQuery() {
		@Override
		public boolean confirm(String question) throws OperationCanceledException {
			return true;
		}

		@Override
		public boolean confirm(String question, Object[] elements) throws OperationCanceledException {
			return true;
		}
	};

	@Override
	public IConfirmQuery createSkipQuery(String queryTitle, int queryID) {
		run(queryID);
		return yesQuery;
	}

	private static class NewNameQuery implements INewNameQuery {
		private final String fName;
		public NewNameQuery(String name) {
			fName= name;
		}
		@Override
		public String getNewName() throws OperationCanceledException {
			return fName;
		}
	}


	@Override
	public INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu, String initialSuggestedName) throws OperationCanceledException {
		return new NewNameQuery(initialSuggestedName + '1');
	}

	@Override
	public INewNameQuery createNewPackageFragmentRootNameQuery(IPackageFragmentRoot root, String initialSuggestedName) throws OperationCanceledException {
		return new NewNameQuery(initialSuggestedName + '1');
	}

	@Override
	public INewNameQuery createNewPackageNameQuery(IPackageFragment pack, String initialSuggestedName) throws OperationCanceledException {
		return new NewNameQuery(initialSuggestedName + '1');
	}

	@Override
	public INewNameQuery createNewResourceNameQuery(IResource res, String initialSuggestedName) throws OperationCanceledException {
		return new NewNameQuery(initialSuggestedName + '1');
	}

	@Override
	public INewNameQuery createNullQuery() {
		return new NewNameQuery(null);
	}

	@Override
	public INewNameQuery createStaticQuery(String newName) throws OperationCanceledException {
		return new NewNameQuery(newName);
	}
}
