package org.eclipse.jdt.internal.ui;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

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

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.compiler.ConfigurableOption;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.packageview.ErrorTickManager;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringPreferencePage;
import org.eclipse.jdt.internal.ui.snippeteditor.SnippetFileDocumentProvider;

/**
 * Represents the java plugin. It provides a series of convenience methods such as
 * access to the workbench, keeps track of elements shared by all editors and viewers
 * of the plugin such as document providers and find-replace-dialogs.
 */
public class JavaPlugin extends AbstractUIPlugin {
		
	private static JavaPlugin fgJavaPlugin;
	private static IRuntimeDefaultPreferences[] fgRuntimeSettings= new IRuntimeDefaultPreferences[] {
		new J9Settings(),
		new JDK122Settings()
	};
	
	private static ResourceBundle fgResourceBundle;

	private CompilationUnitDocumentProvider fCompilationUnitDocumentProvider;
	private ClassFileDocumentProvider fClassFileDocumentProvider;
	private FileDocumentProvider fSnippetDocumentProvider;
	private JavaTextTools fJavaTextTools;
	private ErrorTickManager fErrorTickManager;
	
	private ConfigurableOption[] fCodeFormatterOptions;
	
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
		log(new Status(IStatus.ERROR, getPluginId(), IJavaUIStatus.INTERNAL_ERROR, "Internal Error", e));
	}
	
	// ---------- resource bundle -------------
	
	/**
	 * Gets a string from the JavaPlugin resource bundle.
	 * We don't want to crash because of a missing String.
	 * Returns the key if not found.
	 */
	public static String getResourceString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		} catch (NullPointerException e) {
			return "!" + key + "!";
		}
			
	}
	/**
	 * Gets a string from the resource bundle and formats it with an argument
	 */	
	public static String getFormattedString(String key, String arg) {
		return MessageFormat.format(getResourceString(key), new String[] { arg });
	}
	
	/**
	 * Gets a string from the resource bundle and formats it with arguments
	 */	
	public static String getFormattedString(String key, String[] args) {
		return MessageFormat.format(getResourceString(key), args);
	}
	
	/**
	 * Gets the Java UI resource bundle
	 */
	public static ResourceBundle getResourceBundle() {
		return fgResourceBundle;
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
		try {
			fgResourceBundle= ResourceBundle.getBundle("org.eclipse.jdt.internal.ui.JavaPluginResources");
		} catch (MissingResourceException x) {
			fgResourceBundle= null;
		}		
		
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
	
		fCodeFormatterOptions= CodeFormatter.getDefaultOptions(Locale.getDefault());		
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
		
		fCodeFormatterOptions= null;
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
		return fCodeFormatterOptions;
	}
	
	/**
	 * Creates the Java plugin standard groups in a context menu.
	 */
	public static void createStandardGroups(IMenuManager menu) {
		if (!menu.isEmpty())
			return;
			
		menu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
		menu.add(new Separator(IContextMenuConstants.GROUP_SHOW));
		menu.add(new Separator(IContextMenuConstants.GROUP_NEW));
		menu.add(new Separator(IContextMenuConstants.GROUP_BUILD));
		menu.add(new Separator(IContextMenuConstants.GROUP_REORGANIZE));
		menu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
		menu.add(new Separator(IContextMenuConstants.GROUP_SEARCH));
		menu.add(new Separator(IContextMenuConstants.GROUP_VIEWER_SETUP));
		menu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
		menu.add(new Separator(IContextMenuConstants.GROUP_PROPERTIES));
	}
	
	protected void initializeEditorSettings(IPreferenceStore store) {
		String[] keys= new String[] {
			IPreferencesConstants.HIGHLIGHT_KEYWORDS,
			IPreferencesConstants.HIGHLIGHT_TYPES,
			IPreferencesConstants.HIGHLIGHT_STRINGS,
			IPreferencesConstants.HIGHLIGHT_COMMENTS
		};
		
		for (int i= 0; i < keys.length; i++) {
			if (!store.contains(keys[i]))
				store.setDefault(keys[i], IPreferencesConstants.HIGHLIGHT_DEFAULT);
		}
		
		if (!store.contains(IPreferencesConstants.AUTOINDENT))
			store.setDefault(IPreferencesConstants.AUTOINDENT, IPreferencesConstants.AUTOINDENT_DEFAULT);
		
		if (!store.contains(IPreferencesConstants.MODEL_RECONCILER_DELAY))
			store.setDefault(IPreferencesConstants.MODEL_RECONCILER_DELAY, IPreferencesConstants.MODEL_RECONCILER_DELAY_DEFAULT);		
	}


	//---- Runtime settings -------------------------------------------------------
	
	public static IRuntimeDefaultPreferences[] getRuntimeSettings() {
		return fgRuntimeSettings;
	}
	
	protected void initializeJavaSettings(IPreferenceStore store) {
		IRuntimeDefaultPreferences[] settings= getRuntimeSettings();
		for (int i= 0; i < settings.length; i++) {
			if (settings[i].matches()) {
				settings[i].setDefaultPreferences(store);
				return;
			}
		}
	}
	
	/**
	 * @see AbstractUIPlugin#initializeDefaultPreferences
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		super.initializeDefaultPreferences(store);
		
		store.setDefault(IPreferencesConstants.LINK_PACKAGES_TO_EDITOR, true);
		store.setDefault(IPreferencesConstants.ATTACH_LAUNCH_PORT, "8000");
		store.setDefault(IPreferencesConstants.ATTACH_LAUNCH_HOST, "localhost");
		
		ImportOrganizePreferencePage.initDefaults();
		RefactoringPreferencePage.initDefaults(store);		
		CodeFormatterPreferencePage.initDefaults(store);
		initializeEditorSettings(store);
		initializeJavaSettings(store);
		
	}

	/*private File findSourceJarStandard(File javaHome) {
		String parent= javaHome.getParent();
		if (parent == null)
			return null;
		File jre= new File(parent);
		parent= jre.getParent();
		if (parent == null)
			return null;
		String srcPath= parent+File.separator+"src.jar";
		File srcJar= new File(srcPath);
		if (srcJar.isFile())
			return srcJar;
		return null;
	}
	
	private File findSourceJarWSW(File javaHome) {
		String srcPath= javaHome.toString()+File.separator+"src.jar";
		File srcJar= new File(srcPath);
		if (srcJar.isFile())
			return srcJar;
		return null;
	}*/
}