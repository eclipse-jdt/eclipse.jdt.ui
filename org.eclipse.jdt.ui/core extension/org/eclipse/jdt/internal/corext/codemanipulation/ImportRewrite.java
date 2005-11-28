/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;

/**
 * A rewriter for imports that considers the organize import 
 * settings.
 */
public final class ImportRewrite {
	
	private NewImportRewrite fNewImportRewrite;
	
	public ImportRewrite(NewImportRewrite rewrite) {
		fNewImportRewrite= rewrite;
	}

	/**
	 * Creates a import rewriter with the settings as configured in the preferences
	 * @param cu The compilation unit that contains the imports to change.
	 * @param root The compilation unit node
	 * @throws CoreException
	 */
	public ImportRewrite(ICompilationUnit cu, CompilationUnit root) throws CoreException {
		this(NewImportRewrite.create(root, true));
	}
	
	/**
	 * Creates a import rewriter with the settings as configured in the preferences
	 * @param cu The compilation unit that contains the imports to change.
	 * @throws CoreException
	 */
	public ImportRewrite(ICompilationUnit cu) throws CoreException {
		this(NewImportRewrite.create(cu, true));
	}
	
	public NewImportRewrite getNewImportRewrite() {
		return fNewImportRewrite;
	}
	
	/**
	 * @deprecated Use #createEdit(IDocument, IProgressMonitor) instead
	 */
	public final TextEdit createEdit(IDocument document) throws CoreException {
		return createEdit((IProgressMonitor) null);
	}
	
	public final TextEdit createEdit(IDocument document, IProgressMonitor monitor) throws CoreException {
		return createEdit(monitor);
	}
	
	public final TextEdit createEdit(IProgressMonitor monitor) throws CoreException {
		return fNewImportRewrite.rewriteImports(monitor);
	}
			
	public ICompilationUnit getCompilationUnit() {
		return fNewImportRewrite.getCompilationUnit();
	}
	
	/**
	 * @see ImportsStructure#setFilterImplicitImports(boolean)
	 */
	public void setFilterImplicitImports(boolean filterImplicitImports) {
		fNewImportRewrite.setFilterImplicitImports(filterImplicitImports);
	}
		
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added. This method does not correctly handle type parameters nor type variables.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 * @return Returns the simple type name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 * The type name can contain dimensions.
	 */
	public String addImport(String qualifiedTypeName) {
		return fNewImportRewrite.addImport(qualifiedTypeName);
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
		return fNewImportRewrite.addStaticImport(qualifiedName, selector, isField);
	}
	
	/**
	 * Adds a new static import declaration that is sorted in the structure using
	 * a best match algorithm. 
	 * @param binding The binding of the member to be added
	 * @return Returns the simple name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 */
	public String addStaticImport(IBinding binding) {
		return fNewImportRewrite.addStaticImport(binding);
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.  The type binding can be an array binding, type variable or wildcard.
	 * If the binding is a generic type, the type parameters are ignored. For parameterized types, also the type
	 * arguments are processed and imports added if necessary.
	 * @param binding The type binding of the type to be added
	 * @return Returns the unqualified type if the import could be added or a fully qualified type if
	 * an import conflict prevented the import. The returned string represents a type to which the type binding can
	 * be assigned or casted to. Anonymous types inside type arguments are normalized to their base type, wildcard
	 * of wildcards are ignored.
	 */
	public String addImport(ITypeBinding binding) {
		return fNewImportRewrite.addImport(binding);
	}
	
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.  The type binding can also be an array binding, type variable or wildcard.
	 * If the binding is a generic type, the type parameters are ignored. For parametrized types, also the type
	 * arguments are processed and imports added if necessary.
	 * @param binding The type binding of the type to be added
	 * @param ast The ast to create the node for
	 * @return Returns the unqualified type if the import could be added or a fully qualified type name if
	 * an import conflict prevented the import. The returned type node represents a type to which the type binding can
	 * be assigned or casted to. Anonymous types inside type arguments are normalized to their base type, wildcard of
	 * wildcards are ignored.
	 */
	public Type addImport(ITypeBinding binding, AST ast) {
		return fNewImportRewrite.addImport(binding, ast);
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.  The type signature can be an array binding, type variable or wildcard.
	 * If the type is a generic type, the type parameters are ignored. For parametrized types, also the type
	 * arguments are processed and imports added if necessary.
	 * @param typeSig The type name in signature notations (See {@link org.eclipse.jdt.core.Signature}).
	 * @param ast The ast to create the node for
	 * @return Returns the unqualified type if the import could be added or a fully qualified type name if
	 * an import conflict prevented the import. The returned type node represents a type to which the type binding can
	 * be assigned or casted to. Anonymous types inside type arguments are normalized to their base type, wildcard of
	 * wildcards are ignored.
	 */
	public Type addImportFromSignature(String typeSig, AST ast) {
		return fNewImportRewrite.addImportFromSignature(typeSig, ast);
	}

	/**
	 * Removes an import declaration if it exists. Does not touch on-demand imports.
	 * @param binding The type binding of the type to be removed as import
	 * @return Returns true if an import for the given type existed.
	 */
	public boolean removeImport(ITypeBinding binding) {
		return fNewImportRewrite.removeImport(binding.getTypeDeclaration().getQualifiedName());
	}
	
	/**
	 * Removes an import declaration for a type or an on-demand import.
	 * @param qualifiedTypeName The qualified name the type to be removed as import
	 * @return Returns true if an import for the given type existed.
	 */
	public boolean removeImport(String qualifiedTypeName) {
		return fNewImportRewrite.removeImport(qualifiedTypeName);
	}
	
	/**
	 * Removes a static import declaration for a static member or a static on-demand import.
	 * @param qualifiedName The qualified name the static member to be removed as import
	 * @return Returns true if an import for the given type existed.
	 */
	public boolean removeStaticImport(String qualifiedName) {
		return fNewImportRewrite.removeStaticImport(qualifiedName);
	}
	
	/**
	 * Returns <code>true</code> if the import edit will not change the import
	 * container; otherwise <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if the import edit will not change the import
	 * 	container; otherwise <code>false</code> is returned
	 */
	public boolean isEmpty() {
		return !fNewImportRewrite.hasRecordedChanges();
	}

	public String[] getCreatedImports() {
	    return fNewImportRewrite.getCreatedImports();
	}
	
	public String[] getCreatedStaticImports() {
	    return fNewImportRewrite.getCreatedStaticImports();
	}
	
	public String toString() {
		return fNewImportRewrite.toString();
	}
}
