/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
package org.eclipse.jsp;

import java.io.IOException;
import java.util.*;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.indexsearch.*;
import org.eclipse.core.indexsearch.IIndexQuery;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

/**
 * Implementation for a JSP type query.
 */
public class JspTypeQuery implements IIndexQuery {

	private IType fType;
	private JspMatchLocatorParser fParser;

	public JspTypeQuery(IType type) {
		fType= type;
	}

	@Override
	public void computePathsKeyingIndexFiles(ArrayList requiredIndexKeys) {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		try {
			for (IProject project : workspace.getRoot().getProjects()) {
				if (!project.isAccessible() || !project.hasNature(JavaCore.NATURE_ID))
					continue;
				IPath path= project.getFullPath();
				if (requiredIndexKeys.indexOf(path) == -1) {
					requiredIndexKeys.add(path);
				}
			}
		} catch (CoreException ex) {
			JspUIPlugin.log("jsp query internal error", ex); //$NON-NLS-1$
		}
	}

	@Override
	public void findIndexMatches(IIndex index, HashSet pathCollector, IProgressMonitor progressMonitor) throws IOException {

		String typeName= fType.getFullyQualifiedName();
		String s= JspIndexParser.JSP_TYPE_REF + "/" + typeName; //$NON-NLS-1$
		index.queryPrefix(pathCollector, s);
	}

	@Override
	public void locateMatches(IFile candidate, ISearchResultCollector resultCollector) {
		if (fParser== null)
			fParser= new JspMatchLocatorParser();
		String typeName= fType.getFullyQualifiedName();
		fParser.match(candidate, typeName, resultCollector);
	}
}
