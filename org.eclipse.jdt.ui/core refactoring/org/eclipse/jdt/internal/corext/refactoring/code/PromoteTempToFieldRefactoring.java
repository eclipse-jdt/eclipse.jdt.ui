package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempDeclarationFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

public class PromoteTempToFieldRefactoring extends Refactoring {

	private final int fSelectionStart;
    private final int fSelectionLength;
    private final ICompilationUnit fCu;
	
	public static final int INITIALIZE_IN_FIELD= 1;
	public static final int INITIALIZE_IN_METHOD= 2;
	public static final int INITIALIZE_IN_CONSTRUCTOR= 3;
	
	//------ settings ---------//
	private String fFieldName;
	private int fAccessModifier; 	/*see JdtFlags*/
	private boolean fDeclareStatic;
	private boolean fDeclareFinal;
	private int fInitializeIn; /*see INITIALIZE_IN* constaints */

	//------ fields used for computations ---------//
    private CompilationUnit fCompilationUnitNode;
    private VariableDeclaration fTempDeclarationNode;
	
	public PromoteTempToFieldRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength){
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
		
		fFieldName= "";
		fAccessModifier= JdtFlags.VISIBILITY_CODE_PRIVATE;
		fDeclareStatic= false;
		fDeclareFinal= false;
	}
	

    /*
     * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
     */
    public String getName() {
        return "Promote Local Variable to Field";
    }
    
    public int getAccessModifier() {
        return fAccessModifier;
    }

    public boolean getDeclareFinal() {
        return fDeclareFinal;
    }

    public boolean getDeclareStatic() {
        return fDeclareStatic;
    }

    public String getFieldName() {
        return fFieldName;
    }

    public int getInitializeIn() {
        return fInitializeIn;
    }

    public void setAccessModifier(int accessModifier) {
    	Assert.isTrue(accessModifier == JdtFlags.VISIBILITY_CODE_PRIVATE ||
    					accessModifier == JdtFlags.VISIBILITY_CODE_PACKAGE ||
    					accessModifier == JdtFlags.VISIBILITY_CODE_PROTECTED ||
    					accessModifier == JdtFlags.VISIBILITY_CODE_PUBLIC);
        fAccessModifier= accessModifier;
    }

    public void setDeclareFinal(boolean declareFinal) {
        fDeclareFinal= declareFinal;
    }

    public void setDeclareStatic(boolean declareStatic) {
        fDeclareStatic= declareStatic;
    }

    public void setFieldName(String fieldName) {
    	Assert.isNotNull(fieldName);
        fFieldName= fieldName;
    }

    public void setInitializeIn(int initializeIn) {
    	Assert.isTrue(	initializeIn == INITIALIZE_IN_CONSTRUCTOR ||
    					initializeIn == INITIALIZE_IN_FIELD ||
    					initializeIn == INITIALIZE_IN_METHOD);
        fInitializeIn= initializeIn;
    }
	
	public boolean canEnableSettingStatic(){
		return ! isTempDeclaredInStaticMethod();
	}
	
	public boolean canEnableSettingFinal(){
		if (fInitializeIn == INITIALIZE_IN_CONSTRUCTOR)
			return  canEnableSettingDeclareInConstructors() && ! tempHasAssignmentsOtherThanInitialization();
		else if (fInitializeIn == INITIALIZE_IN_FIELD)	
			return  canEnableSettingDeclareInFieldDeclaration() && ! tempHasAssignmentsOtherThanInitialization();
		else	
			return false;
	}
	
    private boolean tempHasAssignmentsOtherThanInitialization() {
    	TempAssignmentFinder assignmentFinder= new TempAssignmentFinder(fTempDeclarationNode);
    	fCompilationUnitNode.accept(assignmentFinder);
		return assignmentFinder.hasAssignments();
    }
	
	public boolean canEnableSettingDeclareInConstructors(){
		return ! fDeclareStatic &&
				! getMethodDeclaration().isConstructor() &&
				! isDeclaredInAnonymousClass() && 
				! isTempDeclaredInStaticMethod() && 
				tempHasInitializer();
	}
	
	public boolean canEnableSettingDeclareInMethod(){
		return ! fDeclareFinal && 
				tempHasInitializer();
	}
    private boolean tempHasInitializer() {
        return getTempInitializer() != null;
    }

	public boolean canEnableSettingDeclareInFieldDeclaration(){
		return tempHasInitializer();
	}

    private Expression getTempInitializer() {
    	return fTempDeclarationNode.getInitializer();
    }

    private boolean isTempDeclaredInStaticMethod() {
    	return Modifier.isStatic(getMethodDeclaration().getModifiers());
    }
    
    private MethodDeclaration getMethodDeclaration(){
    	return (MethodDeclaration)ASTNodes.getParent(fTempDeclarationNode, MethodDeclaration.class);
    }

    private boolean isDeclaredInAnonymousClass() {
    	return null != ASTNodes.getParent(fTempDeclarationNode, AnonymousClassDeclaration.class);
    }
	
    /*
     * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
     */
    public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
    	try{
    		
    		RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu}));
			if (result.hasFatalError())
				return result;

    		initAST();

			if (fTempDeclarationNode == null)
				return RefactoringStatus.createFatalErrorStatus("Select a declaration or a reference to a local variable.");
			
			if (! Checks.isDeclaredInMethod(fTempDeclarationNode))
				return RefactoringStatus.createFatalErrorStatus("Currently, only local variables declared in methods can be converted to fields.");
			
			if (isMethodParameter())
				return RefactoringStatus.createFatalErrorStatus("Cannot convert method parameters to fields.");

			if (isTempAnExceptionInCatchBlock())
				return RefactoringStatus.createFatalErrorStatus("Cannot convert exceptions declared in catch clauses to fields.");
	
			result.merge(checkLocalTypeUsageInTempDeclaration());

	        return result;
		} catch (CoreException e){	
			throw new JavaModelException(e);
    	} finally {
    		pm.done();
    	}
    }
    
    private RefactoringStatus checkLocalTypeUsageInTempDeclaration() {
    	//TODO
        return null;
    }
    
    private boolean isTempAnExceptionInCatchBlock() {
		return (fTempDeclarationNode.getParent() instanceof CatchClause);
    }
    
    private boolean isMethodParameter() {
    	return (fTempDeclarationNode.getParent() instanceof MethodDeclaration);
    }
    
	private void initAST(){
		fCompilationUnitNode= AST.parseCompilationUnit(fCu, true);
		fTempDeclarationNode= TempDeclarationFinder.findTempDeclaration(fCompilationUnitNode, fSelectionStart, fSelectionLength);
	}

    /*
     * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
     */
    public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
    	try{
	        return new RefactoringStatus();
    	} finally {
    		pm.done();
    	}
    }

    /*
     * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
     */
    public IChange createChange(IProgressMonitor pm) throws JavaModelException {
        return new NullChange();
    }

}
