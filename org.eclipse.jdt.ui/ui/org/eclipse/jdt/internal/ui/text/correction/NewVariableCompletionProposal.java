/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.NameProposer;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewVariableCompletionProposal extends CUCorrectionProposal {

	private IType fParentType;

	private String fVariableName;

	public NewVariableCompletionProposal(IType type, ProblemPosition problemPos, String label, String variableName) throws CoreException {
		super(label, problemPos);
		
		fParentType= type;
		fVariableName= variableName;
	}

	/*
	 * @see JavaCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange changeElement) throws CoreException {
		ProblemPosition problemPos= getProblemPosition();
		
		ICompilationUnit cu= getCompilationUnit();
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		ImportEdit importEdit= new ImportEdit(cu, settings);

		String content= generateStub(importEdit);
		
		int insertPos= MemberEdit.ADD_AT_BEGINNING;
		IJavaElement anchor= fParentType;
		
		MemberEdit memberEdit= new MemberEdit(anchor, insertPos, new String[] { content }, settings.tabWidth);
		memberEdit.setUseFormatter(true);
		
		if (!importEdit.isEmpty()) {
			changeElement.addTextEdit("Add imports", importEdit);
		}
		changeElement.addTextEdit("Add field", memberEdit);
	}
	
	
	private String generateStub(ImportEdit importEdit) {
		StringBuffer buf= new StringBuffer();
		buf.append("private Object ");
		buf.append(fVariableName);
		buf.append(";\n");
		return buf.toString();
	}
		
	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
	}

}
