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

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

/**
 * A special edit that allows add imports to a import container in a structured way.
 * Additionally this edit honors the Organize Imports preferences
 */
public final class ImportEdit extends SimpleTextEdit {
	
	private ICompilationUnit fCUnit;
	private CodeGenerationSettings fSettings;
	private List fAddedImports;
	private List fRemovedImports;
	private boolean fFilterImplicitImports;
	
	public ImportEdit(ICompilationUnit cunit, CodeGenerationSettings settings) {
		Assert.isNotNull(cunit);
		Assert.isNotNull(settings);
		fCUnit= cunit;
		fSettings= settings;
		fAddedImports= new ArrayList(3);
		fRemovedImports= new ArrayList(0);
		fFilterImplicitImports= true;
	}

	private ImportEdit(ICompilationUnit cunit, CodeGenerationSettings settings, List addedImports, List removedImports, boolean filterImplicitImports) {
		this(cunit, settings);
		fAddedImports= new ArrayList(addedImports);
		fRemovedImports= new ArrayList(removedImports);
		fFilterImplicitImports= filterImplicitImports;
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
		int lastDotIndex= qualifiedTypeName.lastIndexOf('.');
		if (lastDotIndex == -1)	// no default package
			return;
		if ("java.lang".equals(qualifiedTypeName.substring(0, lastDotIndex))) //$NON-NLS-1$
			return;
		
		//XXX workaround for 11622, 11537 and related problems with array types
		String bracketsRemoved= removeTrailingBrackets(qualifiedTypeName);			
		
		if (fAddedImports.contains(bracketsRemoved))	//do not add twice
			return;
			
		fAddedImports.add(bracketsRemoved);
	}
	
	public void removeImport(String qualifiedTypeName) {
		if (fRemovedImports.contains(qualifiedTypeName))	//do not remove twice
			return;
		
		fRemovedImports.add(qualifiedTypeName);
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
		return fAddedImports.isEmpty() && fRemovedImports.isEmpty();
	}
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBuffer buffer) throws CoreException {
		ImportsStructure importStructure= new ImportsStructure(fCUnit, fSettings.importOrder, fSettings.importThreshold, true);
		importStructure.setFilterImplicitImports(fFilterImplicitImports);
		for (Iterator iter= fRemovedImports.iterator(); iter.hasNext();) {
			importStructure.removeImport((String)iter.next());
		}
		for (Iterator iter= fAddedImports.iterator(); iter.hasNext();) {
			importStructure.addImport((String)iter.next());
		}
		
		TextRange range= importStructure.getReplaceRange(buffer);
		String text= importStructure.getReplaceString(buffer, range);
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
		return new ImportEdit(fCUnit, fSettings, fAddedImports, fRemovedImports, fFilterImplicitImports);
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

