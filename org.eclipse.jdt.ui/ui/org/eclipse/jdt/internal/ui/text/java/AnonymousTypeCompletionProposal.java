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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.OverrideMethodDialog;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class AnonymousTypeCompletionProposal extends JavaTypeCompletionProposal {

	private IType fDeclaringType;

	public AnonymousTypeCompletionProposal(IJavaProject jproject, ICompilationUnit cu, int start, int length, String constructorCompletion, String displayName, String declaringTypeName, int relevance) {
		super(constructorCompletion, cu, start, length, null, displayName, relevance);
		Assert.isNotNull(declaringTypeName);
		Assert.isNotNull(jproject);

		fDeclaringType= getDeclaringType(jproject, declaringTypeName);
		setImage(getImageForType(fDeclaringType));
		setCursorPosition(constructorCompletion.indexOf('(') + 1);
	}

	private boolean createStubs(StringBuffer buf, ImportsStructure imports) throws CoreException {
		if (fDeclaringType == null)
			return true;
		ICompilationUnit workingCopy= null;
		try {
			final IType currentType= fCompilationUnit.getTypes()[0];
			String dummyName= currentType.getElementName();
			workingCopy= WorkingCopyUtil.getNewWorkingCopy(fCompilationUnit);
			StringBuffer buffer= new StringBuffer();
			final IPackageFragment dummyPackage= currentType.getPackageFragment();
			if (!dummyPackage.isDefaultPackage()) {
				buffer.append("package "); //$NON-NLS-1$
				buffer.append(dummyPackage.getElementName());
				buffer.append(";\n"); //$NON-NLS-1$
			}
			buffer.append("public class "); //$NON-NLS-1$
			buffer.append(dummyName);
			if (fDeclaringType.isInterface())
				buffer.append(" implements "); //$NON-NLS-1$
			else
				buffer.append(" extends "); //$NON-NLS-1$
			buffer.append(fDeclaringType.getFullyQualifiedName('.'));
			int start= buffer.length();
			buffer.append("{\n\n}"); //$NON-NLS-1$
			workingCopy.getBuffer().setContents(buffer.toString());
			synchronized (workingCopy) {
				workingCopy.reconcile(ICompilationUnit.NO_AST, false, null, new NullProgressMonitor());
			}
			RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
			CompilationUnit unit= parser.parse(workingCopy, true);
			IType dummyType= workingCopy.getType(dummyName);
			ITypeBinding binding= null;
			final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(dummyType, unit);
			if (declaration != null) {
				binding= declaration.resolveBinding();
				if (binding != null) {
					IMethodBinding[] bindings= StubUtility2.getOverridableMethods(binding, true);
					CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(fDeclaringType.getJavaProject());
					String[] keys= null;
					boolean annotations= false;
					if (!fDeclaringType.isInterface() && !fDeclaringType.isAnnotation()) {
						OverrideMethodDialog dialog= new OverrideMethodDialog(JavaPlugin.getActiveWorkbenchShell(), null, dummyType, true);
						dialog.setGenerateComment(false);
						dialog.setGenerateAnnotation(false);
						dialog.setElementPositionEnabled(false);
						if (dialog.open() == Window.OK) {
							annotations= dialog.getGenerateAnnotation();
							Object[] selection= dialog.getResult();
							if (selection != null) {
								ArrayList result= new ArrayList(selection.length);
								for (int index= 0; index < selection.length; index++) {
									if (selection[index] instanceof IMethodBinding)
										result.add(((IBinding) selection[index]).getKey());
								}
								keys= (String[]) result.toArray(new String[result.size()]);
								settings.createComments= dialog.getGenerateComment();
							}
						}
					} else {
						settings.createComments= false;
						List list= new ArrayList();
						for (int index= 0; index < bindings.length; index++) {
							if (Modifier.isAbstract(bindings[index].getModifiers()))
								list.add(bindings[index].getKey());
						}
						keys= (String[]) list.toArray(new String[list.size()]);
					}
					if (keys == null) {
						setReplacementString(""); //$NON-NLS-1$
						setReplacementLength(0);
						return false;
					}
					ASTRewrite rewrite= ASTRewrite.create(unit.getAST());
					ListRewrite rewriter= rewrite.getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
					String key= null;
					MethodDeclaration stub= null;
					for (int index= 0; index < keys.length; index++) {
						key= keys[index];
						for (int offset= 0; offset < bindings.length; offset++) {
							if (key.equals(bindings[offset].getKey())) {
								stub= StubUtility2.createImplementationStub(workingCopy, rewrite, imports, unit.getAST(), bindings[offset], binding.getName(), settings, annotations);
								if (stub != null)
									rewriter.insertFirst(stub, null);
								break;
							}
						}
					}
					IDocument document= new Document(workingCopy.getBuffer().getContents());
					try {
						rewrite.rewriteAST(document, fCompilationUnit.getJavaProject().getOptions(true)).apply(document, TextEdit.UPDATE_REGIONS);
						buf.append(document.get(start, document.getLength() - start));
					} catch (MalformedTreeException exception) {
						JavaPlugin.log(exception);
					} catch (IllegalArgumentException exception) {
						JavaPlugin.log(exception);
					} catch (BadLocationException exception) {
						JavaPlugin.log(exception);
					}
				}
			}
			return true;
		} finally {
			if (workingCopy != null)
				workingCopy.discardWorkingCopy();
		}
	}

	private IType getDeclaringType(IJavaProject project, String typeName) {
		try {
			return JavaModelUtil.findType(project, typeName);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	private Image getImageForType(IType type) {
		String imageName= JavaPluginImages.IMG_OBJS_CLASS; // default
		if (type != null) {
			try {
				if (type.isInterface()) {
					imageName= JavaPluginImages.IMG_OBJS_INTERFACE;
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return JavaPluginImages.get(imageName);
	}

	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportsStructure impStructure) throws CoreException, BadLocationException {
		String replacementString= getReplacementString();

		// construct replacement text: an expression to be formatted
		StringBuffer buf= new StringBuffer("new A("); //$NON-NLS-1$
		buf.append(replacementString);

		if (!replacementString.endsWith(")")) { //$NON-NLS-1$
			buf.append(')');
		}

		if (!createStubs(buf, impStructure)) {
			return false;
		}
		if (document.getChar(offset) != ')')
			buf.append(';');

		// use the code formatter
		String lineDelim= StubUtility.getLineDelimiterFor(document);
		int tabWidth= CodeFormatterUtil.getTabWidth(fCompilationUnit.getJavaProject());
		IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
		int indent= Strings.computeIndent(document.get(region.getOffset(), region.getLength()), tabWidth);

		String replacement= CodeFormatterUtil.format(CodeFormatter.K_EXPRESSION, buf.toString(), 0, null, lineDelim, fDeclaringType.getJavaProject());
		replacement= Strings.changeIndent(replacement, 0, tabWidth, CodeFormatterUtil.createIndentString(indent, fCompilationUnit.getJavaProject()), lineDelim);
		setReplacementString(replacement.substring(replacement.indexOf('(') + 1));

		int pos= offset;
		while (pos < document.getLength() && Character.isWhitespace(document.getChar(pos))) {
			pos++;
		}

		if (pos < document.getLength() && document.getChar(pos) == ')') {
			setReplacementLength(pos - offset + 1);
		}
		return true;
	}
}