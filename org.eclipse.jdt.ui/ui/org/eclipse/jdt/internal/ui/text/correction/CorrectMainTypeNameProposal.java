/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Renames the primary type to be compatible with the name of the compilation unit.
 * All constructurs and local references to the type are renamed as well.
  */
public class CorrectMainTypeNameProposal extends ASTRewriteCorrectionProposal {
	
	private String fOldName;
	private String fNewName;

	/**
	 * Constructor for CorrectTypeNameProposal.
	 */
	public CorrectMainTypeNameProposal(ICompilationUnit cu, String oldTypeName, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		fNewName= Signature.getQualifier(cu.getElementName());
		
		setDisplayName(CorrectionMessages.getFormattedString("ReorgCorrectionsSubProcessor.renametype.description", fNewName)); //$NON-NLS-1$
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		
		fOldName= oldTypeName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected OldASTRewrite getRewrite() throws CoreException {
		char[] content= getCompilationUnit().getBuffer().getCharacters();
		CompilationUnit astRoot= AST.parseCompilationUnit(content, fOldName + ".java", getCompilationUnit().getJavaProject()); //$NON-NLS-1$
		OldASTRewrite rewrite= new OldASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		TypeDeclaration decl= findTypeDeclaration(astRoot.types(), fOldName);
		if (decl != null) {
			ASTNode[] sameNodes= LinkedNodeFinder.findByNode(astRoot, decl.getName());
			for (int i= 0; i < sameNodes.length; i++) {
				rewrite.replace(sameNodes[i], ast.newSimpleName(fNewName), null);
			}
		}
		return rewrite;
	}
	
	private TypeDeclaration findTypeDeclaration(List types, String name) {
		for (Iterator iter= types.iterator(); iter.hasNext();) {
			TypeDeclaration decl= (TypeDeclaration) iter.next();
			if (name.equals(decl.getName().getIdentifier())) {
				return decl;
			}
		}
		return null;	
	}

}
