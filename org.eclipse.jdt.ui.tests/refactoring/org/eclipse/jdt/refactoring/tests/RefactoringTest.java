/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests;

import java.io.BufferedReader;import java.io.IOException;import java.io.InputStream;import java.io.InputStreamReader;import java.net.URL;import junit.framework.TestCase;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.ChangeContext;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.IRefactoring;import org.eclipse.jdt.core.refactoring.Refactoring;import org.eclipse.jdt.core.refactoring.RefactoringStatus;import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;import org.eclipse.jdt.core.search.IJavaSearchScope;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.internal.core.JavaModelManager;import org.eclipse.jdt.internal.ui.util.JdtHackFinder;import org.eclipse.jdt.refactoring.tests.infra.TestExceptionHandler;import org.eclipse.jdt.refactoring.tests.infra.TextBufferChangeCreator;import org.eclipse.jdt.testplugin.JavaTestProject;import org.eclipse.jdt.testplugin.JavaTestSetup;

public abstract class RefactoringTest extends TestCase {

	private IPackageFragmentRoot fRoot;
	private IPackageFragment fPackageP;
	private IJavaSearchScope fScope;
	
	public boolean fIsVerbose= false;

	public static final String TEST_PATH_PREFIX= "Refactoring Tests Resources/";

	private static final String TEST_INPUT_INFIX= "/in/";
	private static final String TEST_OUTPUT_INFIX= "/out/";
	private static final String CONTAINER= "src";
	protected static final ITextBufferChangeCreator fgChangeCreator= new TextBufferChangeCreator();
	
	private static final IProgressMonitor fgNullProgressMonitor= new NullProgressMonitor();

	public RefactoringTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		JavaTestProject testProject= JavaTestSetup.getTestProject();

		fRoot= testProject.addSourceContainer(CONTAINER);
		fPackageP= fRoot.createPackageFragment("p", true, null);

		fScope= SearchEngine.createWorkspaceScope();
		if (fIsVerbose){
			System.out.println("---------------------------------------------");
			System.out.println("Test:" + getClass() + "." + name());
		}	
		Refactoring.getUndoManager().flush();
	}

	protected void tearDown() throws Exception {
		JavaTestProject testProject= JavaTestSetup.getTestProject();
		testProject.removeSourceContainer(CONTAINER);
	}

	protected	IPackageFragmentRoot getRoot() {
		return fRoot;
	}

	protected	IPackageFragment getPackageP() {
		return fPackageP;
	}

	protected	IJavaSearchScope getScope() {
		return fScope;
	}

	protected final RefactoringStatus performRefactoring(IRefactoring ref) throws JavaModelException {

		RefactoringStatus status= ref.checkPreconditions(fgNullProgressMonitor);
		if (!status.isOK())
			return status;

		IChange change= ref.createChange(fgNullProgressMonitor);
		change.perform(new ChangeContext(new TestExceptionHandler()), fgNullProgressMonitor);

		JdtHackFinder.fixme("this should be done by someone else");
		Refactoring.getUndoManager().addUndo(ref.getName(), change.getUndoChange());

		return null;
	}

	/****************  helpers  ******************/
	/**** mostly not general, just shortcuts *****/

	protected ICompilationUnit createCU(IPackageFragment pack, String name, String contents) throws Exception {
		ICompilationUnit cu= pack.createCompilationUnit(name, contents, true, null);
		cu.save(null, true);
		forceIndexing();
		return cu;
	}

	//BOGUS??
	protected void forceIndexing() {
		JavaModelManager.getJavaModelManager().getIndexManager().checkIndexConsistency();
	}

	/**
	 * BOGUS: this might be already implemented somewhere else (JDK? Core?)
	 */
	protected String getFileContents(String fileName) throws IOException {
		if (fIsVerbose)
			System.out.println("loading:" + fileName);
			
		InputStream in= getFileInputStream(fileName);
		BufferedReader br= new BufferedReader(new InputStreamReader(in));
		
		StringBuffer sb= new StringBuffer();
		try {
			int read= 0;
			while ((read= br.read()) != -1)
				sb.append((char) read);
		} finally {
			br.close();
		}
		return sb.toString();
	}

	private InputStream getFileInputStream(String fileName) throws IOException {
		String s= RefactoringTest.class.getResource("RefactoringTest.class").toString();
		int index= s.indexOf("/bin/org/eclipse/jdt/refactoring/tests/RefactoringTest.class");
		if (index == -1)
			throw new IllegalArgumentException();
			
		s= s.substring(0, index);
		s= s + "/" + fileName;
		URL url= new URL(s);
		return url.openStream();
	}

	protected IType getType(ICompilationUnit cu, String name) throws JavaModelException {
		IType[] types= cu.getAllTypes();
		for (int i= 0; i < types.length; i++)
			if (types[i].getElementName().equals(name))
				return types[i];
		return null;
	}
	
	/**
	 * subclasses override to inform about the location of their test cases
	 */
	protected String getRefactoringPath() {
		return "";
	}

	/*package*/ String getTestPath() {
		return TEST_PATH_PREFIX + getRefactoringPath();
	}

	private String createTestFileName(String cuName, String infix) {
		return getTestPath() + name() + infix + cuName + ".java";
	}
	
	protected String getInputTestFileName(String cuName) {
		return createTestFileName(cuName, TEST_INPUT_INFIX);
	}
	
	/**
	 * @param subDirName example "p/" or "org/eclipse/jdt/"
	 */
	protected String getInputTestFileName(String cuName, String subDirName) {
		return createTestFileName(cuName, TEST_INPUT_INFIX + subDirName);
	}

	protected String getOutputTestFileName(String cuName) {
		return createTestFileName(cuName, TEST_OUTPUT_INFIX);
	}
	
	/**
	 * @param subDirName example "p/" or "org/eclipse/jdt/"
	 */
	protected String getOutputTestFileName(String cuName, String subDirName) {
		return createTestFileName(cuName, TEST_OUTPUT_INFIX + subDirName);
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName) throws Exception {
		return createCUfromTestFile(pack, cuName, true);
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName, String subDirName) throws Exception {
		return createCUfromTestFile(pack, cuName, subDirName, true);
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName, boolean input) throws Exception {
		String contents= input 
					? getFileContents(getInputTestFileName(cuName))
					: getFileContents(getOutputTestFileName(cuName));
		return createCU(pack, cuName + ".java", contents);
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName, String subDirName, boolean input) throws Exception {
		String contents= input 
			? getFileContents(getInputTestFileName(cuName, subDirName))
			: getFileContents(getOutputTestFileName(cuName, subDirName));
		
		return createCU(pack, cuName + ".java", contents);
	}
}

