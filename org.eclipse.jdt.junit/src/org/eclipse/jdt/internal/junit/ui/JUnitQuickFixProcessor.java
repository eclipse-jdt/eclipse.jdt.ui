/*
 * Created on Jul 2, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
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
				if (s.equals("TestCase") || s.equals("junit.framework.TestCase")) //$NON-NLS-1$ //$NON-NLS-2$
					return true;
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}
