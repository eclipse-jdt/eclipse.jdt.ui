/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.search.SearchMatch;

import org.eclipse.jdt.internal.corext.util.SearchUtils;

/**
 * Collects the results returned by a <code>SearchEngine</code>.
 * Only collects matches in CUs ands offers a scanner to trim match ranges.
 */
public abstract class CuCollectingSearchRequestor extends CollectingSearchRequestor {

	private ICompilationUnit fCuCache;
	private IScanner fScannerCache;
	
	protected IScanner getScanner(ICompilationUnit unit) {
		if (unit.equals(fCuCache))
			return fScannerCache;
		
		fCuCache= unit;
		IJavaProject project= unit.getJavaProject();
		String sourceLevel= project.getOption(JavaCore.COMPILER_SOURCE, true);
		String complianceLevel= project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		fScannerCache= ToolFactory.createScanner(false, false, false, sourceLevel, complianceLevel);
		return fScannerCache;
	}
	
	/**
	 * This is an internal method. Do not call from subclasses!
	 * Use {@link #collectMatch(SearchMatch)} instead.
	 * @param match 
	 * @throws CoreException 
	 * @deprecated
	 */
	public final void acceptSearchMatch(SearchMatch match) throws CoreException {
		ICompilationUnit unit= SearchUtils.getCompilationUnit(match);
		if (unit == null)
			return;
		acceptSearchMatch(unit, match);
	}
	
	public void collectMatch(SearchMatch match) throws CoreException {
		super.acceptSearchMatch(match);
	}
	
	protected abstract void acceptSearchMatch(ICompilationUnit unit, SearchMatch match) throws CoreException;

	public void endReporting() {
		fCuCache= null;
		fScannerCache= null;
	}
}


