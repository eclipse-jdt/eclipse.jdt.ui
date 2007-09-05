/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class ImportsFix extends AbstractFix {
	
	public static IFix createCleanUp(final CompilationUnit cu, CodeGenerationSettings settings, boolean organizeImports, RefactoringStatus status) throws CoreException {
		if (!organizeImports)
			return null;

		final boolean hasAmbiguity[]= new boolean[] { false };
		IChooseImportQuery query= new IChooseImportQuery() {
			public TypeNameMatch[] chooseImports(TypeNameMatch[][] openChoices, ISourceRange[] ranges) {
				hasAmbiguity[0]= true;
				return new TypeNameMatch[0];
			}
		};
		
		OrganizeImportsOperation op= new OrganizeImportsOperation((ICompilationUnit)cu.getJavaElement(), cu, settings.importIgnoreLowercase, false, false, query);
		final TextEdit edit= op.createTextEdit(null);
		if (hasAmbiguity[0]) {
			status.addInfo(Messages.format(ActionMessages.OrganizeImportsAction_multi_error_unresolvable, getLocationString(cu)));
		}
		
		if (op.getParseError() != null) {
			status.addInfo(Messages.format(ActionMessages.OrganizeImportsAction_multi_error_parse, getLocationString(cu)));
			return null;
		}
		
		if (edit == null || (edit instanceof MultiTextEdit && edit.getChildrenSize() == 0))
			return null;

		return new IFix() {
			
			public TextChange createChange() throws CoreException {
				CompilationUnitChange result= new CompilationUnitChange(getDescription(), getCompilationUnit());
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

	private static String getLocationString(final CompilationUnit cu) {
		return cu.getJavaElement().getPath().makeRelative().toString();
	}
	
	protected ImportsFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
	    super(name, compilationUnit, fixRewriteOperations);
    }
}
