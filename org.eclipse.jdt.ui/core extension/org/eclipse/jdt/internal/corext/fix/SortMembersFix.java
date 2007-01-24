/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.util.CompilationUnitSorter;

import org.eclipse.jdt.internal.corext.codemanipulation.SortMembersOperation.DefaultJavaElementComparator;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class SortMembersFix implements IFix {

	public static IFix createCleanUp(CompilationUnit compilationUnit, boolean sortMembers, boolean sortFields) throws CoreException {
		if (!sortMembers && !sortFields)
			return null;

		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		cu.becomeWorkingCopy(null, null);

        IProgressMonitor monitor = null;
        
        String label= FixMessages.SortMembersFix_Change_description;
        CategorizedTextEditGroup group= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
        
        TextEdit edit = CompilationUnitSorter.sort(compilationUnit, new DefaultJavaElementComparator(!sortFields), 0, group, monitor);
        if (edit == null)
        	return null;

        TextChange change= new CompilationUnitChange(label, cu);
        change.setEdit(edit);
		change.addTextEditGroup(group);

		return new SortMembersFix(change, cu);
    }
		
	private final ICompilationUnit fCompilationUnit;
	private final TextChange fChange;

	public SortMembersFix(TextChange change, ICompilationUnit compilationUnit) {
		fChange= change;
		fCompilationUnit= compilationUnit;
    }

	/**
	 * {@inheritDoc}
	 */
	public TextChange createChange() throws CoreException {
		return fChange;
	}

	/**
	 * {@inheritDoc}
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return FixMessages.SortMembersFix_Fix_description;
	}

	/**
	 * {@inheritDoc}
	 */
	public IStatus getStatus() {
	    return StatusInfo.OK_STATUS;
	}
}
