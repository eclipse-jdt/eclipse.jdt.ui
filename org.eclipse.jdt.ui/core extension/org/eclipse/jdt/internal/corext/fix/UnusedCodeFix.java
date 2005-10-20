package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Fix which removes unused code.
 * Supported:
 * 		Remove unused import
 */
public class UnusedCodeFix extends AbstractFix {
	
	public static final String REMOVE_UNUSED_IMPORT= "Remove unused import";
	
	private final ImportDeclaration[] fImports;

	public static IFix createFix(CompilationUnit compilationUnit, IProblemLocation problem, boolean removeUnusedImports) {
		if (removeUnusedImports) {
			ImportDeclaration node= getImportDeclaration(problem, compilationUnit);
			if (node == null)
				return null;
			
			return new UnusedCodeFix(REMOVE_UNUSED_IMPORT, (ICompilationUnit)compilationUnit.getJavaElement(), new ImportDeclaration[] {node});
		}
		return null;
	}
	
	public static ImportDeclaration getImportDeclaration(IProblemLocation problem, CompilationUnit compilationUnit) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode != null) {
			ASTNode node= ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);
			if (node instanceof ImportDeclaration) {
				return (ImportDeclaration)node;
			}
		}
		return null;
	}

	public UnusedCodeFix(String name, ICompilationUnit compilationUnit, ImportDeclaration[] imports) {
		super(name, compilationUnit);
		fImports= imports;
	}

	public TextChange createChange() throws CoreException {
		if (fImports != null && fImports.length > 0) {
			ASTRewrite rewrite= ASTRewrite.create(fImports[0].getAST());
			TextEditGroup[] groups= new TextEditGroup[fImports.length];
			for (int i= 0; i < fImports.length; i++) {
				ImportDeclaration node= fImports[i];
				groups[i]= new TextEditGroup(REMOVE_UNUSED_IMPORT + " " + node.getName()); //$NON-NLS-1$
				rewrite.remove(node, groups[i]);
			}
			
			TextEdit edit= applyEdits(getCompilationUnit(), rewrite, null);
			
			CompilationUnitChange result= new CompilationUnitChange(REMOVE_UNUSED_IMPORT, getCompilationUnit());
			result.setEdit(edit);
			
			//Commented out: Bug in preview of refactoring wizard
//			for (int i= 0; i < groups.length; i++) {
//				result.addTextEditGroup(groups[i]);
//			}
			
			return result;
		}
		return null;
	}

}
