/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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


	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return (problemId == IProblem.NumericValueOutOfRange);
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		for (IProblemLocation location : locations) {
			if (location.getProblemId() == IProblem.NumericValueOutOfRange) {
				return getNumericValueOutOfRangeCorrection(context, location);
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
