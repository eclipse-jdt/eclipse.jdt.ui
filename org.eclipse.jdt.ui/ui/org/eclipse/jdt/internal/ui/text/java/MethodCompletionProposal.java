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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class MethodCompletionProposal extends JavaTypeCompletionProposal {
	
	
	public static void evaluateProposals(IType type, String prefix, int offset, int length, int relevance, Collection result) throws CoreException {
		IMethod[] methods= type.getMethods();
		String constructorName= type.getElementName();
		if (constructorName.length() > 0 && constructorName.startsWith(prefix) && !hasMethod(methods, constructorName)) {
			result.add(new MethodCompletionProposal(type, constructorName, null, offset, length, relevance));
		}
		
		if (prefix.length() > 0 && !constructorName.equals(prefix) && !hasMethod(methods, prefix)) {
			result.add(new MethodCompletionProposal(type, prefix, Signature.SIG_VOID, offset, length, relevance));
		}
	}
	
	private static boolean hasMethod(IMethod[] methods, String name) {
		for (int i= 0; i < methods.length; i++) {
			IMethod curr= methods[i];
			if (curr.getElementName().equals(name) && curr.getParameterTypes().length == 0) {
				return true;
			}
		}
		return false;
	}
	
	private final IType fType;
	private final String fReturnTypeSig;
	private final String fMethodName;

	public MethodCompletionProposal(IType type, String methodName, String returnTypeSig, int start, int length, int relevance) {
		super("", type.getCompilationUnit(), start, length, null, getDisplayName(methodName, returnTypeSig), relevance); //$NON-NLS-1$
		Assert.isNotNull(type);
		Assert.isNotNull(methodName);
		
		fType= type;
		fMethodName= methodName;
		fReturnTypeSig= returnTypeSig;
		
		if (returnTypeSig == null) {
			setProposalInfo(new ProposalInfo(type));

			ImageDescriptor desc= new JavaElementImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE);
			setImage(JavaPlugin.getImageDescriptorRegistry().get(desc));
		} else {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_MISC_PRIVATE));
		}
	}
	
	private static String getDisplayName(String methodName, String returnTypeSig) {
		StringBuffer buf= new StringBuffer();
		buf.append(methodName);
		buf.append('(');
		buf.append(')');
		if (returnTypeSig != null) {
			buf.append("  "); //$NON-NLS-1$
			buf.append(Signature.toString(returnTypeSig));
			buf.append(" - "); //$NON-NLS-1$
			buf.append(JavaTextMessages.getString("MethodCompletionProposal.method.label")); //$NON-NLS-1$
		} else {
			buf.append(" - "); //$NON-NLS-1$
			buf.append(JavaTextMessages.getString("MethodCompletionProposal.constructor.label")); //$NON-NLS-1$
		}
		return buf.toString();
	}

	/* (non-Javadoc)
	 * @see JavaTypeCompletionProposal#updateReplacementString(IDocument, char, int, ImportsStructure)
	 */
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportsStructure impStructure) throws CoreException, BadLocationException {
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		boolean addComments= settings.createComments;

		String[] empty= new String[0];
		String lineDelim= StubUtility.getLineDelimiterFor(document);
		String declTypeName= fType.getTypeQualifiedName('.');

		StringBuffer buf= new StringBuffer();
		if (addComments) {
			String comment= CodeGeneration.getMethodComment(fType.getCompilationUnit(), declTypeName, fMethodName, empty, empty, fReturnTypeSig, null, String.valueOf('\n'));
			if (comment != null) {
				buf.append(comment);
				buf.append('\n');
			}					
		}
		if (fReturnTypeSig != null) {
			buf.append("private "); //$NON-NLS-1$
		} else {
			buf.append("public "); //$NON-NLS-1$
		}
		
		if (fReturnTypeSig != null) {
			buf.append(Signature.toString(fReturnTypeSig));
		}
		buf.append(' ');
		buf.append(fMethodName);
		buf.append("() {\n"); //$NON-NLS-1$
	
		String body= CodeGeneration.getMethodBodyContent(fType.getCompilationUnit(), declTypeName, fMethodName, fReturnTypeSig == null, "", String.valueOf('\n')); //$NON-NLS-1$
		if (body != null) {
			buf.append(body);
			buf.append('\n');
		}
		buf.append("}\n"); //$NON-NLS-1$
		String stub=  buf.toString(); 
		
		// use the code formatter
		IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
		int lineStart= region.getOffset();
		int indent= Strings.computeIndent(document.get(lineStart, getReplacementOffset() - lineStart), settings.tabWidth);

		String replacement= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, stub, indent, null, lineDelim, fType.getJavaProject());
		
		if (replacement.endsWith(lineDelim)) {
			replacement= replacement.substring(0, replacement.length() - lineDelim.length());
		}
		
		setReplacementString(Strings.trimLeadingTabsAndSpaces(replacement));
		return true;
	}
}

