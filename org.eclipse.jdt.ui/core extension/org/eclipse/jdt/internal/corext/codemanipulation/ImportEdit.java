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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

/**
 * A special edit that allows add imports to a import container in a structured way.
 * Additionally this edit honors the Organize Imports preferences
 */
public final class ImportEdit extends SimpleTextEdit {
	
	private ImportsStructure fImportsStructure;
	
	public ImportEdit(ICompilationUnit cunit, CodeGenerationSettings settings) throws JavaModelException {
		Assert.isNotNull(cunit);
		Assert.isNotNull(settings);
		try {
			fImportsStructure= new ImportsStructure(cunit, settings.importOrder, settings.importThreshold, true);
		} catch (JavaModelException e) {
			throw e;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}
	
	private ImportEdit(ImportsStructure importsStructure) {
		fImportsStructure= importsStructure;
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
	 * @return Retuns the simple type name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 * The type name can contain dimensions.
	 */
	public String addImport(String qualifiedTypeName) {
		return fImportsStructure.addImport(qualifiedTypeName);
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.  The type binding can be an array binding. No import is added for unnamed
	 * types (local or anonymous types)
	 * @param binding The type binding of the type to be added
	 * @return Retuns the simple type name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 */
	public String addImport(ITypeBinding binding) {
		return fImportsStructure.addImport(binding);
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
	 * @param binding The qualified name the type to be removed as import
	 * @return Returns true if an import for the given type existed.
	 */
	public boolean removeImport(String qualifiedTypeName) {
		return fImportsStructure.removeImport(qualifiedTypeName);
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
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBuffer buffer) throws CoreException {
		TextRange range= fImportsStructure.getReplaceRange(buffer);
		String text= fImportsStructure.getReplaceString(buffer, range);
		if (text != null) {
			setText(text);
			setTextRange(range);
		} else {
			setText(""); //$NON-NLS-1$
			setTextRange(new TextRange(0,0));
		}
		super.connect(buffer);
	}
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	protected TextEdit copy0(TextEditCopier copier) {
		return new ImportEdit(fImportsStructure);
	}
	
	/* non Java-doc
	 * @see TextEdit#getModifiedElement
	 */
	public Object getModifiedElement() {
		ICompilationUnit cu= fImportsStructure.getCompilationUnit();
		IImportContainer container= cu.getImportContainer();
		if (container.exists())
			return container;
		return cu;
	}	
}

