/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.performance;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.core.filebuffers.tests.FileTool;
import org.eclipse.core.filebuffers.tests.ResourceHelper;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IWidgetTokenKeeper;
import org.eclipse.jface.text.reconciler.AbstractReconciler;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.text.JavaReconciler;


/**
 * @since 3.1
 */
public class EditorTestHelper {

	private static class ImportOverwriteQuery implements IOverwriteQuery {
		@Override
		public String queryOverwrite(String file) {
			return ALL;
		}
	}
	private static class Requestor extends TypeNameRequestor {
	}

	public static final String TEXT_EDITOR_ID= "org.eclipse.ui.DefaultTextEditor";

	public static final String COMPILATION_UNIT_EDITOR_ID= "org.eclipse.jdt.ui.CompilationUnitEditor";

	public static final String RESOURCE_PERSPECTIVE_ID= "org.eclipse.ui.resourcePerspective";

	public static final String JAVA_PERSPECTIVE_ID= "org.eclipse.jdt.ui.JavaPerspective";

	public static final String OUTLINE_VIEW_ID= "org.eclipse.ui.views.ContentOutline";

	public static final String PACKAGE_EXPLORER_VIEW_ID= "org.eclipse.jdt.ui.PackageExplorer";

	public static final String NAVIGATOR_VIEW_ID= "org.eclipse.ui.views.ResourceNavigator";

	public static final String INTRO_VIEW_ID= "org.eclipse.ui.internal.introview";

	public static IEditorPart openInEditor(IFile file, boolean runEventLoop) throws PartInitException {
		IEditorPart part= IDE.openEditor(getActivePage(), file);
		if (runEventLoop)
			runEventQueue(part);
		return part;
	}

	public static IEditorPart openInEditor(IFile file, String editorId, boolean runEventLoop) throws PartInitException {
		IEditorPart part= IDE.openEditor(getActivePage(), file, editorId);
		if (runEventLoop)
			runEventQueue(part);
		return part;
	}

	public static AbstractTextEditor[] openInEditor(IFile[] files, String editorId) throws PartInitException {
		AbstractTextEditor editors[]= new AbstractTextEditor[files.length];
		for (int i= 0; i < files.length; i++) {
			editors[i]= (AbstractTextEditor) openInEditor(files[i], editorId, true);
			joinReconciler(getSourceViewer(editors[i]), 100, 10000, 100);
		}
		return editors;
	}

	public static IDocument getDocument(ITextEditor editor) {
		IDocumentProvider provider= editor.getDocumentProvider();
		IEditorInput input= editor.getEditorInput();
		return provider.getDocument(input);
	}

	public static void revertEditor(ITextEditor editor, boolean runEventQueue) {
		editor.doRevertToSaved();
		if (runEventQueue)
			runEventQueue(editor);
	}

	public static void closeEditor(IEditorPart editor) {
		IWorkbenchPartSite site;
		IWorkbenchPage page;
		if (editor != null && (site= editor.getSite()) != null && (page= site.getPage()) != null)
			page.closeEditor(editor, false);
	}

	public static void closeAllEditors() {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorReference : page.getEditorReferences()) {
					closeEditor(editorReference.getEditor(false));
				}
			}
		}
	}

	/**
	 * Runs the event queue on the current display until it is empty.
	 */
	public static void runEventQueue() {
		IWorkbenchWindow window= getActiveWorkbenchWindow();
		if (window != null)
			runEventQueue(window.getShell());
	}

	public static void runEventQueue(IWorkbenchPart part) {
		runEventQueue(part.getSite().getShell());
	}

	public static void runEventQueue(Shell shell) {
		runEventQueue(shell.getDisplay());
	}

	public static void runEventQueue(Display display) {
		while (display.readAndDispatch()) {
			// do nothing
		}
	}

	/**
	 * Runs the event queue on the current display and lets it sleep until the
	 * timeout elapses.
	 *
	 * @param millis the timeout in milliseconds
	 */
	public static void runEventQueue(long millis) {
		runEventQueue(getActiveDisplay(), millis);
	}

	public static void runEventQueue(IWorkbenchPart part, long millis) {
		runEventQueue(part.getSite().getShell(), millis);
	}

	public static void runEventQueue(Shell shell, long millis) {
		runEventQueue(shell.getDisplay(), millis);
	}

	public static void runEventQueue(Display display, long minTime) {
		if (display != null) {
			DisplayHelper.sleep(display, minTime);
		} else {
			sleep((int) minTime);
		}
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	public static void forceFocus() {
		IWorkbenchWindow window= getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] wbWindows= PlatformUI.getWorkbench().getWorkbenchWindows();
			if (wbWindows.length == 0)
				return;
			window= wbWindows[0];
		}
		Shell shell= window.getShell();
		if (shell != null && !shell.isDisposed()) {
			shell.forceActive();
			shell.forceFocus();
		}
	}

	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow window= getActiveWorkbenchWindow();
		return window != null ? window.getActivePage() : null;
	}

	public static Display getActiveDisplay() {
		IWorkbenchWindow window= getActiveWorkbenchWindow();
		return window != null ? window.getShell().getDisplay() : null;
	}

	public static void joinBackgroundActivities(AbstractTextEditor editor) throws CoreException {
		joinBackgroundActivities(getSourceViewer(editor));
	}

	public static void joinBackgroundActivities(SourceViewer sourceViewer) throws CoreException {
		joinBackgroundActivities();
		joinReconciler(sourceViewer, 500, 0, 500);
	}

	public static void joinBackgroundActivities() throws CoreException {
		// Join Building
		Logger.getGlobal().entering("EditorTestHelper", "joinBackgroundActivities");
		Logger.getGlobal().finer("join builder");
		boolean interrupted= true;
		while (interrupted) {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				interrupted= false;
			} catch (InterruptedException e) {
				interrupted= true;
			}
		}
		// Join indexing
		Logger.getGlobal().finer("join indexer");
		new SearchEngine().searchAllTypeNames(
				null,
				SearchPattern.R_EXACT_MATCH,
				"XXXXXXXXX".toCharArray(), // make sure we search a concrete name. This is faster according to Kent
				SearchPattern.R_EXACT_MATCH,
				IJavaSearchConstants.CLASS,
				SearchEngine.createJavaSearchScope(new IJavaElement[0]),
				new Requestor(),
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
				null);
		// Join jobs
		joinJobs(0, 0, 500);
		Logger.getGlobal().exiting("EditorTestHelper", "joinBackgroundActivities");
	}

	public static boolean joinJobs(long minTime, long maxTime, long intervalTime) {
		Logger.getGlobal().entering("EditorTestHelper", "joinJobs");
		runEventQueue(minTime);

		DisplayHelper helper= new DisplayHelper() {
			@Override
			public boolean condition() {
				return allJobsQuiet();
			}
		};
		boolean quiet= helper.waitForCondition(getActiveDisplay(), maxTime > 0 ? maxTime : Long.MAX_VALUE, intervalTime);
		Logger.getGlobal().exiting("EditorTestHelper", "joinJobs", Boolean.valueOf(quiet));
		return quiet;
	}

	public static void sleep(int intervalTime) {
		try {
			Thread.sleep(intervalTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static boolean allJobsQuiet() {
		IJobManager jobManager= Job.getJobManager();
		for (Job job : jobManager.find(null)) {
			int state= job.getState();
			if (state == Job.RUNNING || state == Job.WAITING) {
				Logger.getGlobal().finest(job.toString());
				return false;
			}
		}
		return true;
	}

	public static boolean isViewShown(String viewId) {
		return getActivePage().findViewReference(viewId) != null;
	}

	public static boolean showView(String viewId, boolean show) throws PartInitException {
		IWorkbenchPage activePage= getActivePage();
		IViewReference view= activePage.findViewReference(viewId);
		boolean shown= view != null;
		if (shown != show)
			if (show)
				activePage.showView(viewId);
			else
				activePage.hideView(view);
		return shown;
	}

	public static void bringToTop() {
		getActiveWorkbenchWindow().getShell().forceActive();
	}

	public static void forceReconcile(SourceViewer sourceViewer) {
		Accessor reconcilerAccessor= new Accessor(getReconciler(sourceViewer), AbstractReconciler.class);
		reconcilerAccessor.invoke("forceReconciling", new Object[0]);
	}

	public static boolean joinReconciler(SourceViewer sourceViewer, long minTime, long maxTime, long intervalTime) {
		Logger.getGlobal().entering("EditorTestHelper", "joinReconciler");
		runEventQueue(minTime);

		AbstractReconciler reconciler= getReconciler(sourceViewer);
		if (reconciler == null)
			return true;
		final Accessor backgroundThreadAccessor= getBackgroundThreadAccessor(reconciler);
		final Accessor javaReconcilerAccessor;
		if (reconciler instanceof JavaReconciler)
			javaReconcilerAccessor= new Accessor(reconciler, JavaReconciler.class);
		else
			javaReconcilerAccessor= null;

		DisplayHelper helper= new DisplayHelper() {
			@Override
			public boolean condition() {
				return !isRunning(javaReconcilerAccessor, backgroundThreadAccessor);
			}
		};
		boolean finished= helper.waitForCondition(getActiveDisplay(), maxTime > 0 ? maxTime : Long.MAX_VALUE, intervalTime);
		Logger.getGlobal().exiting("EditorTestHelper", "joinReconciler", Boolean.valueOf(finished));
		return finished;
	}

	public static AbstractReconciler getReconciler(SourceViewer sourceViewer) {
		return (AbstractReconciler) new Accessor(sourceViewer, SourceViewer.class).get("fReconciler");
	}

	public static SourceViewer getSourceViewer(AbstractTextEditor editor) {
		SourceViewer sourceViewer= (SourceViewer) new Accessor(editor, AbstractTextEditor.class).invoke("getSourceViewer", new Object[0]);
		return sourceViewer;
	}

	private static Accessor getBackgroundThreadAccessor(AbstractReconciler reconciler) {
		Object backgroundThread= new Accessor(reconciler, AbstractReconciler.class).get("fThread");
		return new Accessor(backgroundThread, backgroundThread.getClass());
	}

	private static boolean isRunning(Accessor javaReconcilerAccessor, Accessor backgroundThreadAccessor) {
		return (javaReconcilerAccessor != null ? !isInitialProcessDone(javaReconcilerAccessor) : false) || isDirty(backgroundThreadAccessor) || isActive(backgroundThreadAccessor);
	}

	private static boolean isInitialProcessDone(Accessor javaReconcilerAccessor) {
		return ((Boolean) javaReconcilerAccessor.get("fIninitalProcessDone"));
	}

	private static boolean isDirty(Accessor backgroundThreadAccessor) {
		return ((Boolean) backgroundThreadAccessor.invoke("isDirty", new Object[0]));
	}

	private static boolean isActive(Accessor backgroundThreadAccessor) {
		return ((Boolean) backgroundThreadAccessor.invoke("isActive", new Object[0]));
	}

	public static String showPerspective(String perspective) throws WorkbenchException {
		String shownPerspective= getActivePage().getPerspective().getId();
		if (!perspective.equals(shownPerspective)) {
			IWorkbench workbench= PlatformUI.getWorkbench();
			IWorkbenchWindow activeWindow= workbench.getActiveWorkbenchWindow();
			workbench.showPerspective(perspective, activeWindow);
		}
		return shownPerspective;
	}

	public static void closeAllPopUps(SourceViewer sourceViewer) {
		IWidgetTokenKeeper tokenKeeper= owner -> true;
		sourceViewer.requestWidgetToken(tokenKeeper, Integer.MAX_VALUE);
		sourceViewer.releaseWidgetToken(tokenKeeper);
	}

	public static void resetFolding() {
		JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.EDITOR_FOLDING_ENABLED);
	}

	public static boolean enableFolding(boolean value) {
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		boolean oldValue= preferenceStore.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED);
		if (value != oldValue)
			preferenceStore.setValue(PreferenceConstants.EDITOR_FOLDING_ENABLED, value);
		return oldValue;
	}

	public static IJavaProject createJavaProject(String project, String externalSourceFolder) throws CoreException, JavaModelException {
		return createJavaProject(project, externalSourceFolder, false);
	}

	public static IJavaProject createJavaProject(String project, String externalSourceFolder, boolean linkSourceFolder) throws CoreException, JavaModelException {
		IJavaProject javaProject= JavaProjectHelper.createJavaProject(project, "bin");
		Assert.assertNotNull("JRE is null", JavaProjectHelper.addRTJar(javaProject));
		IFolder folder;
		if (linkSourceFolder)
			folder= ResourceHelper.createLinkedFolder((IProject) javaProject.getUnderlyingResource(), new Path("src"), JdtTextTestPlugin.getDefault(), new Path(externalSourceFolder));
		else {
			folder= ((IProject) javaProject.getUnderlyingResource()).getFolder("src");
			importFilesFromDirectory(FileTool.getFileInPlugin(JdtTextTestPlugin.getDefault(), new Path(externalSourceFolder)), folder.getFullPath(), null);
		}
		Assert.assertNotNull(folder);
		Assert.assertTrue(folder.exists());
		JavaProjectHelper.addSourceContainer(javaProject, "src");
		return javaProject;
	}

	public static IJavaProject createJavaProject15(String project, String externalSourceFolder) throws CoreException, JavaModelException {
		return createJavaProject15(project, externalSourceFolder, false);
	}

	public static IJavaProject createJavaProject15(String project, String externalSourceFolder, boolean linkSourceFolder) throws CoreException, JavaModelException {
		IJavaProject javaProject= JavaProjectHelper.createJavaProject(project, "bin");
		Assert.assertNotNull("JRE is null", JavaProjectHelper.addRTJar_15(javaProject, true));
		IFolder folder;
		if (linkSourceFolder)
			folder= ResourceHelper.createLinkedFolder((IProject) javaProject.getUnderlyingResource(), new Path("src"), JdtTextTestPlugin.getDefault(), new Path(externalSourceFolder));
		else {
			folder= ((IProject) javaProject.getUnderlyingResource()).getFolder("src");
			importFilesFromDirectory(FileTool.getFileInPlugin(JdtTextTestPlugin.getDefault(), new Path(externalSourceFolder)), folder.getFullPath(), null);
		}
		Assert.assertNotNull(folder);
		Assert.assertTrue(folder.exists());
		JavaProjectHelper.addSourceContainer(javaProject, "src");
		return javaProject;
	}

	public static IFile[] findFiles(IResource resource) throws CoreException {
		List<IResource> files= new ArrayList<>();
		findFiles(resource, files);
		return files.toArray(new IFile[files.size()]);
	}

	private static void findFiles(IResource resource, List<IResource> files) throws CoreException {
		if (resource instanceof IFile) {
			files.add(resource);
			return;
		}
		if (resource instanceof IContainer) {
			for (IResource res : ((IContainer) resource).members()) {
				findFiles(res, files);
			}
		}
	}

	public static boolean setDialogEnabled(String id, boolean enabled) {
		boolean wasEnabled= OptionalMessageDialog.isDialogEnabled(id);
		if (wasEnabled != enabled)
			OptionalMessageDialog.setDialogEnabled(id, enabled);
		return wasEnabled;
	}

	public static void importFilesFromDirectory(File rootDir, IPath destPath, IProgressMonitor monitor) throws CoreException {
		try {
			IImportStructureProvider structureProvider= FileSystemStructureProvider.INSTANCE;
			List<File> files= new ArrayList<>(100);
			addJavaFiles(rootDir, files);
			ImportOperation op= new ImportOperation(destPath, rootDir, structureProvider, new ImportOverwriteQuery(), files);
			op.setCreateContainerStructure(false);
			op.run(monitor);
		} catch (Exception x) {
			throw newCoreException(x);
		}
	}

	private static CoreException newCoreException(Throwable x) {
		return new CoreException(new Status(IStatus.ERROR, JdtTextTestPlugin.PLUGIN_ID, -1, "", x));
	}

	private static void addJavaFiles(File dir, List<File> collection) throws IOException {
		List<File> subDirs= new ArrayList<>(2);
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				collection.add(file);
			} else if (file.isDirectory()) {
				subDirs.add(file);
			}
		}
		Iterator<File> iter= subDirs.iterator();
		while (iter.hasNext()) {
			File subDir= iter.next();
			addJavaFiles(subDir, collection);
		}
	}

	private EditorTestHelper() {
	}
}
