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
package org.eclipse.jdt.internal.ui.fix;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

public abstract class AbstractMultiFix implements IMultiFix {

	/**
	 * Helper method to convert an <code>IProblem</code> into an
	 * <code>IProblemLocation</code>.
	 * 
	 * @param problem The <code>IProblem</code> not null
	 * @return The <code>IProblemLocation</code> not null
	 */
	protected IProblemLocation getProblemLocation(IProblem problem) {
		int offset= problem.getSourceStart();
		int length= problem.getSourceEnd() - offset + 1;
		return new ProblemLocation(offset, length, problem.getID(), problem.getArguments(), problem.isError());
	}

}
