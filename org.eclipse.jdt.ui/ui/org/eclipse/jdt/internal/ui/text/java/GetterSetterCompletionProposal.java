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

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class GetterSetterCompletionProposal extends JavaTypeCompletionProposal {
	
	
	public static void evaluateProposals(IType type, String prefix, int offset, int length, int relevance, Set suggestedMethods, Collection result) throws CoreException {
		if (prefix.length() == 0) {
			relevance--;
		}
		
		// make sure we get all the fields
		ICompilationUnit cu= type.getCompilationUnit();
		if (cu != null && !cu.isConsistent()) {
			JavaModelUtil.reconcile(cu);
		}
		IField[] fields= type.getFields();
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < fields.length; i++) {
			IField curr= fields[i];
			String getterName= GetterSetterUtil.getGetterName(curr, null);
			if (getterName.startsWith(prefix) && !hasMethod(methods, getterName) && suggestedMethods.add(getterName)) {
				result.add(new GetterSetterCompletionProposal(curr, offset, length, true, relevance));
			}
				
			String setterName= GetterSetterUtil.getSetterName(curr, null);
			if (setterName.startsWith(prefix) && !hasMethod(methods, setterName) && suggestedMethods.add(setterName)) {
				result.add(new GetterSetterCompletionProposal(curr, offset, length, false, relevance));
			}
		}
	}
	
	private static boolean hasMethod(IMethod[] methods, String name) {
		for (int i= 0; i < methods.length; i++) {
			if (methods[i].getElementName().equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	private final IField fField;
	private final boolean fIsGetter;

	public GetterSetterCompletionProposal(IField field, int start, int length, boolean isGetter, int relevance) throws JavaModelException {
		super("", field.getCompilationUnit(), start, length, JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC), getDisplayName(field, isGetter), relevance); //$NON-NLS-1$
		Assert.isNotNull(field);
		
		fField= field;
		fIsGetter= isGetter;
		setProposalInfo(new ProposalInfo(field));
	}
	
	private static String getDisplayName(IField field, boolean isGetter) throws JavaModelException {
		StringBuffer buf= new StringBuffer();
		if (isGetter) {
			buf.append(GetterSetterUtil.getGetterName(field, null));
			buf.append("()  "); //$NON-NLS-1$
			buf.append(Signature.toString(field.getTypeSignature()));
			buf.append(" - "); //$NON-NLS-1$
			buf.append(JavaTextMessages.getFormattedString("GetterSetterCompletionProposal.getter.label", field.getElementName())); //$NON-NLS-1$
		} else {
			buf.append(GetterSetterUtil.getSetterName(field, null));
			buf.append('(').append(Signature.toString(field.getTypeSignature())).append(')');
			buf.append("  "); //$NON-NLS-1$
			buf.append(Signature.toString(Signature.SIG_VOID));
			buf.append(" - "); //$NON-NLS-1$
			buf.append(JavaTextMessages.getFormattedString("GetterSetterCompletionProposal.setter.label", field.getElementName())); //$NON-NLS-1$
		}
		return buf.toString();
	}

	/* (non-Javadoc)
	 * @see JavaTypeCompletionProposal#updateReplacementString(IDocument, char, int, ImportsStructure)
	 */
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportsStructure impStructure) throws CoreException, BadLocationException {
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		boolean addComments= settings.createComments;
		int flags= Flags.AccPublic | (fField.getFlags() & Flags.AccStatic);
		
		String stub;
		if (fIsGetter) {
			String getterName= GetterSetterUtil.getGetterName(fField, null);
			stub= GetterSetterUtil.getGetterStub(fField, getterName, addComments, flags);
		} else {
			String setterName= GetterSetterUtil.getSetterName(fField, null);
			stub= GetterSetterUtil.getSetterStub(fField, setterName, addComments, flags);
		}
		
		// use the code formatter
		String lineDelim= StubUtility.getLineDelimiterFor(document);
		
		IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
		int lineStart= region.getOffset();
		int indent= Strings.computeIndent(document.get(lineStart, getReplacementOffset() - lineStart), settings.tabWidth);

		String replacement= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, stub, indent, null, lineDelim, fField.getJavaProject());
		
		if (replacement.endsWith(lineDelim)) {
			replacement= replacement.substring(0, replacement.length() - lineDelim.length());
		}
		
		setReplacementString(Strings.trimLeadingTabsAndSpaces(replacement));
		return true;
	}
}

