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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class RenamePrivateFieldTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "RenamePrivateField/";

	private String fPrefixPref;

	public RenamePrivateFieldTests() {
		rts= new RefactoringTestSetup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Override
	public void genericbefore() throws Exception {
		super.genericbefore();
		Hashtable<String, String> options= JavaCore.getOptions();
		fPrefixPref= options.get(JavaCore.CODEASSIST_FIELD_PREFIXES);
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, getPrefixes());
		JavaCore.setOptions(options);
		fIsPreDeltaTest= true;
	}

	@Override
	public void genericafter() throws Exception {
		super.genericafter();
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, fPrefixPref);
		JavaCore.setOptions(options);
	}

	private String getPrefixes(){
		return "f";
	}

	private void helper1_0(String fieldName, String newFieldName, String typeName, boolean renameGetter, boolean renameSetter) throws Exception{
		IType declaringType= getType(createCUfromTestFile(getPackageP(), "A"), typeName);
		IField field= declaringType.getField(fieldName);
		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_FIELD);
		descriptor.setJavaElement(field);
		descriptor.setNewName(newFieldName);
		descriptor.setUpdateReferences(true);
		descriptor.setRenameGetters(renameGetter);
		descriptor.setRenameSetters(renameSetter);
		RefactoringStatus result= performRefactoring(descriptor);
		assertNotNull("precondition was supposed to fail", result);
	}

	private void helper1_0(String fieldName, String newFieldName) throws Exception{
		helper1_0(fieldName, newFieldName, "A", false, false);
	}

	private void helper1() throws Exception{
		helper1_0("f", "g");
	}

	private void helper2(String fieldName, String newFieldName, boolean updateReferences, boolean updateTextualMatches,
											boolean renameGetter, boolean renameSetter,
											boolean expectedGetterRenameEnabled, boolean expectedSetterRenameEnabled) throws Exception{
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
		IField field= classA.getField(fieldName);
		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_FIELD);
		descriptor.setJavaElement(field);
		descriptor.setNewName(newFieldName);
		descriptor.setUpdateReferences(updateReferences);
		descriptor.setUpdateTextualOccurrences(updateTextualMatches);
		descriptor.setRenameGetters(renameGetter);
		descriptor.setRenameSetters(renameSetter);

		RenameRefactoring refactoring= (RenameRefactoring) createRefactoring(descriptor);
		RenameFieldProcessor processor= (RenameFieldProcessor) refactoring.getProcessor();
		assertEquals("getter rename enabled", expectedGetterRenameEnabled, processor.canEnableGetterRenaming() == null);
		assertEquals("setter rename enabled", expectedSetterRenameEnabled, processor.canEnableSetterRenaming() == null);

		String newGetterName= processor.getNewGetterName();
		String newSetterName= processor.getNewSetterName();

		List<IAnnotatable> elements= new ArrayList<>();
		elements.add(field);
		List<RenameArguments> args= new ArrayList<>();
		args.add(new RenameArguments(newFieldName, updateReferences));
		if (renameGetter && expectedGetterRenameEnabled) {
			elements.add(processor.getGetter());
			args.add(new RenameArguments(newGetterName, updateReferences));
		}
		if (renameSetter && expectedSetterRenameEnabled) {
			elements.add(processor.getSetter());
			args.add(new RenameArguments(newSetterName, updateReferences));
		}
		String[] renameHandles= ParticipantTesting.createHandles(elements.toArray());

		RefactoringStatus result= performRefactoring(refactoring);
		assertNull("was supposed to pass", result);
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());

		ParticipantTesting.testRename(
			renameHandles,
			args.toArray(new RenameArguments[args.size()]));

		assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertFalse("! anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assertFalse("! anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}

	private void helper2(boolean updateReferences) throws Exception{
		helper2("f", "g", updateReferences, false, false, false, false, false);
	}

	private void helper2() throws Exception{
		helper2(true);
	}

	//--------- tests ----------
	@Test
	public void testFail0() throws Exception{
		helper1();
	}

	@Test
	public void testFail1() throws Exception{
		helper1();
	}

	@Test
	public void testFail2() throws Exception{
		helper1();
	}

	@Test
	public void testFail3() throws Exception{
		helper1();
	}

	@Test
	public void testFail4() throws Exception{
		helper1();
	}

	@Test
	public void testFail5() throws Exception{
		helper1();
	}

	@Test
	public void testFail6() throws Exception{
		helper1();
	}

	@Test
	public void testFail7() throws Exception{
		helper1();
	}

	@Test
	public void testFail8() throws Exception{
		helper1_0("gg", "f", "A", false, false);
	}

	@Test
	public void testFail9() throws Exception{
		helper1_0("y", "e", "getE", true, true);
	}

	@Test
	public void testFail10() throws Exception{
		helper1_0("y", "e", "setE", true, true);
	}

	// ------
	@Test
	public void test0() throws Exception{
		helper2();
	}

	@Test
	public void test1() throws Exception{
		helper2();
	}

	@Test
	public void test2() throws Exception{
		helper2(false);
	}

	@Test
	public void test3() throws Exception{
		helper2("f", "gg", true, true, false, false, false, false);
	}

	@Test
	public void test4() throws Exception{
		helper2("fMe", "fYou", true, false, true, true, true, true);
	}

	@Test
	public void test5() throws Exception{
		//regression test for 9895
		helper2("fMe", "fYou", true, false, true, false, true, false);
	}

	@Test
	public void test6() throws Exception{
		//regression test for 9895 - opposite case
		helper2("fMe", "fYou", true, false, false, true, false, true);
	}

	@Test
	public void test7() throws Exception{
		//regression test for 21292
		helper2("fBig", "fSmall", true, false, true, true, true, true);
	}

	@Test
	public void test8() throws Exception{
		//regression test for 26769
		helper2("f", "g", true, false, true, false, true, false);
	}

	@Test
	public void test9() throws Exception{
		//regression test for 30906
		helper2("fBig", "fSmall", true, false, true, true, true, true);
	}

	@Ignore("BUG_81084")
	@Test
	public void test10() throws Exception{
		//regression test for 81084
		helper2("fList", "fElements", true, false, false, false, false, false);
	}

	@Ignore("BUG_75642_GENERIC_METHOD_SEARCH")
	@Test
	public void test11() throws Exception{
		helper2("fList", "fElements", true, false, true, true, true, true);
	}

	@Test
	public void testUnicode01() throws Exception{
		//regression test for 180331
		helper2("field", "feel", true, false, true, true, true, true);
	}
}
