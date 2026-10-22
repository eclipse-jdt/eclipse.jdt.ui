/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class MainMethodSearchEngine{

	private static class MethodCollector extends SearchRequestor {
			private List<IType> fResult;
			private int fStyle;

			public MethodCollector(List<IType> result, int style) {
				Assert.isNotNull(result);
				fResult= result;
				fStyle= style;
			}

			private boolean considerExternalJars() {
				return (fStyle & IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS) != 0;
			}

			private boolean considerBinaries() {
				return (fStyle & IJavaElementSearchConstants.CONSIDER_BINARIES) != 0;
			}

			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				Object enclosingElement= match.getElement();
				if (enclosingElement instanceof IMethod) { // defensive code
					try {
						IMethod curr= (IMethod) enclosingElement;
						// Java 25 main method forms only apply to projects with source compliance 25 or higher
						boolean isMain= JavaModelUtil.is25OrHigher(curr.getJavaProject())
								? curr.isMainMethodCandidate()
								: curr.isMainMethod();
						if (isMain) {
							if (!considerExternalJars()) {
								IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(curr);
								if (root == null || root.isArchive()) {
									return;
								}
							}
							if (!considerBinaries() && curr.isBinary()) {
								return;
							}
							IType declaringType= curr.getDeclaringType();
							if (declaringType != null) {
								fResult.add(declaringType);
							}
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e.getStatus());
					}
				}
			}
	}

	/**
	 * Searches for all main methods in the given scope.
	 * Valid styles are IJavaElementSearchConstants.CONSIDER_BINARIES and
	 * IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS
	 * @param pm progress monitor
	 * @param scope the search scope
	 * @param style search style constants (see {@link IJavaElementSearchConstants})
	 * @return the types found
	 */
	public IType[] searchMainMethods(IProgressMonitor pm, IJavaSearchScope scope, int style) throws CoreException {
		List<IType> typesFound= new ArrayList<>(200);

		// Since Java 25 a main method may also be an instance method or have no parameters, so search all
		// methods named 'main'. Otherwise keep the old pattern. The collector filters per project compliance.
		String signature= hasJava25OrHigherProject(scope) ? "main" : "main(String[]) void"; //$NON-NLS-1$ //$NON-NLS-2$
		SearchPattern pattern= SearchPattern.createPattern(signature,
				IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
		SearchRequestor requestor= new MethodCollector(typesFound, style);
		new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), scope, requestor, pm);
		return typesFound.toArray(new IType[typesFound.size()]);
	}

	private static boolean hasJava25OrHigherProject(IJavaSearchScope scope) {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		for (IPath path : scope.enclosingProjectsAndJars()) {
			if (root.findMember(path) instanceof IProject project && JavaModelUtil.is25OrHigher(JavaCore.create(project))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Searches for all main methods in the given scope.
	 * Valid styles are IJavaElementSearchConstants.CONSIDER_BINARIES and
	 * IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS
	 * @param context runnable context
	 * @param scope the search scope
	 * @param style style search style constants (see {@link IJavaElementSearchConstants})
	 * @return the types found
	 */
	public IType[] searchMainMethods(IRunnableContext context, final IJavaSearchScope scope, final int style) throws InvocationTargetException, InterruptedException  {
		int allFlags=  IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS | IJavaElementSearchConstants.CONSIDER_BINARIES;
		Assert.isTrue((style | allFlags) == allFlags);

		final IType[][] res= new IType[1][];

		IRunnableWithProgress runnable= pm -> {
			try {
				res[0]= searchMainMethods(pm, scope, style);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};
		context.run(true, true, runnable);

		return res[0];
	}

}
