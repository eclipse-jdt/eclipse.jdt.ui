/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.core.refactoring;import org.eclipse.core.resources.IResource;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.ISourceRange;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;import org.eclipse.jdt.internal.compiler.ast.AstNode;import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;import org.eclipse.jdt.internal.core.CompilationUnit;import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;public abstract class AbstractRefactoringASTAnalyzer  extends AbstractSyntaxTreeVisitorAdapter{

	private RefactoringStatus fResult;
	private CompilationUnit fCu;
	
	private int[] fLineSeparatorPositions; //set in visit(CompilationUnitDeclaration)
		protected AbstractRefactoringASTAnalyzer(){		fResult= new RefactoringStatus();	}		public void setCU(ICompilationUnit cu){		fCu= (CompilationUnit)cu;	}		public RefactoringStatus getStatus(){		return fResult;	}	
	public final RefactoringStatus analyze(ICompilationUnit cu) throws JavaModelException{
		fCu= (CompilationUnit)cu;
		fCu.accept(this);
		return fResult;	
	}
	
	public boolean doVisit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope){
		return true;
	}
	
	/* non java-doc
	 * sublasses implement doVisit instead
	 */ 
	public final boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		fLineSeparatorPositions= compilationUnitDeclaration.compilationResult.lineSeparatorPositions;
		return doVisit(compilationUnitDeclaration, scope);
	}
	
	protected int getLineNumber(AstNode node){
		Assert.isNotNull(fLineSeparatorPositions);
		return ProblemHandler.searchLineNumber(fLineSeparatorPositions, node.sourceStart);
	}
		protected void addFatalError(String msg){		fResult.addFatalError(msg);	}		protected void addFatalError(String msg, int start, int end){		fResult.addFatalError(msg, getResource(fCu), createSourceRange(start, end));	}	
	protected void addError(String msg){
		fResult.addError(msg);
	}		protected void addError(String msg, int start, int end){		fResult.addError(msg, getResource(fCu), createSourceRange(start, end));	}	
	protected void addWarning(String msg){
		fResult.addWarning(msg);
	}		protected void addWarning(String msg, int start, int end){		fResult.addWarning(msg, getResource(fCu), createSourceRange(start, end));	}		protected void addInfo(String msg){		fResult.addInfo(msg);	}	protected void addInfo(String msg, int start, int end){		fResult.addInfo(msg, getResource(fCu), createSourceRange(start, end));	}	
	protected static String getFullPath(ICompilationUnit cu) {
		Assert.isTrue(cu.exists());
		try {
			return Refactoring.getResource(cu).getFullPath().toString();
		} catch (JavaModelException e) {
			return cu.getElementName();
		}
	}		private static IResource getResource(ICompilationUnit cu){		try{			return Refactoring.getResource(cu);		} catch (JavaModelException e){			return null;		}	}		private static ISourceRange createSourceRange(final int start, final int end){		return new SourceRange(start, end - start + 1);	}

	protected String cuFullPath() {
		return getFullPath(fCu);
	}	
}
