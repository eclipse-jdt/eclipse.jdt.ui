package org.eclipse.jdt.ui.tests.actions;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.reorg.CopySourceReferencesToClipboardAction;
import org.eclipse.jdt.internal.ui.reorg.PasteSourceReferencesAction;

public class PasteSourceReferenceActionTests extends RefactoringTest {

	private static final Class clazz= PasteSourceReferenceActionTests.class;
	private static final String REFACTORING_PATH= "PasteSourceReference/";
	
	public PasteSourceReferenceActionTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	private void helper() throws Exception{
		String oldCuName= "A";
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), oldCuName);
		
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
		
		IType typeA= cuA.getType("A");
		ISelectionProvider provider= new FakeSelectionProvider(new IType[]{typeA});
		
		CopySourceReferencesToClipboardAction copyAction= new CopySourceReferencesToClipboardAction(provider);
		copyAction.update();
		assertTrue("copy enabled", copyAction.isEnabled());
		copyAction.run();

		IType typeB= cuB.getType("B");
		provider= new FakeSelectionProvider(new IType[]{typeB});
		
		PasteSourceReferencesAction pasteAction= new PasteSourceReferencesAction(provider);
		pasteAction.update();
		assertTrue("paste enabled", pasteAction.isEnabled());
		pasteAction.run();
		
		ICompilationUnit newcuA= getPackageP().getCompilationUnit(oldCuName + ".java");
		assertEquals("incorrect paste in A", getFileContents(getOutputTestFileName(oldCuName)), newcuA.getSource());	
		
		ICompilationUnit newcuB= getPackageP().getCompilationUnit("B" + ".java");
		assertEquals("incorrect paste in B", getFileContents(getOutputTestFileName("B")), newcuB.getSource());	
	}	
	//---- tests 
	
	public void test0() throws Exception{
		helper();
	}
}

