package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Message;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

class CodeRefactoringUtil {

    private CodeRefactoringUtil() {}

    public static RefactoringStatus checkMethodSyntaxErrors(int selectionStart, int selectionLength, CompilationUnit cuNode, ICompilationUnit cu, String invalidSelectionMessage){
			SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(selectionStart, selectionLength), true);
			cuNode.accept(analyzer);
			ASTNode lastCovering= analyzer.getLastCoveringNode();
			if (! (lastCovering instanceof Block))
				return RefactoringStatus.createFatalErrorStatus(invalidSelectionMessage); 
			Message[] messages= ASTNodes.getMessages(lastCovering, ASTNodes.INCLUDE_ALL_PARENTS);
			RefactoringStatus result= new RefactoringStatus();
			for (int i= 0; i < messages.length; i++) {
                Context context= JavaSourceContext.create(cu, new SourceRange(messages[i].getStartPosition(), messages[i].getLength()));
                result.addFatalError(messages[i].getMessage(), context);
            }
            if (result.hasFatalError())
				return result;
			else	
				return RefactoringStatus.createFatalErrorStatus(invalidSelectionMessage); 
	}
}
