package org.eclipse.jdt.internal.corext.refactoring.code;

import java.text.MessageFormat;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

class CodeRefactoringUtil {

    private CodeRefactoringUtil() {}

    public static RefactoringStatus checkMethodSyntaxErrors(int selectionStart, int selectionLength, CompilationUnit cuNode, ICompilationUnit cu, String invalidSelectionMessage){
			SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(selectionStart, selectionLength), true);
			cuNode.accept(analyzer);
			ASTNode coveringNode= analyzer.getLastCoveringNode();
			if (! (coveringNode instanceof Block) || ! (coveringNode.getParent() instanceof MethodDeclaration))
				return RefactoringStatus.createFatalErrorStatus(invalidSelectionMessage); 
			if (ASTNodes.getMessages(coveringNode, ASTNodes.NODE_ONLY).length == 0)
				return RefactoringStatus.createFatalErrorStatus(invalidSelectionMessage); 

			MethodDeclaration methodDecl= (MethodDeclaration)coveringNode.getParent();
			String key= "Compilation errors in method ''{0}'' prevent analyzing the body of the method. Please fix the errors first.";
			String message= MessageFormat.format(key, new String[]{methodDecl.getName().getIdentifier()});
			return RefactoringStatus.createFatalErrorStatus(message);	
	}
}
