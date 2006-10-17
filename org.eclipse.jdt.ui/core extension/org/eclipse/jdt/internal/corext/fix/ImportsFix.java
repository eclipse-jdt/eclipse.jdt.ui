/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import org.eclipse.core.runtime.IStatus;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class ImportsFix extends AbstractFix {
	
	public static IFix createCleanUp(final CompilationUnit cu, CodeGenerationSettings settings, boolean organizeImports) throws CoreException {
		if (!organizeImports)
			return null;
		
		OrganizeImportsOperation op= new OrganizeImportsOperation((ICompilationUnit)cu.getJavaElement(), cu, settings.importIgnoreLowercase, false, true, null);
		final TextEdit edit= op.createTextEdit(null);
		if (edit == null)
			return null;
		
		if (op.getNumberOfImportsAdded() == 0 && op.getNumberOfImportsRemoved() == 0)
			return null;

		return new IFix() {
			
			public TextChange createChange() throws CoreException {
				CompilationUnitChange result= new CompilationUnitChange("", getCompilationUnit()); //$NON-NLS-1$
				result.setEdit(edit);
				String label= getDescription();
				result.addTextEditGroup(new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label))));
	            return result;
            }

			public ICompilationUnit getCompilationUnit() {
	            return (ICompilationUnit)cu.getJavaElement();
            }

			public String getDescription() {
	            return FixMessages.ImportsFix_OrganizeImports_Description;
            }

			public IStatus getStatus() {
	            return StatusInfo.OK_STATUS;
         	  }
    	};
    }
	
	protected ImportsFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
	    super(name, compilationUnit, fixRewriteOperations);
    }
}
