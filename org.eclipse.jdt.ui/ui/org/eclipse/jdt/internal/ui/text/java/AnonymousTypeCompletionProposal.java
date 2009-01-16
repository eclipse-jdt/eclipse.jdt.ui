/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.text.edits.MalformedTreeException;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.OverrideMethodDialog;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;


public class AnonymousTypeCompletionProposal extends JavaTypeCompletionProposal implements ICompletionProposalExtension4 {

	private final String fDeclarationSignature;
	private final IType fSuperType;

	private boolean fIsContextInformationComputed;
	private int fContextInformationPosition;

	private ImportRewrite fImportRewrite;


	public AnonymousTypeCompletionProposal(IJavaProject jproject, ICompilationUnit cu, JavaContentAssistInvocationContext invocationContext, int start, int length, String constructorCompletion, StyledString displayName, String declarationSignature, IType superType, int relevance) {
		super(constructorCompletion, cu, start, length, null, displayName, relevance, null, invocationContext);
		Assert.isNotNull(declarationSignature);
		Assert.isNotNull(jproject);
		Assert.isNotNull(cu);
		Assert.isNotNull(superType);

		fDeclarationSignature= declarationSignature;
		fSuperType= superType;

		setImage(getImageForType(fSuperType));
		setCursorPosition(constructorCompletion.indexOf('(') + 1);
	}

	private String createDummyType(String name) throws JavaModelException {
		StringBuffer buffer= new StringBuffer();

		buffer.append("abstract class "); //$NON-NLS-1$
		buffer.append(name);
		if (fSuperType.isInterface())
			buffer.append(" implements "); //$NON-NLS-1$
		else
			buffer.append(" extends "); //$NON-NLS-1$

		if (fDeclarationSignature != null)
			buffer.append(Signature.toString(fDeclarationSignature));
		else
			buffer.append(fSuperType.getFullyQualifiedParameterizedName());
		buffer.append(" {"); //$NON-NLS-1$
		buffer.append("\n"); // Using newline is ok since source is used in dummy compilation unit //$NON-NLS-1$
		buffer.append("}"); //$NON-NLS-1$
		return buffer.toString();
	}

	private String createNewBody(ImportRewrite importRewrite) throws CoreException {
		if (importRewrite == null)
			return null;

		ICompilationUnit workingCopy= null;
		try {
			String name= "Type" + System.currentTimeMillis(); //$NON-NLS-1$
			workingCopy= fCompilationUnit.getPrimary().getWorkingCopy(null);


			ISourceRange range= fSuperType.getSourceRange();
			boolean sameUnit= range != null && fCompilationUnit.equals(fSuperType.getCompilationUnit());

			// creates a type that extends the super type
			String dummyClassContent= createDummyType(name);

			StringBuffer workingCopyContents= new StringBuffer(fCompilationUnit.getSource());
			int insertPosition;
			if (sameUnit) {
				insertPosition= range.getOffset() + range.getLength();
			} else {
				ISourceRange firstTypeRange= fCompilationUnit.getTypes()[0].getSourceRange();
				insertPosition= firstTypeRange.getOffset();
			}
			if (fSuperType.isLocal()) {
				// add an extra block: helps the AST to recover
				workingCopyContents.insert(insertPosition, '{' + dummyClassContent + '}');
				insertPosition++;
			} else {
				/*
				 * The two empty lines are added because the trackedDeclaration uses the covered range
				 * and hence would also included comments that directly follow the dummy class.
				 */
				workingCopyContents.insert(insertPosition, dummyClassContent + "\n\n"); //$NON-NLS-1$
			}

			workingCopy.getBuffer().setContents(workingCopyContents.toString());

			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setResolveBindings(true);
			parser.setStatementsRecovery(true);
			parser.setSource(workingCopy);

			CompilationUnit astRoot= (CompilationUnit) parser.createAST(new NullProgressMonitor());
			ASTNode newType= NodeFinder.perform(astRoot, insertPosition, dummyClassContent.length());
			if (!(newType instanceof AbstractTypeDeclaration))
				return null;

			AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) newType;
			ITypeBinding dummyTypeBinding= declaration.resolveBinding();
			if (dummyTypeBinding == null)
				return null;

			IMethodBinding[] bindings= StubUtility2.getOverridableMethods(astRoot.getAST(), dummyTypeBinding, true);
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(fSuperType.getJavaProject());

			IMethodBinding[] methodsToOverride= null;

			IType type= null;
			if (!fSuperType.isInterface() && !fSuperType.isAnnotation()) {
				IJavaElement typeElement= dummyTypeBinding.getJavaElement();
				// add extra checks here as the recovered code is fragile
				if (typeElement instanceof IType && name.equals(typeElement.getElementName()) && typeElement.exists()) {
					type= (IType) typeElement;
				}
			}

			if (type != null) {
				OverrideMethodDialog dialog= new OverrideMethodDialog(JavaPlugin.getActiveWorkbenchShell(), null, type, true);
				dialog.setGenerateComment(false);
				dialog.setElementPositionEnabled(false);
				if (dialog.open() == Window.OK) {
					Object[] selection= dialog.getResult();
					ArrayList result= new ArrayList(selection.length);
					for (int i= 0; i < selection.length; i++) {
						if (selection[i] instanceof IMethodBinding)
							result.add(selection[i]);
					}
					methodsToOverride= (IMethodBinding[]) result.toArray(new IMethodBinding[result.size()]);
					settings.createComments= dialog.getGenerateComment();
				} else {
					// cancelled
					setReplacementString(""); //$NON-NLS-1$
					setReplacementLength(0);
					return null;
				}
			} else {
				settings.createComments= false;
				List result= new ArrayList();
				for (int i= 0; i < bindings.length; i++) {
					IMethodBinding curr= bindings[i];
					if (Modifier.isAbstract(curr.getModifiers()))
						result.add(curr);
				}
				methodsToOverride= (IMethodBinding[]) result.toArray(new IMethodBinding[result.size()]);
			}
			ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
			ITrackedNodePosition trackedDeclaration= rewrite.track(declaration);

			ListRewrite rewriter= rewrite.getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
			for (int i= 0; i < methodsToOverride.length; i++) {
				IMethodBinding curr= methodsToOverride[i];
				MethodDeclaration stub= StubUtility2.createImplementationStub(workingCopy, rewrite, importRewrite, null, curr, dummyTypeBinding.getName(), settings, dummyTypeBinding.isInterface());
				rewriter.insertFirst(stub, null);
			}


			IDocument document= new Document(workingCopy.getSource());
			try {
				rewrite.rewriteAST().apply(document);

				int bodyStart= trackedDeclaration.getStartPosition() + dummyClassContent.indexOf('{');
				int bodyEnd= trackedDeclaration.getStartPosition() + trackedDeclaration.getLength();
				return document.get(bodyStart, bodyEnd - bodyStart);
			} catch (MalformedTreeException exception) {
				JavaPlugin.log(exception);
			} catch (BadLocationException exception) {
				JavaPlugin.log(exception);
			}
			return null;
		} finally {
			if (workingCopy != null)
				workingCopy.discardWorkingCopy();
		}
	}

	private Image getImageForType(IType type) {
		String imageName= JavaPluginImages.IMG_OBJS_CLASS; // default
		try {
			if (type.isAnnotation()) {
				imageName= JavaPluginImages.IMG_OBJS_ANNOTATION;
			} else if (type.isInterface()) {
				imageName= JavaPluginImages.IMG_OBJS_INTERFACE;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
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
		fImportRewrite= impRewrite;
		String replacementString= getReplacementString();

		// construct replacement text: an expression to be formatted
		StringBuffer buf= new StringBuffer("new A("); //$NON-NLS-1$
		buf.append(replacementString);

		if (!replacementString.endsWith(")")) { //$NON-NLS-1$
			buf.append(')');
		}

		String newBody= createNewBody(impRewrite);
		if (newBody == null)
			return false;

		buf.append(newBody);

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

	/*
	 * @see ICompletionProposalExtension#getContextInformationPosition()
	 * @since 3.4
	 */
	public int getContextInformationPosition() {
		if (!fIsContextInformationComputed)
			setContextInformation(computeContextInformation());
		return fContextInformationPosition;
	}


	/*
	 * @see ICompletionProposal#getContextInformation()
	 * @since 3.4
	 */
	public final IContextInformation getContextInformation() {
		if (!fIsContextInformationComputed)
			setContextInformation(computeContextInformation());
		return super.getContextInformation();
	}

	protected IContextInformation computeContextInformation() {
		try {
			ProposalInfo proposalInfo= getProposalInfo();
			fContextInformationPosition= getReplacementOffset() - 1;
			if (!(proposalInfo instanceof MemberProposalInfo))
				return null;

			CompletionProposal proposal= ((MemberProposalInfo)proposalInfo).fProposal;
			// no context information for METHOD_NAME_REF proposals (e.g. for static imports)
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=94654
			if (hasParameters() && (getReplacementString().endsWith(")") || getReplacementString().length() == 0)) { //$NON-NLS-1$
				ProposalContextInformation contextInformation= new ProposalContextInformation(proposal);
				fContextInformationPosition= getReplacementOffset() + getCursorPosition();
				if (fContextInformationPosition != 0 && proposal.getCompletion().length == 0)
					contextInformation.setContextInformationPosition(fContextInformationPosition);
				return contextInformation;
			}
			return null;
		} finally {
			fIsContextInformationComputed= true;
		}
	}

	/**
	 * Returns <code>true</code> if the method being inserted has at least one parameter. Note
	 * that this does not say anything about whether the argument list should be inserted.
	 *
	 * @return <code>true</code> if the method has any parameters, <code>false</code> if it has no parameters
	 * @since 3.4
	 */
	private boolean hasParameters() {
		ProposalInfo proposalInfo= getProposalInfo();
		if (!(proposalInfo instanceof MemberProposalInfo))
			return false;

		CompletionProposal proposal= ((MemberProposalInfo)proposalInfo).fProposal;
		return Signature.getParameterCount(proposal.getSignature()) > 0;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal#createLazyJavaTypeCompletionProposal(org.eclipse.jdt.core.CompletionProposal, org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext)
	 * @since 3.5
	 */
	protected LazyJavaCompletionProposal createRequiredTypeCompletionProposal(CompletionProposal completionProposal, JavaContentAssistInvocationContext invocationContext) {
		return new LazyJavaTypeCompletionProposal(completionProposal, invocationContext, fImportRewrite);
	}

}
