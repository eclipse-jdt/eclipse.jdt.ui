package org.eclipse.jdt.ui.tests.refactoring;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
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
		runRefactoring(new RunRefactoringParameter());
	}

	public void testDelegateCreation() throws Exception {
		Map renamings= new HashMap();
		renamings.put("a", "newA");
		renamings.put("b", "newB");
		renamings.put("d", "newD");
		RunRefactoringParameter param= new RunRefactoringParameter();
		param.getters=true;
		param.setters=true;
		param.delegate=true;
		param.renamings=renamings;
		runRefactoring(param);
	}

	public void testDelegateCreationCodeStyle() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		Map originalOptions= javaProject.getOptions(false);
		try {
			Hashtable newOptions= new Hashtable();
			newOptions.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
			newOptions.put(JavaCore.CODEASSIST_FIELD_SUFFIXES, "G");
			javaProject.setOptions(newOptions);
			
			Map renamings= new HashMap();
			renamings.put("fDG", "newD");
			RunRefactoringParameter param= new RunRefactoringParameter();
			param.getters= true;
			param.setters= true;
			param.delegate= true;
			param.renamings= renamings;
			runRefactoring(param);
		} finally {
			javaProject.setOptions(originalOptions);
		}
	}
	
	public void testSimpleEnclosingCodeStyle() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		Map originalOptions= javaProject.getOptions(false);
		try {
			Hashtable newOptions= new Hashtable();
			newOptions.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
			newOptions.put(JavaCore.CODEASSIST_FIELD_SUFFIXES, "G");
			newOptions.put(JavaCore.CODEASSIST_ARGUMENT_PREFIXES, "a");
			newOptions.put(JavaCore.CODEASSIST_ARGUMENT_SUFFIXES, "M");
			javaProject.setOptions(newOptions);
			
			RunRefactoringParameter param= new RunRefactoringParameter();
			runRefactoring(param);
		} finally {
			javaProject.setOptions(originalOptions);
		}
	}
	
	public void testImportAddEnclosing() throws Exception {
		Map renamings= new HashMap();
		renamings.put("a", "permissions");
		createCaller(null);
		RunRefactoringParameter param= new RunRefactoringParameter();
		param.renamings=renamings;
		runRefactoring(param);
		checkCaller(null);
	}
	
	public void testInlineRename() throws Exception {
		RunRefactoringParameter param= new RunRefactoringParameter();
		Set useParams= new HashSet();
		useParams.add("xg");
		useParams.add("yg");
		param.useParams= useParams;
		runRefactoring(param);
	}

	public void testSimpleEnclosing() throws Exception{
		runRefactoring(new RunRefactoringParameter());
	}
	
	public void testVarArgsNotReordered() throws Exception{
		runRefactoring(new RunRefactoringParameter());
	}
	
	public void testVarArgsReordered() throws Exception{
		RunRefactoringParameter param= new RunRefactoringParameter();
		param.indexMap= createIndexMap(new String[] { "is", "a" });
		runRefactoring(param);
	}
	
	private Map createIndexMap(String[] strings) {
		Map result=new HashMap();
		for (int i= 0; i < strings.length; i++) {
			result.put(strings[i], new Integer(i));
		}
		return result;
	}

	public void testReorderGetter() throws Exception{
		RunRefactoringParameter param= new RunRefactoringParameter();
		param.getters=true;
		param.indexMap= createIndexMap(new String[] { "d", "a", "b" });
		runRefactoring(param);
	}
	
	public void testRecursiveSimple() throws Exception {
		RunRefactoringParameter param= new RunRefactoringParameter();
		runRefactoring(param);
	}
	
	public void testRecursiveSimpleReordered() throws Exception {
		RunRefactoringParameter param= new RunRefactoringParameter();
		runRefactoring(param);
	}
	
	public void testRecursiveReordered() throws Exception {
		RunRefactoringParameter param= new RunRefactoringParameter();
		param.indexMap= createIndexMap(new String[] { "y", "x" });
		runRefactoring(param);
	}
	
	public void testImportAddTopLevel() throws Exception {
		createCaller(DEFAULT_SUB_DIR);
		RunRefactoringParameter params= new RunRefactoringParameter();
		params.topLevel=true;
		params.className="TestImportAddTopLevelParameter";
		params.packageName="p.parameters";
		runRefactoring(params);
		checkCaller(DEFAULT_SUB_DIR);
	}
	
	public void testInterfaceMethod() throws Exception {
		createAdditionalFile(null, "TestInterfaceMethod2Impl");
		createAdditionalFile(null, "ITestInterfaceMethod");
		RunRefactoringParameter params= new RunRefactoringParameter();
		params.topLevel=true;
		params.expectFailure=true;
		runRefactoring(params);
		params.useSuggestedMethod=true;
		runRefactoring(params);
		checkAdditionalFile(null, "ITestInterfaceMethod");
		checkAdditionalFile(null, "TestInterfaceMethod2Impl");
	}
	
	public void testDefaultPackagePoint() throws Exception {
		RunRefactoringParameter params= new RunRefactoringParameter();
		params.className= "ArrayList";
		params.inputPackage= "";
		runRefactoring(params);
	}
	
	public void testDefaultPackagePointTopLevel() throws Exception {
		RunRefactoringParameter params= new RunRefactoringParameter();
		params.className= "ArrayList";
		params.topLevel= true;
		params.inputPackage= "";
		runRefactoring(params);
	}
	
	public void testImportNameSimple() throws Exception {
		RunRefactoringParameter params= new RunRefactoringParameter();
		params.className= "ArrayList";
		params.parameterName= "p";
		params.topLevel= true;
		runRefactoring(params);
	}

	private void createCaller(String subDir) throws Exception {
		createAdditionalFile(subDir, getCUName(true));
	}

	private void createAdditionalFile(String subDir, String fileName) throws Exception {
		IPackageFragment pack= getSubPackage(subDir);
		ICompilationUnit cu= createCUfromTestFile(pack, fileName, true);
		assertNotNull(cu);
		assertTrue(cu.exists());
	}

	private IPackageFragment getSubPackage(String subDir) throws Exception {
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
		checkAdditionalFile(subdir, getCUName(true));
	}

	private void checkAdditionalFile(String subdir, String fileName) throws Exception, JavaModelException, IOException {
		IPackageFragment pack= getSubPackage(subdir);
		ICompilationUnit cu= pack.getCompilationUnit(fileName+".java");
		assertNotNull(cu);
		assertTrue(cu.getPath() + " does not exist", cu.exists());
		String actual= cu.getSource();
		String expected= getFileContents(getOutputTestFileName(fileName));
		assertEqualLines(expected, actual);
	}


	public static class RunRefactoringParameter {
		public String parameterName;
		public Set useParams;
		public boolean useSuggestedMethod;
		public boolean topLevel;
		public boolean getters;
		public boolean setters;
		public boolean delegate;
		public Map renamings;
		public String className;
		public String packageName;
		public String inputPackage;
		public Map indexMap;
		public boolean commments;
		public boolean expectFailure;
	}

	private void runRefactoring(final RunRefactoringParameter parameter) throws Exception {
		IPackageFragment pack= getPackageP();
		if (parameter.inputPackage!=null){
			pack= getRoot().createPackageFragment(parameter.inputPackage,true,null);
		}
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
		configureRefactoring(parameter, ref);
		RefactoringStatus status= performRefactoring(ref);
		if (parameter.expectFailure) {
			assertNotNull(status);
			if (parameter.useSuggestedMethod){
				final RefactoringStatusEntry entry= status.getEntryMatchingSeverity(RefactoringStatus.FATAL);
				if (entry.getCode() == RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD || entry.getCode() == RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE) {
					final Object element= entry.getData();
					ref=new IntroduceParameterObjectRefactoring((IMethod) element);
					configureRefactoring(parameter, ref);
					status= performRefactoring(ref);
				}
			} else {
				return;
			}
		} 
		assertNull(status+"",status);
		String expected= getFileContents(getOutputTestFileName(getCUName(false)));
		assertNotNull(expected);
		ICompilationUnit resultCU= pack.getCompilationUnit(getCUFileName(false));
		assertNotNull(resultCU);
		assertTrue(resultCU.exists());
		String result= resultCU.getSource();
		assertNotNull(result);
		assertEqualLines(expected, result);
		if (parameter.topLevel){
			pack=getRoot().getPackageFragment(ref.getPackage());
			assertNotNull(pack);
			String parameterClassFile= ref.getClassName()+".java";
			ICompilationUnit unit= pack.getCompilationUnit(parameterClassFile);
			assertNotNull(unit);
			assertTrue(unit.exists());
			expected=getFileContents(getOutputTestFileName(ref.getClassName()));
			result=unit.getSource();
			assertNotNull(result);
			assertEqualLines(expected, result);
		}
	}

	private void configureRefactoring(final RunRefactoringParameter parameter, IntroduceParameterObjectRefactoring ref) {
		ref.setCreateAsTopLevel(parameter.topLevel);
		ref.setCreateGetter(parameter.getters);
		ref.setCreateSetter(parameter.setters);
		ref.setDelegateUpdating(parameter.delegate);
		ref.setCreateComments(parameter.commments);
		if (parameter.className != null)
			ref.setClassName(parameter.className);
		if (parameter.packageName != null)
			ref.setPackage(parameter.packageName);
		if (parameter.parameterName != null)
			ref.setParameterName(parameter.parameterName);
		List pis= ref.getParameterInfos();
		for (Iterator iter= pis.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (!pi.isAdded())
				pi.setCreateField(true);
			if (parameter.renamings != null) {
				String newName= (String) parameter.renamings.remove(pi.getNewName());
				if (newName != null) {
					pi.setNewName(newName);
				}
			}
			if (parameter.useParams != null) {
				pi.setCreateField(parameter.useParams.contains(pi.getNewName()));
			}
		}
		if (parameter.renamings != null)
			assertTrue("Some renamings did not match:"+parameter.renamings,parameter.renamings.size()==0);
		if (parameter.indexMap != null){
			Collections.sort(pis, new Comparator() {
			
				public int compare(Object arg0, Object arg1) {
					ParameterInfo pi0=(ParameterInfo) arg0;
					ParameterInfo pi1=(ParameterInfo) arg1;
					Integer idx0= (Integer) parameter.indexMap.get(pi0.getNewName());
					Integer idx1= (Integer) parameter.indexMap.get(pi1.getNewName());
					return idx0.compareTo(idx1);
				}
			
			});
		}
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
