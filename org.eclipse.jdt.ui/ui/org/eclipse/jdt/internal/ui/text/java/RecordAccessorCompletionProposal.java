/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.viewers.StyledString;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.manipulation.CodeGeneration;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;


/**
 * Record accessor declaration proposal.
 */
public class RecordAccessorCompletionProposal extends JavaTypeCompletionProposal {


	public static void evaluateProposals(IType type, String prefix, int offset, int length, int relevance, Set<String> suggestedMethods, Collection<IJavaCompletionProposal> result) throws CoreException {
		IMethod[] methods= type.getMethods();
		if (!type.isRecord()) {
			return;
		}
		if (prefix.length() > 0 && !hasMethod(methods, prefix)) {
			if (!JavaConventionsUtil.validateMethodName(prefix, type).matches(IStatus.ERROR)) {
				IField[] fields= type.getRecordComponents();
				for (IField field : fields) {
					if (field.getElementName().startsWith(prefix)) {
						if (suggestedMethods.add(field.getElementName())) {
							result.add(new RecordAccessorCompletionProposal(type, field.getElementName(), field.getTypeSignature(), offset, length, relevance + 1));
						}
					}
				}
			}
		}
	}

	private static boolean hasMethod(IMethod[] methods, String name) {
		for (IMethod curr : methods) {
			if (curr.getElementName().equals(name) && curr.getParameterTypes().length == 0) {
				return true;
			}
		}
		return false;
	}

	private final IType fType;
	private final String fReturnTypeSig;
	private final String fMethodName;

	public RecordAccessorCompletionProposal(IType type, String methodName, String returnTypeSig, int start, int length, int relevance) {
		super("", type.getCompilationUnit(), start, length, null, getDisplayName(methodName, returnTypeSig), relevance); //$NON-NLS-1$
		Assert.isNotNull(type);
		Assert.isNotNull(methodName);
		Assert.isNotNull(returnTypeSig);

		fType= type;
		fMethodName= methodName;
		fReturnTypeSig= returnTypeSig;

		setImage(JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC));
	}

	private static StyledString getDisplayName(String methodName, String returnTypeSig) {
		StyledString buf= new StyledString();
		buf.append(methodName);
		buf.append('(');
		buf.append(')');
		buf.append(" : "); //$NON-NLS-1$
		buf.append(Signature.toString(returnTypeSig));
		buf.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
		buf.append(JavaTextMessages.RecordAccessorCompletionProposal_accessor_label, StyledString.QUALIFIER_STYLER);
		return buf;
	}

	@Override
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportRewrite impRewrite) throws CoreException, BadLocationException {

		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(fType.getJavaProject());
		boolean addComments= settings.createComments;

		String[] empty= new String[0];
		String lineDelim= TextUtilities.getDefaultLineDelimiter(document);
		String declTypeName= fType.getTypeQualifiedName('.');

		StringBuilder buf= new StringBuilder();
		if (addComments) {
			String comment= CodeGeneration.getMethodComment(fType.getCompilationUnit(), declTypeName, fMethodName, empty, empty, fReturnTypeSig, empty, null, lineDelim);
			if (comment != null) {
				buf.append(comment);
				buf.append(lineDelim);
			}
		}
		buf.append("public "); //$NON-NLS-1$

		if (fReturnTypeSig != null) {
			buf.append(Signature.toString(fReturnTypeSig));
		}
		buf.append(' ');
		buf.append(fMethodName);
		buf.append("() {"); //$NON-NLS-1$
		buf.append(lineDelim);

		String returnStatement= "return " + fMethodName + ";"; //$NON-NLS-1$ //$NON-NLS-2$
		buf.append(returnStatement);
		buf.append(lineDelim);
		buf.append("}"); //$NON-NLS-1$
		buf.append(lineDelim);
		String stub=  buf.toString();

		// use the code formatter
		IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
		int lineStart= region.getOffset();
		int indent= Strings.computeIndentUnits(document.get(lineStart, getReplacementOffset() - lineStart), settings.tabWidth, settings.indentWidth);

		String replacement= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, stub, indent, lineDelim, FormatterProfileManager.getProjectSettings(fType.getJavaProject()));

		if (replacement.endsWith(lineDelim)) {
			replacement= replacement.substring(0, replacement.length() - lineDelim.length());
		}

		setReplacementString(Strings.trimLeadingTabsAndSpaces(replacement));
		return true;
	}

	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return ""; // don't let method stub proposals complete incrementally //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4#isAutoInsertable()
	 */
	@Override
	public boolean isAutoInsertable() {
		return false;
	}

}
