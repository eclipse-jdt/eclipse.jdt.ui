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
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class OverrideCompletionProposal extends JavaTypeCompletionProposal {

	private boolean fAnnotations= true;

	private IJavaProject fJavaProject;

	private String fMethodName;

	private String[] fParamTypes;

	public OverrideCompletionProposal(IJavaProject jproject, ICompilationUnit cu, String methodName, String[] paramTypes, int start, int length, String displayName, String completionProposal) {
		super(completionProposal, cu, start, length, null, displayName, 0);
		Assert.isNotNull(jproject);
		Assert.isNotNull(methodName);
		Assert.isNotNull(paramTypes);

		fParamTypes= paramTypes;
		fMethodName= methodName;

		fJavaProject= jproject;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getPrefixCompletionText(org.eclipse.jface.text.IDocument,int)
	 */
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return fMethodName;
	}

	public void setAnnotations(boolean generate) {
		fAnnotations= generate;
	}

	/*
	 * @see JavaTypeCompletionProposal#updateReplacementString(IDocument,char,int,ImportsStructure)
	 */
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportsStructure structure) throws CoreException, BadLocationException {
		IType type= null;
		if (structure != null) {
			ICompilationUnit unit= structure.getCompilationUnit();
			JavaModelUtil.reconcile(unit);
			IJavaElement element= unit.getElementAt(offset);
			if (element != null)
				type= (IType) element.getAncestor(IJavaElement.TYPE);
		}
		RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);
		ITypeBinding binding= null;
		final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unit);
		if (declaration != null) {
			binding= declaration.resolveBinding();
			if (binding != null) {
				ASTRewrite rewrite= ASTRewrite.create(unit.getAST());
				IMethodBinding[] bindings= StubUtility2.getOverridableMethods(binding, true);
				if (bindings != null && bindings.length > 0) {
					IMethodBinding methodBinding= Bindings.findMethodInHierarchy(binding, fMethodName, fParamTypes);
					if (methodBinding != null) {
						CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(fJavaProject);
						ListRewrite rewriter= rewrite.getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
						String key= methodBinding.getKey();
						MethodDeclaration stub= null;
						for (int index= 0; index < bindings.length; index++) {
							if (key.equals(bindings[index].getKey())) {
								stub= StubUtility2.createImplementationStub(fCompilationUnit, rewrite, structure, unit.getAST(), bindings[index], binding.getName(), settings, fAnnotations && fJavaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true).equals(JavaCore.VERSION_1_5) && fJavaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true).equals(JavaCore.VERSION_1_5));
								if (stub != null)
									rewriter.insertFirst(stub, null);
								break;
							}
						}
						if (stub != null) {
							IDocument contents= new Document(fCompilationUnit.getBuffer().getContents());
							IRegion region= contents.getLineInformationOfOffset(getReplacementOffset());
							ITrackedNodePosition position= rewrite.track(stub);
							String indent= Strings.getIndentString(contents.get(region.getOffset(), region.getLength()), settings.tabWidth);
							try {
								rewrite.rewriteAST(contents, fJavaProject.getOptions(true)).apply(contents, TextEdit.UPDATE_REGIONS);
							} catch (MalformedTreeException exception) {
								JavaPlugin.log(exception);
							} catch (IllegalArgumentException exception) {
								JavaPlugin.log(exception);
							} catch (BadLocationException exception) {
								JavaPlugin.log(exception);
							}
							setReplacementString(Strings.changeIndent(Strings.trimIndentation(contents.get(position.getStartPosition(), position.getLength()), settings.tabWidth, false), 0, settings.tabWidth, indent, StubUtility.getLineDelimiterFor(contents)));
						}
					}
				}
			}
		}
		return true;
	}
}