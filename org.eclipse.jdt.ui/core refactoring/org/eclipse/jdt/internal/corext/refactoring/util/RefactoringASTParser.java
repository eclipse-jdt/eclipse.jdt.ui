/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RefactoringASTParser {

	private ASTParser fParser;
	
	public RefactoringASTParser(int level) {
		fParser= ASTParser.newParser(level);
	}
	
	public static final String SOURCE_PROPERTY= "org.eclipse.jdt.ui.refactoring.ast_source"; //$NON-NLS-1$
	
	public CompilationUnit parse(ICompilationUnit unit, boolean resolveBindings) {
		fParser.setResolveBindings(resolveBindings);
		fParser.setSource(unit);
		fParser.setCompilerOptions(getCompilerOptions(unit));
		CompilationUnit result= (CompilationUnit)fParser.createAST(null);
		result.setProperty(SOURCE_PROPERTY, unit);
		return result;
	}
	
	public static ICompilationUnit getCompilationUnit(ASTNode node) {
		Object source= node.getRoot().getProperty(SOURCE_PROPERTY);
		if (source instanceof ICompilationUnit)
			return (ICompilationUnit)source;
		return null;
	}
	
	private static Map getCompilerOptions(IJavaElement element) {
		IJavaProject project= element.getJavaProject();
		Map options= project.getOptions(true);
		// turn all errors and warnings into ignore. The customizable set of compiler
		// options only contains additional Eclipse options. The standard JDK compiler
		// options can't be changed anyway.
		for (Iterator iter= options.keySet().iterator(); iter.hasNext();) {
			String key= (String)iter.next();
			String value= (String)options.get(key);
			if ("error".equals(value) || "warning".equals(value)) {  //$NON-NLS-1$//$NON-NLS-2$
				System.out.println("Ignoring - " + key);
				options.put(key, "ignore"); //$NON-NLS-1$
			}
		}
		return options;
	}
}
