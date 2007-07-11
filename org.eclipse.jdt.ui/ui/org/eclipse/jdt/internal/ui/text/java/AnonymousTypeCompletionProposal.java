/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.OverrideMethodDialog;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class AnonymousTypeCompletionProposal extends JavaTypeCompletionProposal implements ICompletionProposalExtension4 {

	private String fDeclarationSignature;
	private IType fSuperType;

	public AnonymousTypeCompletionProposal(IJavaProject jproject, ICompilationUnit cu, int start, int length, String constructorCompletion, String displayName, String declarationSignature, int relevance) {
		super(constructorCompletion, cu, start, length, null, displayName, relevance);
		Assert.isNotNull(declarationSignature);
		Assert.isNotNull(jproject);
		Assert.isNotNull(cu);

		fDeclarationSignature= declarationSignature;
		fSuperType= getDeclaringType(jproject, SignatureUtil.stripSignatureToFQN(String.valueOf(declarationSignature)));

		setImage(getImageForType(fSuperType));
		setCursorPosition(constructorCompletion.indexOf('(') + 1);
	}

	private int createDummy(String name, StringBuffer buffer) throws JavaModelException {
		String lineDelim= "\n"; // Using newline is ok since source is used in dummy compilation unit //$NON-NLS-1$
		buffer.append("class "); //$NON-NLS-1$
		buffer.append(name);
		if (fSuperType.isInterface())
			buffer.append(" implements "); //$NON-NLS-1$
		else
			buffer.append(" extends "); //$NON-NLS-1$
		if (fDeclarationSignature != null)
			buffer.append(Signature.toString(fDeclarationSignature));
		else
			buffer.append(fSuperType.getFullyQualifiedParameterizedName());
		int start= buffer.length();
		buffer.append("{"); //$NON-NLS-1$
		buffer.append(lineDelim);
		buffer.append(lineDelim);
		buffer.append("}"); //$NON-NLS-1$
		return start;
	}

	private boolean createStubs(StringBuffer buffer, ImportRewrite importRewrite) throws CoreException {
		if (importRewrite == null)
			return false;
		if (fSuperType == null)
			return true;
		ICompilationUnit copy= null;
		try {
			final String name= "Type" + System.currentTimeMillis(); //$NON-NLS-1$
			copy= fCompilationUnit.getPrimary().getWorkingCopy(null);
			final StringBuffer contents= new StringBuffer();
			int start= 0;
			int end= 0;
			ISourceRange range= fSuperType.getSourceRange();
			final boolean sameUnit= range != null && fCompilationUnit.equals(fSuperType.getCompilationUnit());
			final StringBuffer dummy= new StringBuffer();
			final int length= createDummy(name, dummy);
			contents.append(fCompilationUnit.getBuffer().getContents());
			if (sameUnit) {
				final int size= range.getOffset() + range.getLength();
				start= size + length;
				end= contents.length() - size;
				contents.insert(size, dummy.toString());
			} else {
				range= fCompilationUnit.getTypes()[0].getSourceRange();
				start= range.getOffset() + length;
				end= contents.length() - range.getOffset();
				contents.insert(range.getOffset(), dummy.toString());
			}
			copy.getBuffer().setContents(contents.toString());
			JavaModelUtil.reconcile(copy);
			final ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setResolveBindings(true);
			parser.setSource(copy);
			final CompilationUnit unit= (CompilationUnit) parser.createAST(new NullProgressMonitor());
			IType type= null;
			IType[] types= copy.getAllTypes();
			for (int index= 0; index < types.length; index++) {
				IType result= types[index];
				if (result.getElementName().equals(name)) {
					type= result;
					break;
				}
			}
			if (type != null && type.exists()) {
				ITypeBinding binding= null;
				final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(NodeFinder.perform(unit, type.getNameRange()), AbstractTypeDeclaration.class);
				if (declaration != null) {
					binding= declaration.resolveBinding();
					if (binding != null) {
						IMethodBinding[] bindings= StubUtility2.getOverridableMethods(unit.getAST(), binding, true);
						CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(fSuperType.getJavaProject());
						String[] keys= null;
						if (!fSuperType.isInterface() && !fSuperType.isAnnotation()) {
							OverrideMethodDialog dialog= new OverrideMethodDialog(JavaPlugin.getActiveWorkbenchShell(), null, type, true);
							dialog.setGenerateComment(false);
							dialog.setElementPositionEnabled(false);
							if (dialog.open() == Window.OK) {
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
									stub= StubUtility2.createImplementationStub(copy, rewrite, importRewrite, bindings[offset], binding.getName(), binding.isInterface(), settings);
									if (stub != null)
										rewriter.insertFirst(stub, null);
									break;
								}
							}
						}
						IDocument document= new Document(copy.getBuffer().getContents());
						try {
							rewrite.rewriteAST(document, fCompilationUnit.getJavaProject().getOptions(true)).apply(document, TextEdit.UPDATE_REGIONS);
							buffer.append(document.get(start, document.getLength() - start - end));
						} catch (MalformedTreeException exception) {
							JavaPlugin.log(exception);
						} catch (BadLocationException exception) {
							JavaPlugin.log(exception);
						}
					}
				}
			}
			return true;
		} finally {
			if (copy != null)
				copy.discardWorkingCopy();
		}
	}

	private IType getDeclaringType(IJavaProject project, String typeName) {
		try {
			return project.findType(typeName, (IProgressMonitor) null);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	private Image getImageForType(IType type) {
		String imageName= JavaPluginImages.IMG_OBJS_CLASS; // default
		if (type != null) {
			try {
				if (type.isAnnotation()) {
					imageName= JavaPluginImages.IMG_OBJS_ANNOTATION;
				} else if (type.isInterface()) {
					imageName= JavaPluginImages.IMG_OBJS_INTERFACE;
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return JavaPluginImages.get(imageName);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4#isAutoInsertable()
	 */
	public boolean isAutoInsertable() {
		return false;
	}

	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportRewrite impRewrite) throws CoreException, BadLocationException {
		String replacementString= getReplacementString();

		// construct replacement text: an expression to be formatted
		StringBuffer buf= new StringBuffer("new A("); //$NON-NLS-1$
		buf.append(replacementString);

		if (!replacementString.endsWith(")")) { //$NON-NLS-1$
			buf.append(')');
		}

		if (!createStubs(buf, impRewrite)) {
			return false;
		}
		if (document.getChar(offset) != ')')
			buf.append(';');

		// use the code formatter
		String lineDelim= TextUtilities.getDefaultLineDelimiter(document);
		final IJavaProject project= fCompilationUnit.getJavaProject();
		IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
		int indent= Strings.computeIndentUnits(document.get(region.getOffset(), region.getLength()), project);

		String replacement= CodeFormatterUtil.format(CodeFormatter.K_EXPRESSION, buf.toString(), 0, lineDelim, project);
		replacement= Strings.changeIndent(replacement, 0, project, CodeFormatterUtil.createIndentString(indent, project), lineDelim);
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
