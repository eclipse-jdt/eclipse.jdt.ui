/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring;

import org.eclipse.core.runtime.NullProgressMonitor;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.PromoteTempToFieldRefactoring;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
public class PromoteTempToFieldTests extends RefactoringTest{
	
	private static final Class clazz= PromoteTempToFieldTests.class;
	private static final String REFACTORING_PATH= "PromoteTempToField/";
	
	public PromoteTempToFieldTests(String name){
		super(name);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	private String getSimpleTestFileName(boolean canRename, boolean input){
		String fileName = "A_" + getName();
		if (canRename)
			fileName += input ? "_in": "_out";
		return fileName + ".java"; 
	}

	private String getSimpleEnablementTestFileName(){
		return "A_" + getName() + ".java"; 
	}
	
	private String getTestFileName(boolean canRename, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += (canRename ? "canPromote/": "cannotPromote/");
		return fileName + getSimpleTestFileName(canRename, input);
	}
	
	private String getEnablementTestFileName(){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += "testEnablement/";
		return fileName + getSimpleEnablementTestFileName();
	}
	
	
	private String getFailingTestFileName(){
		return getTestFileName(false, false);
	}
	private String getPassingTestFileName(boolean input){
		return getTestFileName(true, input);
	}
	
	//------------
	protected final ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canPromote, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canPromote, input), getFileContents(getTestFileName(canPromote, input)));
	}

	protected final ICompilationUnit createCUfromEnablementTestFile(IPackageFragment pack) throws Exception {
		return createCU(pack, getSimpleEnablementTestFileName(), getFileContents(getEnablementTestFileName()));
	}
	
    private PromoteTempToFieldRefactoring getRefactoringObject(ISourceRange selection, ICompilationUnit cu, String newName, boolean declareStatic, boolean declareFinal, int initializeIn, int accessModifier) {
        PromoteTempToFieldRefactoring ref= new PromoteTempToFieldRefactoring(cu, selection.getOffset(), selection.getLength());
        ref.setFieldName(newName);
        ref.setDeclareFinal(declareFinal);
        ref.setDeclareStatic(declareStatic);
        ref.setInitializeIn(initializeIn);
        ref.setAccessModifier(accessModifier);
        return ref;
    }

	private void passHelper(int startLine, int startColumn, int endLine, int endColumn, 
						  String newName,
						  boolean declareStatic,
						  boolean declareFinal,
						  int initializeIn,
						  int accessModifier) throws Exception{
						  	
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);						  	
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
        PromoteTempToFieldRefactoring ref= getRefactoringObject(selection, cu, newName, declareStatic, declareFinal, initializeIn, accessModifier);
		
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEquals("incorrect changes", getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private void failHelper(int startLine, int startColumn, int endLine, int endColumn, 
						  String newName,
						  boolean declareStatic,
						  boolean declareFinal,
						  int initializeIn,
						  int accessModifier,
						  int expectedSeverity) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);						  	
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
        PromoteTempToFieldRefactoring ref= getRefactoringObject(selection, cu, newName, declareStatic, declareFinal, initializeIn, accessModifier);
		
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
		assertEquals("incorrect severity:", expectedSeverity, result.getSeverity());
	}

	private void enablementHelper(int startLine, int startColumn, int endLine, int endColumn,
						  String newName,
						  boolean declareStatic,
						  boolean declareFinal,
						  int initializeIn,
						  int accessModifier,
						  boolean expectedCanEnableSettingFinal,
						  boolean expectedCanEnableSettingStatic,
						  boolean expectedCanEnableInitInField,
						  boolean expectedCanEnableInitInMethod,
  						  boolean expectedCanEnableInitInConstructors) throws Exception{
		ICompilationUnit cu= createCUfromEnablementTestFile(getPackageP());
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
        PromoteTempToFieldRefactoring ref= getRefactoringObject(selection, cu, newName, declareStatic, declareFinal, initializeIn, accessModifier);
		RefactoringStatus result= ref.checkActivation(new NullProgressMonitor());
		assertEquals("activation checking was supposed to pass", RefactoringStatus.OK, result.getSeverity());
		
		assertEquals("incorrect in-constructor enablement", expectedCanEnableInitInConstructors, ref.canEnableSettingDeclareInConstructors());
		assertEquals("incorrect in-field enablement", expectedCanEnableInitInField, ref.canEnableSettingDeclareInFieldDeclaration());
		assertEquals("incorrect in-method enablement", expectedCanEnableInitInMethod, ref.canEnableSettingDeclareInMethod());
		assertEquals("incorrect static enablement", expectedCanEnableSettingStatic, ref.canEnableSettingStatic());
		assertEquals("incorrect final enablement", expectedCanEnableSettingFinal, ref.canEnableSettingFinal());
	}
	private void enablementHelper1(int startLine, int startColumn, int endLine, int endColumn,
						  boolean expectedCanEnableSettingFinal,
						  boolean expectedCanEnableSettingStatic,
						  boolean expectedCanEnableInitInField,
						  boolean expectedCanEnableInitInMethod,
  						  boolean expectedCanEnableInitInConstructors) throws Exception{
  	   enablementHelper(startLine, startColumn, endLine, endColumn, "i", false, false, PromoteTempToFieldRefactoring.INITIALIZE_IN_METHOD, JdtFlags.VISIBILITY_CODE_PRIVATE, 
  	   				expectedCanEnableSettingFinal, expectedCanEnableSettingStatic, expectedCanEnableInitInField, expectedCanEnableInitInMethod, expectedCanEnableInitInConstructors);
	}	

	///---------------------- tests -------------------------//
	
	public void testEnablement0() throws Exception{
        boolean expectedCanEnableInitInConstructors	= true;
        boolean expectedCanEnableInitInMethod			= true;
        boolean expectedCanEnableInitInField			= true;
        boolean expectedCanEnableSettingStatic			= true;
        boolean expectedCanEnableSettingFinal			= true;
        
        String newName= "i";
		boolean declareStatic = false;
	  	boolean declareFinal= false;
	  	int initializeIn= PromoteTempToFieldRefactoring.INITIALIZE_IN_FIELD;
	  	int accessModifier= JdtFlags.VISIBILITY_CODE_PRIVATE;
        
		enablementHelper(5, 13, 5, 14, newName, declareStatic, declareFinal, initializeIn, accessModifier,
					expectedCanEnableSettingFinal, expectedCanEnableSettingStatic, expectedCanEnableInitInField, expectedCanEnableInitInMethod, expectedCanEnableInitInConstructors);
	}

	public void testEnablement1() throws Exception{
        boolean expectedCanEnableInitInConstructors	= false;
        boolean expectedCanEnableInitInMethod			= false;
        boolean expectedCanEnableInitInField			= false;
        boolean expectedCanEnableSettingStatic			= true;
        boolean expectedCanEnableSettingFinal			= false;
		enablementHelper1(5, 13, 5, 14, expectedCanEnableSettingFinal, expectedCanEnableSettingStatic, expectedCanEnableInitInField, expectedCanEnableInitInMethod, expectedCanEnableInitInConstructors);
	}
	
	public void testEnablement2() throws Exception{
        boolean expectedCanEnableInitInConstructors	= false;
        boolean expectedCanEnableInitInMethod			= false;
        boolean expectedCanEnableInitInField			= false;
        boolean expectedCanEnableSettingStatic			= true;
        boolean expectedCanEnableSettingFinal			= false;
		enablementHelper1(5, 13, 5, 14, expectedCanEnableSettingFinal, expectedCanEnableSettingStatic, expectedCanEnableInitInField, expectedCanEnableInitInMethod, expectedCanEnableInitInConstructors);
	}
	
	public void testEnablement3() throws Exception{
        boolean expectedCanEnableInitInConstructors	= true;
        boolean expectedCanEnableInitInMethod			= true;
        boolean expectedCanEnableInitInField			= true;
        boolean expectedCanEnableSettingStatic			= true;
        boolean expectedCanEnableSettingFinal			= false;
		enablementHelper1(5, 13, 5, 14, expectedCanEnableSettingFinal, expectedCanEnableSettingStatic, expectedCanEnableInitInField, expectedCanEnableInitInMethod, expectedCanEnableInitInConstructors);
	}
	
	public void testEnablement4() throws Exception{
        boolean expectedCanEnableInitInConstructors	= false;
        boolean expectedCanEnableInitInMethod			= true;
        boolean expectedCanEnableInitInField			= true;
        boolean expectedCanEnableSettingStatic			= true;
        boolean expectedCanEnableSettingFinal			= true;

        String newName= "i";
		boolean declareStatic = false;
	  	boolean declareFinal= false;
	  	int initializeIn= PromoteTempToFieldRefactoring.INITIALIZE_IN_FIELD;
	  	int accessModifier= JdtFlags.VISIBILITY_CODE_PRIVATE;

		enablementHelper(5, 13, 5, 14, newName, declareStatic, declareFinal, initializeIn, accessModifier,
					expectedCanEnableSettingFinal, expectedCanEnableSettingStatic, expectedCanEnableInitInField, expectedCanEnableInitInMethod, expectedCanEnableInitInConstructors);
	}
	
	public void testEnablement5() throws Exception{
        boolean expectedCanEnableInitInConstructors	= false;
        boolean expectedCanEnableInitInMethod			= true;
        boolean expectedCanEnableInitInField			= true;
        boolean expectedCanEnableSettingStatic			= true;
        boolean expectedCanEnableSettingFinal			= true;
        
        String newName= "i";
		boolean declareStatic = false;
	  	boolean declareFinal= false;
	  	int initializeIn= PromoteTempToFieldRefactoring.INITIALIZE_IN_FIELD;
	  	int accessModifier= JdtFlags.VISIBILITY_CODE_PRIVATE;
        
		enablementHelper(7, 21, 7, 22, newName, declareStatic, declareFinal, initializeIn, accessModifier,
					expectedCanEnableSettingFinal, expectedCanEnableSettingStatic, expectedCanEnableInitInField, expectedCanEnableInitInMethod, expectedCanEnableInitInConstructors);
	}
	
	public void testEnablement6() throws Exception{
        boolean expectedCanEnableInitInConstructors	= false;
        boolean expectedCanEnableInitInMethod			= false;
        boolean expectedCanEnableInitInField			= false;
        boolean expectedCanEnableSettingStatic			= true;
        boolean expectedCanEnableSettingFinal			= false;
		enablementHelper1(7, 21, 7, 22, expectedCanEnableSettingFinal, expectedCanEnableSettingStatic, expectedCanEnableInitInField, expectedCanEnableInitInMethod, expectedCanEnableInitInConstructors);
	}
	
	public void testEnablement7() throws Exception{
        boolean expectedCanEnableInitInConstructors	= false;
        boolean expectedCanEnableInitInMethod			= true;
        boolean expectedCanEnableInitInField			= true;
        boolean expectedCanEnableSettingStatic			= false;
        boolean expectedCanEnableSettingFinal			= false;
		enablementHelper1(5, 13, 5, 14, expectedCanEnableSettingFinal, expectedCanEnableSettingStatic, expectedCanEnableInitInField, expectedCanEnableInitInMethod, expectedCanEnableInitInConstructors);
	}
	
}