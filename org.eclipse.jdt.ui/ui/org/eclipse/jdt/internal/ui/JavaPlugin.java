/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.compiler.ConfigurableOption;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.launcher.VMPreferencePage;
import org.eclipse.jdt.internal.ui.packageview.ErrorTickManager;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.preferences.ClasspathVariablesPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaEditorPreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringPreferencePage;
import org.eclipse.jdt.internal.ui.snippeteditor.SnippetFileDocumentProvider;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.text.JavaTextTools;

/**
 * Represents the java plugin. It provides a series of convenience methods such as
 * access to the workbench, keeps track of elements shared by all editors and viewers
 * of the plugin such as document providers and find-replace-dialogs.
 */
public class JavaPlugin extends AbstractUIPlugin {
		
	private static JavaPlugin fgJavaPlugin;

	private CompilationUnitDocumentProvider fCompilationUnitDocumentProvider;
	private ClassFileDocumentProvider fClassFileDocumentProvider;
	private FileDocumentProvider fSnippetDocumentProvider;
	private JavaTextTools fJavaTextTools;
	private ErrorTickManager fErrorTickManager;
	
	
	public static JavaPlugin getDefault() {
		return fgJavaPlugin;
	}
	
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	public static IWorkbenchPage getActivePage() {
		return getDefault().internalGetActivePage();
	}
	
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}
	
	public static Shell getActiveWorkbenchShell() {
		return getActiveWorkbenchWindow().getShell();
	}
	
	/**
	 * Returns an array of all dirty editor parts.
	 * @return an array of all dirty editor parts.
	 */
	public static IEditorPart[] getDirtyEditors() {
		List result= new ArrayList(0);
		IWorkbench workbench= getDefault().getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorPart[] editors= pages[x].getEditors();
				for (int z= 0; z < editors.length; z++) {
					IEditorPart editor= editors[z];
					if (editor.isDirty())
						result.add(editor);
				}
			}
		}
		return (IEditorPart[])result.toArray(new IEditorPart[result.size()]);
	}
		
	public static String getPluginId() {
		return getDefault().getDescriptor().getUniqueIdentifier();
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IJavaUIStatus.INTERNAL_ERROR, message, null));
	}

	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi= new MultiStatus(getPluginId(), IJavaUIStatus.INTERNAL_ERROR, message, null);
		multi.add(status);
		log(multi);
	}
	
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IJavaUIStatus.INTERNAL_ERROR, JavaUIMessages.getString("JavaPlugin.internal_error"), e)); //$NON-NLS-1$
	}
	
	public static boolean isDebug() {
		return getDefault().isDebugging();
	}
	
	public static ErrorTickManager getErrorTickManager() {
		return getDefault().fErrorTickManager;
	}
	
	/* package */ static IPath getInstallLocation() {
		return new Path(getDefault().getDescriptor().getInstallURL().getFile());
	}
	
	public JavaPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgJavaPlugin= this;
	}
	
	/* (non - Javadoc)
	 * Method declared in Plugin
	 */
	public void startup() {
		IAdapterManager manager= Platform.getAdapterManager();
		manager.registerAdapters(new JavaElementAdapterFactory(), IJavaElement.class);
		manager.registerAdapters(new MarkerAdapterFactory(), IMarker.class);
		manager.registerAdapters(new EditorInputAdapterFactory(), IEditorInput.class);
		manager.registerAdapters(new ResourceAdapterFactory(), IResource.class);
				
		fErrorTickManager= new ErrorTickManager();
		
		try {
			VMPreferencePage.initializeVMInstall();
		} catch (CoreException e) {
			log(e.getStatus());
		}
	}
		
	/* (non - Javadoc)
	 * Method declared in AbstractUIPlugin
	 */
	protected ImageRegistry createImageRegistry() {
		return JavaPluginImages.getImageRegistry();
	}

	/* (non - Javadoc)
	 * Method declared in Plugin
	 */
	public void shutdown() throws CoreException {
		
		super.shutdown();
		
		if (fCompilationUnitDocumentProvider != null) {
			fCompilationUnitDocumentProvider.shutdown();
			fCompilationUnitDocumentProvider= null;
		}
				
		if (fJavaTextTools != null) {
			fJavaTextTools.dispose();
			fJavaTextTools= null;
		}
	}
	
	private IWorkbenchPage internalGetActivePage() {
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
		
	public CompilationUnitDocumentProvider getCompilationUnitDocumentProvider() {
		if (fCompilationUnitDocumentProvider == null) {
			fCompilationUnitDocumentProvider= new CompilationUnitDocumentProvider();
		}
		return fCompilationUnitDocumentProvider;
	}
	
	public ClassFileDocumentProvider getClassFileDocumentProvider() {
		if (fClassFileDocumentProvider == null)
			fClassFileDocumentProvider= new ClassFileDocumentProvider();
		return fClassFileDocumentProvider;
	}
	
	public IDocumentProvider getSnippetDocumentProvider() {
		if (fSnippetDocumentProvider == null)
			fSnippetDocumentProvider= new SnippetFileDocumentProvider();
		return fSnippetDocumentProvider;
	}

	public IWorkingCopyManager getWorkingCopyManager() {
		return getCompilationUnitDocumentProvider();
	}
	
	public JavaTextTools getJavaTextTools() {
		if (fJavaTextTools == null)
			fJavaTextTools= new JavaTextTools();
		return fJavaTextTools;
	}
	
	public ConfigurableOption[] getCodeFormatterOptions() {
		return CodeFormatterPreferencePage.getCurrentOptions();
	}
	
	/**
	 * Creates the Java plugin standard groups in a context menu.
	 */
	public static void createStandardGroups(IMenuManager menu) {
		if (!menu.isEmpty())
			return;
			
		menu.add(new Separator(IContextMenuConstants.GROUP_NEW));
		menu.add(new GroupMarker(IContextMenuConstants.GROUP_GOTO));
		menu.add(new GroupMarker(IContextMenuConstants.GROUP_OPEN));
		menu.add(new Separator(IContextMenuConstants.GROUP_SHOW));
		menu.add(new Separator(IContextMenuConstants.GROUP_BUILD));
		menu.add(new Separator(IContextMenuConstants.GROUP_REORGANIZE));
		menu.add(new GroupMarker(IContextMenuConstants.GROUP_GENERATE));
		menu.add(new Separator(IContextMenuConstants.GROUP_SEARCH));
		menu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
		menu.add(new Separator(IContextMenuConstants.GROUP_VIEWER_SETUP));
		menu.add(new Separator(IContextMenuConstants.GROUP_PROPERTIES));
	}

	/**
	 * @see AbstractUIPlugin#initializeDefaultPreferences
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		super.initializeDefaultPreferences(store);
		
		store.setDefault(IPreferencesConstants.ATTACH_LAUNCH_PORT, "8000"); //$NON-NLS-1$
		store.setDefault(IPreferencesConstants.ATTACH_LAUNCH_HOST, "localhost"); //$NON-NLS-1$
		
		store.setDefault(IPreferencesConstants.EDITOR_SHOW_HOVER, true);
		store.setDefault(IPreferencesConstants.EDITOR_SHOW_SEGMENTS, false);
		
		JavaBasePreferencePage.initDefaults(store);
		ImportOrganizePreferencePage.initDefaults(store);
		ClasspathVariablesPreferencePage.initDefaults(store);
		RefactoringPreferencePage.initDefaults(store);		
		CodeFormatterPreferencePage.initDefaults(store);
		
		PackageExplorerPart.initDefaults(store);
		JavaEditorPreferencePage.initDefaults(store);
		
	}
}