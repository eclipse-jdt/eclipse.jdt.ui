package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.Iterator;

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
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempDeclarationFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;


public class InlineTempRefactoring extends Refactoring {

	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	
	//the following fields are set after the construction
	private VariableDeclaration fTempDeclaration;
	private CompilationUnit fCompilationUnitNode;
	private boolean fSaveChanges;

	public InlineTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
		fSaveChanges= true;//XX
	}
	
	public void setSaveChanges(boolean save){
		fSaveChanges= save;
	}
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("InlineTempRefactoring.name"); //$NON-NLS-1$
	}
	
	private String getTempName(){
		return fTempDeclaration.getName().getIdentifier();
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			
			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu}));
			if (result.hasFatalError())
				return result;
				
			if (! fCu.isStructureKnown())		
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTempRefactoring.syntax_errors")); //$NON-NLS-1$
				
			initializeAST();
				
			result.merge(checkSelection());
			if (result.hasFatalError())
				return result;
			
			result.merge(checkInitializer());	
			return result;
		} catch (CoreException e){		
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}	
	}

    private RefactoringStatus checkInitializer() {
    	switch(fTempDeclaration.getInitializer().getNodeType()){
    		case ASTNode.NULL_LITERAL:
    			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTemRefactoring.error.message.nulLiteralsCannotBeInlined")); //$NON-NLS-1$
    		case ASTNode.ARRAY_INITIALIZER:
    			return RefactoringStatus.createFatalErrorStatus("Array variables initialized with constants cannot be inlined."); 	
    		default:	
		        return null;
    	}
    }

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}

	private void initializeAST() throws JavaModelException {
		fCompilationUnitNode= AST.parseCompilationUnit(fCu, true);
	}
	
	private RefactoringStatus checkSelection() throws JavaModelException {
		fTempDeclaration= TempDeclarationFinder.findTempDeclaration(fCompilationUnitNode, fSelectionStart, fSelectionLength);
		
		if (fTempDeclaration == null){
			String message= RefactoringCoreMessages.getString("InlineTempRefactoring.select_temp");//$NON-NLS-1$
			return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCompilationUnitNode, message);
		}	

		if (fTempDeclaration.getParent() instanceof FieldDeclaration)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTemRefactoring.error.message.fieldsCannotBeInlined")); //$NON-NLS-1$

		if (fTempDeclaration.getParent() instanceof MethodDeclaration)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTempRefactoring.method_parameter")); //$NON-NLS-1$
		
		if (fTempDeclaration.getParent() instanceof CatchClause)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTempRefactoring.exceptions_declared")); //$NON-NLS-1$
		
		if (ASTNodes.getParent(fTempDeclaration, ASTNode.FOR_STATEMENT) != null){
			ForStatement forStmt= (ForStatement)ASTNodes.getParent(fTempDeclaration, ASTNode.FOR_STATEMENT);
			for (Iterator iter= forStmt.initializers().iterator(); iter.hasNext();) {
				if (ASTNodes.isParent(fTempDeclaration, (Expression) iter.next()))
					return RefactoringStatus.createFatalErrorStatus("Cannot inline variables declared in the initializer list of a 'for' statement.");
			}
		}
		
		if (fTempDeclaration.getInitializer() == null){
			String message= RefactoringCoreMessages.getFormattedString("InlineTempRefactoring.not_initialized", getTempName());//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
				
		return checkAssignments();
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
		String message= RefactoringCoreMessages.getFormattedString("InlineTempRefactoring.assigned_more_once", getTempName());//$NON-NLS-1$
		return RefactoringStatus.createFatalErrorStatus(message, context);
	}
	
	//----- changes
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("InlineTempRefactoring.preview"), 2); //$NON-NLS-1$
			CompilationUnitChange change= new CompilationUnitChange(RefactoringCoreMessages.getString("InlineTempRefactoring.inline"), fCu); //$NON-NLS-1$
			change.setSave(fSaveChanges);
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
		pm.beginTask("", offsets.length); //$NON-NLS-1$
		String changeName= RefactoringCoreMessages.getString("InlineTempRefactoring.inline_edit_name") + getTempName(); //$NON-NLS-1$
		int length= getTempName().length();
		for(int i= 0; i < offsets.length; i++){
			int offset= offsets[i].intValue();
            String sourceToInline= getInitializerSource(needsBrackets(offset));
			change.addTextEdit(changeName, SimpleTextEdit.createReplace(offset, length, sourceToInline));
			pm.worked(1);	
		}
	}
	
    private boolean needsBrackets(int offset) {
    	if (neverNeedsBracketsAroundReferences())
    		return false;
    	SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(offset, getTempName().length()), true);
    	fCompilationUnitNode.accept(analyzer);
    	ASTNode firstSelected= analyzer.getFirstSelectedNode();
    	if (firstSelected == null)
    		return true;
    	ASTNode parent= firstSelected.getParent();
    	if (parent instanceof VariableDeclarationFragment){
    		VariableDeclarationFragment vdf= (VariableDeclarationFragment)parent;
    		if (vdf.getInitializer().equals(firstSelected))
    			return false;
    	} else if (parent instanceof MethodInvocation){
    		MethodInvocation mi= (MethodInvocation)parent;
    		if (mi.arguments().contains(firstSelected))
    			return false;
    	}
        return true;
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
		String changeName= RefactoringCoreMessages.getString("InlineTempRefactoring.remove_edit_name") + getTempName();  //$NON-NLS-1$
		change.addTextEdit(changeName, new LineEndDeleteTextEdit(offset, length, fCu.getSource()));
	}
	
	private String getInitializerSource(boolean brackets) throws JavaModelException{
		if (brackets)
			return '(' + getRawInitializerSource() + ')'; 
		else
			return getRawInitializerSource(); 
	}
	
	private String getRawInitializerSource() throws JavaModelException{
		int start= fTempDeclaration.getInitializer().getStartPosition();
		int length= fTempDeclaration.getInitializer().getLength();
		int end= start + length;
		return fCu.getSource().substring(start, end);
	}
	
    private boolean neverNeedsBracketsAroundReferences() {
		Expression expression= fTempDeclaration.getInitializer();
		if (ASTNodes.needsParenthesis(expression))	
			return false;	
		if (expression instanceof Assignment)//for estetic reasons
			return false;	
		return true;		
    }
	
	private Integer[] getOccurrenceOffsets() throws JavaModelException{
		return TempOccurrenceFinder.findTempOccurrenceOffsets(fTempDeclaration, true, false);
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
			if (! prefixExpression.getOperator().equals(PrefixExpression.Operator.DECREMENT) &&
				! prefixExpression.getOperator().equals(PrefixExpression.Operator.INCREMENT))
				return true;
			SimpleName simpleName= (SimpleName)prefixExpression.getOperand();	
			if (! isNameReferenceToTemp(simpleName))
				return true;
			
			fFirstAssignment= prefixExpression;
			return false;	
		}
	}
	
}