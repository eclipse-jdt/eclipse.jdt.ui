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

import org.eclipse.jdt.internal.corext.refactoring.DebugUtils;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.reorg.CopySourceReferencesToClipboardAction;
import org.eclipse.jdt.internal.ui.reorg.DeleteSourceReferencesAction;
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
	
	//---- tests 
	
	public void test0() throws Exception{
		ICompilationUnit newcuA= null;
		ICompilationUnit newcuB= null;
		try{
			String oldCuName= "A";
			ICompilationUnit cuA= createCUfromTestFile(getPackageP(), oldCuName);
			
			ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
			
			IType typeA= cuA.getType("A");
			ISelectionProvider provider= new FakeSelectionProvider(new IType[]{typeA});
	
			assertTrue("A does not exist", typeA.exists());
			
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
			
			newcuA= getPackageP().getCompilationUnit(oldCuName + ".java");
			assertEquals("incorrect paste in A", getFileContents(getOutputTestFileName(oldCuName)), newcuA.getSource());	
			
			newcuB= getPackageP().getCompilationUnit("B" + ".java");
			assertEquals("incorrect paste in B", getFileContents(getOutputTestFileName("B")), newcuB.getSource());	
		} finally{
			if (newcuA != null)
				newcuA.delete(false, null);
			if (newcuB != null)	
				newcuB.delete(false, null);
		}
	}
	
	public void test1() throws Exception{
		printTestDisabledMessage("should not show dialog when testing");	
		if (true)
			return;
			
		ICompilationUnit newcuA= null;
		ICompilationUnit newcuB= null;
		try{
			String oldCuName= "A";
			ICompilationUnit cuA= createCUfromTestFile(getPackageP(), oldCuName);
			assertTrue("A.java does not exist", cuA.exists());
			
			ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
			
			IType typeA= cuA.getType("A");
			ISelectionProvider provider= new FakeSelectionProvider(new IType[]{typeA});
			
			assertTrue("A does not exist", typeA.exists());
			CopySourceReferencesToClipboardAction copyAction= new CopySourceReferencesToClipboardAction(provider);
			copyAction.update();
			assertTrue("copy enabled", copyAction.isEnabled());
			copyAction.run();
	
			DeleteSourceReferencesAction deleteAction= new DeleteSourceReferencesAction(provider);
			deleteAction.update();
			assertTrue("delete action enabled", deleteAction.isEnabled());
			deleteAction.run();
			
	
			IType typeB= cuB.getType("B");
			provider= new FakeSelectionProvider(new IType[]{typeB});
			
			PasteSourceReferencesAction pasteAction= new PasteSourceReferencesAction(provider);
			pasteAction.update();
			assertTrue("paste enabled", pasteAction.isEnabled());
			pasteAction.run();
			
			newcuA= getPackageP().getCompilationUnit(oldCuName + ".java");
			assertEquals("incorrect paste in A", getFileContents(getOutputTestFileName(oldCuName)), newcuA.getSource());	
			
			newcuB= getPackageP().getCompilationUnit("B" + ".java");
			assertEquals("incorrect paste in B", getFileContents(getOutputTestFileName("B")), newcuB.getSource());	
		} finally{
			if (newcuA != null)
				newcuA.delete(false, null);
			if (newcuB != null)	
				newcuB.delete(false, null);
		}	
	}	
}

