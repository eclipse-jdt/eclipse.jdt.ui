/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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

import junit.framework.Assert;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.filebuffers.tests.FileTool;
import org.eclipse.core.filebuffers.tests.ResourceHelper;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IWidgetTokenKeeper;
import org.eclipse.jface.text.IWidgetTokenOwner;
import org.eclipse.jface.text.reconciler.AbstractReconciler;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.util.AllTypesCache;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.text.JavaReconciler;


/**
 * @since 3.1
 */
public class EditorTestHelper {
	
	private static class ImportOverwriteQuery implements IOverwriteQuery {
		public String queryOverwrite(String file) {
			return ALL;
		}	
	}		
	private static class Requestor implements ITypeNameRequestor {
		public void acceptClass(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
		}
		public void acceptInterface(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
		}
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
		IWorkbenchPage page= getActivePage();
		if (page != null)
			page.closeEditor(editor, false);
	}
	
	public static void closeAllEditors() {
		IWorkbenchPage page= getActivePage();
		if (page != null)
			page.closeAllEditors(false);
	}
	
	public static void runEventQueue() {
		IWorkbenchWindow window= getActiveWorkbenchWindow();
		if (window != null)
			runEventQueue(window.getShell());
	}
	
	public static void runEventQueue(IWorkbenchPart part) {
		runEventQueue(part.getSite().getShell());
	}
	
	public static void runEventQueue(Shell shell) {
		while (shell.getDisplay().readAndDispatch()) {
			// do nothing
		}
	}
	
	public static void runEventQueue(long minTime) {
		long nextCheck= System.currentTimeMillis() + minTime;
		while (System.currentTimeMillis() < nextCheck) {
			runEventQueue();
			sleep(1);
		}
	}
	
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
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
		boolean interrupted= true;
		while (interrupted) {
			try {
				Platform.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				interrupted= false;
			} catch (InterruptedException e) {
				interrupted= true;
			}
		}
		// Join indexing
		new SearchEngine().searchAllTypeNames(
				null,
				null,
				SearchPattern.R_EXACT_MATCH,
				IJavaSearchConstants.CLASS,
				SearchEngine.createJavaSearchScope(new IJavaElement[0]),
				new Requestor(),
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
				null);
		// Join all types cache
		AllTypesCache.getTypes(SearchEngine.createJavaSearchScope(new IJavaElement[0]), 
				IJavaSearchConstants.CLASS, new NullProgressMonitor(), new ArrayList());
		// Join jobs
		joinJobs(0, 0, 500);
	}
	
	public static boolean joinJobs(long minTime, long maxTime, long intervalTime) {
		long startTime= System.currentTimeMillis() + minTime;
		runEventQueue();
		while (System.currentTimeMillis() < startTime)
			runEventQueue(intervalTime);
		
		long endTime= maxTime > 0  && maxTime < Long.MAX_VALUE ? System.currentTimeMillis() + maxTime : Long.MAX_VALUE;
		boolean calm= allJobsQuiet();
		while (!calm && System.currentTimeMillis() < endTime) {
			runEventQueue(intervalTime);
			calm= allJobsQuiet();
		}
		return calm;
	}
	
	public static void sleep(int intervalTime) {
		try {
			Thread.sleep(intervalTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean allJobsQuiet() {
		IJobManager jobManager= Platform.getJobManager();
		Job[] jobs= jobManager.find(null);
		for (int i= 0; i < jobs.length; i++) {
			Job job= jobs[i];
			int state= job.getState();
			if (state == Job.RUNNING || state == Job.WAITING)
				return false;
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
		if (minTime > 0)
			runEventQueue(minTime);
		
		long endTime= maxTime > 0 && maxTime < Long.MAX_VALUE ? System.currentTimeMillis() + maxTime : Long.MAX_VALUE;
		AbstractReconciler reconciler= getReconciler(sourceViewer);
		if (reconciler == null)
			return true;
		Accessor backgroundThreadAccessor= getBackgroundThreadAccessor(reconciler);
		Accessor javaReconcilerAccessor= null;
		if (reconciler instanceof JavaReconciler)
			javaReconcilerAccessor= new Accessor(reconciler, JavaReconciler.class);
		boolean isRunning= isRunning(javaReconcilerAccessor, backgroundThreadAccessor);
		while (isRunning && System.currentTimeMillis() < endTime) {
			runEventQueue(intervalTime);
			isRunning= isRunning(javaReconcilerAccessor, backgroundThreadAccessor);
		}
		return !isRunning;
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
		return ((Boolean) javaReconcilerAccessor.get("fIninitalProcessDone")).booleanValue();
	}
	
	private static boolean isDirty(Accessor backgroundThreadAccessor) {
		return ((Boolean) backgroundThreadAccessor.invoke("isDirty", new Object[0])).booleanValue();
	}
	
	private static boolean isActive(Accessor backgroundThreadAccessor) {
		return ((Boolean) backgroundThreadAccessor.invoke("isActive", new Object[0])).booleanValue();
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
		IWidgetTokenKeeper tokenKeeper= new IWidgetTokenKeeper() {
			public boolean requestWidgetToken(IWidgetTokenOwner owner) {
				return true;
			}
		};
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
	
	public static IFile[] findFiles(IResource resource) throws CoreException {
		List files= new ArrayList();
		findFiles(resource, files);
		return (IFile[]) files.toArray(new IFile[files.size()]);
	}
	
	private static void findFiles(IResource resource, List files) throws CoreException {
		if (resource instanceof IFile) {
			files.add(resource);
			return;
		}
		if (resource instanceof IContainer) {
			IResource[] resources= ((IContainer) resource).members();
			for (int i= 0; i < resources.length; i++)
				findFiles(resources[i], files);
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
			IImportStructureProvider structureProvider = FileSystemStructureProvider.INSTANCE;
			List files = new ArrayList(100);
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

	private static void addJavaFiles(File dir, List collection) throws IOException {
		File[] files = dir.listFiles();
		List subDirs = new ArrayList(2);
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				collection.add(files[i]);
			} else if (files[i].isDirectory()) {
				subDirs.add(files[i]);
			}
		}
		Iterator iter = subDirs.iterator();
		while (iter.hasNext()) {
			File subDir = (File)iter.next();
			addJavaFiles(subDir, collection);
		}
	}
}
