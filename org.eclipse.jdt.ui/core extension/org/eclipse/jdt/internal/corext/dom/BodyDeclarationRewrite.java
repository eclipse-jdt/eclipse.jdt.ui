/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;


public class BodyDeclarationRewrite {

	private ASTNode fTypeNode;
	private ListRewrite fListRewrite;

	public static BodyDeclarationRewrite create(ASTRewrite rewrite, ASTNode typeNode) {
		return new BodyDeclarationRewrite(rewrite, typeNode);
	}

	private BodyDeclarationRewrite(ASTRewrite rewrite, ASTNode typeNode) {
		ChildListPropertyDescriptor property= ASTNodes.getBodyDeclarationsProperty(typeNode);
		fTypeNode= typeNode;
		fListRewrite= rewrite.getListRewrite(typeNode, property);
	}

	public void insert(BodyDeclaration decl, TextEditGroup description) {
		List container= ASTNodes.getBodyDeclarations(fTypeNode);
		int index= ASTNodes.getInsertionIndex(decl, container);
		fListRewrite.insertAt(decl, index, description);
	}
}
