package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.jdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry.Context;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempDeclarationFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;

public class InlineTempRefactoring extends Refactoring {

	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	
	//the following fields are set after the construction
	private LocalDeclaration fTempDeclaration;
	private AST fAST;

	public InlineTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Inline local variable";
	}
	
	private String getTempName(){
		return fTempDeclaration.name();
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1);
			if (!(fCu instanceof CompilationUnit))
				return RefactoringStatus.createFatalErrorStatus("Internal Error");
				
			initializeAST();
							
			return checkSelection();			
		} finally {
			pm.done();
		}	
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1);
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}

	private void initializeAST() throws JavaModelException {
		fAST= new AST(fCu);
	}
	
	private void initializeSelection() throws JavaModelException {
		fTempDeclaration= TempDeclarationFinder.findTempDeclaration(fAST, fCu, fSelectionStart, fSelectionLength);
	}
	
	private RefactoringStatus checkSelection() throws JavaModelException {
		if (fAST.hasProblems()){
			RefactoringStatus compileErrors= Checks.checkCompileErrors(fAST, fCu);
			if (compileErrors.hasFatalError())
				return compileErrors;
		}
		initializeSelection(); //cannot initialize earlier
		
		if (fTempDeclaration == null)
			return RefactoringStatus.createFatalErrorStatus("A local variable declaration or reference must be selected to activate this refactoring");
		
		if (! isTempInitializedAtDeclaration())
			return RefactoringStatus.createFatalErrorStatus("Local variable '" + getTempName() + "' is not initialized at declaration.");
		
		return checkAssignments();
	}
	
	private boolean isTempInitializedAtDeclaration(){
		return fTempDeclaration.initialization != null;
	}
	
	private RefactoringStatus checkAssignments() throws JavaModelException {
		TempAssignmentFinder assignmentFinder= new TempAssignmentFinder(fTempDeclaration);
		fAST.accept(assignmentFinder);
		if (! assignmentFinder.hasAssignments())
			return new RefactoringStatus();
		int start= assignmentFinder.getFirstAssignment().sourceStart;
		int end= assignmentFinder.getFirstAssignment().sourceEnd;
		ISourceRange range= new SourceRange(start, end - start + 1);
		Context context= JavaSourceContext.create(fCu, range);	
		return RefactoringStatus.createFatalErrorStatus("Local variable '" + getTempName()+ "' is assigned to more than once", context);
	}
	
	//----- changes
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("Creating preview", 2);
			TextChange change= new CompilationUnitChange("Inline local variable", fCu);
			inlineTemp(change, new SubProgressMonitor(pm, 1));
			removeTemp(change);
			return change;
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} finally{
			pm.done();	
		}	
	}

	private void inlineTemp(TextChange change, IProgressMonitor pm) throws JavaModelException {
		Integer[] offsets= getOccurrenceOffsets();
		pm.beginTask("", offsets.length);
		String changeName= "Inline local variable:'" + getTempName() + "'";
		int length= getTempName().length();
		String initializerSource= getInitializerSource();
		for(int i= 0; i < offsets.length; i++){
			change.addTextEdit(changeName, SimpleTextEdit.createReplace(offsets[i].intValue(), length, initializerSource));
			pm.worked(1);	
		}
	}

	private void removeTemp(TextChange change) throws JavaModelException {
		int offset= fTempDeclaration.type.sourceStart;
		int end= fTempDeclaration.declarationSourceEnd;
		int length= end - offset + 1;
		String changeName= "Remove local variable '" + getTempName() + "'"; 
		change.addTextEdit(changeName, new LineEndDeleteTextEdit(offset, length, fCu.getSource()));
	}
	
	private String getInitializerSource() throws JavaModelException{
		Assert.isTrue(isTempInitializedAtDeclaration());
		int start= fTempDeclaration.initialization.sourceStart;
		int end= fTempDeclaration.initialization.sourceEnd;
		String rawSource= fCu.getSource().substring(start, end + 1);
		if (! needsBracketsAroundReferences(fTempDeclaration.initialization))
			return rawSource;
		else 
			return "(" + rawSource + ")";
	}
	
	private Integer[] getOccurrenceOffsets() throws JavaModelException{
		TempOccurrenceFinder offsetFinder= new TempOccurrenceFinder(fTempDeclaration, true, false);
		fAST.accept(offsetFinder);
		return offsetFinder.getOccurrenceOffsets();
	}	
	
	private static boolean needsBracketsAroundReferences(Expression expression){
		if (expression instanceof BinaryExpression)	
			return true;	
		if (expression instanceof PrefixExpression)	
			return true;	
		if (expression instanceof PostfixExpression)	
			return true;	
		if (expression instanceof ConditionalExpression)	
			return true;	
		if (expression instanceof Assignment)//for estetic reasons
			return true;	
		return false;		
	}
	
	//--- private helper classes
	private static class TempAssignmentFinder extends AbstractSyntaxTreeVisitorAdapter{
		private Assignment fFirstAssignment;
		private LocalDeclaration fTempDeclaration;
		
		TempAssignmentFinder(LocalDeclaration tempDeclaration){
			fTempDeclaration= tempDeclaration;
		}
		
		private boolean isAssignmentToTemp(Assignment assignment){
			if (! (assignment.lhs instanceof NameReference))
				return false;
			NameReference ref= (NameReference)assignment.lhs;
			if (! (ref.binding instanceof LocalVariableBinding))
				return false;
			LocalVariableBinding localBinding= (LocalVariableBinding)ref.binding;
			return (fTempDeclaration.equals(localBinding.declaration));
		}
		
		boolean hasAssignments(){
			return fFirstAssignment != null;
		}
		
		Assignment getFirstAssignment(){
			return fFirstAssignment;
		}
		
		private boolean checkAssignment(Assignment assignment){
			if (! isAssignmentToTemp(assignment))
				return true;
			
			fFirstAssignment= assignment;
			return false;
		}
		
		//-- visit methods
		
		public boolean visit(Assignment assignment, BlockScope scope) {
			return checkAssignment(assignment);
		}
		
		public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
			return checkAssignment(compoundAssignment);
		}
		
		public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
			return checkAssignment(postfixExpression);
		}
		public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
			return checkAssignment(prefixExpression);
		}
	}
}