package org.eclipse.jdt.ui.tests.model;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class ContentProviderTests extends TestCase {
	
	private IWorkspace fWorkspace;
	private IJavaProject fJProject1;
	private boolean fEnableAutoBuildAfterTesting;
	private IWorkbench fWorkbench;
	private IWorkbenchPage fPage;
	private MockPluginView fMyPart;
	private ITreeContentProvider fProvider;
	private IJavaElement fPackageFragment1;
	private IJavaElement fPackageFragment2;
	private IFile fFile1;

	protected void setUp() throws Exception {
		super.setUp();
		
		fWorkspace= ResourcesPlugin.getWorkspace();
		assertNotNull(fWorkspace);
		IWorkspaceDescription workspaceDesc= fWorkspace.getDescription();
		fEnableAutoBuildAfterTesting= workspaceDesc.isAutoBuilding();
		if (fEnableAutoBuildAfterTesting)
			JavaProjectHelper.setAutoBuilding(false);
		
		assertNotNull(fWorkspace);	
		
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");//$NON-NLS-1$//$NON-NLS-2$
		assertNotNull("project1 null", fJProject1);//$NON-NLS-1$
		// Use the project root as the classpath
		fJProject1.setRawClasspath(null, null);
		
		// Create some packages
		IProject project = (IProject)fJProject1.getResource();
		project.getFolder("f1").create(false, true, null);
		fPackageFragment1 = JavaCore.create(project.getFolder("f1"));
		project.getFolder("f2").create(false, true, null);
		fPackageFragment2 = JavaCore.create(project.getFolder("f2"));
		
		//Create a non-Java file in one of the packges
		fFile1 = project.getFile("f1/b");
		fFile1.create(new ByteArrayInputStream("".getBytes()), false, null);
		
		
		setUpMockView();
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
		if (fEnableAutoBuildAfterTesting)
			JavaProjectHelper.setAutoBuilding(true);
		tearDownMockView();
		super.tearDown();
	}
	
	private void setUpMockView() throws CoreException {
		fWorkbench= PlatformUI.getWorkbench();
		assertNotNull(fWorkbench);
		
		fPage= fWorkbench.getActiveWorkbenchWindow().getActivePage();
		assertNotNull(fPage);
		
		//just testing to make sure my part can be created
		IViewPart myPart= new MockPluginView();
		assertNotNull(myPart);
		
		myPart= fPage.showView("org.eclipse.jdt.ui.tests.model.MockPluginView");//$NON-NLS-1$
		if (myPart instanceof MockPluginView) {
			fMyPart= (MockPluginView) myPart;
			fProvider= fMyPart.getContentProvider();
			fMyPart.setProject(fJProject1);
		}else assertTrue("Unable to get view",false);//$NON-NLS-1$
	
		assertNotNull(fProvider);	
	}
	
	private void tearDownMockView() {
		fPage.hideView(fMyPart);
	}

	private boolean compareArrays(Object[] children, Object[] expectedChildren) {
		if(children.length!=expectedChildren.length)
			return false;
		for (int i= 0; i < children.length; i++) {
			Object child= children[i];
			if (!contains(child, expectedChildren))
					return false;
		}
		return true;
	}
	private boolean contains(Object expected, Object[] expectedChildren) {
		for (int i= 0; i < expectedChildren.length; i++) {
			Object object= expectedChildren[i];
			if(object.equals(expected))
				return true;
		}
		return false;
	}
	
	public void testOutgoingDeletion148118() throws CoreException {
		IProject project = (IProject)fJProject1.getResource();
		fMyPart.addOutgoingDeletion(project, "f1/a");
		fMyPart.addOutgoingDeletion(project, "f2/a");
		fMyPart.addOutgoingDeletion(project, "f3/");
		
		// Children of project
		Object[] expectedChildren = new Object[] { fPackageFragment1,  fPackageFragment2, project.getFolder("f3/")};
		Object[] children = fProvider.getChildren(fJProject1);
		assertTrue("Expected children of project does not match actual children", compareArrays(children, expectedChildren));
		
		// Children of fragment 1
		expectedChildren = new Object[] { fFile1, ((IFolder)fPackageFragment1.getResource()).getFile("a")};
		children = fProvider.getChildren(fPackageFragment1);
		
		// Children of fragment 2
		expectedChildren = new Object[] { ((IFolder)fPackageFragment2.getResource()).getFile("a")};
		children = fProvider.getChildren(fPackageFragment2);
	}
}