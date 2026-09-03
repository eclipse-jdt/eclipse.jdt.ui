package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import org.eclipse.core.tests.harness.FussyProgressMonitor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTesterCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeRecordSignatureProcessor;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java16Setup;

public class ChangeRecordSignatureTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "ChangeRecordSignature/";

	public ChangeRecordSignatureTests() {
		rts= new Java16Setup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private String getSimpleTestFileName(boolean input){
		StringBuilder fileName = new StringBuilder("A_").append(getName());
		fileName.append(input ? "_in": "_out");
		return fileName.append(".java").toString();
	}

	private String getTestFileName(boolean input){
		String fileName= getTestFolderPath();
		return fileName + getSimpleTestFileName(input);
	}

	private String getTestFolderPath() {
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		return fileName.toString();
	}

	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(input), getFileContents(getTestFileName(input)));
	}

	static ParameterInfo[] createNewParamInfos(String[] newTypes, String[] newNames, String[] newDefaultValues) {
		if (newTypes == null)
			return new ParameterInfo[0];
		ParameterInfo[] result= new ParameterInfo[newTypes.length];
		for (int i= 0; i < newDefaultValues.length; i++) {
			result[i]= ParameterInfo.createInfoForAddedParameter(newTypes[i], newNames[i], newDefaultValues[i]);
		}
		return result;
	}

	static void addInfos(List<ParameterInfo> list, ParameterInfo[] newParamInfos, int[] newIndices) {
		if (newParamInfos == null || newIndices == null)
			return;
		for (int i= newIndices.length - 1; i >= 0; i--) {
			list.add(newIndices[i], newParamInfos[i]);
		}
	}

	private void helperAdd(ParameterInfo[] newParamInfos, int[] newIndices) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		IType classA= getType(cu, "A");
		assertTrue("refactoring not available", RefactoringAvailabilityTesterCore.isChangeRecordSignatureAvailable(classA));

		ChangeRecordSignatureProcessor processor= new ChangeRecordSignatureProcessor(classA);
		Refactoring ref= new ProcessorBasedRefactoring(processor);

		addInfos(processor.getParameterInfos(), newParamInfos, newIndices);
		FussyProgressMonitor testMonitor= new FussyProgressMonitor();
		RefactoringStatus initialConditions= ref.checkInitialConditions(testMonitor);
		testMonitor.assertUsedUp();
		assertTrue("precondition was supposed to pass: " + initialConditions.getEntryWithHighestSeverity(), initialConditions.isOK());

		RefactoringStatus result= performRefactoring(ref);
		assertNull("refactoring was supposed to succeed", result);

		String expectedFileContents= getFileContents(getTestFileName(false));
		assertEqualLines("unexpected result", expectedFileContents, cu.getSource());
	}

	private void helperAddFail(ParameterInfo[] newParamInfos, int[] newIndices, int expectedSeverity) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		IType classA= getType(cu, "A");
		assertTrue("refactoring not available", RefactoringAvailabilityTesterCore.isChangeRecordSignatureAvailable(classA));

		ChangeRecordSignatureProcessor processor= new ChangeRecordSignatureProcessor(classA);
		Refactoring ref= new ProcessorBasedRefactoring(processor);

		addInfos(processor.getParameterInfos(), newParamInfos, newIndices);
		FussyProgressMonitor testMonitor= new FussyProgressMonitor();
		RefactoringStatus result= ref.checkInitialConditions(testMonitor);
		testMonitor.assertUsedUp();

		if (result.isOK()) {
			result= performRefactoring(ref);
		}
		assertNotNull("precondition was supposed to fail", result);
		assertEquals("Severity: " + result.getMessageMatchingSeverity(result.getSeverity()), expectedSeverity, result.getSeverity());
	}

	@Test
	public void test01() throws Exception {
		ParameterInfo[] newParamInfos= createNewParamInfos(
				new String[]{"String"},
				new String[]{"c"},
				new String[]{"\"default\""});
		helperAdd(newParamInfos, new int[]{2});
	}

	@Test
	public void testFailDuplicateName() throws Exception {
		// adding a component named "a" to record A(int a, String b) — duplicate name
		ParameterInfo[] newParamInfos= createNewParamInfos(
				new String[]{"int"},
				new String[]{"a"},
				new String[]{"0"});
		helperAddFail(newParamInfos, new int[]{2}, RefactoringStatus.FATAL);
	}

}
