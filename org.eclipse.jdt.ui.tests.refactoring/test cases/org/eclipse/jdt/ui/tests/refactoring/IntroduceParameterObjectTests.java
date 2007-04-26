package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.structure.IntroduceParameterObjectRefactoring;

public class IntroduceParameterObjectTests extends RefactoringTest {

	private static final String DEFAULT_SUB_DIR= "sub";
	private static final Class CLAZZ= IntroduceParameterObjectTests.class;
	private static final String REFACTORING_PATH= "IntroduceParameterObject/";

	public IntroduceParameterObjectTests(String name) {
		super(name);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(CLAZZ));
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}

	public void testBodyUpdate() throws Exception {
		runDefaultRefactoring();
	}

	private void runDefaultRefactoring() throws Exception {
		runRefactoring(false, false, false, false, Collections.EMPTY_MAP);
	}

	public void testDelegateCreation() throws Exception {
		Map renamings= new HashMap();
		renamings.put("a", "newA");
		renamings.put("b", "newB");
		renamings.put("d", "newD");
		runRefactoring(false, true, true, true, renamings);
	}

	public void testImportAddEnclosing() throws Exception {
		Map renamings= new HashMap();
		renamings.put("a", "permissions");
		createCaller(null);
		runRefactoring(false, false, false, false, renamings);
		checkCaller(null);
	}

	public void testSimpleEnclosing() throws Exception{
		runDefaultRefactoring();
	}
	
	public void testVarArgsNotReordered() throws Exception{
		runDefaultRefactoring();
	}
	
	public void testVarArgsReordered() throws Exception{
		Map indexMap= new HashMap();
		indexMap.put("is", new Integer(0));
		indexMap.put("a", new Integer(1));
		runRefactoring(false, false, false, false, Collections.EMPTY_MAP, null, null, indexMap);
	}
	
	public void testReorderGetter() throws Exception{
		Map indexMap= new HashMap();
		indexMap.put("d", new Integer(0));
		indexMap.put("a", new Integer(1));
		indexMap.put("b", new Integer(2));
		runRefactoring(false, true, false, false, Collections.EMPTY_MAP, null, null, indexMap);
	}
	
	public void donottestImportAddTopLevel() throws Exception { //XXX Enable later
		createCaller(DEFAULT_SUB_DIR);
		runRefactoring(true, false, false, false, Collections.EMPTY_MAP,"TestImportAddTopLevelParameter","p.parameters");
		checkCaller(DEFAULT_SUB_DIR);
	}

	private void createCaller(String subDir) throws Exception {
		IPackageFragment pack= getPackage(subDir);
		ICompilationUnit cu= createCUfromTestFile(pack, getCUName(true), true);
		assertNotNull(cu);
		assertTrue(cu.exists());
	}

	private IPackageFragment getPackage(String subDir) throws Exception {
		IPackageFragment pack= getPackageP();
		if (subDir != null) {
			String packageName= pack.getElementName() + "." + subDir;
			pack= getRoot().getPackageFragment(packageName);
			if (!pack.exists()) {
				IPackageFragment create= getRoot().createPackageFragment(packageName, true, new NullProgressMonitor());
				assertNotNull(create);
				assertTrue(create.exists());
				return create;
			}
		}
		return pack;
	}

	private void checkCaller(String subdir) throws Exception {
		IPackageFragment pack= getPackage(subdir);
		ICompilationUnit cu= pack.getCompilationUnit(getCUFileName(true));
		assertNotNull(cu);
		assertTrue(cu.getPath() + " does not exist", cu.exists());
		String actual= cu.getSource();
		String expected= getFileContents(getOutputTestFileName(getCUName(true)));
		assertEqualLines(expected, actual);
	}

	private void runRefactoring(boolean topLevel, boolean getters, boolean setters, boolean delegate, Map renamings) throws Exception {
		runRefactoring(topLevel, getters, setters, delegate, renamings, null, null, null);
	}

	private void runRefactoring(boolean topLevel, boolean getters, boolean setters, boolean delegate, Map renamings, String className, String packageName) throws Exception {
		runRefactoring(topLevel, getters, setters, delegate, renamings, className, packageName, null);
	}
	
	private void runRefactoring(boolean topLevel, boolean getters, boolean setters, boolean delegate, Map renamings, String className, String packageName, final Map indexMap) throws Exception {
		IPackageFragment pack= getPackageP();
		ICompilationUnit cu= createCUfromTestFile(pack, getCUName(false), true);
		IType type= cu.getType(getCUName(false));
		assertNotNull(type);
		assertTrue(type.exists());
		IMethod fooMethod= null;
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			IMethod method= methods[i];
			if ("foo".equals(method.getElementName())) {
				fooMethod= method;
			}
		}
		assertNotNull(fooMethod);
		assertTrue(fooMethod.exists());
		IntroduceParameterObjectRefactoring ref= new IntroduceParameterObjectRefactoring(fooMethod);
		ref.setCreateAsTopLevel(topLevel);
		ref.setCreateGetter(getters);
		ref.setCreateSetter(setters);
		ref.setDelegateUpdating(delegate);
		if (className != null)
			ref.setClassName(className);
		if (packageName != null)
			ref.setPackage(packageName);
		List pis= ref.getParameterInfos();
		for (Iterator iter= pis.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (!pi.isAdded())
				pi.setCreateField(true);
			if (renamings != null) {
				String newName= (String) renamings.get(pi.getNewName());
				if (newName != null)
					pi.setNewName(newName);
			}
		}
		if (indexMap!=null){
			Collections.sort(pis, new Comparator() {
			
				public int compare(Object arg0, Object arg1) {
					ParameterInfo pi0=(ParameterInfo) arg0;
					ParameterInfo pi1=(ParameterInfo) arg1;
					Integer idx0= (Integer) indexMap.get(pi0.getNewName());
					Integer idx1= (Integer) indexMap.get(pi1.getNewName());
					return idx0.compareTo(idx1);
				}
			
			});
		}
		RefactoringStatus status= performRefactoring(ref);
		assertNull(status+"",status);
		String expected= getFileContents(getOutputTestFileName(getCUName(false)));
		assertNotNull(expected);
		ICompilationUnit resultCU= pack.getCompilationUnit(getCUFileName(false));
		assertNotNull(resultCU);
		assertTrue(resultCU.exists());
		String result= resultCU.getSource();
		assertNotNull(result);
		assertEqualLines(expected, result);
	}

	private String getCUFileName(boolean caller) {
		StringBuffer sb= new StringBuffer();
		sb.append(getCUName(caller));
		sb.append(".java");
		return sb.toString();
	}

	private String getCUName(boolean caller) {
		StringBuffer sb= new StringBuffer();
		sb.append(Character.toUpperCase(getName().charAt(0)) + getName().substring(1));
		if (caller)
			sb.append("Caller");
		return sb.toString();
	}
}
