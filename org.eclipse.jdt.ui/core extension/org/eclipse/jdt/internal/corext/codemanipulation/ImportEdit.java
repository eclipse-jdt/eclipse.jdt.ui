/*
 * (c) Copyright IBM Corp. 2000, 2001, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

/**
 * A special edit that allows add imports to a import container in a structured way.
 * Additionally this edit honors the Organize Imports preferences
 */
public class ImportEdit extends SimpleTextEdit {
	
	private ICompilationUnit fCUnit;
	private CodeGenerationSettings fSettings;
	private List fImports;
	private boolean fFilterImplicitImports;
	
	public ImportEdit(ICompilationUnit cunit, CodeGenerationSettings settings) {
		Assert.isNotNull(cunit);
		Assert.isNotNull(settings);
		fCUnit= cunit;
		fSettings= settings;
		fImports= new ArrayList(3);
		fFilterImplicitImports= true;
	}

	private ImportEdit(ICompilationUnit cunit, CodeGenerationSettings settings, List imports) {
		this(cunit, settings);
		fImports= new ArrayList(imports);
	}
	
	/**
	 * @see ImportsStructure#setFilterImplicitImports(boolean)
	 */
	public void setFilterImplicitImports(boolean filterImplicitImports) {
		fFilterImplicitImports= filterImplicitImports;
	}
	
	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 */			
	public void addImport(String qualifiedTypeName) {
		int index= qualifiedTypeName.lastIndexOf('.');
		if (index == -1)	// no default package
			return;
		if ("java.lang".equals(qualifiedTypeName.substring(0, index)))
			return;
		fImports.add(qualifiedTypeName);
	}
	
	/**
	 * Returns <code>true</code> if the import edit will not change the import
	 * container; otherwise <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if the import edit will not change the import
	 * 	container; otherwise <code>false</code> is returned
	 */
	public boolean isEmpty() {
		return fImports.isEmpty();
	}
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBufferEditor editor) throws CoreException {
		TextBuffer buffer= editor.getTextBuffer();
		ImportsStructure importStructure= new ImportsStructure(fCUnit, fSettings.importOrder, fSettings.importThreshold, true);
		importStructure.setFilterImplicitImports(fFilterImplicitImports);
		for (Iterator iter= fImports.iterator(); iter.hasNext();) {
			importStructure.addImport((String)iter.next());
		}
		TextRange range= importStructure.getReplaceRange(buffer);
		String text= importStructure.getReplaceString(buffer, range);
		if (text != null) {
			setText(text);
			setTextRange(range);
		}
	}
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public TextEdit copy() throws CoreException {
		return new ImportEdit(fCUnit, fSettings, fImports);
	}
	
	/* non Java-doc
	 * @see TextEdit#getModifiedElement
	 */
	public Object getModifiedElement() {
		IImportContainer container= fCUnit.getImportContainer();
		if (container.exists())
			return container;
		return fCUnit;
	}	
}

