/*
 * (c) Copyright IBM Corp. 2000, 2001, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.JavaModelException;

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
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 * @return Retuns the simple type name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 */
	public String addImport(String qualifiedTypeName) {
		//XXX workaround for 11622, 11537 and related problems with array types
		qualifiedTypeName= removeTrailingBrackets(qualifiedTypeName);
		
		return fImportsStructure.addImport(qualifiedTypeName);
	}
	
	public void removeImport(String qualifiedTypeName) {
		fImportsStructure.removeImport(qualifiedTypeName);
	}
	
	private static String removeTrailingBrackets(String s){
		if (s.indexOf('[') == -1)
			return s;
		else
			return s.substring(0, s.indexOf('['));	
	}
	
	/**
	 * Returns <code>true</code> if the import edit will not change the import
	 * container; otherwise <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if the import edit will not change the import
	 * 	container; otherwise <code>false</code> is returned
	 */
	public boolean isEmpty() {
		return fImportsStructure.hasChanges();
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

