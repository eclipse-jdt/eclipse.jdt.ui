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

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
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

public class NewConstructorCompletionProposal extends CUCorrectionProposal {

	private boolean fIsLocalChange;
	private IType fDestType;
	private List fArguments;

	private MemberEdit fMemberEdit;

	public NewConstructorCompletionProposal(String label, ICompilationUnit currCU, IType destType, List arguments, int relevance) throws CoreException {
		super(label, destType.getCompilationUnit(), relevance, JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC));

		fDestType= destType;
		fIsLocalChange= destType.getCompilationUnit().equals(currCU);
		fArguments= arguments;
		
		fMemberEdit= null;
	}
		
	private boolean isLocalChange() {
		return fIsLocalChange;
	}
	
	
	/*
	 * @see JavaCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange changeElement) throws CoreException {
		ICompilationUnit changedCU= changeElement.getCompilationUnit();
		TextEdit root= changeElement.getEdit();	
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		ImportEdit importEdit= new ImportEdit(changedCU, settings);

		String content= generateStub(importEdit, settings);

		int insertPos= MemberEdit.ADD_AT_BEGINNING;
		IJavaElement anchor= fDestType;
	
		IMethod[] methods= fDestType.getMethods();
		for (int i = methods.length - 1; i >= 0; i--) {
			if (methods[i].isConstructor()) {
				anchor= methods[i];
				insertPos= MemberEdit.INSERT_AFTER;
				break;
			}
		}
		
		fMemberEdit= new MemberEdit(anchor, insertPos, new String[] { content }, settings.tabWidth);
		fMemberEdit.setUseFormatter(true);
		
		if (!importEdit.isEmpty()) {
			root.add( importEdit); //$NON-NLS-1$
		}
		root.add(fMemberEdit); //$NON-NLS-1$
	}
	
	
	private String generateStub(ImportEdit importEdit, CodeGenerationSettings settings) throws CoreException {
		
		String constructorName= fDestType.getElementName();
				
		boolean isSameType= isLocalChange();
			
		String[] paramTypes= new String[fArguments.size()];
		for (int i= 0; i < paramTypes.length; i++) {
			ITypeBinding binding= evaluateParameterType((Expression) fArguments.get(i), importEdit);
			if (binding != null && !binding.isAnonymous() && !binding.isNullType()) {
				paramTypes[i]= binding.getName();
			} else {
				paramTypes[i]= "Object"; //$NON-NLS-1$
			}
		}
		String[] paramNames= getParameterNames(paramTypes, fArguments);
		
		StringBuffer buf= new StringBuffer();
		
		if (settings.createComments) {
			StubUtility.genJavaDocStub("Constructor " + constructorName, paramNames, null, null, buf); //$NON-NLS-1$
		}
		
		buf.append("public "); //$NON-NLS-1$
		buf.append(constructorName);
		buf.append('(');
		
		for (int i= 0; i < paramTypes.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(paramTypes[i]);
			buf.append(' ');
			buf.append(paramNames[i]);
		}
		buf.append(')');
		buf.append("{\n}\n\n"); //$NON-NLS-1$
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
