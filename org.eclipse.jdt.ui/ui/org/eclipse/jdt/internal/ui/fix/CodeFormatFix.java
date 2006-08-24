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
package org.eclipse.jdt.internal.ui.fix;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class CodeFormatFix implements IFix {

	public static IFix createCleanUp(CompilationUnit compilationUnit, boolean format) throws JavaModelException {
		if (!format)
			return null;
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();

        Map fomatterSettings= new HashMap(cu.getJavaProject().getOptions(true));
        
    	String content= cu.getBuffer().getContents();
		Document document= new Document(content);
		
		TextEdit edit= CodeFormatterUtil.format2(compilationUnit, content, 0, TextUtilities.getDefaultLineDelimiter(document), fomatterSettings);
		if (edit == null || !edit.hasChildren())
			return null;
		
		TextChange change= new CompilationUnitChange("", cu); //$NON-NLS-1$
    	change.setEdit(edit);
    	
    	String label= MultiFixMessages.CodeFormatFix_description;
		change.addTextEditGroup(new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label))));
		
		return new CodeFormatFix(change, cu);
	}
	
	private final ICompilationUnit fCompilationUnit;
	private final TextChange fChange;

	public CodeFormatFix(TextChange change, ICompilationUnit compilationUnit) {
		fChange= change;
		fCompilationUnit= compilationUnit;
    }
	
	/* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.fix.IFix#createChange()
     */
    public TextChange createChange() throws CoreException {
    	return fChange;
    }

	/* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.fix.IFix#getCompilationUnit()
     */
    public ICompilationUnit getCompilationUnit() {
	    return fCompilationUnit;
    }

	/* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.fix.IFix#getDescription()
     */
    public String getDescription() {
	    return MultiFixMessages.CodeFormatFix_description;
    }

}