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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;


public abstract class AbstractFix implements IFix {
	
	public static abstract class AbstractFixRewriteOperation implements IFixRewriteOperation {
		
		protected Type importType(final ITypeBinding toImport, final ASTNode accessor, ImportRewrite imports, final CompilationUnit compilationUnit) {
			ImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(compilationUnit, accessor.getStartPosition(), imports);
			return imports.addImport(toImport, compilationUnit.getAST(), importContext);
		}
		
		protected TextEditGroup createTextEditGroup(String label) {
			if (label.length() > 0){
				return new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
			} else {
				return new TextEditGroup(label);
			}
		}
	}
	
	private final String fName;
	private final ICompilationUnit fCompilationUnit;
	private final IFixRewriteOperation[] fFixRewrites;
	private final CompilationUnit fUnit;
	private IStatus fStatus;
	
	protected AbstractFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		fName= name;
		fCompilationUnit= (ICompilationUnit)compilationUnit.getJavaElement();
		fFixRewrites= fixRewriteOperations;
		fUnit= compilationUnit;
		fStatus= StatusInfo.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#getName()
	 */
	public String getDescription() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#getCompilationUnit()
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#createChange()
	 */
	public TextChange createChange() throws CoreException {
		if (fFixRewrites == null || fFixRewrites.length == 0)
			return null;

		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite(getCompilationUnit(), fUnit);

		List/*<TextEditGroup>*/ groups= new ArrayList();
		
		for (int i= 0; i < fFixRewrites.length; i++) {
			fFixRewrites[i].rewriteAST(cuRewrite, groups);
		}
		
		CompilationUnitChange result= cuRewrite.createChange(getDescription(), true, null);
		if (result == null)
			return null;
		
		for (Iterator iter= groups.iterator(); iter.hasNext();) {
			TextEditGroup group= (TextEditGroup)iter.next();
			result.addTextEditGroup(group);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#getStatus()
	 */
	public IStatus getStatus() {
	    return fStatus;
	}
	
    public void setStatus(IStatus status) {
    	fStatus= status;
    }
}
