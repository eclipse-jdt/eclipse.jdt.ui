/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> - bug 48696
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.util.ArrayList;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;

public class JUnitQuickFixProcessor implements IQuickFixProcessor {
	
	private static final int JUNIT3= 1;
	private static final int JUNIT4= 2;

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickFixProcessor#hasCorrections(org.eclipse.jdt.core.ICompilationUnit, int)
	 */
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return IProblem.UndefinedType == problemId || IProblem.ImportNotFound == problemId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickFixProcessor#getCorrections(org.eclipse.jdt.ui.text.java.IInvocationContext, org.eclipse.jdt.ui.text.java.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getCorrections(final IInvocationContext context, IProblemLocation[] locations)  {
		int res= isJUnitProblem(context, locations);
		if (res != 0) {
			ArrayList proposals= new ArrayList(1);
			IJavaProject javaProject= context.getCompilationUnit().getJavaProject();
			if (JUnitStubUtility.is50OrHigher(javaProject) && ((res & JUNIT4) != 0)) {
				proposals.add(new JUnitAddLibraryProposal(true, context, 10));
			}
			if ((res & JUNIT3) != 0) {
				proposals.add(new JUnitAddLibraryProposal(false, context, 8));
			}
			return (IJavaCompletionProposal[]) proposals.toArray(new IJavaCompletionProposal[proposals.size()]);
		}
		return null;
	}

	private int isJUnitProblem(IInvocationContext context, IProblemLocation[] locations) {
		ICompilationUnit unit= context.getCompilationUnit();
		for (int i= 0; i < locations.length; i++) {
			IProblemLocation location= locations[i];
			if (! hasCorrections(context.getCompilationUnit(), location.getProblemId()))
				break; 
			try {
				String s= unit.getBuffer().getText(location.getOffset(), location.getLength());
				if (s.equals("org.junit")) { //$NON-NLS-1$
					return JUNIT4;
				}
				if (s.equals("TestCase") || s.equals("TestSuite") || s.equals("junit")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					return JUNIT3;
				}
				if (s.equals("Test")) { //$NON-NLS-1$
					return JUNIT3 | JUNIT4;
				}
			} catch (JavaModelException e) {
			    JUnitPlugin.log(e.getStatus());
			}
		}
		return 0;
	}
}
