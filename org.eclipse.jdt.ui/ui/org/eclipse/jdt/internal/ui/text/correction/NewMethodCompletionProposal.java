/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.NameProposer;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewMethodCompletionProposal extends CUCorrectionProposal {

	private IType fParentType;

	private String fMethodName;
	private String[] fParamTypes;

	public NewMethodCompletionProposal(IType type, ProblemPosition problemPos, String label, String methodName, String[] paramTypes) throws CoreException {
		super(label, problemPos);
		
		fParentType= type;
		fMethodName= methodName;
		fParamTypes= paramTypes;
	}

	/*
	 * @see JavaCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange changeElement) throws CoreException {
		ProblemPosition problemPos= getProblemPosition();
		
		ICompilationUnit cu= getCompilationUnit();
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		ImportEdit importEdit= new ImportEdit(cu, settings);

		String content= generateStub(importEdit);
		
		int insertPos= MemberEdit.ADD_AT_END;
		IJavaElement anchor= fParentType;
		IJavaElement elem= cu.getElementAt(problemPos.getOffset());
		if (elem.getElementType() == IJavaElement.METHOD) {
			anchor= elem;
			insertPos= MemberEdit.INSERT_AFTER;
		}
		
		MemberEdit memberEdit= new MemberEdit(anchor, insertPos, new String[] { content }, settings.tabWidth);
		memberEdit.setUseFormatter(true);
		
		if (!importEdit.isEmpty()) {
			changeElement.addTextEdit("Add imports", importEdit);
		}
		changeElement.addTextEdit("Add method", memberEdit);
	}
	
	
	private String generateStub(ImportEdit importEdit) {
		StringBuffer buf= new StringBuffer();
		
		ITypeBinding returnType= evaluateMethodType(importEdit);
		String returnTypeName= returnType.getName();
	
		buf.append("private ");
		buf.append(returnTypeName);
		buf.append(' ');
		buf.append(fMethodName);
		buf.append("(");
		if (fParamTypes.length > 0) {
			String[] paramNames= new NameProposer().proposeParameterNames(fParamTypes);
			for (int i= 0; i < fParamTypes.length; i++) {
				if (i > 0) {
					buf.append(", ");
				}
				String curr= fParamTypes[i];
				if (curr.indexOf('.') != -1) {
					importEdit.addImport(curr);
					curr= Signature.getSimpleName(curr);
				}
				buf.append(curr);
				buf.append(" ");
				buf.append(paramNames[i]);
			}
		}
		buf.append(") {\n");

		if (!returnType.isPrimitive()) {
			buf.append("return null;\n");
		} else if (returnTypeName.equals("boolean")) {
			buf.append("return false;\n");
		} else if (!returnTypeName.equals("void")) {
			buf.append("return 0;\n");
		}
		buf.append("}\n");
		return buf.toString();
	}
	
	private ITypeBinding evaluateMethodType(ImportEdit importEdit) {
		ProblemPosition pos= getProblemPosition(); 
		CompilationUnit cu= AST.parseCompilationUnit(getCompilationUnit(), true);

		ASTNode node= ASTResolving.findSelectedNode(cu, pos.getOffset(), pos.getLength());
		

		ITypeBinding binding= ASTResolving.getTypeBinding(node.getParent());
		if (binding != null) {
			ITypeBinding baseType= binding.isArray() ? binding.getElementType() : binding;
			if (!baseType.isPrimitive()) {
				importEdit.addImport(Bindings.getFullyQualifiedName(baseType));
			}
			return binding;
		}
		return cu.getAST().resolveWellKnownType("void");
	}	
		
	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
	}

}
