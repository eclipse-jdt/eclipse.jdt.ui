/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OverrideMethodQuery;
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
	
	private IType getDeclaringType(IJavaProject project, String typeName) {
		try {
			return project.findType(typeName);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see JavaTypeCompletionProposal#updateReplacementString(char, int, ImportsStructure)
	 */
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportsStructure impStructure) throws CoreException, BadLocationException {
		String replacementString= getReplacementString();
		
		// construct replacement text
		StringBuffer buf= new StringBuffer();
		buf.append(replacementString);
		
		if (!replacementString.endsWith(")")) { //$NON-NLS-1$
			buf.append(')');
		}	
		buf.append(" {\n"); //$NON-NLS-1$
		if (!createStubs(buf, impStructure)) {
			return false;
		}
		buf.append("}"); //$NON-NLS-1$
		
		// use the code formatter
		String lineDelim= StubUtility.getLineDelimiterFor(document);
		int tabWidth= CodeFormatterUtil.getTabWidth();
		IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
		int indent= Strings.computeIndent(document.get(region.getOffset(), region.getLength()), tabWidth);
		
		String replacement= StubUtility.codeFormat(buf.toString(), indent, lineDelim);
		setReplacementString(Strings.trimLeadingTabsAndSpaces(replacement));
		int pos= offset;
		while (pos < document.getLength() && Character.isWhitespace(document.getChar(pos))) {
			pos++;
		}
		if (pos < document.getLength() && document.getChar(pos) == ')') {
			setReplacementLength(pos - offset + 1);
		}
		return true;
	}	
	
	private boolean createStubs(StringBuffer buf, ImportsStructure imports) throws CoreException {
		if (fDeclaringType == null) {
			return true;
		}
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		settings.createComments= false;

		ITypeHierarchy hierarchy= fDeclaringType.newSupertypeHierarchy(null);
		OverrideMethodQuery selectionQuery= fDeclaringType.isClass() ? new OverrideMethodQuery(JavaPlugin.getActiveWorkbenchShell(), true) : null;
		String[] unimplemented= StubUtility.evalUnimplementedMethods(fDeclaringType, hierarchy, true, settings, selectionQuery, imports);
		if (unimplemented != null) {
			for (int i= 0; i < unimplemented.length; i++) {
				buf.append(unimplemented[i]);
				if (i < unimplemented.length - 1) {
					buf.append('\n');
				}
			}
			return true;
		}
		return false;
	}

}

