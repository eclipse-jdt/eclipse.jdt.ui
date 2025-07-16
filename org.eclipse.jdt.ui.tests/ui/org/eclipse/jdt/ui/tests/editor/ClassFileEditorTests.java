package org.eclipse.jdt.ui.tests.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.jdt.internal.ui.javaeditor.DelayedEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.InternalClassFileEditorInput;

public class ClassFileEditorTests {

	class DelayedEditorInputTestController {

		private IEditorInput fInput;
		private boolean fDelayFinished;

		public DelayedEditorInputTestController() {

		}

		public void setDelayFinished(boolean delayFinished) {
			fDelayFinished= delayFinished;
		}

		public void setInput(IEditorInput input) {
			fInput= input;
		}

		public IEditorInput getInput() {
			return fInput;
		}

		public boolean isDelayFinished() {
			return fDelayFinished;
		}
	}

	private static boolean welcomeClosed;
	IViewPart view;
	private IWorkbenchWindow window;
	private IJavaProject javaProject ;
	private IEditorPart editor;

	@Before
	public void setUp() throws Exception {
		window = setupWindow();
		javaProject = createProject();
		DisplayHelper.driveEventQueue(Display.getDefault());
	}

	@After
	public void tearDown() throws Exception {
		if (window != null) {
			if (editor != null) {
				window.getActivePage().closeEditor(editor, false);
			}
			if (view != null) {
				window.getActivePage().hideView(view);
			}
			DisplayHelper.driveEventQueue(Display.getDefault());
		}
		if (javaProject != null) {
			JavaProjectHelper.delete(javaProject);
		}
	}

	private static IWorkbenchWindow setupWindow() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (!welcomeClosed) {
			closeIntro(workbench);
		}
		IWorkbenchWindow wwindow = workbench.getActiveWorkbenchWindow();
		if (wwindow == null) {
			wwindow =  workbench.getWorkbenchWindows()[0];
		}
		return wwindow;
	}

	private static void closeIntro(final IWorkbench wb) {
		IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
		if (window != null) {
			IIntroManager im = wb.getIntroManager();
			IIntroPart intro = im.getIntro();
			if (intro != null) {
				welcomeClosed = im.closeIntro(intro);
				DisplayHelper.driveEventQueue(Display.getDefault());
			}
		}
	}

	@Test
	public void delayedEditorInputNeg0() throws PartInitException, OperationCanceledException, InterruptedException {
		var testController = new DelayedEditorInputTestController();
		DelayedEditorInput input = new DelayedEditorInput(javaProject, null) {

			@Override
			public org.eclipse.ui.IEditorInput getDelayedIEditorInput() {
				return testController.getInput();
			}

			@Override
			public boolean delayIsFinished() {
				return testController.isDelayFinished();
			}

		};
		testController.setInput(input);
		testController.setDelayFinished(false);

		IEditorPart ceditor = IDE.openEditor(window.getActivePage(), input, "org.eclipse.jdt.ui.ClassFileEditor", true);
		assertNotNull("Class file editor should be opened", ceditor);
		assertTrue("Class File editor should be of same type", ceditor instanceof ClassFileEditor);

		Composite composite = ceditor.getAdapter(Composite.class);
		assertNotNull("The parent composite of the class file editor should exist", composite);

		assertEquals("Composite must have two children", 2, composite.getChildren().length);
		assertTrue("First child must be a label", composite.getChildren()[0] instanceof Label);
		assertSame("The icon must be the default working icon of SWT",
				Display.getDefault().getSystemImage(SWT.ICON_WORKING),
				((Label)composite.getChildren()[0]).getImage());

		assertTrue("Second child must be a label", composite.getChildren()[1] instanceof Label);
		assertEquals("The hint didn't matched",
				"Classpath initialization of the corresponding project is in progress. The content of this editor is going to be loaded.",
				((Label)composite.getChildren()[1]).getText());

		testController.setDelayFinished(true);

		Job.getJobManager().join(DelayedEditorInput.class, new NullProgressMonitor());

		assertEquals("Composite must have two children", 2, composite.getChildren().length);
		assertTrue("First child must be a label", composite.getChildren()[0] instanceof Label);
		assertSame("The icon must be the default error icon of SWT",
				Display.getDefault().getSystemImage(SWT.ICON_ERROR),
				((Label)composite.getChildren()[0]).getImage());

		assertTrue("Second child must be a label", composite.getChildren()[1] instanceof Label);
		assertEquals("The hint didn't matched",
				"Classpath initialization is done, but the correct input couldn't be restored.",
				((Label)composite.getChildren()[1]).getText());

		DisplayHelper.driveEventQueue(Display.getDefault());
	}

	@Test
	public void delayedEditorInput() throws JavaModelException, PartInitException, OperationCanceledException, InterruptedException {
		IOrdinaryClassFile classfile = (IOrdinaryClassFile) javaProject.findElement(new Path("java/lang/String.java"));
		assertNotNull("java.lang.String class not found", classfile);
		InternalClassFileEditorInput origInp = new InternalClassFileEditorInput(classfile);

		var testController = new DelayedEditorInputTestController();
		DelayedEditorInput input = new DelayedEditorInput(javaProject, null) {

			@Override
			public org.eclipse.ui.IEditorInput getDelayedIEditorInput() {
				return testController.getInput();
			}

			@Override
			public boolean delayIsFinished() {
				return testController.isDelayFinished();
			}

		};
		testController.setInput(input);
		testController.setDelayFinished(false);

		IEditorPart ceditor = IDE.openEditor(window.getActivePage(), input, "org.eclipse.jdt.ui.ClassFileEditor", true);
		assertNotNull("Class file editor should be opened", ceditor);
		assertTrue("Class File editor should be of same type", ceditor instanceof ClassFileEditor);

		Composite composite = ceditor.getAdapter(Composite.class);
		assertNotNull("The parent composite of the class file editor should exist", composite);

		assertNull(((ClassFileEditor) ceditor).getViewer());

		assertEquals("Composite must have two children", 2, composite.getChildren().length);
		assertTrue("First child must be a label", composite.getChildren()[0] instanceof Label);
		assertSame("The icon must be the default working icon of SWT",
				Display.getDefault().getSystemImage(SWT.ICON_WORKING),
				((Label)composite.getChildren()[0]).getImage());

		assertTrue("Second child must be a label", composite.getChildren()[1] instanceof Label);
		assertEquals("The hint didn't matched",
				"Classpath initialization of the corresponding project is in progress. The content of this editor is going to be loaded.",
				((Label)composite.getChildren()[1]).getText());

		testController.setInput(origInp);
		testController.setDelayFinished(true);

		Job.getJobManager().join(DelayedEditorInput.class, new NullProgressMonitor());

		assertNotNull(((ClassFileEditor) ceditor).getViewer());

		DisplayHelper.driveEventQueue(Display.getDefault());
	}

	private IJavaProject createProject() throws CoreException {
		IJavaProject project = JavaProjectHelper.createJavaProject("SmokeViewsTest", "bin");

		String javaHome = System.getProperty("java.home") + File.separator;
		String latestSupportedJavaVersion = JavaCore.latestSupportedJavaVersion();
		String jdkRelease = System.getProperty("java.specification.version");
		if(jdkRelease.compareTo(latestSupportedJavaVersion) >= 0) {
			JavaProjectHelper.setLatestCompilerOptions(project);
		} else {
			// Smallest version available for tests on Jenkins/jipp
			JavaProjectHelper.set18CompilerOptions(project);
		}

		Path bootModPath = new Path(javaHome + "/lib/jrt-fs.jar");
		Path sourceAttachment = new Path(javaHome + "/lib/src.zip");

		IClasspathAttribute[] attributes = { JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, "true") };
		IClasspathEntry jrtEntry = JavaCore.newLibraryEntry(bootModPath, sourceAttachment, null, null, attributes, false);
		IClasspathEntry[] old = project.getRawClasspath();
		IClasspathEntry[] newPath = new IClasspathEntry[old.length + 1];
		System.arraycopy(old, 0, newPath, 0, old.length);
		newPath[old.length] = jrtEntry;
		project.setRawClasspath(newPath, null);
		DisplayHelper.driveEventQueue(Display.getDefault());
		return project;
	}
}
