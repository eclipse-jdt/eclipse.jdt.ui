package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
  */
public class ASTRewriteCorrectionProposal extends CUCorrectionProposal {

	private ASTRewrite fRewrite;
	private ImportEdit fImportEdit;

	public ASTRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) throws CoreException {
		super(name, cu, relevance, image);
		fRewrite= rewrite;
		fImportEdit= null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#createCompilationUnitChange(String, ICompilationUnit, TextEdit)
	 */
	protected CompilationUnitChange createCompilationUnitChange(String name, ICompilationUnit cu, TextEdit rootEdit) throws CoreException {
		CompilationUnitChange change= super.createCompilationUnitChange(name, cu, rootEdit);
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(change.getFile());
			ArrayList groupDescriptions= new ArrayList(5);
			getRewrite().rewriteNode(buffer, rootEdit, groupDescriptions);
			for (int i= 0; i < groupDescriptions.size(); i++) {
				change.addGroupDescription((GroupDescription) groupDescriptions.get(i));
			}
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
		}
		if (fImportEdit != null && !fImportEdit.isEmpty()) {
			rootEdit.add(fImportEdit);
		}
		return change;
	}
	
	public void addImport(String qualifiedTypeName) {
		if (fImportEdit == null) {
			fImportEdit= new ImportEdit(getCompilationUnit(), JavaPreferencesSettings.getCodeGenerationSettings());
		}
		fImportEdit.addImport(qualifiedTypeName);
	}
	
	protected ASTRewrite getRewrite() {
		return fRewrite;
	}


}
