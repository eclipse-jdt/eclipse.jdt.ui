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

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

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
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewMethodCompletionProposal extends CUCorrectionProposal {

	private IType fParentType;

	private String fMethodName;
	private String[] fParamTypes;
	
	private ProblemPosition fProblemPosition;
	
	private MemberEdit fMemberEdit;

	public NewMethodCompletionProposal(IType type, ProblemPosition problemPos, String label, String methodName, String[] paramTypes, int relevance) throws CoreException {
		super(label, type.getCompilationUnit(), !type.getCompilationUnit().isWorkingCopy(), relevance);
		
		fParentType= type;
		fMethodName= methodName;
		fParamTypes= paramTypes;
		
		fProblemPosition= problemPos;
		
		fMemberEdit= null;
	}
	
	private boolean isLocalChange() {
		return fParentType.getCompilationUnit().equals(fProblemPosition.getCompilationUnit());
	}
	
	
	/*
	 * @see JavaCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange changeElement) throws CoreException {
		ProblemPosition problemPos= fProblemPosition;
		
		ICompilationUnit changedCU= changeElement.getCompilationUnit();
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		ImportEdit importEdit= new ImportEdit(changedCU, settings);

		String content= generateStub(importEdit, settings);
		
		int insertPos= MemberEdit.ADD_AT_END;
		IJavaElement anchor= fParentType;
		if (isLocalChange()) {
			IJavaElement elem= problemPos.getCompilationUnit().getElementAt(problemPos.getOffset());
			if (elem.getElementType() == IJavaElement.METHOD) {
				anchor= elem;
				insertPos= MemberEdit.INSERT_AFTER;
			}
		}
		
		fMemberEdit= new MemberEdit(anchor, insertPos, new String[] { content }, settings.tabWidth);
		fMemberEdit.setUseFormatter(true);
		
		if (!importEdit.isEmpty()) {
			changeElement.addTextEdit("Add imports", importEdit); //$NON-NLS-1$
		}
		changeElement.addTextEdit("Add method", fMemberEdit); //$NON-NLS-1$
	}
	
	
	private String generateStub(ImportEdit importEdit, CodeGenerationSettings settings) throws CoreException {
		StringBuffer buf= new StringBuffer();
		
		boolean isInterface= fParentType.isInterface();
		boolean isSameType= isLocalChange();
		
		ITypeBinding returnType= evaluateMethodType(importEdit);
		String returnTypeName= returnType.getName();
		
		if (settings.createComments) {
			StubUtility.genJavaDocStub("Method " + fMethodName, fParamTypes, Signature.createTypeSignature(returnTypeName, true), null, buf);
		}
		
		if (isSameType) {
			buf.append("private "); //$NON-NLS-1$
		} else if (!isInterface) {
			buf.append("public "); //$NON-NLS-1$
		}
		
		buf.append(returnTypeName);
		buf.append(' ');
		buf.append(fMethodName);
		buf.append('(');
		if (fParamTypes.length > 0) {
			String[] paramNames= new NameProposer().proposeParameterNames(fParamTypes);
			for (int i= 0; i < fParamTypes.length; i++) {
				if (i > 0) {
					buf.append(", "); //$NON-NLS-1$
				}
				String curr= fParamTypes[i];
				if (curr.indexOf('.') != -1) {
					importEdit.addImport(curr);
					curr= Signature.getSimpleName(curr);
				}
				buf.append(curr);
				buf.append(' ');
				buf.append(paramNames[i]);
			}
		}
		buf.append(')');
		if (isInterface) {
			buf.append(";\n");  //$NON-NLS-1$
		} else {
			buf.append("{\n"); //$NON-NLS-1$
	
			if (!returnType.isPrimitive()) {
				buf.append("return null;\n"); //$NON-NLS-1$
			} else if (returnTypeName.equals("boolean")) { //$NON-NLS-1$
				buf.append("return false;\n"); //$NON-NLS-1$
			} else if (!returnTypeName.equals("void")) { //$NON-NLS-1$
				buf.append("return 0;\n"); //$NON-NLS-1$
			}
			buf.append("}\n"); //$NON-NLS-1$
		}
		return buf.toString();
	}
	
	private ITypeBinding evaluateMethodType(ImportEdit importEdit) {
		CompilationUnit cu= AST.parseCompilationUnit(fProblemPosition.getCompilationUnit(), true);

		ASTNode node= ASTResolving.findSelectedNode(cu, fProblemPosition.getOffset(), fProblemPosition.getLength());
		if (node != null) {
			ITypeBinding binding= ASTResolving.getTypeBinding(node.getParent());
			if (binding != null) {
				ITypeBinding baseType= binding.isArray() ? binding.getElementType() : binding;
				if (!baseType.isPrimitive()) {
					importEdit.addImport(Bindings.getFullyQualifiedName(baseType));
				}
				return binding;
			}
		}
		return cu.getAST().resolveWellKnownType("void"); //$NON-NLS-1$
	}	
		
	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
	}
	
	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		super.apply(document);
		if (isLocalChange()) {
			return;
		}
		
		if (fMemberEdit != null) {
			try {
				CompilationUnitChange change= getCompilationUnitChange();
				TextRange range= change.getNewTextRange(fMemberEdit);
			
				IEditorPart part= EditorUtility.openInEditor(fParentType, true);
				if (part instanceof ITextEditor && range != null) {
					((ITextEditor) part).selectAndReveal(range.getOffset(), range.getLength());
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}	
	

}
