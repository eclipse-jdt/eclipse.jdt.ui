package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.core.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.ui.TestPluginUILauncher;

import experiments.AbstractFieldRefactoring;

public class AbstractFieldTests extends RefactoringTest {

	private static final String REFACTORING_PATH= "AbstractField/";
	
	public AbstractFieldTests(String name) {
		super(name);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static void main(String[] args) {
		TestPluginUILauncher.run(TestPluginLauncher.getLocationFromProperties(), AbstractFieldTests.class, args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}
	
	public static Test noSetupSuite() {
		return new TestSuite(AbstractFieldTests.class);
	}
		
	private String getSimpleTestFileName(boolean canReorder, boolean input){
		String fileName = "A_" + name();
		if (canReorder)
			fileName += input ? "_in": "_out";
		return fileName + ".java"; 
	}
	
	private String getTestFileName(boolean canReorder, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += (canReorder ? "can/": "cannot/");
		return fileName + getSimpleTestFileName(canReorder, input);
	}
	
	private String getPassingTestFileName(boolean input){
		return getTestFileName(true, input);
	}

	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canRename, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canRename, input), getFileContents(getTestFileName(canRename, input)));
	}
	
	private AbstractFieldRefactoring createRefactoring(IField field, boolean createGetter, boolean createSetter, int getterModifier, int setterModifier, 
						 							String getterName, String setterName, boolean asymetric){
		AbstractFieldRefactoring ref= new AbstractFieldRefactoring(fgChangeCreator, field, asymetric);
		ref.setCreateGetter(createGetter);
		ref.setCreateSetter(createSetter);
		ref.setGetterModifier(getterModifier);
		ref.setSetterModifier(setterModifier);
		ref.setGetterName(getterName);
		ref.setSetterName(setterName);
		return ref;
	}
	
	private void helper1(String fieldName, boolean createGetter, boolean createSetter, int getterModifier, int setterModifier, 
						 String getterName, String setterName, boolean asymetric) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		
		IRefactoring ref= createRefactoring(classA.getField(fieldName), createGetter, 
						createSetter, getterModifier, setterModifier, getterName, setterName, asymetric);
		
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assert(newCuName + " does not exist", newcu.exists());
		assertEquals("invalid renaming", getFileContents(getTestFileName(true, false)), newcu.getSource());		
	}
	
}

