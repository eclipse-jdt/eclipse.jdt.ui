/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;

import org.eclipse.jdt.internal.ui.javaeditor.ClassFileDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.preferences.MembersOrderPreferenceCache;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemMarkerManager;

/**
 * Represents the java plugin. It provides a series of convenience methods such as
 * access to the workbench, keeps track of elements shared by all editors and viewers
 * of the plugin such as document providers and find-replace-dialogs.
 */
public class JavaPlugin extends AbstractUIPlugin {

	// TODO: Evaluate if we should move these ID's to JavaUI
	/**
	 * The id of the best match hover contributed for extension point
	 * <code>javaEditorTextHovers</code>.
	 *
	 * @since 2.1
	 */
	public static String ID_BESTMATCH_HOVER= "org.eclipse.jdt.ui.BestMatchHover"; //$NON-NLS-1$

	/**
	 * The id of the source code hover contributed for extension point
	 * <code>javaEditorTextHovers</code>.
	 *
	 * @since 2.1
	 */
	public static String ID_SOURCE_HOVER= "org.eclipse.jdt.ui.JavaSourceHover"; //$NON-NLS-1$

	/**
	 * The id of the javadoc hover contributed for extension point
	 * <code>javaEditorTextHovers</code>.
	 *
	 * @since 2.1
	 */
	public static String ID_JAVADOC_HOVER= "org.eclipse.jdt.ui.JavadocHover"; //$NON-NLS-1$

	/**
	 * The id of the problem hover contributed for extension point
	 * <code>javaEditorTextHovers</code>.
	 *
	 * @since 2.1
	 */
	public static String ID_PROBLEM_HOVER= "org.eclipse.jdt.ui.ProblemHover"; //$NON-NLS-1$


		
	private static JavaPlugin fgJavaPlugin;

	private CompilationUnitDocumentProvider fCompilationUnitDocumentProvider;
	private ClassFileDocumentProvider fClassFileDocumentProvider;
	private JavaTextTools fJavaTextTools;
	private ProblemMarkerManager fProblemMarkerManager;
	private ImageDescriptorRegistry fImageDescriptorRegistry;
	
	private JavaElementAdapterFactory fJavaElementAdapterFactory;
	private MarkerAdapterFactory fMarkerAdapterFactory;
	private EditorInputAdapterFactory fEditorInputAdapterFactory;
	private ResourceAdapterFactory fResourceAdapterFactory;
	private MembersOrderPreferenceCache fMembersOrderPreferenceCache;
	private IPropertyChangeListener fFontPropertyChangeListener;
	
	
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
	 * Returns an array of all editors that have an unsaved content. If the identical content is 
	 * presented in more than one editor, only one of those editor parts is part of the result.
	 * 
	 * @return an array of all dirty editor parts.
	 */
	public static IEditorPart[] getDirtyEditors() {
		Set inputs= new HashSet();
		List result= new ArrayList(0);
		IWorkbench workbench= getDefault().getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorPart[] editors= pages[x].getDirtyEditors();
				for (int z= 0; z < editors.length; z++) {
					IEditorPart ep= editors[z];
					IEditorInput input= ep.getEditorInput();
					if (!inputs.contains(input)) {
						inputs.add(input);
						result.add(ep);
					}
				}
			}
		}
		return (IEditorPart[])result.toArray(new IEditorPart[result.size()]);
	}
	
	/**
	 * Returns an array of all instanciated editors. 
	 */
	public static IEditorPart[] getInstanciatedEditors() {
		List result= new ArrayList(0);
		IWorkbench workbench= getDefault().getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorReference[] references= pages[i].getEditorReferences();
				for (int j= 0; j < references.length; j++) {
					IEditorPart editor= references[j].getEditor(false);
					if (editor != null)
						result.add(editor);
				}
			}
		}
		return (IEditorPart[])result.toArray(new IEditorPart[result.size()]);
	}	public static String getPluginId() {
		return getDefault().getDescriptor().getUniqueIdentifier();
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, null));
	}

	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi= new MultiStatus(getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, null);
		multi.add(status);
		log(multi);
	}
	
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, JavaUIMessages.getString("JavaPlugin.internal_error"), e)); //$NON-NLS-1$
	}
	
	public static boolean isDebug() {
		return getDefault().isDebugging();
	}
	
	/* package */ static IPath getInstallLocation() {
		return new Path(getDefault().getDescriptor().getInstallURL().getFile());
	}
	
	public static ImageDescriptorRegistry getImageDescriptorRegistry() {
		return getDefault().internalGetImageDescriptorRegistry();
	}
	
	public JavaPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgJavaPlugin= this;
	}
	
	/* (non - Javadoc)
	 * Method declared in Plugin
	 */
	public void startup() throws CoreException {
		super.startup();
		registerAdapters();
		
		/*
		 * Backward compatibility: set the Java editor font in this plug-in's
		 * preference store to let older versions access it. Since 2.1 the
		 * Java editor font is managed by the workbench font preference page.
		 */
		fFontPropertyChangeListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (PreferenceConstants.EDITOR_TEXT_FONT.equals(event.getProperty()))
					PreferenceConverter.setValue(getPreferenceStore(), JFaceResources.TEXT_FONT, JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT).getFontData());
			}
		};
		JFaceResources.getFontRegistry().addListener(fFontPropertyChangeListener);
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
		
		if (fImageDescriptorRegistry != null)
			fImageDescriptorRegistry.dispose();

		unregisterAdapters();
		
		super.shutdown();
		
		if (fCompilationUnitDocumentProvider != null) {
			fCompilationUnitDocumentProvider.shutdown();
			fCompilationUnitDocumentProvider= null;
		}
				
		if (fJavaTextTools != null) {
			fJavaTextTools.dispose();
			fJavaTextTools= null;
		}
		
		JavaDocLocations.shutdownJavadocLocations();
		
		JFaceResources.getFontRegistry().removeListener(fFontPropertyChangeListener);
	}
	
	private IWorkbenchPage internalGetActivePage() {
		IWorkbenchWindow window= getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
		
	public synchronized CompilationUnitDocumentProvider getCompilationUnitDocumentProvider() {
		if (fCompilationUnitDocumentProvider == null)
			fCompilationUnitDocumentProvider= new CompilationUnitDocumentProvider();
		return fCompilationUnitDocumentProvider;
	}
	
	public synchronized ClassFileDocumentProvider getClassFileDocumentProvider() {
		if (fClassFileDocumentProvider == null)
			fClassFileDocumentProvider= new ClassFileDocumentProvider();
		return fClassFileDocumentProvider;
	}

	public synchronized IWorkingCopyManager getWorkingCopyManager() {
		return getCompilationUnitDocumentProvider();
	}
	
	public synchronized ProblemMarkerManager getProblemMarkerManager() {
		if (fProblemMarkerManager == null)
			fProblemMarkerManager= new ProblemMarkerManager();
		return fProblemMarkerManager;
	}	
	
	public synchronized JavaTextTools getJavaTextTools() {
		if (fJavaTextTools == null)
			fJavaTextTools= new JavaTextTools(getPreferenceStore());
		return fJavaTextTools;
	}
	
	public synchronized MembersOrderPreferenceCache getMemberOrderPreferenceCache() {
		if (fMembersOrderPreferenceCache == null)
			fMembersOrderPreferenceCache= new MembersOrderPreferenceCache();
		return fMembersOrderPreferenceCache;
	}	
		
	/**
	 * Creates the Java plugin standard groups in a context menu.
	 */
	public static void createStandardGroups(IMenuManager menu) {
		if (!menu.isEmpty())
			return;
			
		menu.add(new Separator(IContextMenuConstants.GROUP_NEW));
		menu.add(new GroupMarker(IContextMenuConstants.GROUP_GOTO));
		menu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
		menu.add(new GroupMarker(IContextMenuConstants.GROUP_SHOW));
		menu.add(new Separator(IContextMenuConstants.GROUP_REORGANIZE));
		menu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
		menu.add(new Separator(IContextMenuConstants.GROUP_SEARCH));
		menu.add(new Separator(IContextMenuConstants.GROUP_BUILD));
		menu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
		menu.add(new Separator(IContextMenuConstants.GROUP_VIEWER_SETUP));
		menu.add(new Separator(IContextMenuConstants.GROUP_PROPERTIES));
	}

	/**
	 * @see AbstractUIPlugin#initializeDefaultPreferences
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		super.initializeDefaultPreferences(store);
		
		PreferenceConstants.initializeDefaultValues(store);
	}
	
	private synchronized ImageDescriptorRegistry internalGetImageDescriptorRegistry() {
		if (fImageDescriptorRegistry == null)
			fImageDescriptorRegistry= new ImageDescriptorRegistry();
		return fImageDescriptorRegistry;
	}

	private void registerAdapters() {
		fJavaElementAdapterFactory= new JavaElementAdapterFactory();
		fMarkerAdapterFactory= new MarkerAdapterFactory();
		fEditorInputAdapterFactory= new EditorInputAdapterFactory();
		fResourceAdapterFactory= new ResourceAdapterFactory();

		IAdapterManager manager= Platform.getAdapterManager();		
		manager.registerAdapters(fJavaElementAdapterFactory, IJavaElement.class);
		manager.registerAdapters(fMarkerAdapterFactory, IMarker.class);
		manager.registerAdapters(fEditorInputAdapterFactory, IEditorInput.class);
		manager.registerAdapters(fResourceAdapterFactory, IResource.class);
	}

	private void unregisterAdapters() {
		IAdapterManager manager= Platform.getAdapterManager();
		manager.unregisterAdapters(fJavaElementAdapterFactory);
		manager.unregisterAdapters(fMarkerAdapterFactory);
		manager.unregisterAdapters(fEditorInputAdapterFactory);
		manager.unregisterAdapters(fResourceAdapterFactory);
	}
}