/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
 *     Red Hat Ltd - refactored parts to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class CleanUpRefactoringCore {

	public static class CleanUpTarget {

		private final ICompilationUnit fCompilationUnit;

		public CleanUpTarget(ICompilationUnit unit) {
			fCompilationUnit= unit;
		}

		public ICompilationUnit getCompilationUnit() {
			return fCompilationUnit;
		}
	}

	public static class MultiFixTarget extends CleanUpTarget {

		private final IProblemLocation[] fProblems;

		public MultiFixTarget(ICompilationUnit unit, IProblemLocation[] problems) {
			super(unit);
			fProblems= problems;
		}

		public IProblemLocation[] getProblems() {
			return fProblems;
		}
	}

}
