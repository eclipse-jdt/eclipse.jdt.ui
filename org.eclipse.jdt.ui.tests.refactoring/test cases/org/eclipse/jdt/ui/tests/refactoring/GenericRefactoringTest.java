/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public abstract class GenericRefactoringTest {
	protected static final Charset ENCODING= StandardCharsets.UTF_8;

	@Rule
	public RefactoringTestSetup rts;

	@Rule
	public TestName tn= new TestName();

	public GenericRefactoringTest() {
	}

	public GenericRefactoringTest(RefactoringTestSetup rts) {
		this.rts=rts;
	}

	/**
	 * If <code>true</code> a descriptor is created from the change.
	 * The new descriptor is then used to create the refactoring again
	 * and run the refactoring. As this is very time consuming this should
	 * be <code>false</code> by default.
	 */
	private static final boolean DESCRIPTOR_TEST= false;

	protected IPackageFragmentRoot fRoot;
	protected IPackageFragment fPackageP;
	protected IPackageFragment fPackageQ;

	public boolean fIsVerbose= false;
	public boolean fIsPreDeltaTest= false;

	public static final String TEST_PATH_PREFIX= "";

	protected static final String TEST_INPUT_INFIX= "/in/";
	protected static final String TEST_OUTPUT_INFIX= "/out/";
	protected static final String CONTAINER= "src";

	protected static final List<String> PROJECT_RESOURCE_CHILDREN= Collections.unmodifiableList(Arrays.asList(".project", ".classpath", ".settings"));

	@Before
	public void genericbefore() throws Exception {
		fRoot= rts.getDefaultSourceFolder();
		fPackageP= rts.getPackageP();
		fPackageQ= rts.getPackageQ();
		fIsPreDeltaTest= false;

		if (fIsVerbose){
			System.out.println("\n---------------------------------------------");
			System.out.println("\nTest:" + getClass() + "." + getName());
		}
		RefactoringCore.getUndoManager().flush();
	}

	protected String getName() {
		return tn.getMethodName();
	}

	protected void mustPerformDummySearch() throws Exception {
		JavaProjectHelper.mustPerformDummySearch(getPackageP());
	}

	protected void performDummySearch() throws Exception {
		JavaProjectHelper.performDummySearch(getPackageP());
	}

	/**
	 * Removes contents of {@link #getPackageP()}, of {@link #getRoot()} (except for p) and of the
	 * Java project (except for src and the JRE library).
	 *
	 * @throws Exception in case of errors
	 */
	@After
	public void genericafter() throws Exception {
		refreshFromLocal();
		performDummySearch();

		final boolean pExists= getPackageP().exists();
		if (pExists) {
			tryDeletingAllJavaChildren(getPackageP());
			tryDeletingAllNonJavaChildResources(getPackageP());
		}

		final boolean qExists= getPackageQ().exists();
		if (qExists) {
			tryDeletingAllJavaChildren(getPackageQ());
			tryDeletingAllNonJavaChildResources(getPackageQ());
		}

		if (getRoot().exists()){
			for (IJavaElement p : getRoot().getChildren()) {
				IPackageFragment pack= (IPackageFragment) p;
				if (!pack.equals(getPackageP()) && !pack.equals(getPackageQ()) && pack.exists() && !pack.isReadOnly())
					if (pack.isDefaultPackage())
						JavaProjectHelper.deletePackage(pack); // also delete packages with subpackages
					else
						JavaProjectHelper.delete(pack.getResource()); // also delete packages with subpackages
			}
			// Restore package 'p'
			if (!pExists)
				getRoot().createPackageFragment("p", true, null);
			// Restore package 'q'
			if (!qExists)
				getRoot().createPackageFragment("q", true, null);

			tryDeletingAllNonJavaChildResources(getRoot());
		}

		restoreTestProject();
	}

	private void restoreTestProject() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		if (javaProject.exists()) {
			IClasspathEntry srcEntry= getRoot().getRawClasspathEntry();
			IClasspathEntry jreEntry= rts.getJRELibrary().getRawClasspathEntry();
			ArrayList<IClasspathEntry> newCPEs= new ArrayList<>();
			boolean cpChanged= false;
			for (IClasspathEntry cpe : javaProject.getRawClasspath()) {
				if (cpe.equals(srcEntry) || cpe.equals(jreEntry)) {
					newCPEs.add(cpe);
				} else {
					cpChanged= true;
				}
			}
			if (cpChanged) {
				IClasspathEntry[] newCPEsArray= newCPEs.toArray(new IClasspathEntry[newCPEs.size()]);
				javaProject.setRawClasspath(newCPEsArray, null);
			}

			for (Object kid : javaProject.getNonJavaResources()) {
				if (kid instanceof IResource) {
					IResource resource= (IResource) kid;
					if (! PROJECT_RESOURCE_CHILDREN.contains(resource.getName())) {
						JavaProjectHelper.delete(resource);
					}
				}
			}
		}
	}

	private void refreshFromLocal() throws CoreException {
		if (getRoot().exists())
			getRoot().getResource().refreshLocal(IResource.DEPTH_INFINITE, null);
		else if (getPackageP().exists())//don't refresh package if root already refreshed
			getPackageP().getResource().refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	private static void tryDeletingAllNonJavaChildResources(IPackageFragment pack) throws CoreException {
		for (Object nonJavaKid : pack.getNonJavaResources()) {
			if (nonJavaKid instanceof IResource) {
				IResource resource= (IResource) nonJavaKid;
				JavaProjectHelper.delete(resource);
			}
		}
	}

	private static void tryDeletingAllNonJavaChildResources(IPackageFragmentRoot root) throws CoreException {
		for (Object nonJavaKid : root.getNonJavaResources()) {
			if (nonJavaKid instanceof IResource) {
				IResource resource= (IResource) nonJavaKid;
				JavaProjectHelper.delete(resource);
			}
		}
	}

	private static void tryDeletingAllJavaChildren(IPackageFragment pack) throws CoreException {
		for (IJavaElement kid : pack.getChildren()) {
			if (kid instanceof ISourceManipulation) {
				if (kid.exists() && !kid.isReadOnly()) {
					JavaProjectHelper.delete(kid);
				}
			}
		}
	}

	protected IPackageFragmentRoot getRoot() {
		return fRoot;
	}

	protected IPackageFragment getPackageP() {
		return fPackageP;
	}

	protected IPackageFragment getPackageQ() {
		return fPackageQ;
	}

	protected final RefactoringStatus performRefactoring(RefactoringDescriptor descriptor) throws Exception {
		return performRefactoring(descriptor, true);
	}

	protected final RefactoringStatus performRefactoring(RefactoringDescriptor descriptor, boolean providesUndo) throws Exception {
		Refactoring refactoring= createRefactoring(descriptor);
		return performRefactoring(refactoring, providesUndo);
	}

    protected final Refactoring createRefactoring(RefactoringDescriptor descriptor) throws CoreException {
	    RefactoringStatus status= new RefactoringStatus();
		Refactoring refactoring= descriptor.createRefactoring(status);
		assertNotNull("refactoring should not be null", refactoring);
		assertTrue("status should be ok", status.isOK());
	    return refactoring;
    }

	protected final RefactoringStatus performRefactoring(Refactoring ref) throws Exception {
		return performRefactoring(ref, true);
	}

	protected final RefactoringStatus performRefactoring(Refactoring ref, boolean providesUndo) throws Exception {
		performDummySearch();
		IUndoManager undoManager= getUndoManager();
		if (DESCRIPTOR_TEST){
			final CreateChangeOperation create= new CreateChangeOperation(
					new CheckConditionsOperation(ref, CheckConditionsOperation.ALL_CONDITIONS),
					RefactoringStatus.FATAL);
			create.run(new NullProgressMonitor());
			RefactoringStatus checkingStatus= create.getConditionCheckingStatus();
			if (!checkingStatus.isOK())
				return checkingStatus;
			Change change= create.getChange();
			ChangeDescriptor descriptor= change.getDescriptor();
			if (descriptor instanceof RefactoringChangeDescriptor) {
				RefactoringChangeDescriptor rcd= (RefactoringChangeDescriptor) descriptor;
				RefactoringDescriptor refactoringDescriptor= rcd.getRefactoringDescriptor();
				if (refactoringDescriptor instanceof JavaRefactoringDescriptor) {
					JavaRefactoringDescriptor jrd= (JavaRefactoringDescriptor) refactoringDescriptor;
					RefactoringStatus validation= jrd.validateDescriptor();
					if (!validation.isOK())
						return validation;
					RefactoringStatus refactoringStatus= new RefactoringStatus();
					Class<? extends JavaRefactoringDescriptor> expected= jrd.getClass();
					RefactoringContribution contribution= RefactoringCore.getRefactoringContribution(jrd.getID());
					jrd= (JavaRefactoringDescriptor) contribution.createDescriptor(jrd.getID(), jrd.getProject(), jrd.getDescription(), jrd.getComment(), contribution.retrieveArgumentMap(jrd), jrd.getFlags());
					assertEquals(expected, jrd.getClass());
					ref= jrd.createRefactoring(refactoringStatus);
					if (!refactoringStatus.isOK())
						return refactoringStatus;
					TestRenameParticipantSingle.reset();
					TestCreateParticipantSingle.reset();
					TestMoveParticipantSingle.reset();
					TestDeleteParticipantSingle.reset();
				}
			}
		}
		final CreateChangeOperation create= new CreateChangeOperation(
			new CheckConditionsOperation(ref, CheckConditionsOperation.ALL_CONDITIONS),
			RefactoringStatus.FATAL);
		final PerformChangeOperation perform= new PerformChangeOperation(create);
		perform.setUndoManager(undoManager, ref.getName());
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		if (fIsPreDeltaTest) {
			IResourceChangeListener listener= event -> {
				if (create.getConditionCheckingStatus().isOK() &&  perform.changeExecuted()) {
					TestModelProvider.assertTrue(event.getDelta());
				}
			};
			try {
				TestModelProvider.clearDelta();
				workspace.checkpoint(false);
				workspace.addResourceChangeListener(listener);
				executePerformOperation(perform, workspace);
			} finally {
				workspace.removeResourceChangeListener(listener);
			}
		} else {
			executePerformOperation(perform, workspace);
		}
		RefactoringStatus status= create.getConditionCheckingStatus();
		if (!status.isOK())
			return status;
		assertTrue("Change wasn't executed", perform.changeExecuted());
		Change undo= perform.getUndoChange();
		if (providesUndo) {
			assertNotNull("Undo doesn't exist", undo);
			assertTrue("Undo manager is empty", undoManager.anythingToUndo());
		} else {
			assertNull("Undo manager contains undo but shouldn't", undo);
		}
		return null;
	}

	protected void executePerformOperation(final PerformChangeOperation perform, IWorkspace workspace) throws CoreException {
		workspace.run(perform, new NullProgressMonitor());
	}

	public RefactoringStatus performRefactoringWithStatus(Refactoring ref) throws Exception, CoreException {
		RefactoringStatus status= performRefactoring(ref);
		if (status == null)
			return new RefactoringStatus();
		return status;
	}

	protected final Change performChange(Refactoring refactoring, boolean storeUndo) throws Exception {
		CreateChangeOperation create= new CreateChangeOperation(refactoring);
		PerformChangeOperation perform= new PerformChangeOperation(create);
		if (storeUndo) {
			perform.setUndoManager(getUndoManager(), refactoring.getName());
		}
		ResourcesPlugin.getWorkspace().run(perform, new NullProgressMonitor());
		assertTrue("Change wasn't executed", perform.changeExecuted());
		return perform.getUndoChange();
	}

	protected final Change performChange(final Change change) throws Exception {
		PerformChangeOperation perform= new PerformChangeOperation(change);
		ResourcesPlugin.getWorkspace().run(perform, new NullProgressMonitor());
		assertTrue("Change wasn't executed", perform.changeExecuted());
		return perform.getUndoChange();
	}

	protected IUndoManager getUndoManager() {
		IUndoManager undoManager= RefactoringCore.getUndoManager();
		undoManager.flush();
		return undoManager;
	}

	/* ===================  helpers  ================= */
	protected IType getType(ICompilationUnit cu, String name) throws JavaModelException {
		for (IType type : cu.getAllTypes()) {
			if (type.getTypeQualifiedName('.').equals(name) || type.getElementName().equals(name)) {
				return type;
			}
		}
		return null;
	}

	/*
	 * subclasses override to inform about the location of their test cases
	 */
	protected String getRefactoringPath() {
		return "";
	}

	/*
	 *  example "RenameType/"
	 */
	protected String getTestPath() {
		return TEST_PATH_PREFIX + getRefactoringPath();
	}

	protected String createTestFileName(String cuName, String infix) {
		return getTestPath() + getName() + infix + cuName + ".java";
	}

	protected String getInputTestFileName(String cuName) {
		return createTestFileName(cuName, TEST_INPUT_INFIX);
	}

	/*
	 * @param subDirName example "p/" or "org/eclipse/jdt/"
	 */
	protected String getInputTestFileName(String cuName, String subDirName) {
		return createTestFileName(cuName, TEST_INPUT_INFIX + subDirName);
	}

	protected String getOutputTestFileName(String cuName) {
		return createTestFileName(cuName, TEST_OUTPUT_INFIX);
	}

	/*
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

	//-----------------------
	public static InputStream getStream(String content){
		return new ByteArrayInputStream(content.getBytes(ENCODING));
	}

	public static IPackageFragmentRoot getSourceFolder(IJavaProject javaProject, String name) throws JavaModelException{
		for (IPackageFragmentRoot root : javaProject.getPackageFragmentRoots()) {
			if (!root.isArchive() && root.getElementName().equals(name)) {
				return root;
			}
		}
		return null;
	}

	public String getFileContents(String fileName) throws IOException {
		return getContents(getFileInputStream(fileName));
	}

	public static String getContents(IFile file) throws IOException, CoreException {
		return getContents(file.getContents());
	}

	public static ICompilationUnit createCU(IPackageFragment pack, String name, String contents) throws Exception {
		assertFalse(pack.getCompilationUnit(name).exists());
		ICompilationUnit cu= pack.createCompilationUnit(name, contents, true, null);
		cu.save(null, true);
		return cu;
	}

	public static String getContents(InputStream in) throws IOException {
		StringBuilder sb= new StringBuilder(300);
		try (BufferedReader br= new BufferedReader(new InputStreamReader(in, ENCODING))){
			int read= 0;
			while ((read= br.read()) != -1)
				sb.append((char) read);
		}
		return sb.toString();
	}

	public static InputStream getFileInputStream(String fileName) throws IOException {
		return RefactoringTestPlugin.getDefault().getTestResourceStream(fileName);
	}

	public static String removeExtension(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf('.'));
	}

	public static IMember[] merge(IMember[] a1, IMember[] a2, IMember[] a3){
		return JavaElementUtil.merge(JavaElementUtil.merge(a1, a2), a3);
	}

	public static IMember[] merge(IMember[] a1, IMember[] a2){
		return JavaElementUtil.merge(a1, a2);
	}

	public static IField[] getFields(IType type, String[] names) {
		if (names == null )
			return new IField[0];
		Set<IField> fields= new HashSet<>();
		for (String name : names) {
			IField field= type.getField(name);
			assertTrue("field " + field.getElementName() + " does not exist", field.exists());
			fields.add(field);
		}
		return fields.toArray(new IField[fields.size()]);
	}

	public static IType[] getMemberTypes(IType type, String[] names) {
		if (names == null )
			return new IType[0];
		Set<IType> memberTypes= new HashSet<>();
		for (String name : names) {
			IType memberType;
			if (name.indexOf('.') != -1) {
				String[] path= name.split("\\.");
				memberType= type.getType(path[0]);
				for (int j= 1; j < path.length; j++) {
					memberType= memberType.getType(path[j]);
				}
			} else {
				memberType= type.getType(name);
			}
			assertTrue("member type " + memberType.getElementName() + " does not exist", memberType.exists());
			memberTypes.add(memberType);
		}
		return memberTypes.toArray(new IType[memberTypes.size()]);
	}

	public static IMethod[] getMethods(IType type, String[] names, String[][] signatures) {
		if (names == null || signatures == null)
			return new IMethod[0];
		List<IMethod> methods= new ArrayList<>(names.length);
		for (int i = 0; i < names.length; i++) {
			IMethod method= type.getMethod(names[i], signatures[i]);
			assertTrue("method " + method.getElementName() + " does not exist", method.exists());
			if (!methods.contains(method))
				methods.add(method);
		}
		return methods.toArray(new IMethod[methods.size()]);
	}

	public static IType[] findTypes(IType[] types, String[] namesOfTypesToPullUp) {
		List<IType> found= new ArrayList<>(types.length);
		for (IType type : types) {
			for (String name : namesOfTypesToPullUp) {
				if (type.getElementName().equals(name))
					found.add(type);
			}
		}
		return found.toArray(new IType[found.size()]);
	}

	public static IField[] findFields(IField[] fields, String[] namesOfFieldsToPullUp) {
		List<IField> found= new ArrayList<>(fields.length);
		for (IField field : fields) {
			for (String name : namesOfFieldsToPullUp) {
				if (field.getElementName().equals(name))
					found.add(field);
			}
		}
		return found.toArray(new IField[found.size()]);
	}

	public static IMethod[] findMethods(IMethod[] selectedMethods, String[] namesOfMethods, String[][] signaturesOfMethods){
		List<IMethod> found= new ArrayList<>(selectedMethods.length);
		for (IMethod method : selectedMethods) {
			String[] paramTypes= method.getParameterTypes();
			for (int j= 0; j < namesOfMethods.length; j++) {
				String methodName= namesOfMethods[j];
				if (! methodName.equals(method.getElementName()))
					continue;
				String[] methodSig= signaturesOfMethods[j];
				if (! areSameSignatures(paramTypes, methodSig))
					continue;
				found.add(method);
			}
		}
		return found.toArray(new IMethod[found.size()]);
	}

	private static boolean areSameSignatures(String[] s1, String[] s2){
		if (s1.length != s2.length)
			return false;
		for (int i= 0; i < s1.length; i++) {
			if (! s1[i].equals(s2[i]))
				return false;
		}
		return true;
	}

	/**
	 * Line-based version of junit.framework.Assert.assertEquals(String, String)
	 * without considering line delimiters.
	 * @param expected the expected value
	 * @param actual the actual value
	 */
	public static void assertEqualLines(String expected, String actual) {
		assertEqualLines("", expected, actual);
	}

	/**
	 * Line-based version of junit.framework.Assert.assertEquals(String, String, String)
	 * without considering line delimiters.
	 * @param message the message
	 * @param expected the expected value
	 * @param actual the actual value
	 */
	public static void assertEqualLines(String message, String expected, String actual) {
		String[] expectedLines= Strings.convertIntoLines(expected);
		String[] actualLines= Strings.convertIntoLines(actual);

		String expected2= (expectedLines == null ? null : Strings.concatenate(expectedLines, "\n"));
		String actual2= (actualLines == null ? null : Strings.concatenate(actualLines, "\n"));
		assertEquals(message, expected2, actual2);
	}

}
