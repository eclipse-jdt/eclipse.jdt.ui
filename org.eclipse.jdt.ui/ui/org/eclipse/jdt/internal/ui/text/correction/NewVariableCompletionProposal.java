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

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewVariableCompletionProposal extends CUCorrectionProposal {

	private IType fParentType;

	private String fVariableName;

	public NewVariableCompletionProposal(IType type, ProblemPosition problemPos, String label, String variableName) throws CoreException {
		super(label, problemPos);
		
		fParentType= type;
		fVariableName= variableName;
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
		
		int insertPos= MemberEdit.ADD_AT_BEGINNING;
		IJavaElement anchor= fParentType;
		
		MemberEdit memberEdit= new MemberEdit(anchor, insertPos, new String[] { content }, settings.tabWidth);
		memberEdit.setUseFormatter(true);
		
		if (!importEdit.isEmpty()) {
			changeElement.addTextEdit("Add imports", importEdit);
		}
		changeElement.addTextEdit("Add field", memberEdit);
	}
	
	
	private String generateStub(ImportEdit importEdit) {
		StringBuffer buf= new StringBuffer();
		String varType= evaluateVariableType(importEdit);
		
		buf.append("private ");
		buf.append(varType);
		buf.append(' ');
		buf.append(fVariableName);
		buf.append(";\n");
		return buf.toString();
	}
		
	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
	}
	
	private String evaluateVariableType(ImportEdit importEdit) {
		ProblemPosition pos= getProblemPosition(); 
		CompilationUnit cu= AST.parseCompilationUnit(getCompilationUnit(), true);

		ASTNode node= ASTResolving.findSelectedNode(cu, pos.getOffset(), pos.getLength());

		ITypeBinding binding= ASTResolving.getTypeBinding(node);
		if (binding != null) {
			ITypeBinding baseType= binding.isArray() ? binding.getElementType() : binding;
			if (!baseType.isPrimitive()) {
				importEdit.addImport(Bindings.getFullyQualifiedName(baseType));
			}
			return binding.getName();
		}
		return "Object";
	}
	


}
