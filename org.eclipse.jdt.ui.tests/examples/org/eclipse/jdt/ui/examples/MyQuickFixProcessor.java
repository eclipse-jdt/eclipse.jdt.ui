/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.examples;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;

/**
 *
 */
public class MyQuickFixProcessor implements IQuickFixProcessor {
/* Active on files with the name A.java

<extension
      point="org.eclipse.jdt.ui.quickFixProcessors">
   <quickFixProcessor
         id= "org.eclipse.jdt.ui.examples.MyQuickFixProcessor"
         class="org.eclipse.jdt.ui.examples.MyQuickFixProcessor"
         name="Example Quick Fix Processor">
         <objectState adaptable="org.eclipse.core.resources.IResource">
           <property name="name" value="A.java"/>
        </objectState>
   </quickFixProcessor>
</extension>

*/


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ICorrectionProcessor#hasCorrections(org.eclipse.jdt.core.ICompilationUnit, int)
	 */
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return (problemId == IProblem.NumericValueOutOfRange);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ICorrectionProcessor#getCorrections(org.eclipse.jdt.internal.ui.text.correction.IAssistContext, org.eclipse.jdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		for (int i= 0; i < locations.length; i++) {
			if (locations[i].getProblemId() == IProblem.NumericValueOutOfRange) {
				return getNumericValueOutOfRangeCorrection(context, locations[i]);
			}
		}
		return null;
	}

	private IJavaCompletionProposal[] getNumericValueOutOfRangeCorrection(IInvocationContext context, IProblemLocation location) {
		ICompilationUnit cu= context.getCompilationUnit();

		ReplaceCorrectionProposal proposal= new ReplaceCorrectionProposal("Change to 0", cu, location.getOffset(), location.getLength(), "0", 5);
		return new IJavaCompletionProposal[] { proposal };
	}

}
