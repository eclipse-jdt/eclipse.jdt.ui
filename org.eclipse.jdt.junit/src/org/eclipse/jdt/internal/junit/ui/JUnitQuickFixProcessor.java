/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> - bug 48696
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

public class JUnitQuickFixProcessor implements IQuickFixProcessor {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickFixProcessor#hasCorrections(org.eclipse.jdt.core.ICompilationUnit, int)
	 */
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return IProblem.SuperclassNotFound == problemId || IProblem.ImportNotFound == problemId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickFixProcessor#getCorrections(org.eclipse.jdt.ui.text.java.IInvocationContext, org.eclipse.jdt.ui.text.java.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getCorrections(final IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		if (isJUnitProblem(context, locations))
			return new IJavaCompletionProposal[] { new JUnitAddLibraryProposal(context) };
		return new IJavaCompletionProposal[] {};
	}

	private boolean isJUnitProblem(IInvocationContext context, IProblemLocation[] locations) {
		ICompilationUnit unit= context.getCompilationUnit();
		for (int i= 0; i < locations.length; i++) {
			IProblemLocation location= locations[i];
			try {
				String s= unit.getBuffer().getText(location.getOffset(), location.getLength());
				if (s.equals("TestCase") //$NON-NLS-1$
						|| s.equals("junit") //$NON-NLS-1$
						|| s.equals("TestSuite") //$NON-NLS-1$
						|| s.equals("Test"))
					return true; //$NON-NLS-1$
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}
