package org.eclipse.jdt.ui.tests.dialogs;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.test.harness.DialogCheck;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.codemanipulation.CodeManipulationMessages;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog;
import org.eclipse.jdt.internal.ui.launcher.AddExceptionDialog;
import org.eclipse.jdt.internal.ui.packageview.PackageViewerSorter;
import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;
import org.eclipse.jdt.internal.ui.util.TypeInfo;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.tests.core.AddUnimplementedMethodsTest;

public class DialogsTest extends TestCase {
	
	private static final String PROJECT_NAME = "DummyProject";

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), DialogsTest.class, args);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite(AddUnimplementedMethodsTest.class.getName());
		suite.addTest(new DialogsTest("testElementListSelectionDialog"));
		suite.addTest(new DialogsTest("testMultiElementSelectionDialog"));
		suite.addTest(new DialogsTest("testTwoPaneSelectionDialog"));

		suite.addTest(new DialogsTest("testElementTreeSelectionDialog"));
		suite.addTest(new DialogsTest("testElementListSelectionDialog"));
		suite.addTest(new DialogsTest("testAddExceptionDialog"));
		return suite;
	}


	public DialogsTest(String name) {
		super(name);
	}
	private Shell getShell() {
		return DialogCheck.getShell();
	}
	private IWorkbench getWorkbench() {
		return WorkbenchPlugin.getDefault().getWorkbench();
	}
	
	public void testTwoPaneSelectionDialog() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addSourceContainer(jproject, "src1");
		JavaProjectHelper.addRTJar(jproject);
		
		IProject project= jproject.getProject();

		OpenTypeSelectionDialog dialog= new OpenTypeSelectionDialog(getShell(), new ProgressMonitorDialog(getShell()), 
			SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_TYPES);
	
		dialog.setTitle(JavaUIMessages.getString("OpenTypeAction.dialogTitle")); //$NON-NLS-1$
		dialog.setMessage(JavaUIMessages.getString("OpenTypeAction.dialogMessage")); //$NON-NLS-1$

		DialogCheck.assertDialog(dialog, this);
		
		JavaProjectHelper.delete(jproject);
	}
	
	private TypeInfo[] getRefs(ArrayList list, int off, int len) {
		TypeInfo[] res= new TypeInfo[len];
		for (int i= 0; i < len; i++) {
			res[i]= (TypeInfo) list.get(off + i);
		}
		return res;
	}
	
	public void testMultiElementSelectionDialog() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addSourceContainer(jproject, "src1");
		JavaProjectHelper.addRTJar(jproject);

		ILabelProvider labelProvider= new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_FULLYQUALIFIED);

		ArrayList list= new ArrayList(200);
		IProject project= jproject.getProject();

		IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IResource[] { project });		
		AllTypesSearchEngine searchEngine= new AllTypesSearchEngine(project.getWorkspace());
		searchEngine.searchTypes(list, searchScope, IJavaElementSearchConstants.CONSIDER_TYPES, null);

		MultiElementListSelectionDialog dialog= new MultiElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle(CodeManipulationMessages.getString("OrganizeImportsOperation.dialog.title")); //$NON-NLS-1$
		dialog.setMessage(CodeManipulationMessages.getString("OrganizeImportsOperation.dialog.message")); //$NON-NLS-1$
		dialog.setPageInfoMessage(CodeManipulationMessages.getString("OrganizeImportsOperation.dialog.pageinfo")); //$NON-NLS-1$
	
		assert(list.size() > 15);
		TypeInfo[][] refs= new TypeInfo[][] { getRefs(list, 0, 3), getRefs(list, 4, 6), getRefs(list, 10, 5) };
		dialog.setElements(refs);
		dialog.setInitialSelections(new String[refs.length]);
		
		DialogCheck.assertDialog(dialog, this);
		
		JavaProjectHelper.delete(jproject);		
	
	}
	
	public void testElementTreeSelectionDialog() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addSourceContainer(jproject, "src1");
		Object initSelection= JavaProjectHelper.addSourceContainer(jproject, "src2");

		IJavaProject jproject2= JavaProjectHelper.createJavaProject("Project2", "bin");
		JavaProjectHelper.addSourceContainer(jproject2, "src1");
		JavaProjectHelper.addSourceContainer(jproject2, "src2");

		JavaElementContentProvider provider= new JavaElementContentProvider();
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT); 
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), labelProvider, provider);
		dialog.setSorter(new PackageViewerSorter());
		dialog.setTitle(NewWizardMessages.getString("ContainerPage.ChooseSourceContainerDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("ContainerPage.ChooseSourceContainerDialog.description")); //$NON-NLS-1$
		
		dialog.setInput(jproject.getJavaModel());
		dialog.setInitialSelection(initSelection);
		
		DialogCheck.assertDialog(dialog, this);
		
		JavaProjectHelper.delete(jproject);	
		JavaProjectHelper.delete(jproject2);
	}
	
	public void testElementListSelectionDialog() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root=  JavaProjectHelper.addRTJar(jproject);
		assertTrue(root != null);
		Object[] elements= root.getChildren();

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
		dialog.setIgnoreCase(false);
		dialog.setTitle(NewWizardMessages.getString("TypePage.ChoosePackageDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("TypePage.ChoosePackageDialog.description")); //$NON-NLS-1$
		dialog.setEmptyListMessage(NewWizardMessages.getString("TypePage.ChoosePackageDialog.empty")); //$NON-NLS-1$
		
		dialog.setElements(Arrays.asList(elements));
		
		DialogCheck.assertDialog(dialog, this);
		
		JavaProjectHelper.delete(jproject);	
	}
	
	public void testAddExceptionDialog() throws Exception {	
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addRTJar(jproject);
		
		AddExceptionDialog dialog= new AddExceptionDialog(getShell());

		DialogCheck.assertDialog(dialog, this);
		
		JavaProjectHelper.delete(jproject);	
	}


}

