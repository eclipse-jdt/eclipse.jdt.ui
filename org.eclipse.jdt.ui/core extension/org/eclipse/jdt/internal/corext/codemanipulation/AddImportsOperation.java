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
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Add imports to a compilation unit.
 * The input is an array of full qualified type names. No elimination of unnecessary
 * imports is done (use ImportStructure for this). Duplicates are not added.
 * If the compilation unit is open in an editor, be sure to pass over its working copy.
 */
public class AddImportsOperation implements IWorkspaceRunnable {
	
	public static interface IChooseImportQuery {
		/**
		 * Selects an import from a list of choices.
		 * @param openChoices Array of found types
		 * @param containerName Name type to be imported
		 * @return Returns <code>null</code> to cancel the operation, or the
		 *         selected imports.
		 */
		TypeInfo chooseImport(TypeInfo[] openChoices, String containerName);
	}
	
	private ICompilationUnit fCompilationUnit;
	private final IDocument fDocument;
	private final ITextSelection fSelection;
	private final IChooseImportQuery fQuery;
	private IStatus fStatus;
	
	
	/**
	 * Generate import statements for the passed java elements
	 * Elements must be of type IType (-> single import) or IPackageFragment
	 * (on-demand-import). Other JavaElements are ignored
	 * @param cu The compilation unit
	 * @param document Document corrsponding to the compilation unit
	 * @param selection Current test selection
	 * @param query Query element to be used fo UI interaction
	 */
	public AddImportsOperation(ICompilationUnit cu, IDocument document, ITextSelection selection, IChooseImportQuery query) {
		super();
		Assert.isNotNull(cu);
		Assert.isNotNull(document);
		Assert.isNotNull(selection);
		Assert.isNotNull(query);
		
		fCompilationUnit= cu;
		fDocument= document;
		fSelection= selection;
		fQuery= query;
		fStatus= Status.OK_STATUS;
	}
	
	/**
	 * @return Returns the status.
	 */
	public IStatus getStatus() {
		return fStatus;
	}

	/**
	 * Runs the operation.
	 * @param monitor The progress monito
	 * @throws CoreException  
	 * @throws OperationCanceledException Runtime error thrown when operation is cancelled.
	 */
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}
			
			monitor.beginTask(CodeGenerationMessages.getString("AddImportsOperation.description"), 2); //$NON-NLS-1$

			
			int nameStart= getNameStart(fDocument, fSelection.getOffset());
			int nameEnd= getNameEnd(fDocument, fSelection.getOffset() + fSelection.getLength());
			int len= nameEnd - nameStart;
			
			String name= fDocument.get(nameStart, len).trim();
			
			String simpleName= Signature.getSimpleName(name);
			String containerName= Signature.getQualifier(name);
			
			IImportDeclaration existingImport= JavaModelUtil.findImport(fCompilationUnit, simpleName);
			if (existingImport != null) {
				if (containerName.length() == 0) {
					return;
				}
				
				if (!existingImport.getElementName().equals(name)) {
					fStatus= JavaUIStatus.createError(IStatus.ERROR, CodeGenerationMessages.getFormattedString("AddImportsOperation.error.importclash", existingImport.getElementName()), null); //$NON-NLS-1$
					return;
				} else {
					removeQualification(fDocument, nameStart, containerName);
				}
				return;
			}			
			
			IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IJavaElement[] { fCompilationUnit.getJavaProject() });
			
			TypeInfo[] types= findAllTypes(simpleName, searchScope, new SubProgressMonitor(monitor, 1));
			if (types.length== 0) {
				fStatus= JavaUIStatus.createError(IStatus.ERROR, CodeGenerationMessages.getFormattedString("AddImportsOperation.error.notresolved.message", simpleName), null); //$NON-NLS-1$
				return;
			}
			
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			
			TypeInfo chosen= fQuery.chooseImport(types, containerName);
			if (chosen == null) {
				throw new OperationCanceledException();
			}

			removeQualification(fDocument, nameStart, chosen.getTypeContainerName());
		
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();

			ImportsStructure impStructure= new ImportsStructure(fCompilationUnit, settings.importOrder, settings.importThreshold, true);
			impStructure.setFindAmbiguousImports(true);
			impStructure.addImport(chosen.getFullyQualifiedName(), false);
			impStructure.create(false, new SubProgressMonitor(monitor, 1));
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		} finally {
			monitor.done();
		}
	}
	
	private int getNameStart(IDocument doc, int pos) throws BadLocationException {
		while (pos > 0) {
			char ch= doc.getChar(pos - 1);
			if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
				return pos;
			}
			pos--;
		}
		return pos;
	}
	
	private int getNameEnd(IDocument doc, int pos) throws BadLocationException {
		if (pos > 0) {
			if (Character.isWhitespace(doc.getChar(pos - 1))) {
				return pos;
			}
		}
		int len= doc.getLength();
		while (pos < len) {
			char ch= doc.getChar(pos);
			if (!Character.isJavaIdentifierPart(ch)) {
				return pos;
			}
			pos++;
		}
		return pos;
	}
	
	private void removeQualification(IDocument doc, int nameStart, String containerName) throws BadLocationException {
		int containerLen= containerName.length();
		int docLen= doc.getLength();
		if ((containerLen > 0) && (nameStart + containerLen + 1 < docLen)) {
			for (int k= 0; k < containerLen; k++) {
				if (doc.getChar(nameStart + k) != containerName.charAt(k)) {
					return;
				}
			}
			if (doc.getChar(nameStart + containerLen) == '.') {
				doc.replace(nameStart, containerLen + 1, ""); //$NON-NLS-1$
			}
		}
	}
	
	/*
	 * Finds a type by the simple name.
	 */
	private static TypeInfo[] findAllTypes(String simpleTypeName, IJavaSearchScope searchScope, IProgressMonitor monitor) {
		return AllTypesCache.getTypesForName(simpleTypeName, searchScope, monitor);
	}
	
	
	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
		
}
