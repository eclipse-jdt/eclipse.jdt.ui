/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;

public class TextEditFix implements IFix {

	private final TextEdit fEdit;
	private final ICompilationUnit fUnit;
	private final String fChangeDescription;

	public TextEditFix(TextEdit edit, ICompilationUnit unit, String changeDescription) {
		fEdit= edit;
		fUnit= unit;
		fChangeDescription= changeDescription;
	}

	/**
	 * {@inheritDoc}
	 */
	public CompilationUnitChange createChange() throws CoreException {
		String label= fChangeDescription;
		CompilationUnitChange result= new CompilationUnitChange(label, fUnit);
		result.setEdit(fEdit);
		result.addTextEditGroup(new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label))));
		return result;
	}

}
