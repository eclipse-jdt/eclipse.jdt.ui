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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.NameProposer;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewMethodCompletionProposal extends CUCorrectionProposal {

	private boolean fIsLocalChange;
	private IType fDestType;
	private MethodInvocation fNode;

	private MemberEdit fMemberEdit;

	public NewMethodCompletionProposal(String label, MethodInvocation node, ICompilationUnit currCU, IType destType, int relevance) throws CoreException {
		super(label, destType.getCompilationUnit(), relevance, JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC));
		
		fDestType= destType;
		fIsLocalChange= destType.getCompilationUnit().equals(currCU);
		fNode= node;
		
		fMemberEdit= null;
	}
		
	private boolean isLocalChange() {
		return fIsLocalChange;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#createCompilationUnitChange(String, ICompilationUnit, TextEdit)
	 */
	protected CompilationUnitChange createCompilationUnitChange(String name, ICompilationUnit cu, TextEdit root) throws CoreException {
		CompilationUnitChange change= super.createCompilationUnitChange(name, cu, root);
			
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		ImportEdit importEdit= new ImportEdit(cu, settings);

		String content= generateStub(importEdit, settings);
		
		int insertPos= MemberEdit.ADD_AT_END;
		IJavaElement anchor= fDestType;
		if (isLocalChange()) {
			IJavaElement elem= cu.getElementAt(fNode.getStartPosition());
			if (elem != null && elem.getElementType() == IJavaElement.METHOD) {
				anchor= elem;
				insertPos= MemberEdit.INSERT_AFTER;
			}	
		}
		
		fMemberEdit= new MemberEdit(anchor, insertPos, new String[] { content }, settings.tabWidth);
		fMemberEdit.setUseFormatter(true);
		
		if (!importEdit.isEmpty()) {
			root.add( importEdit); //$NON-NLS-1$
		}
		root.add( fMemberEdit); //$NON-NLS-1$
		return null;
	}
	
	
	private String generateStub(ImportEdit importEdit, CodeGenerationSettings settings) throws CoreException {
		
		String methodName= fNode.getName().getIdentifier();
		List arguments= fNode.arguments();
		
		boolean isStatic= false;
		Expression sender= fNode.getExpression();
		if (sender != null) {
			if (sender instanceof Name) {
				IBinding binding= ((Name) sender).resolveBinding();
				if (binding != null) {
					isStatic= (binding.getKind() == binding.TYPE);
				}
			}
		} else {
			isStatic= ASTResolving.isInStaticContext(fNode);
		}
		
		boolean isInterface= fDestType.isInterface();
		boolean isSameType= isLocalChange();
		
		ITypeBinding returnType= evaluateMethodType(fNode, importEdit);
		String returnTypeName= returnType.getName();
		
		String[] paramTypes= new String[arguments.size()];
		for (int i= 0; i < paramTypes.length; i++) {
			ITypeBinding binding= evaluateParameterType((Expression) arguments.get(i), importEdit);
			if (binding != null && !binding.isAnonymous() && !binding.isNullType()) {
				paramTypes[i]= binding.getName();
			} else {
				paramTypes[i]= "Object"; //$NON-NLS-1$
			}
		}
		String[] paramNames= getParameterNames(paramTypes, arguments);
		
		StringBuffer buf= new StringBuffer();
		
		if (settings.createComments) {
			StubUtility.genJavaDocStub("Method " + methodName, paramNames, Signature.createTypeSignature(returnTypeName, true), null, buf); //$NON-NLS-1$
		}
		
		if (isSameType) {
			buf.append("private "); //$NON-NLS-1$
		} else if (!isInterface) {
			buf.append("public "); //$NON-NLS-1$
		}
		
		if (isStatic) {
			buf.append("static "); //$NON-NLS-1$
		}
		
		buf.append(returnTypeName);
		buf.append(' ');
		buf.append(methodName);
		buf.append('(');
		
		if (!arguments.isEmpty()) {
			for (int i= 0; i < arguments.size(); i++) {
				if (i > 0) {
					buf.append(", "); //$NON-NLS-1$
				}
				buf.append(paramTypes[i]);
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
	
	private String[] getParameterNames(String[] paramTypes, List arguments) {
		ArrayList names= new ArrayList(paramTypes.length);
		NameProposer nameProposer= new NameProposer();
		for (int i= 0; i < paramTypes.length; i++) {
			String name;
			Object currArg= arguments.get(i);
			if (currArg instanceof SimpleName) {
				name= ((SimpleName) currArg).getIdentifier();
			} else {
				name= nameProposer.proposeParameterName(paramTypes[i]);
			}
			while (names.contains(name)) {
				name= name + '1';
			}
			names.add(name);
		}
		return (String[]) names.toArray(new String[names.size()]);
	}
	
	private ITypeBinding evaluateMethodType(MethodInvocation invocation, ImportEdit importEdit) {
		ITypeBinding binding= ASTResolving.getTypeBinding(invocation);
		if (binding != null) {
			ITypeBinding baseType= binding.isArray() ? binding.getElementType() : binding;
			if (!baseType.isPrimitive()) {
				importEdit.addImport(Bindings.getFullyQualifiedName(baseType));
			}
			return binding;
		}
		return invocation.getAST().resolveWellKnownType("void"); //$NON-NLS-1$
	}
	
	private ITypeBinding evaluateParameterType(Expression expr, ImportEdit importEdit) {
		ITypeBinding binding= expr.resolveTypeBinding();
		if (binding != null) {
			ITypeBinding baseType= binding.isArray() ? binding.getElementType() : binding;
			if (!baseType.isPrimitive()) {
				importEdit.addImport(Bindings.getFullyQualifiedName(baseType));
			}
			if (binding.getName().equals("null")) { //$NON-NLS-1$
				binding= expr.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			}
		}
		return binding;
	}
	
		
	/* (non-Javadoc)
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		try {
			IEditorPart part= null;
			if (!isLocalChange()) {
				part= EditorUtility.openInEditor(fDestType.getCompilationUnit(), true);
			}
			super.apply(document);
		
			if (part instanceof ITextEditor) {
				TextRange range= getCompilationUnitChange().getNewTextRange(fMemberEdit);		
				((ITextEditor) part).selectAndReveal(range.getOffset(), range.getLength());
			}
		} catch (PartInitException e) {
			JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}		
	}

}
