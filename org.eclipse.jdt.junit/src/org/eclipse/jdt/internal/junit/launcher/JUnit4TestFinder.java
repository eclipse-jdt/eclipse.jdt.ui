/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.launcher;

import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;


public class JUnit4TestFinder implements ITestFinder {
	private static class Annotation {
		private static final JUnit4TestFinder.Annotation RUN_WITH= new JUnit4TestFinder.Annotation(new String[] { "RunWith", "org.junit.runner.RunWith" });

		private static final JUnit4TestFinder.Annotation TEST= new JUnit4TestFinder.Annotation(new String[] { "Test", "org.junit.Test" });

		private final String[] names;

		private Annotation(String[] names) {
			this.names= names;
		}

		public boolean foundIn(String source) {
			IScanner scanner= ToolFactory.createScanner(false, true, false, false);
			scanner.setSource(source.toCharArray());
			try {
				int tok;
				do {
					tok= scanner.getNextToken();
					if (tok == ITerminalSymbols.TokenNameAT) {
						String annotationName= ""; //$NON-NLS-1$
						tok= scanner.getNextToken();
						while (tok == ITerminalSymbols.TokenNameIdentifier || tok == ITerminalSymbols.TokenNameDOT) {
							annotationName+= String.valueOf(scanner.getCurrentTokenSource());
							tok= scanner.getNextToken();
						}
						for (int i= 0; i < names.length; i++) {
							String annotation= names[i];
							if (annotationName.equals(annotation))
								return true;
						}
					}
				} while (tok != ITerminalSymbols.TokenNameEOF);
			} catch (InvalidInputException e) {
				return false;
			}
			return false;
		}

		boolean annotates(IMember member) throws JavaModelException {
			ISourceRange sourceRange= member.getSourceRange();
			ISourceRange nameRange= member.getNameRange();
			int charsToSearch= nameRange.getOffset() - sourceRange.getOffset();
			String source= member.getSource().substring(0, charsToSearch);
			return foundIn(source);
		}

		boolean annotatesAtLeastOneMethod(IType type) throws JavaModelException {
			IMethod[] methods= type.getMethods();
			for (int i= 0; i < methods.length; i++) {
				if (annotates(methods[i]))
					return true;
			}
			return false;
		}
	}

	public void findTestsInContainer(IJavaElement container, Set result, IProgressMonitor pm) {
		try {
			if (container instanceof IJavaProject) {
				IJavaProject project= (IJavaProject) container;
				findTestsInProject(project, result);
			} else if (container instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) container;
				findTestsInPackageFragmentRoot(root, result);
			} else if (container instanceof IPackageFragment) {
				IPackageFragment fragment= (IPackageFragment) container;
				findTestsInPackageFragment(fragment, result);
			}
		} catch (JavaModelException e) {
			// do nothing
		}
	}

	private void findTestsInProject(IJavaProject project, Set result) throws JavaModelException {
		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			findTestsInPackageFragmentRoot(root, result);
		}
	}

	private void findTestsInPackageFragmentRoot(IPackageFragmentRoot root, Set result) throws JavaModelException {
		IJavaElement[] children= root.getChildren();
		for (int j= 0; j < children.length; j++) {
			IPackageFragment fragment= (IPackageFragment) children[j];
			findTestsInPackageFragment(fragment, result);
		}
	}

	private void findTestsInPackageFragment(IPackageFragment fragment, Set result) throws JavaModelException {
		ICompilationUnit[] compilationUnits= fragment.getCompilationUnits();
		for (int k= 0; k < compilationUnits.length; k++) {
			ICompilationUnit unit= compilationUnits[k];
			IType[] types= unit.getAllTypes();
			for (int l= 0; l < types.length; l++) {
				IType type= types[l];
				if (isTest(type))
					result.add(type);
			}
		}
	}

	public boolean isTest(IType type) throws JavaModelException {
		return Annotation.RUN_WITH.annotates(type) || Annotation.TEST.annotatesAtLeastOneMethod(type);
	}
}
