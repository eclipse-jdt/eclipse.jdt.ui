package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry.Context;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempDeclarationFinder2;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceFinder2;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;

public class InlineTempRefactoring extends Refactoring {

	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	
	//the following fields are set after the construction
	private VariableDeclaration fTempDeclaration;
	private CompilationUnit fCompilationUnitNode;

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
		return fTempDeclaration.getName().getIdentifier();
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1);
				
			if (! fCu.isStructureKnown())		
				return RefactoringStatus.createFatalErrorStatus("This file has syntax errors - please fix them first");
				
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
		fCompilationUnitNode= AST.parseCompilationUnit(fCu, true);
	}
	
	private RefactoringStatus checkSelection() throws JavaModelException {
		fTempDeclaration= TempDeclarationFinder2.findTempDeclaration(fCompilationUnitNode, fSelectionStart, fSelectionLength);
		
		if (fTempDeclaration == null)
			return RefactoringStatus.createFatalErrorStatus("A local variable declaration or reference must be selected to activate this refactoring");
		
		if (! isTempInitializedAtDeclaration())
			return RefactoringStatus.createFatalErrorStatus("Local variable '" + getTempName() + "' is not initialized at declaration.");
		
		return checkAssignments();
	}
	
	private boolean isTempInitializedAtDeclaration(){
		return fTempDeclaration.getInitializer() != null;
	}
	
	private RefactoringStatus checkAssignments() throws JavaModelException {
		TempAssignmentFinder assignmentFinder= new TempAssignmentFinder(fTempDeclaration);
		fCompilationUnitNode.accept(assignmentFinder);
		if (! assignmentFinder.hasAssignments())
			return new RefactoringStatus();
		int start= assignmentFinder.getFirstAssignment().getStartPosition();
		int length= assignmentFinder.getFirstAssignment().getLength();
		ISourceRange range= new SourceRange(start, length);
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
		//FIX ME - multi declarations
		
		if (fTempDeclaration.getParent() instanceof VariableDeclarationStatement){
			VariableDeclarationStatement vds= (VariableDeclarationStatement)fTempDeclaration.getParent();
			if (vds.fragments().size() == 1){
				removeDeclaration(change, vds.getStartPosition(), vds.getLength());
				return;
			} else {
				//FIX ME
				return;
			}
		}
		
		removeDeclaration(change, fTempDeclaration.getStartPosition(), fTempDeclaration.getLength());
	}
	
	private void removeDeclaration(TextChange change, int offset, int length)  throws JavaModelException {
		String changeName= "Remove local variable '" + getTempName() + "'"; 
		change.addTextEdit(changeName, new LineEndDeleteTextEdit(offset, length, fCu.getSource()));
	}
	
	private String getInitializerSource() throws JavaModelException{
		Assert.isTrue(isTempInitializedAtDeclaration());
		int start= fTempDeclaration.getInitializer().getStartPosition();
		int length= fTempDeclaration.getInitializer().getLength();
		int end= start + length;
		String rawSource= fCu.getSource().substring(start, end);
		if (! needsBracketsAroundReferences(fTempDeclaration.getInitializer()))
			return rawSource;
		else 
			return "(" + rawSource + ")";
	}
	
	private Integer[] getOccurrenceOffsets() throws JavaModelException{
		return TempOccurrenceFinder2.findTempOccurrenceOffsets(fCompilationUnitNode, fTempDeclaration, true, false);
	}	
	
	private static boolean needsBracketsAroundReferences(Expression expression){
		if (expression instanceof InfixExpression)	
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
	
	private static class TempAssignmentFinder extends ASTVisitor{
		private ASTNode fFirstAssignment;
		private IVariableBinding fTempBinding;
		
		TempAssignmentFinder(VariableDeclaration tempDeclaration){
			fTempBinding= tempDeclaration.resolveBinding();
		}
		
		private boolean isNameReferenceToTemp(Name name){
			return fTempBinding == name.resolveBinding();
		}
			
		private boolean isAssignmentToTemp(Assignment assignment){
			if (fTempBinding == null)
				return false;
				
			if (! (assignment.getLeftHandSide() instanceof Name))
				return false;
			Name ref= (Name)assignment.getLeftHandSide();
			return isNameReferenceToTemp(ref);
		}
		
		boolean hasAssignments(){
			return fFirstAssignment != null;
		}
		
		ASTNode getFirstAssignment(){
			return fFirstAssignment;
		}
		
		//-- visit methods
		
		public boolean visit(Assignment assignment) {
			if (! isAssignmentToTemp(assignment))
				return true;
			
			fFirstAssignment= assignment;
			return false;
		}
		
		public boolean visit(PostfixExpression postfixExpression) {
			if (postfixExpression.getOperand() == null)
				return true;
			if (! (postfixExpression.getOperand() instanceof SimpleName))
				return true;	
			SimpleName simpleName= (SimpleName)postfixExpression.getOperand();	
			if (! isNameReferenceToTemp(simpleName))
				return true;
			
			fFirstAssignment= postfixExpression;
			return false;	
		}
		
		public boolean visit(PrefixExpression prefixExpression) {
			if (prefixExpression.getOperand() == null)
				return true;
			if (! (prefixExpression.getOperand() instanceof SimpleName))
				return true;	
			SimpleName simpleName= (SimpleName)prefixExpression.getOperand();	
			if (! isNameReferenceToTemp(simpleName))
				return true;
			
			fFirstAssignment= prefixExpression;
			return false;	
		}
	}
	
}