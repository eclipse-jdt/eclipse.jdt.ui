package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class ImportEditManager {
	
	private final Map fImportEdits; //ICompilationUnit -> ImportEdit
	private final CodeGenerationSettings fPreferenceSettings;

	public ImportEditManager(CodeGenerationSettings preferenceSettings) {
		Assert.isNotNull(preferenceSettings);
		fPreferenceSettings= preferenceSettings;
		fImportEdits= new HashMap();
	}

	private ImportEdit getImportEdit(ICompilationUnit cu) throws JavaModelException{
		if (fImportEdits.containsKey(cu))
			return (ImportEdit)fImportEdits.get(cu);
		
		ImportEdit edit= new ImportEdit(cu, fPreferenceSettings);	
		fImportEdits.put(cu, edit);	
		return edit;
	}

	public void addImportTo(String fullyQualifiedName, ICompilationUnit cu) throws JavaModelException{
		getImportEdit(cu).addImport(fullyQualifiedName);
	}
	
	public void addImportTo(IType type, ICompilationUnit cu) throws JavaModelException{
		addImportTo(JavaModelUtil.getFullyQualifiedName(type), cu);
	}
	
	public void removeImportTo(IType type, ICompilationUnit cu) throws JavaModelException{
		removeImportTo(JavaModelUtil.getFullyQualifiedName(type), cu);
	}
	
	public void removeImportTo(String fullyQualifiedName, ICompilationUnit cu) throws JavaModelException{
		getImportEdit(cu).removeImport(fullyQualifiedName);
	}
	
	public void fill(TextChangeManager manager) throws CoreException{
		for (Iterator iter= fImportEdits.keySet().iterator(); iter.hasNext();) {
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit) iter.next());
			ImportEdit edit= (ImportEdit)fImportEdits.get(cu);
			if (edit != null && ! edit.isEmpty())
				manager.get(cu).addTextEdit("Update Imports", edit);
		}
	}	
}
