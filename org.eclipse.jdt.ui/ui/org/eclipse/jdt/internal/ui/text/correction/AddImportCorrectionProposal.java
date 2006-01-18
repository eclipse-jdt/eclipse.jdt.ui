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
package org.eclipse.jdt.internal.ui.text.correction;



import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.util.History;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoHistory;
import org.eclipse.jdt.internal.corext.util.TypeInfoUtil;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class AddImportCorrectionProposal extends ASTRewriteCorrectionProposal {
	
	private final String fTypeName;
	private final ICompilationUnit fCu;
	private final SimpleName fNode;
	private final String fQualifierName;

	public AddImportCorrectionProposal(String name, ICompilationUnit cu, int relevance, Image image, String qualifierName, String typeName, SimpleName node) {
		super(name, cu, ASTRewrite.create(node.getAST()), relevance, image);
		fTypeName= typeName;
		fCu= cu;
		fNode= node;
		fQualifierName= qualifierName;
	}
	
	public String getQualifiedTypeName() {
		return fQualifierName + '.' + fTypeName;
	}

	protected void rememberSelection() {
		try {
			TypeInfo info= TypeInfoUtil.searchTypeInfo(fCu.getJavaProject(), fNode, getQualifiedTypeName());
			if (info != null) {
				History history= TypeInfoHistory.getDefault();
				history.accessed(info);
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, CorrectionMessages.CUCorrectionProposal_error_title, CorrectionMessages.CUCorrectionProposal_error_message);
		}
	}

}