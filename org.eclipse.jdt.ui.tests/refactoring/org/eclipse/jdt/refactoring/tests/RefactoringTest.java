/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.refactoring.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import junit.framework.TestCase;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.refactoring.tests.infra.TestExceptionHandler;
import org.eclipse.jdt.refactoring.tests.infra.TextBufferChangeCreator;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

public abstract class RefactoringTest extends TestCase {

	private IPackageFragmentRoot fRoot;
	private IPackageFragment fPackageP;
	private IJavaProject fJavaProject;
	
	public boolean fIsVerbose= false;

	public static final String TEST_PATH_PREFIX= "";

	private static final String TEST_INPUT_INFIX= "/in/";
	private static final String TEST_OUTPUT_INFIX= "/out/";
	private static final String CONTAINER= "src";
	protected static final ITextBufferChangeCreator fgChangeCreator= new TextBufferChangeCreator();
	
	private static final IProgressMonitor fgNullProgressMonitor= new NullProgressMonitor();

	public RefactoringTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		fJavaProject= MySetup.getProject();
		fRoot= MySetup.getDefaultSourceFolder();
		fPackageP= MySetup.getPackageP();
		
		if (fIsVerbose){
			System.out.println("\n---------------------------------------------");
			System.out.println("\nTest:" + getClass() + "." + getName());
		}	
		Refactoring.getUndoManager().flush();
	}

	protected void tearDown() throws Exception {
		//JavaProjectHelper.removeSourceContainer(fJavaProject, CONTAINER);
		if (fPackageP.exists()){	
			IJavaElement[] kids= fPackageP.getChildren();
			for (int i= 0; i < kids.length; i++){
				if (kids[i] instanceof ISourceManipulation)
					((ISourceManipulation)kids[i]).delete(true, null);
			}
		}	
		
		if (fRoot.exists()){
			IJavaElement[] packages= fRoot.getChildren();
			for (int i= 0; i < packages.length; i++){
				IPackageFragment pack= (IPackageFragment)packages[i];
				if (! pack.equals(fPackageP))	
					pack.delete(true, null);
			}
		}
	}

	protected IPackageFragmentRoot getRoot() {
		return fRoot;
	}

	protected IPackageFragment getPackageP() {
		return fPackageP;
	}

	protected final RefactoringStatus performRefactoring(IRefactoring ref) throws JavaModelException {
		if (ref instanceof Refactoring)
			((Refactoring)ref).setUnsavedFiles(new IFile[0]);
		RefactoringStatus status= ref.checkPreconditions(fgNullProgressMonitor);
		if (!status.isOK())
			return status;

		IChange change= ref.createChange(fgNullProgressMonitor);
		performChange(change);
		
		// XXX: this should be done by someone else
		Refactoring.getUndoManager().addUndo(ref.getName(), change.getUndoChange());

		return null;
	}
	
	protected void performChange(IChange change) throws JavaModelException{
		change.perform(new ChangeContext(new TestExceptionHandler()), fgNullProgressMonitor);
	}

	/****************  helpers  ******************/
	/**** mostly not general, just shortcuts *****/

	/**
	 * @param pack
	 * @param name
	 * @param contents
	 */
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

	protected InputStream getFileInputStream(String fileName) throws IOException {
		IPluginDescriptor plugin= Platform.getPluginRegistry().getPluginDescriptors("Refactoring Tests Resources")[0];
		URL url= new URL(plugin.getInstallURL().toString() + fileName);
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

	/**
	 *  example "RenameType/"
	 */
	protected String getTestPath() {
		return TEST_PATH_PREFIX + getRefactoringPath();
	}

	/**
	 * @param cuName
	 * @param infix
	 * example "RenameTest/test0 + infix + cuName.java"
	 */
	private String createTestFileName(String cuName, String infix) {
		return getTestPath() + getName() + infix + cuName + ".java";
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
	
	protected void printTestDisabledMessage(String explanation){
		System.out.println("\n" +getClass().getName() + "::"+ getName() + " disabled (" + explanation + ")");
	}
}

