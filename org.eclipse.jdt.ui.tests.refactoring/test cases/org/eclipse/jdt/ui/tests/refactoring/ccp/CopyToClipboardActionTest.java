/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.ccp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.refactoring.TypedSource;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaElementTransfer;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtilsCore;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.tests.refactoring.GenericRefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockClipboard;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockWorkbenchSite;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

import org.eclipse.jdt.internal.ui.refactoring.reorg.CopyToClipboardAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.TypedSourceTransfer;

public class CopyToClipboardActionTest extends GenericRefactoringTest {
	private static final String CU_A_NAME= "A";
	private static final String CU_B_NAME= "B";

	private ILabelProvider fLabelProvider;

	private Clipboard fClipboard;

	private ICompilationUnit fCuA;
	private ICompilationUnit fCuB;
	private IPackageFragment fPackage_Q;
	private IPackageFragment fPackage_Q_R;
	private IPackageFragment fDefaultPackage;
	private IFile faTxt;
	private IFolder fOlder;

	public CopyToClipboardActionTest() {
		rts= new RefactoringTestSetup();
	}

	@Override
	public void genericbefore() throws Exception {
		super.genericbefore();
		fClipboard= new MockClipboard(Display.getDefault());
		fDefaultPackage= rts.getDefaultSourceFolder().createPackageFragment("", true, null);

		fCuA= createCU(getPackageP(), CU_A_NAME + ".java",
			"""
				package p;\
				import java.util.List;\
				class A{\
				int f;\
				{}\
				void foo(){}\
				class Inner{}\
				}""");

		fPackage_Q= rts.getDefaultSourceFolder().createPackageFragment("q", true, null);
		fCuB= createCU(fPackage_Q, CU_B_NAME + ".java",
				"""
					package q;\
					import java.util.Set;\
					class B{\
					int x;\
					void bar(){}\
					class InnerB{}\
					}""");

		fPackage_Q_R= rts.getDefaultSourceFolder().createPackageFragment("q.r", true, null);

		faTxt= createFile((IFolder)getPackageP().getUnderlyingResource(), "a.txt");
		fOlder= createFolder(rts.getProject().getProject(), "fOlder");

		fLabelProvider= new JavaElementLabelProvider(	JavaElementLabelProvider.SHOW_VARIABLE +
														JavaElementLabelProvider.SHOW_PARAMETERS +
														JavaElementLabelProvider.SHOW_TYPE);
		assertTrue("A.java does not exist", fCuA.exists());
		assertTrue("B.java does not exist", fCuB.exists());
		assertTrue("q does not exist", fPackage_Q.exists());
		assertTrue("q.r does not exist", fPackage_Q_R.exists());
		assertTrue("a.txt does not exist", faTxt.exists());
		assertTrue("fOlder does not exist", fOlder.exists());
	}

	@Override
	public void genericafter() throws Exception {
		super.genericafter();
		performDummySearch();
		fClipboard.dispose();
		fLabelProvider.dispose();
	}

	private IFile createFile(IFolder folder, String fileName) throws Exception {
		IFile file= folder.getFile(fileName);
		file.create(getStream("aa"), true, null);
		return file;
	}

	private IFolder createFolder(IProject project, String name) throws CoreException{
		IFolder folder= project.getFolder(name);
		folder.create(true, true, null);
		return folder;
	}

	private void checkDisabled(Object[] elements){
		CopyToClipboardAction copyAction= new CopyToClipboardAction(new MockWorkbenchSite(elements), fClipboard);
		copyAction.setAutoRepeatOnFailure(true);
		copyAction.update(copyAction.getSelection());
		assertFalse("action should be disabled", copyAction.isEnabled());
	}

	private void checkEnabled(Object[] elements) throws Exception {
		CopyToClipboardAction copyAction= new CopyToClipboardAction(new MockWorkbenchSite(elements), fClipboard);
		copyAction.setAutoRepeatOnFailure(true);
		copyAction.update(copyAction.getSelection());
		assertTrue("action should be enabled", copyAction.isEnabled());
		copyAction.run();
		checkClipboard(elements);
	}

	private void checkClipboard(Object[] elementsCopied) throws Exception {
		IResource[] resourcesCopied= getResources(elementsCopied);
		IJavaElement[] javaElementsCopied= getJavaElements(elementsCopied);
		IType[] mainTypesCopied= ReorgUtilsCore.getMainTypes(javaElementsCopied);

		IResource[] resourcesExpected= computeResourcesExpectedInClipboard(resourcesCopied, mainTypesCopied, javaElementsCopied);
		IJavaElement[] javaElementsExpected= computeJavaElementsExpectedInClipboard(javaElementsCopied, mainTypesCopied);

		String[] clipboardFiles= getClipboardFiles();
		IResource[] clipboardResources= getClipboardResources();
		String clipboardText= getClipboardText();
		IJavaElement[] clipboardJavaElements= getClipboardJavaElements();
		TypedSource[] clipboardTypedSources= getClipboardTypedSources();

		checkNames(resourcesCopied, javaElementsCopied, clipboardText);
		checkFiles(resourcesCopied, javaElementsCopied, mainTypesCopied, clipboardFiles);
		checkTypedSources(javaElementsCopied, clipboardTypedSources);
		checkElements(resourcesExpected, clipboardResources);
		checkElements(javaElementsExpected, clipboardJavaElements);
	}

	private void checkTypedSources(IJavaElement[] javaElementsCopied, TypedSource[] clipboardTypedSources) throws Exception {
		TypedSource[] typedSources= TypedSource.createTypedSources(javaElementsCopied);
		assertEquals("different number", typedSources.length, clipboardTypedSources.length);
		TypedSource.sortByType(typedSources);
		TypedSource.sortByType(clipboardTypedSources);
		for (int i= 0; i < typedSources.length; i++) {
			assertEquals("different typed sources", typedSources[i], clipboardTypedSources[i]);
		}
	}

	private IResource[] computeResourcesExpectedInClipboard(IResource[] resourcesCopied, IType[] mainTypesCopied, IJavaElement[] javaElementsCopied) {
		IResource[] cuResources= ReorgUtilsCore.getResources(getCompilationUnits(javaElementsCopied));
		return ReorgUtilsCore.union(cuResources, ReorgUtilsCore.union(resourcesCopied, ReorgUtilsCore.getResources(ReorgUtilsCore.getCompilationUnits(mainTypesCopied))));
	}

	private static IJavaElement[] computeJavaElementsExpectedInClipboard(IJavaElement[] javaElementsExpected, IType[] mainTypesCopied) {
		return ReorgUtilsCore.union(javaElementsExpected, ReorgUtilsCore.getCompilationUnits(mainTypesCopied));
	}

	private String getName(IResource resource){
		return fLabelProvider.getText(resource);
	}
	private String getName(IJavaElement javaElement){
		return fLabelProvider.getText(javaElement);
	}

	private static void checkElements(Object[] copied, Object[] retreivedFromClipboard) {
		assertEquals("different number of elements", copied.length, retreivedFromClipboard.length);
		sortByName(copied);
		sortByName(retreivedFromClipboard);
		for (int i= 0; i < retreivedFromClipboard.length; i++) {
			Object retreived= retreivedFromClipboard[i];
			assertTrue("element does not exist", exists(retreived));
			assertEquals("different copied " + getName(copied[i]) + " retreived: " + getName(retreived), copied[i], retreivedFromClipboard[i]);
		}
	}

	private static boolean exists(Object element) {
		if (element instanceof IJavaElement)
			return ((IJavaElement)element).exists();
		if (element instanceof IResource)
			return ((IResource)element).exists();
		fail();
		return false;
	}

	private static String getName(Object object) {
		if (object instanceof IJavaElement)
			return ((IJavaElement)object).getElementName();
		if (object instanceof IResource)
			return ((IResource)object).getName();
		return object == null ? null : object.toString();
	}

	private static void sortByName(Object[] copied) {
		Arrays.sort(copied, (arg0, arg1) -> getName(arg0).compareTo(getName(arg1)));
	}

	private void checkNames(IResource[] resourcesCopied, IJavaElement[] javaElementsCopied, String clipboardText){
		List<String> stringLines= Arrays.asList(Strings.convertIntoLines(clipboardText));
		assertEquals("different number of names", resourcesCopied.length + javaElementsCopied.length, stringLines.size());
		for (IResource resource : resourcesCopied) {
			String name= getName(resource);
			assertTrue("name not in set:" + name, stringLines.contains(name));
		}
		for (IJavaElement element : javaElementsCopied) {
			if (! ReorgUtilsCore.isInsideCompilationUnit(element)){
				String name= getName(element);
				assertTrue("name not in set:" + name, stringLines.contains(name));
			}
		}
	}

	private static void checkFiles(IResource[] resourcesCopied, IJavaElement[] javaElementsCopied, IType[] mainTypes, String[] clipboardFiles) {
		int expected= 0;
		expected += resourcesCopied.length;
		expected += countResources(javaElementsCopied);
		expected += mainTypes.length;

		//we cannot compare file names here because they're absolute and depend on the worspace location
		assertEquals("different number of files in clipboard", expected, clipboardFiles.length);
	}

	private static int countResources(IJavaElement[] javaElementsCopied) {
		int count= 0;
		for (IJavaElement element : javaElementsCopied) {
			switch (element.getElementType()) {
				case IJavaElement.JAVA_PROJECT :
				case IJavaElement.PACKAGE_FRAGMENT_ROOT :
				case IJavaElement.PACKAGE_FRAGMENT :
				case IJavaElement.COMPILATION_UNIT :
				case IJavaElement.CLASS_FILE :
					count++;
					break;
				default:
					break;
			}
		}
		return count;
	}

	private static IJavaElement[] getCompilationUnits(IJavaElement[] javaElements) {
		List<?> cus= ReorgUtilsCore.getElementsOfType(javaElements, IJavaElement.COMPILATION_UNIT);
		return cus.toArray(new ICompilationUnit[cus.size()]);
	}

	private static IResource[] getResources(Object[] elements) {
		return ReorgUtilsCore.getResources(Arrays.asList(elements));
	}

	private static IJavaElement[] getJavaElements(Object[] elements) {
		return ReorgUtilsCore.getJavaElements(Arrays.asList(elements));
	}

	private IJavaElement[] getClipboardJavaElements() {
		IJavaElement[] elements= (IJavaElement[])fClipboard.getContents(JavaElementTransfer.getInstance());
		return elements == null ? new IJavaElement[0]: elements;
	}

	private String[] getClipboardFiles() {
		String[] files= (String[])fClipboard.getContents(FileTransfer.getInstance());
		return files == null ? new String[0]: files;
	}

	private IResource[] getClipboardResources() {
		IResource[] resources= (IResource[])fClipboard.getContents(ResourceTransfer.getInstance());
		return resources == null ? new IResource[0]: resources;
	}

	private TypedSource[] getClipboardTypedSources() {
		TypedSource[] typedSources= (TypedSource[])fClipboard.getContents(TypedSourceTransfer.getInstance());
		return typedSources == null ? new TypedSource[0]: typedSources;
	}

	private String getClipboardText() {
		return (String)fClipboard.getContents(TextTransfer.getInstance());
	}

	///---------tests

	@Test
	public void testDisabled0() {
		Object[] elements= {};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled1() throws Exception {
		Object[] elements= {null};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled2() throws Exception {
		Object[] elements= {this};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled3() throws Exception {
		Object[] elements= {rts.getProject(), getPackageP()};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled4() throws Exception {
		checkDisabled(new Object[]{getPackageP(), fCuA});
	}

	@Test
	public void testDisabled5() throws Exception {
		checkDisabled(new Object[]{getRoot(), fCuA});
	}

	@Test
	public void testDisabled6() throws Exception {
		checkDisabled(new Object[]{getRoot(), fPackage_Q});
	}

	@Test
	public void testDisabled7() throws Exception {
		checkDisabled(new Object[]{getRoot(), faTxt});
	}

	@Test
	public void testDisabled8() throws Exception {
		checkDisabled(new Object[]{getRoot(), getRoot().getJavaProject()});
	}

	@Test
	public void testDisabled9() throws Exception {
		checkDisabled(new Object[]{rts.getProject().getPackageFragmentRoots()});
	}

	@Test
	public void testDisabled10() throws Exception {
		checkDisabled(new Object[]{fCuA, fCuB});
	}

	@Test
	public void testDisabled11() throws Exception {
		checkDisabled(new Object[]{fDefaultPackage});
	}

	@Test
	public void testDisabled12() throws Exception {
		checkDisabled(new Object[]{getRoot().getJavaProject(), fCuA});
	}

	@Test
	public void testDisabled13() throws Exception {
		checkDisabled(new Object[]{getRoot().getJavaProject(), fPackage_Q});
	}

	@Test
	public void testDisabled14() throws Exception {
		checkDisabled(new Object[]{getRoot().getJavaProject(), faTxt});
	}

	@Test
	public void testDisabled15() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object classA= fCuA.getType("A");
		Object[] elements= {fieldF, classA};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled16() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, fCuA};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled17() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, fDefaultPackage};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled18() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, fPackage_Q};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled19() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, faTxt};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled20() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, getRoot()};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled21() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, rts.getProject()};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled22() throws Exception {
		Object typeA= fCuA.getType("A");
		Object typeB= fCuB.getType("B");
		Object[] elements= {typeA, typeB};
		checkDisabled(elements);
	}

	@Test
	public void testDisabled23() throws Exception {
		checkDisabled(new Object[]{faTxt, fCuB});
	}

	@Test
	public void testEnabled0() throws Exception {
		Object[] elements= {rts.getProject()};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled1() throws Exception {
		Object[] elements= {getPackageP()};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled2() throws Exception {
		Object[] elements= {getRoot()};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled3() throws Exception {
		Object[] elements= {rts.getDefaultSourceFolder()};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled4() throws Exception {
		checkEnabled(new Object[]{faTxt});
	}

	@Test
	public void testEnabled5() throws Exception {
		checkEnabled(new Object[]{getRoot()});
	}

	@Test
	public void testEnabled6() throws Exception {
		checkEnabled(new Object[]{fCuA});
	}

	@Test
	public void testEnabled7() throws Exception {
		checkEnabled(new Object[]{getRoot().getJavaProject()});
	}

	@Test
	public void testEnabled8() throws Exception {
		checkEnabled(new Object[]{getPackageP()});
	}

	@Test
	public void testEnabled9() throws Exception {
		checkEnabled(new Object[]{getPackageP(), fPackage_Q, fPackage_Q_R});
	}

	@Test
	public void testEnabled10() throws Exception {
		Object packDecl= fCuA.getPackageDeclarations()[0];
		Object[] elements= {packDecl};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled11() throws Exception {
		Object importD= fCuA.getImports()[0];
		Object[] elements= {importD};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled12() throws Exception {
//		printTestDisabledMessage("disabled due to bug 37750");
//		if (true)
//			return;
		IJavaElement importContainer= fCuA.getImportContainer();
		Object[] elements= {importContainer};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled13() throws Exception {
		Object classA= fCuA.getType("A");
		Object[] elements= {classA};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled14() throws Exception {
		Object methodFoo= fCuA.getType("A").getMethod("foo", new String[0]);
		Object[] elements= {methodFoo};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled15() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled16() throws Exception {
		Object initializer= fCuA.getType("A").getInitializer(1);
		Object[] elements= {initializer};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled17() throws Exception {
		Object innerClass= fCuA.getType("A").getType("Inner");
		Object[] elements= {innerClass};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled18() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object methodFoo= fCuA.getType("A").getMethod("foo", new String[0]);
		Object innerClass= fCuA.getType("A").getType("Inner");
		Object initializer= fCuA.getType("A").getInitializer(1);
		Object[] elements= {fieldF, methodFoo, initializer, innerClass};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled19() throws Exception {
//		printTestDisabledMessage("disabled due to bug 37750");
//		if (true)
//			return;

		Object classA= fCuA.getType("A");
		Object importContainer= fCuA.getImportContainer();
		Object packDecl= fCuA.getPackageDeclarations()[0];
		Object[] elements= {classA, importContainer, packDecl};
		checkEnabled(elements);
	}

	@Test
	public void testEnabled20() throws Exception {
		checkEnabled(new Object[]{faTxt, fCuA});
	}

	@Test
	public void testEnabled21() throws Exception {
		checkEnabled(new Object[]{fOlder});
	}

	@Test
	public void testEnabled22() throws Exception {
//		printTestDisabledMessage("bug 39410");
		Object classA= fCuA.getType("A");
		Object packDecl= fCuA.getPackageDeclarations()[0];
		Object[] elements= {classA, packDecl};
		checkEnabled(elements);
	}
}
