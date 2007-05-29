/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.ccp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IConfirmQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgQueries;

public class MockReorgQueries implements IReorgQueries {
	private final List fQueriesRun= new ArrayList();

	public IConfirmQuery createYesNoQuery(String queryTitle, boolean allowCancel, int queryID) {
		run(queryID);
		return yesQuery;
	}

	public IConfirmQuery createYesYesToAllNoNoToAllQuery(String queryTitle, boolean allowCancel, int queryID) {
		run(queryID);
		return yesQuery;
	}

	private void run(int queryID) {
		fQueriesRun.add(new Integer(queryID));
	}

	//List<Integer>
	public List getRunQueryIDs() {
		return fQueriesRun;
	}

	private final IConfirmQuery yesQuery= new IConfirmQuery() {
		public boolean confirm(String question) throws OperationCanceledException {
			return true;
		}

		public boolean confirm(String question, Object[] elements) throws OperationCanceledException {
			return true;
		}
	};

	public IConfirmQuery createSkipQuery(String queryTitle, int queryID) {
		run(queryID);
		return yesQuery;
	}
}
