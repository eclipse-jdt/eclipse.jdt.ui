/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.ProblemsLabelDecorator;

import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;

/**
 * Decorates top-level types with problem markers that
 * are above the first type.
 */
class TopLevelTypeProblemsLabelDecorator extends ProblemsLabelDecorator {

	public TopLevelTypeProblemsLabelDecorator(ImageDescriptorRegistry registry) {
		super(registry);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.ProblemsLabelDecorator#isInside(int, ISourceReference)
	 */
	protected boolean isInside(int pos, ISourceReference sourceElement) throws CoreException {
		if (!(sourceElement instanceof IType) || ((IType)sourceElement).getDeclaringType() != null)
			return false;

		ICompilationUnit cu= ((IType)sourceElement).getCompilationUnit();
		if (cu == null)
			return false;
		IType[] types= cu.getAllTypes();
		if (types.length < 1)
			return false;
		ISourceRange range= types[0].getSourceRange();
		return pos < range.getOffset();
	}
}
