/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.actions.OverrideMethodsAction.OverrideTreeSelectionDialog;
import org.eclipse.jdt.ui.actions.OverrideMethodsAction.OverrideMethodSorter;
import org.eclipse.jdt.ui.actions.OverrideMethodsAction.OverrideMethodValidator;
import org.eclipse.jdt.ui.actions.OverrideMethodsAction.OverrideMethodContentProvider;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
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
			return JavaModelUtil.findType(project, typeName);
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
	
	private OverrideTreeSelectionDialog newOverrideTreeSelectionDialog(Shell shell, IMethod[] methods, IMethod[] defaultSelected, ITypeHierarchy typeHierarchy) {
		  HashSet types= new HashSet(methods.length);
		  for (int i= 0; i < methods.length; i++) {
			  types.add(methods[i].getDeclaringType());
		  }
		  Object[] typesArrays= types.toArray();
		  ViewerSorter sorter= new OverrideMethodSorter(typeHierarchy);
		  sorter.sort(null, typesArrays);

		  JavaElementLabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		  OverrideMethodContentProvider contentProvider = new OverrideMethodContentProvider(methods, typesArrays);			
		
		  HashSet expanded= new HashSet(defaultSelected.length); 
		  for (int i= 0; i < defaultSelected.length; i++) {
			  expanded.add(defaultSelected[i].getDeclaringType());
		  }
		  
		  OverrideTreeSelectionDialog dialog= new OverrideTreeSelectionDialog(shell, contentProvider, labelProvider, null, typeHierarchy.getType());
		  if (expanded.isEmpty() && typesArrays.length > 0) {
			  expanded.add(typesArrays[0]);
		  }
				
		  dialog.setValidator(new OverrideMethodValidator(methods.length));
		  dialog.setTitle(ActionMessages.getString("OverrideMethodDialog.dialog.title")); //$NON-NLS-1$
		  dialog.setInitialSelections(defaultSelected);
		  dialog.setExpandedElements(expanded.toArray());
		  dialog.setContainerMode(true);
		  dialog.setSorter(sorter);
		  dialog.setSize(60, 18);			
		  dialog.setInput(new Object());
		  dialog.setMessage(null);
		  return dialog;
	}
	
	private boolean createStubs(StringBuffer buf, ImportsStructure imports) throws CoreException {
		if (fDeclaringType == null) {
			return true;
		}
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();

		ITypeHierarchy hierarchy= fDeclaringType.newSupertypeHierarchy(null);
		IType type = fDeclaringType;
		IMethod[] inheritedMethods= StubUtility.getOverridableMethods(type, hierarchy, true);
				
		List toImplement= new ArrayList();
		for (int i1= 0; i1 < inheritedMethods.length; i1++) {
			IMethod curr= inheritedMethods[i1];
			if (JdtFlags.isAbstract(curr)) {
				toImplement.add(curr);
			}
		}		
		IMethod[] toImplementArray= (IMethod[]) toImplement.toArray(new IMethod[toImplement.size()]);		
		if (fDeclaringType.isClass()) {
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			OverrideTreeSelectionDialog selectionDialog= newOverrideTreeSelectionDialog(shell, inheritedMethods, toImplementArray, hierarchy);
			int selectionResult= selectionDialog.open();
			if (selectionResult == OverrideTreeSelectionDialog.OK) {
				Object[] checkedElements= selectionDialog.getResult();
				if (checkedElements != null) {
					ArrayList result= new ArrayList(checkedElements.length);
					for (int i= 0; i < checkedElements.length; i++) {
						Object curr= checkedElements[i];
						if (curr instanceof IMethod) {
							result.add(curr);
						}
					}
					toImplementArray= (IMethod[]) result.toArray(new IMethod[result.size()]);
				}
			}
			settings.createComments= selectionDialog.getGenerateComment();
		}
		
		String[] unimplemented= StubUtility.genOverrideStubs(toImplementArray, type, hierarchy, settings, imports);
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

