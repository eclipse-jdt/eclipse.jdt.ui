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
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility.GenStubSettings;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

/**
 * Creates a custom constructor with fields initialized.
 * If the type is open in an editor, be sure to pass over the types working working copy.
 */
public class AddCustomConstructorOperation implements IWorkspaceRunnable {
	private IJavaElement fInsertPosition;
	private IField[] fSelected;
	private IType fType;
	private IMethod fConstructorCreated;
	private boolean fDoSave;
	private CodeGenerationSettings fSettings;
	private IMethod fSuperConstructor;
	private boolean fOmitSuper;
	private int fVisibility;
	
	public AddCustomConstructorOperation(IType type, CodeGenerationSettings settings, IField[] selected, boolean save, IJavaElement insertPosition, IMethod superConstructor) {
		super();
		fType= type;
		fDoSave= save;
		fConstructorCreated= null;
		fSettings= settings;
		fSelected= selected;
		fInsertPosition= insertPosition;
		fSuperConstructor= superConstructor;
		fVisibility= 0;
		fOmitSuper= false;
	}

	/**
	 * Runs the operation.
	 * @throws OperationCanceledException Runtime error thrown when operation is cancelled.
	 */
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		// Only calculating one constructor here
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}			
			monitor.setTaskName(CodeGenerationMessages.getString("AddCustomConstructorOperation.description")); //$NON-NLS-1$
			monitor.beginTask("", 7); //$NON-NLS-1$			
			monitor.worked(1);
			
			ImportsStructure imports= new ImportsStructure(fType.getCompilationUnit(), fSettings.importOrder, fSettings.importThreshold, true);
			ITypeHierarchy hierarchy= fType.newSupertypeHierarchy(new SubProgressMonitor(monitor, 1));
			IJavaProject project= fType.getJavaProject();
			String[] superConstructorParamNames= StubUtility.suggestArgumentNames(project, fSuperConstructor.getParameterNames());

			String defaultConstructor= genOverrideConstructorStub(fSuperConstructor, superConstructorParamNames, fType, hierarchy, fSettings, imports, fOmitSuper);					
			int closingBraceIndex= defaultConstructor.lastIndexOf('}'); //$NON-NLS-1$

			IScanner scanner= ToolFactory.createScanner(true, false, false, false);
			scanner.setSource(defaultConstructor.toCharArray());
			TokenScanner tokenScanner= new TokenScanner(scanner);
			int closingParenIndex= tokenScanner.getTokenStartOffset(ITerminalSymbols.TokenNameRPAREN, 0);

			monitor.worked(1);

			String[] params= new String[fSelected.length];					
			String[] excludedNames= new String[superConstructorParamNames.length + params.length];
			for (int i= 0; i < excludedNames.length; i++) {
				excludedNames[i]= ""; //$NON-NLS-1$
			}
			
			ArrayList superNames= new ArrayList(superConstructorParamNames.length);
			for (int i= 0; i < superConstructorParamNames.length; i++) {
				superNames.add(superConstructorParamNames[i]);
				excludedNames[i]= superConstructorParamNames[i];
			}
			
			StringBuffer buf= new StringBuffer(defaultConstructor.substring(0, closingParenIndex));
										
			monitor.worked(1);
				
			for (int i= 0, k= superConstructorParamNames.length; i < fSelected.length; i++, k++) {
				if (k > 0) {
					buf.append(", "); //$NON-NLS-1$	
				}
				// http://bugs.eclipse.org/bugs/show_bug.cgi?id=42341
				if (Flags.isFinal(fSelected[i].getFlags()))
					buf.append("final "); //$NON-NLS-1$
				buf.append(Signature.toString(fSelected[i].getTypeSignature()));				
				buf.append(' ');
				String accessName= NamingConventions.removePrefixAndSuffixForFieldName(project, fSelected[i].getElementName(), fSelected[i].getFlags());
				// Allow no collisions with super constructor parameter names
				String paramName= StubUtility.suggestArgumentName(project, accessName, excludedNames);

				excludedNames[k]= new String(paramName);
				params[i]= paramName;
				buf.append(paramName);		
			}
			monitor.worked(1);
			
			buf.append(defaultConstructor.substring(closingParenIndex, closingBraceIndex));
				
			for (int i= 0; i < fSelected.length; i++) {
				String fieldName= fSelected[i].getElementName();
				boolean isStatic= Flags.isStatic(fSelected[i].getFlags());
				if (isStatic) {
					buf.append(fSelected[i].getParent().getElementName());
					buf.append("."); //$NON-NLS-1$
				}	
				else if ((fSettings.useKeywordThis) || params[i].equals(fieldName) || superNames.contains(fieldName))
					buf.append("this."); //$NON-NLS-1$				
				buf.append(fieldName);
				buf.append(" = "); //$NON-NLS-1$
				buf.append(params[i]);
				buf.append(";\n"); //$NON-NLS-1$
			}
			buf.append("}"); //$NON-NLS-1$
			monitor.worked(1);
			
			// calculate the javadoc comment here now that we can be sure of the @param values
			if (fSettings.createComments) {
				String[] javadocParams= new String[superConstructorParamNames.length + params.length];
				int count= 0;
				for (int i= 0; i < fSuperConstructor.getNumberOfParameters(); i++) {
					javadocParams[count++]= superConstructorParamNames[i];
				}
				for (int i= 0; i < params.length; i++) {
					javadocParams[count++]= params[i];
				}				
				String lineDelimiter= String.valueOf('\n');
				String comment= StubUtility.getMethodComment(fType.getCompilationUnit(), fType.getElementName(), fSuperConstructor.getElementName(), javadocParams, fSuperConstructor.getExceptionTypes(), null, null, lineDelimiter);		
				if (comment != null)
					buf.insert(0, comment);
				 else {
					buf.append("/**").append(lineDelimiter); //$NON-NLS-1$
					buf.append(" *").append(lineDelimiter); //$NON-NLS-1$
					buf.append(" */").append(lineDelimiter); //$NON-NLS-1$							
				}				
			}

			String lineDelim= StubUtility.getLineDelimiterUsed(fType);
			int indent= StubUtility.getIndentUsed(fType) + 1;
			
			String formattedContent= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, buf.toString(), indent, null, lineDelim, null) + lineDelim;
			fConstructorCreated= fType.createMethod(formattedContent, fInsertPosition, true, null);

			monitor.worked(1);		
			imports.create(fDoSave, null);
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}

	private String genOverrideConstructorStub(IMethod constructorToImplement, String[] constructorParamNames, IType type, ITypeHierarchy hierarchy, CodeGenerationSettings settings, IImportsStructure imports, boolean omitSuper) throws CoreException {
		GenStubSettings genStubSettings= new GenStubSettings(settings);
		genStubSettings.methodOverwrites= true;
		genStubSettings.methodModifiers= fVisibility | JdtFlags.clearAccessModifiers(constructorToImplement.getFlags());
		if (omitSuper)
			genStubSettings.callSuper= false;
		else
			genStubSettings.callSuper= true;

		IMethod overrides= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, type, constructorToImplement.getElementName(), constructorToImplement.getParameterTypes(), constructorToImplement.isConstructor());
		if (overrides != null) {
			constructorToImplement= overrides;
		}
		IMethod desc= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, constructorToImplement.getElementName(), constructorToImplement.getParameterTypes(), constructorToImplement.isConstructor());
		if (desc == null) {
			desc= constructorToImplement;
		}
		
		String methName= constructorToImplement.getElementName();
		String bodyStatement= StubUtility.getDefaultMethodBodyStatement(methName, constructorParamNames, null, genStubSettings.callSuper);			

		StringBuffer buf= new StringBuffer();
		StubUtility.genMethodDeclaration(type.getElementName(), constructorToImplement, genStubSettings.methodModifiers, bodyStatement, imports, buf);
		return buf.toString();
	}
	
	/**
	 * Returns the created constructor. To be called after a sucessful run.
	 */	
	public IMethod getCreatedConstructor() {
		return fConstructorCreated;
	}	
	
	public void setOmitSuper(boolean callSuper) {
		fOmitSuper= callSuper;
	}

	public int getVisbility() {
		return fVisibility;
	}
		
	public void setVisbility(int visbility) {
		fVisibility= visbility;
	}
}
