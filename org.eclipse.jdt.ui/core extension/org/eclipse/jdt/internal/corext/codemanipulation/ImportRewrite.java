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

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * A rewriter for imports that considers the organize import 
 * settings.
 */
public final class ImportRewrite {
	
	private ImportsStructure fImportsStructure;
	
	private ImportRewrite(ICompilationUnit cu, String[] preferenceOrder, int importThreshold) throws CoreException {
		Assert.isNotNull(cu);
		Assert.isNotNull(preferenceOrder);
		fImportsStructure= new ImportsStructure(cu, preferenceOrder, importThreshold, true);
	}
	
	/**
	 * Creates a import rewriter with the settings as configured in the preferences
	 * @param cunit The compilation unit that contains the imports to change.
	 * @throws CoreException
	 */
	public ImportRewrite(ICompilationUnit cunit) throws CoreException {
		this(cunit, JavaPreferencesSettings.getImportOrderPreference(cunit.getJavaProject()), JavaPreferencesSettings.getImportNumberThreshold(cunit.getJavaProject()));
	}
	
	public final TextEdit createEdit(IDocument document) throws CoreException {
		return createEdit(document, null);
	}
	
	public final TextEdit createEdit(IDocument document, IProgressMonitor monitor) throws CoreException {
		try {
			return fImportsStructure.getResultingEdits(document, monitor);
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		}
	}
			
	public ICompilationUnit getCompilationUnit() {
		return fImportsStructure.getCompilationUnit();
	}
	
	/**
	 * @see ImportsStructure#setFilterImplicitImports(boolean)
	 */
	public void setFilterImplicitImports(boolean filterImplicitImports) {
		fImportsStructure.setFilterImplicitImports(filterImplicitImports);
	}
	
	/**
	 * @see ImportsStructure#setFindAmbiguousImports(boolean)
	 */
	public void setFindAmbiguosImports(boolean findAmbiguosImports) {
		fImportsStructure.setFindAmbiguousImports(findAmbiguosImports);
	}	
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 * @return Returns the simple type name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 * The type name can contain dimensions.
	 */
	public String addImport(String qualifiedTypeName) {
		return fImportsStructure.addImport(qualifiedTypeName);
	}
	
	/**
	 * Adds a new static import declaration that is sorted in the structure using
	 * a best match algorithm. 
	 * @param qualifiedName The name of the static member type
	 * 	@param selector The name of the static member
	 * @return Returns the simple type name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 * The type name can contain dimensions.
	 */
	public String addStaticImport(String qualifiedName, String selector, boolean isField) {
		return fImportsStructure.addStaticImport(qualifiedName, selector, isField);
	}
	
	/**
	 * Adds a new static import declaration that is sorted in the structure using
	 * a best match algorithm. 
	 * @param binding The binding of the member to be added
	 * @return Returns the simple name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 */
	public String addStaticImport(IBinding binding) {
		return fImportsStructure.addStaticImport(binding);
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.  The type binding can be an array binding. No import is added for unnamed
	 * types (local or anonymous types)
	 * @param binding The type binding of the type to be added
	 * @return Returns the simple type name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 */
	public String addImport(ITypeBinding binding) {
		return fImportsStructure.addImport(binding);
	}
	
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.  The type binding can be an array binding. No import is added for unnamed
	 * types (local or anonymous types)
	 * @param binding The type binding of the type to be added
	 * @param ast The ast to create the node for
	 * @return Returns the simple type name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 */
	public Type addImport(ITypeBinding binding, AST ast) {
		return fImportsStructure.addImport(binding, ast);
	}
	
	/**
	 * Looks if there already is single import for the given name.
	 * @param simpleName The simple name to find
	 * @return Returns the qualified import name or <code>null</code>.
	 */	
	public String findImport(String simpleName) {
		return fImportsStructure.findImport(simpleName);
	}
	

	/**
	 * Removes an import declaration if it exists. Does not touch on-demand imports.
	 * @param binding The type binding of the type to be removed as import
	 * @return Returns true if an import for the given type existed.
	 */
	public boolean removeImport(ITypeBinding binding) {
		return fImportsStructure.removeImport(binding);
	}
	
	/**
	 * Removes an import declaration for a type or an on-demand import.
	 * @param qualifiedTypeName The qualified name the type to be removed as import
	 * @return Returns true if an import for the given type existed.
	 */
	public boolean removeImport(String qualifiedTypeName) {
		return fImportsStructure.removeImport(qualifiedTypeName);
	}
	
	/**
	 * Removes a static import declaration for a static member or a static on-demand import.
	 * @param qualifiedName The qualified name the static member to be removed as import
	 * @return Returns true if an import for the given type existed.
	 */
	public boolean removeStaticImport(String qualifiedName) {
		return fImportsStructure.removeStaticImport(qualifiedName);
	}
	
	/**
	 * Returns <code>true</code> if the import edit will not change the import
	 * container; otherwise <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if the import edit will not change the import
	 * 	container; otherwise <code>false</code> is returned
	 */
	public boolean isEmpty() {
		return !fImportsStructure.hasChanges();
	}
}

