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
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;

public class JavaElementMapper extends GenericVisitor {

	private IMember fElement;
	private int fStart;
	private int fLength;
	private int fEnd;
	private ASTNode fResult;
	
	private JavaElementMapper(IMember element) throws JavaModelException {
		super(true);
		Assert.isNotNull(element);
		fElement= element;
		ISourceRange sourceRange= fElement.getNameRange();
		fStart= sourceRange.getOffset();
		fLength= sourceRange.getLength();
		fEnd= fStart + fLength;
	}

	public static ASTNode perform(IMember member, Class type) throws JavaModelException {
		JavaElementMapper mapper= new JavaElementMapper(member);
		ICompilationUnit unit= member.getCompilationUnit();
		ASTParser parser= ASTParser.newParser(AST.JLS2);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		CompilationUnit node= (CompilationUnit) parser.createAST(null);
		node.accept(mapper);
		ASTNode result= mapper.fResult;
		while(result != null && !type.isInstance(result)) {
			result= result.getParent();
		}
		return result;
	}	
	
	protected boolean visitNode(ASTNode node) {
		if (fResult != null) {
			return false;
		} 
		int nodeStart= node.getStartPosition();
		int nodeLength= node.getLength();
		int nodeEnd= nodeStart + nodeLength;
		if (nodeStart == fStart && nodeLength == fLength) {
			fResult= node;
			return false;
		} else if ( nodeStart <= fStart && fEnd <= nodeEnd) {
			return true;
		}
		return false;
	}	
}

