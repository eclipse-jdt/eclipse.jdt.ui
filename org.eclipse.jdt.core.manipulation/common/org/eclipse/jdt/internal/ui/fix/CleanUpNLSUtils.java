/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSLine;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSScanner;

public class CleanUpNLSUtils {

	public static NLSLine scanCurrentLine(ICompilationUnit cu, ASTNode exp) {
		CompilationUnit cUnit= (CompilationUnit)exp.getRoot();
		int startLine= cUnit.getLineNumber(exp.getStartPosition());
		int lineStart= cUnit.getPosition(startLine, 0);
		int endOfLine= cUnit.getPosition(startLine + 1, 0);
		NLSLine[] lines;
		try {
			lines= NLSScanner.scan(cu.getBuffer().getText(lineStart, endOfLine - lineStart));
			if (lines.length > 0) {
				return lines[0];
			}
		} catch (IndexOutOfBoundsException | JavaModelException | InvalidInputException | BadLocationException e) {
			e.printStackTrace();
			// fall-through
		}
		return null;
	}

}
