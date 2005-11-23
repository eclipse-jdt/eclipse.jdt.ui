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
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Fix which solves various issues with strings.
 * Supported:
 * 		Add missing $NON-NLS$ tag
 * 		Remove unnecessary $NON-NLS$ tag
 *
 */
public class StringFix extends AbstractFix {
	
	private final TextEdit fEdit;
	private TextChange fChange;

	public static StringFix createFix(CompilationUnit compilationUnit, IProblemLocation problem, boolean removeNLSTag, boolean addNLSTag) throws CoreException {
		TextEdit addEdit= null;
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (addNLSTag) {
			addEdit= NLSUtil.createNLSEdit(cu, problem.getOffset());
		}
		ReplaceEdit removeEdit= null;
		if (removeNLSTag) {
			IBuffer buffer= cu.getBuffer();
			if (buffer != null) {
				removeEdit= getReplace(problem.getOffset(), problem.getLength(), buffer, true);
			}
		}
		if (addEdit == null && removeEdit == null) {
			return null;
		} else if (addEdit != null && removeEdit != null) {
			MultiTextEdit root= new MultiTextEdit();
			TextChangeCompatibility.insert(root, addEdit);
			TextChangeCompatibility.insert(root, removeEdit);
			StringFix stringFix= new StringFix(FixMessages.StringFix_AddRemoveNonNls_description, root, cu);
			return stringFix;
		} else if (addEdit != null) {
			return new StringFix(FixMessages.StringFix_AddNonNls_description, addEdit, cu);
		} else {
			return new StringFix(FixMessages.StringFix_RemoveNonNls_description, removeEdit, cu);
		}
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, boolean addNLSTag, boolean removeNLSTag) throws CoreException, JavaModelException {
		if (!addNLSTag && !removeNLSTag)
			return null;
		
		IProblem[] problems= compilationUnit.getProblems();
		
		if (problems.length == 0)
			return null;
		
		CompilationUnitChange result= null;
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			if (addNLSTag && problem.getID() == IProblem.NonExternalizedStringLiteral) {
				TextEdit edit= NLSUtil.createNLSEdit(cu, problem.getSourceStart());
				if (edit != null) {
					if (result == null) 
						result= new CompilationUnitChange("", cu); //$NON-NLS-1$
					TextChangeCompatibility.addTextEdit(result, FixMessages.StringFix_AddNonNls_description, edit);
				}
			}
			if (removeNLSTag && problem.getID() == IProblem.UnnecessaryNLSTag) {
				IBuffer buffer= cu.getBuffer();
				if (buffer != null) {
					TextEdit edit= StringFix.getReplace(problem.getSourceStart(), problem.getSourceEnd() - problem.getSourceStart() + 1, buffer, false);
					if (edit != null) {
						if (result == null)
							result= new CompilationUnitChange("", cu); //$NON-NLS-1$
						TextChangeCompatibility.addTextEdit(result, FixMessages.StringFix_RemoveNonNls_description, edit);
					}
				}
			}
		}
		if (result == null)
			return null;
		
		return new TextChangeFix("", cu, result); //$NON-NLS-1$
	}
	
	private static ReplaceEdit getReplace(int offset, int length, IBuffer buffer, boolean removeLeadingIndents) {
		
		String replaceString= new String();
		boolean hasMoreInComment= false;
		
		// look after the tag
		int next= offset + length;
		while (next < buffer.getLength()) {
			char ch= buffer.getChar(next);
			if (Strings.isIndentChar(ch)) {
				next++; // remove all whitespace
			} else if (Strings.isLineDelimiterChar(ch)) {
				length= next - offset; 
				break;
			} else if (ch == '/') {
				next++;
				if (next == buffer.getLength() || buffer.getChar(next) != '/') {
					replaceString= "//"; //$NON-NLS-1$
				} else {
					length= next - offset - 1;
				}
				hasMoreInComment= true;
				break;
			} else {
				replaceString= "//"; //$NON-NLS-1$
				hasMoreInComment= true;
				break;
			}
		}
		if (!hasMoreInComment && removeLeadingIndents) {
			while (offset > 0 && Strings.isIndentChar(buffer.getChar(offset - 1))) {
				offset--;
				length++;
			}
		}
		if (length > 0) {
			ReplaceEdit replaceEdit= new ReplaceEdit(offset, length, replaceString);
			return replaceEdit;
		} else {
			return null;
		}
	}

	private StringFix(String name, TextEdit edit, ICompilationUnit compilationUnit) {
		super(name, compilationUnit);
		fEdit= edit;
	}

	public TextChange createChange() {
		if (fChange == null) {
			fChange= new CompilationUnitChange(getDescription(), getCompilationUnit());
			fChange.setEdit(fEdit);
		}
		return fChange;
	}

}
