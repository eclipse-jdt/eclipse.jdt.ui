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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility.GenStubSettings;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class OverrideCompletionProposal extends JavaTypeCompletionProposal {
	
	private String fTypeName;
	private String fMethodName;
	private String[] fParamTypes;
	private IJavaProject fJavaProject;

	public OverrideCompletionProposal(IJavaProject jproject, ICompilationUnit cu, String declaringTypeName, String methodName, String[] paramTypes, int start, int length, String displayName, String completionProposal) {
		super(completionProposal, cu, start, length, null, displayName, 0);
		Assert.isNotNull(jproject);
		Assert.isNotNull(methodName);
		Assert.isNotNull(declaringTypeName);
		Assert.isNotNull(paramTypes);

		fTypeName= declaringTypeName;
		fParamTypes= paramTypes;
		fMethodName= methodName;

		fJavaProject= jproject;
	}

	/* (non-Javadoc)
	 * @see JavaTypeCompletionProposal#updateReplacementString(IDocument, char, int, ImportsStructure)
	 */
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportsStructure impStructure) throws CoreException, BadLocationException {

	
		IType definingType= null;
		if (impStructure != null) {
			IJavaElement currElem= impStructure.getCompilationUnit().getElementAt(offset);
			if (currElem != null) {
				definingType= (IType) currElem.getAncestor(IJavaElement.TYPE);
			}
		}
		
		IMethod method= null;
		if (definingType != null) {
			ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(definingType);
			method= JavaModelUtil.findMethodDefininition(hierarchy, definingType, fMethodName, fParamTypes, false, true);
		} else {
			IType declaringType= JavaModelUtil.findType(fJavaProject, fTypeName);
			if (declaringType != null) {
				method= JavaModelUtil.findMethod(fMethodName, fParamTypes, false, declaringType);
			}
		}
		
		if (method != null) {
			GenStubSettings settings= new GenStubSettings(JavaPreferencesSettings.getCodeGenerationSettings(fJavaProject));
			int flags= method.getFlags();

			settings.noBody= (definingType != null) && definingType.isInterface();
			settings.callSuper= !JdtFlags.isAbstract(method) && !Flags.isStatic(flags);
			settings.methodOverwrites= !Flags.isStatic(flags);
			settings.methodModifiers= flags;
			
			String definingTypeName= (definingType != null) ? definingType.getElementName() : ""; //$NON-NLS-1$
			
			String stub= StubUtility.genStub(fCompilationUnit, definingTypeName, method, method.getDeclaringType(), settings, impStructure);

			// use the code formatter
			String lineDelim= StubUtility.getLineDelimiterFor(document);
			IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
			int lineStart= region.getOffset();
			int indent= Strings.computeIndent(document.get(lineStart, getReplacementOffset() - lineStart), settings.tabWidth);

			String replacement= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, stub, indent, null, lineDelim, fJavaProject);
			
			if (replacement.endsWith(lineDelim)) {
				replacement= replacement.substring(0, replacement.length() - lineDelim.length());
			}
			
			setReplacementString(Strings.trimLeadingTabsAndSpaces(replacement));
			return true;
		}

		return false;
	}
	
	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getPrefixCompletionText(org.eclipse.jface.text.IDocument, int)
	 */
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return fMethodName;
	}
	
}

