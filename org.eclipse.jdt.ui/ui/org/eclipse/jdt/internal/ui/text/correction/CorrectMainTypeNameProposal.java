package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.SimpleNameRenamer;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Renames the primary type to be compatible with the name of the compilation unit.
 * All constructurs and local references to the type are renamed as well.
  */
public class CorrectMainTypeNameProposal extends ASTRewriteCorrectionProposal {
	
	private String fOldName;
	private String fNewName;

	/**
	 * Constructor for CorrectTypeNameProposal.
	 */
	public CorrectMainTypeNameProposal(ICompilationUnit cu, String oldTypeName, int relevance) {
		super("", cu, null, relevance, null);
		fNewName= Signature.getQualifier(cu.getElementName());
		
		setDisplayName(CorrectionMessages.getFormattedString("ReorgCorrectionsSubProcessor.renametype.description", fNewName));
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		
		fOldName= oldTypeName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		char[] content= getCompilationUnit().getBuffer().getCharacters();
		CompilationUnit astRoot= AST.parseCompilationUnit(content, fOldName + ".java", getCompilationUnit().getJavaProject());
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		TypeDeclaration decl= findTypeDeclaration(astRoot.types(), fOldName);
		if (decl != null) {
			IBinding binding= decl.resolveBinding();
			if (binding != null) {
				SimpleNameRenamer.perform(rewrite, new IBinding[] { binding }, new String[] { fNewName }, astRoot);
			} else {
				// no binding: only rename type
				rewrite.markAsReplaced(decl.getName(), astRoot.getAST().newSimpleName(fNewName));
			}
		}
		return rewrite;
	}
	
	private TypeDeclaration findTypeDeclaration(List types, String name) {
		for (Iterator iter= types.iterator(); iter.hasNext();) {
			TypeDeclaration decl= (TypeDeclaration) iter.next();
			if (name.equals(decl.getName().getIdentifier())) {
				return decl;
			}
		}
		return null;	
	}

}
