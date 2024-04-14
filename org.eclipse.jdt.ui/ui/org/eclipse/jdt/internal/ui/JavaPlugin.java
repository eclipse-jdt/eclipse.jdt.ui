/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.themes.IThemeManager;

import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ConfigurationElementSorter;
import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapperDynamic;
import org.eclipse.jdt.internal.corext.fix.CleanUpRegistry;
import org.eclipse.jdt.internal.corext.template.java.AbstractJavaContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaPostfixContextType;
import org.eclipse.jdt.internal.corext.template.java.SWTContextType;
import org.eclipse.jdt.internal.corext.util.OpenTypeHistory;
import org.eclipse.jdt.internal.corext.util.QualifiedTypeNameHistory;
import org.eclipse.jdt.internal.corext.util.TypeFilter;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.WorkingCopyManager;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.SaveParticipantRegistry;
import org.eclipse.jdt.internal.ui.preferences.MembersOrderPreferenceCache;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileStore;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileDocumentProvider;
import org.eclipse.jdt.internal.ui.text.PreferencesAdapter;
import org.eclipse.jdt.internal.ui.text.folding.JavaFoldingStructureProviderRegistry;
import org.eclipse.jdt.internal.ui.text.java.ContentAssistHistory;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaEditorTextHoverDescriptor;
import org.eclipse.jdt.internal.ui.text.spelling.SpellCheckEngine;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.ImagesOnFileSystemRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemMarkerManager;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ClasspathAttributeConfigurationDescriptors;
import org.eclipse.jdt.internal.ui.workingsets.DynamicSourcesWorkingSetUpdater;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetMessages;


/**
 * Represents the java plug-in. It provides a series of convenience methods such as
 * access to the workbench, keeps track of elements shared by all editors and viewers
 * of the plug-in such as document providers and find-replace-dialogs.
 */
public class JavaPlugin extends AbstractUIPlugin implements DebugOptionsListener {

	/**
	 * The key to store customized templates.
	 * @since 3.0
	 */
	private static final String TEMPLATES_KEY= "org.eclipse.jdt.ui.text.custom_templates"; //$NON-NLS-1$
	/**
	 * The key to store customized code templates.
	 * @since 3.0
	 */
	private static final String CODE_TEMPLATES_KEY= "org.eclipse.jdt.ui.text.custom_code_templates"; //$NON-NLS-1$

	public static boolean DEBUG_AST_PROVIDER;

	public static boolean DEBUG_BREADCRUMB_ITEM_DROP_DOWN;

	public static boolean DEBUG_RESULT_COLLECTOR;

	private static JavaPlugin fgJavaPlugin;

	private static LinkedHashMap<String, Long> fgRepeatedMessages= new LinkedHashMap<>(20, 0.75f, true) {
		private static final long serialVersionUID= 1L;
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
			return size() >= 20;
		}
	};

	/**
	 * The template context type registry for the java editor.
	 * @since 3.0
	 */
	private volatile ContextTypeRegistry fContextTypeRegistry;
	/**
	 * The code template context type registry for the java editor.
	 * @since 3.0
	 */
	private volatile ContextTypeRegistry fCodeTemplateContextTypeRegistry;

	/**
	 * The template store for the java editor.
	 * @since 3.0
	 */
	private volatile TemplateStore fTemplateStore;
	/**
	 * The coded template store for the java editor.
	 * @since 3.0
	 */
	private volatile TemplateStore fCodeTemplateStore;

	private volatile WorkingCopyManager fWorkingCopyManager;

	private static final String CODE_ASSIST_MIGRATED= "code_assist_migrated"; //$NON-NLS-1$

	private static final String TYPEFILTER_MIGRATED= "typefilter_migrated_2"; //$NON-NLS-1$

	/**
	 * @deprecated to avoid deprecation warning
	 */
	@Deprecated
	private org.eclipse.jdt.core.IBufferFactory fBufferFactory;
	private volatile ICompilationUnitDocumentProvider fCompilationUnitDocumentProvider;
	private volatile ClassFileDocumentProvider fClassFileDocumentProvider;
	private volatile JavaTextTools fJavaTextTools;
	private volatile ProblemMarkerManager fProblemMarkerManager;
	private volatile ImageDescriptorRegistry fImageDescriptorRegistry;

	private volatile MembersOrderPreferenceCache fMembersOrderPreferenceCache;

	private volatile JavaEditorTextHoverDescriptor[] fJavaEditorTextHoverDescriptors;

	/**
	 * The AST provider.
	 * @since 3.0
	 */
	private volatile ASTProvider fASTProvider;

	/**
	 * The combined preference store.
	 * @since 3.0
	 */
	private volatile IPreferenceStore fCombinedPreferenceStore;

	/**
	 * The extension point registry for the <code>org.eclipse.jdt.ui.javaFoldingStructureProvider</code>
	 * extension point.
	 *
	 * @since 3.0
	 */
	private volatile JavaFoldingStructureProviderRegistry fFoldingStructureProviderRegistry;

	/**
	 * The shared Java properties file document provider.
	 * @since 3.1
	 */
	private volatile IDocumentProvider fPropertiesFileDocumentProvider;

	/**
	 * Content assist history.
	 * @since 3.2
	 */
	private volatile ContentAssistHistory fContentAssistHistory;

	/**
	 * The save participant registry.
	 * @since 3.3
	 */
	private volatile SaveParticipantRegistry fSaveParticipantRegistry;

	/**
	 * The clean up registry
	 * @since 3.4
	 */
	private volatile CleanUpRegistry fCleanUpRegistry;

	/**
	 * The descriptors from the 'classpathAttributeConfiguration' extension point.
	 * @since 3.3
	 */
	private volatile ClasspathAttributeConfigurationDescriptors fClasspathAttributeConfigurationDescriptors;

	private volatile FormToolkit fDialogsFormToolkit;

	private volatile ImagesOnFileSystemRegistry fImagesOnFSRegistry;

	/**
	 * Theme listener.
	 * @since 3.3
	 */
	private IPropertyChangeListener fThemeListener;

	private BundleContext fBundleContext;

	private ServiceRegistration<DebugOptionsListener> fDebugRegistration;

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
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	public static Shell getActiveWorkbenchShell() {
		 IWorkbenchWindow window= getActiveWorkbenchWindow();
		 if (window != null) {
		 	return window.getShell();
		 }
		 return null;
	}

	public static String getPluginId() {
		return JavaUI.ID_PLUGIN;
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
		log(new Status(IStatus.ERROR, getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, JavaUIMessages.JavaPlugin_internal_error, e));
	}

	/**
	 * Log a message that is potentially repeated after a very short time.
	 * The first time this method is called with a given message, the
	 * message is written to the log along with the detail message and a stack trace.
	 * <p>
	 * Only intended for use in debug statements.
	 *
	 * @param message the (generic) message
	 * @param detail the detail message
	 */
	public static void logRepeatedMessage(String message, String detail) {
		long now= System.currentTimeMillis();
		boolean writeToLog= true;
		if (fgRepeatedMessages.containsKey(message)) {
			long last= fgRepeatedMessages.get(message);
			writeToLog= now - last > 5000;
		}
		fgRepeatedMessages.put(message, now);
		if (writeToLog)
			log(new Exception(message + detail).fillInStackTrace());
	}

	public static boolean isDebug() {
		return getDefault().isDebugging();
	}

	public static ImageDescriptorRegistry getImageDescriptorRegistry() {
		return getDefault().internalGetImageDescriptorRegistry();
	}

	public JavaPlugin() {
		super();
		fgJavaPlugin = this;
	}

	/* (non - Javadoc)
	 * Method declared in plug-in
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext= context;

		// register debug options listener
		Hashtable<String, String> properties= new Hashtable<>(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, getPluginId());
		fDebugRegistration= context.registerService(DebugOptionsListener.class, this, properties);

		WorkingCopyOwner.setPrimaryBufferProvider(new WorkingCopyOwner() {
			@Override
			public IBuffer createBuffer(ICompilationUnit workingCopy) {
				ICompilationUnit original= workingCopy.getPrimary();
				IResource resource= original.getResource();
				if (resource instanceof IFile)
					return new DocumentAdapter(workingCopy, (IFile) resource);
				return DocumentAdapter.NULL;
			}
		});

		// set the Preferences node id to get preferences from which is needed
		// by the MembersOrderPreferenceCache common logic
		JavaManipulation.setPreferenceNodeId(getPluginId());

		IPreferenceStore store= getPreferenceStore();

		// must add here to guarantee that it is the first in the listener list
		fMembersOrderPreferenceCache= new MembersOrderPreferenceCache();
		fMembersOrderPreferenceCache.install(store);

		// set core methods for MethodWrapper
		MethodWrapper.setMethodWrapperDynamic(new MethodWrapperDynamic());

		FormatterProfileStore.checkCurrentOptionsVersion();

		// make sure org.eclipse.jdt.core.manipulation is loaded too
		// can be removed if JavaElementPropertyTester is moved down to jdt.core (bug 127085)
		JavaManipulation.class.toString();

		if (PlatformUI.isWorkbenchRunning()) {
			// Initialize AST provider
			getASTProvider();

			fThemeListener= event -> {
				if (IThemeManager.CHANGE_CURRENT_THEME.equals(event.getProperty()))
					JavaUIPreferenceInitializer.setThemeBasedPreferences(PreferenceConstants.getPreferenceStore(), true);
			};
			PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fThemeListener);

			createOrUpdateWorkingSet(DynamicSourcesWorkingSetUpdater.MAIN_NAME, DynamicSourcesWorkingSetUpdater.MAIN_OLD_NAME, WorkingSetMessages.JavaMainSourcesWorkingSet_name, IWorkingSetIDs.DYNAMIC_SOURCES);
			createOrUpdateWorkingSet(DynamicSourcesWorkingSetUpdater.TEST_NAME, DynamicSourcesWorkingSetUpdater.TEST_OLD_NAME, WorkingSetMessages.JavaTestSourcesWorkingSet_name, IWorkingSetIDs.DYNAMIC_SOURCES);

			new InitializeAfterLoadJob().schedule(); // last call in start, see bug 191193
		}

		JavaManipulation.setCodeTemplateStore(getCodeTemplateStore());
		JavaManipulation.setCodeTemplateContextRegistry(getCodeTemplateContextRegistry());
		disableNewCodeAssistCategoryPreferences();
		setTypeFilterPreferences();
	}

	private void createOrUpdateWorkingSet(String name, String oldname, String label, final String id) {
		IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSet workingSet= workingSetManager.getWorkingSet(name);
		if (workingSet == null) {
			workingSet= workingSetManager.createWorkingSet(name, new IAdaptable[0]);
			workingSet.setLabel(label);
			workingSet.setId(id);
			workingSetManager.addWorkingSet(workingSet);
		} else {
			if(id.equals(workingSet.getId())) {
				if (!label.equals(workingSet.getLabel()))
					workingSet.setLabel(label);
			} else {
				logErrorMessage("found existing workingset with name=\"" + name + "\" but id=\"" + workingSet.getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
			}
		}
		IWorkingSet oldWorkingSet= workingSetManager.getWorkingSet(oldname);
		if (oldWorkingSet != null && id.equals(oldWorkingSet.getId())) {
			workingSetManager.removeWorkingSet(oldWorkingSet);
		}
	}

	/* package */ static void initializeAfterLoad(IProgressMonitor monitor) {
		OpenTypeHistory.getInstance().checkConsistency(monitor);
	}

	/*
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#createImageRegistry()
	 */
	@Override
	protected ImageRegistry createImageRegistry() {
		return JavaPluginImages.getImageRegistry();
	}

	/*
	 * @see org.eclipse.core.runtime.Plugin#stop
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			// unregister debug options listener
			fDebugRegistration.unregister();
			fDebugRegistration= null;

			if (fImageDescriptorRegistry != null)
				fImageDescriptorRegistry.dispose();

			if (fASTProvider != null) {
				fASTProvider.dispose();
				fASTProvider= null;
			}

			if (fWorkingCopyManager != null) {
				fWorkingCopyManager.shutdown();
				fWorkingCopyManager= null;
			}

			if (fCompilationUnitDocumentProvider != null) {
				fCompilationUnitDocumentProvider.shutdown();
				fCompilationUnitDocumentProvider= null;
			}

			if (fJavaTextTools != null) {
				fJavaTextTools.dispose();
				fJavaTextTools= null;
			}

			if (fContentAssistHistory != null) {
				ContentAssistHistory.store(fContentAssistHistory, getPluginPreferences(), PreferenceConstants.CODEASSIST_LRU_HISTORY);
				fContentAssistHistory= null;
			}

			if (fTemplateStore != null) {
				fTemplateStore.stopListeningForPreferenceChanges();
				fTemplateStore= null;
			}

			if (fCodeTemplateStore != null) {
				fCodeTemplateStore.stopListeningForPreferenceChanges();
				fCodeTemplateStore= null;
			}

			if (fMembersOrderPreferenceCache != null) {
				fMembersOrderPreferenceCache.dispose();
				fMembersOrderPreferenceCache= null;
			}

			if (fSaveParticipantRegistry != null) {
				fSaveParticipantRegistry.dispose();
				fSaveParticipantRegistry= null;
			}

			if (fDialogsFormToolkit != null) {
				fDialogsFormToolkit.dispose();
				fDialogsFormToolkit= null;
			}

			if (fThemeListener != null) {
				PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fThemeListener);
				fThemeListener= null;
			}

			if (fImagesOnFSRegistry != null) {
				fImagesOnFSRegistry.dispose();
				fImagesOnFSRegistry= null;
			}

			SpellCheckEngine.shutdownInstance();

			QualifiedTypeNameHistory.getDefault().save();

			// must add here to guarantee that it is the first in the listener list

			OpenTypeHistory.shutdown();
		} finally {
			super.stop(context);
		}
	}

	private IWorkbenchPage internalGetActivePage() {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
		return window.getActivePage();
	}

	/**
	 * Private deprecated method to avoid deprecation warnings
	 *
	 * @return the deprecated buffer factory
	 * @deprecated to avoid deprecation warnings
	 */
	@Deprecated
	public synchronized org.eclipse.jdt.core.IBufferFactory getBufferFactory() {
		if (fBufferFactory == null)
			fBufferFactory= new org.eclipse.jdt.internal.ui.javaeditor.CustomBufferFactory();
		return fBufferFactory;
	}

	public ICompilationUnitDocumentProvider getCompilationUnitDocumentProvider() {
		ICompilationUnitDocumentProvider result= fCompilationUnitDocumentProvider;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fCompilationUnitDocumentProvider == null) { // Second check (with locking)
				fCompilationUnitDocumentProvider= new CompilationUnitDocumentProvider();
			}
			return fCompilationUnitDocumentProvider;
		}
	}

	/**
	 * Returns the shared document provider for Java properties files
	 * used by this plug-in instance.
	 *
	 * @return the shared document provider for Java properties files
	 * @since 3.1
	 */
	public IDocumentProvider getPropertiesFileDocumentProvider() {
		IDocumentProvider result= fPropertiesFileDocumentProvider;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fPropertiesFileDocumentProvider == null) { // Second check (with locking)
				fPropertiesFileDocumentProvider= new PropertiesFileDocumentProvider();
			}
			return fPropertiesFileDocumentProvider;
		}
	}

	public ClassFileDocumentProvider getClassFileDocumentProvider() {
		ClassFileDocumentProvider result= fClassFileDocumentProvider;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fClassFileDocumentProvider == null) { // Second check (with locking)
				fClassFileDocumentProvider= new ClassFileDocumentProvider();
			}
			return fClassFileDocumentProvider;
		}
	}

	public WorkingCopyManager getWorkingCopyManager() {
		WorkingCopyManager result= fWorkingCopyManager;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fWorkingCopyManager == null) { // Second check (with locking)
				ICompilationUnitDocumentProvider provider= getCompilationUnitDocumentProvider();
				fWorkingCopyManager= new WorkingCopyManager(provider);
			}
			return fWorkingCopyManager;
		}
	}

	public ProblemMarkerManager getProblemMarkerManager() {
		ProblemMarkerManager result= fProblemMarkerManager;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fProblemMarkerManager == null) { // Second check (with locking)
				fProblemMarkerManager= new ProblemMarkerManager();
			}
			return fProblemMarkerManager;
		}
	}

	public JavaTextTools getJavaTextTools() {
		JavaTextTools result= fJavaTextTools;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fJavaTextTools == null) { // Second check (with locking)
				fJavaTextTools= new JavaTextTools(getPreferenceStore(), getJavaCorePluginPreferences());
			}
			return fJavaTextTools;
		}
	}

	/**
	 * Returns the Java Core plug-in preferences.
	 *
	 * @return the Java Core plug-in preferences
	 * @since 3.7
	 */
	public static org.eclipse.core.runtime.Preferences getJavaCorePluginPreferences() {
		return JavaCore.getPlugin().getPluginPreferences();
	}

	/**
	 * Returns the AST provider.
	 *
	 * @return the AST provider
	 * @since 3.0
	 */
	public ASTProvider getASTProvider() {
		ASTProvider result= fASTProvider;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fASTProvider == null) { // Second check (with locking)
				fASTProvider= new ASTProvider();
			}
			return fASTProvider;
		}
	}

	public MembersOrderPreferenceCache getMemberOrderPreferenceCache() {
		// initialized on startup
		return fMembersOrderPreferenceCache;
	}


	public TypeFilter getTypeFilter() {
		return JavaManipulationPlugin.getDefault().getTypeFilter();
	}

	public FormToolkit getDialogsFormToolkit() {
		if (fDialogsFormToolkit == null) {
			// Single threaded init as it only works in UI thread
			FormColors colors= new FormColors(Display.getCurrent());
			colors.setBackground(null);
			colors.setForeground(null);
			fDialogsFormToolkit= new FormToolkit(colors);
		}
		return fDialogsFormToolkit;
	}

	/**
	 * Returns all Java editor text hovers contributed to the workbench.
	 *
	 * @return an array of JavaEditorTextHoverDescriptor
	 * @since 2.1
	 */
	public JavaEditorTextHoverDescriptor[] getJavaEditorTextHoverDescriptors() {
		JavaEditorTextHoverDescriptor[] result= fJavaEditorTextHoverDescriptors;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fJavaEditorTextHoverDescriptors == null) { // Second check (with locking)
				JavaEditorTextHoverDescriptor[] tmp= JavaEditorTextHoverDescriptor.getContributedHovers();
				ConfigurationElementSorter sorter= new ConfigurationElementSorter() {
					/*
					 * @see org.eclipse.ui.texteditor.ConfigurationElementSorter#getConfigurationElement(java.lang.Object)
					 */
					@Override
					public IConfigurationElement getConfigurationElement(Object object) {
						return ((JavaEditorTextHoverDescriptor)object).getConfigurationElement();
					}
				};
				sorter.sort(tmp);

				// Move Best Match hover to front
				for (int i= 0; i < tmp.length - 1; i++) {
					if (PreferenceConstants.ID_BESTMATCH_HOVER.equals(tmp[i].getId())) {
						JavaEditorTextHoverDescriptor hoverDescriptor= tmp[i];
						for (int j= i; j > 0; j--)
							tmp[j]= tmp[j-1];
						tmp[0]= hoverDescriptor;
						break;
					}

				}
				fJavaEditorTextHoverDescriptors= tmp;
			}
			return fJavaEditorTextHoverDescriptors;
		}
	}

	/**
	 * Resets the Java editor text hovers contributed to the workbench.
	 * <p>
	 * This will force a rebuild of the descriptors the next time
	 * a client asks for them.
	 * </p>
	 *
	 * @since 2.1
	 */
	public synchronized void resetJavaEditorTextHoverDescriptors() {
		fJavaEditorTextHoverDescriptors= null;
	}

	/**
	 * Creates the Java plug-in's standard groups for view context menus.
	 *
	 * @param menu the menu manager to be populated
	 */
	public static void createStandardGroups(IMenuManager menu) {
		if (!menu.isEmpty())
			return;

		menu.add(new Separator(IContextMenuConstants.GROUP_NEW));
		menu.add(new GroupMarker(IContextMenuConstants.GROUP_GOTO));
		menu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
		menu.add(new GroupMarker(IContextMenuConstants.GROUP_SHOW));
		menu.add(new Separator(ICommonMenuConstants.GROUP_EDIT));
		menu.add(new Separator(IContextMenuConstants.GROUP_REORGANIZE));
		menu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
		menu.add(new Separator(IContextMenuConstants.GROUP_SEARCH));
		menu.add(new Separator(IContextMenuConstants.GROUP_BUILD));
		menu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
		menu.add(new Separator(IContextMenuConstants.GROUP_VIEWER_SETUP));
		menu.add(new Separator(IContextMenuConstants.GROUP_PROPERTIES));
	}

	/**
	 * Returns the template context type registry for the java plug-in.
	 *
	 * @return the template context type registry for the java plug-in
	 * @since 3.0
	 */
	public ContextTypeRegistry getTemplateContextRegistry() {
		ContextTypeRegistry result= fContextTypeRegistry;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fContextTypeRegistry == null) { // Second check (with locking)
				ContributionContextTypeRegistry registry= new ContributionContextTypeRegistry(JavaUI.ID_CU_EDITOR);

				TemplateContextType all_contextType= registry.getContextType(JavaContextType.ID_ALL);
				((AbstractJavaContextType) all_contextType).initializeContextTypeResolvers();

				registerJavaContext(registry, JavaContextType.ID_MEMBERS, all_contextType);
				registerJavaContext(registry, JavaContextType.ID_STATEMENTS, all_contextType);
				registerJavaContext(registry, JavaContextType.ID_MODULE, all_contextType);
				registerJavaContext(registry, JavaContextType.ID_EMPTY, all_contextType);

				registerJavaContext(registry, SWTContextType.ID_ALL, all_contextType);
				all_contextType= registry.getContextType(SWTContextType.ID_ALL);

				registerJavaContext(registry, SWTContextType.ID_MEMBERS, all_contextType);
				registerJavaContext(registry, SWTContextType.ID_STATEMENTS, all_contextType);

				registerJavaContext(registry, JavaPostfixContextType.ID_ALL, all_contextType);
				all_contextType= registry.getContextType(JavaPostfixContextType.ID_ALL);
				fContextTypeRegistry= registry;
			}
			return fContextTypeRegistry;
		}
	}

	/**
	 * Registers the given Java template context.
	 *
	 * @param registry the template context type registry
	 * @param id the context type id
	 * @param parent the parent context type
	 * @since 3.4
	 */
	private static void registerJavaContext(ContributionContextTypeRegistry registry, String id, TemplateContextType parent) {
		TemplateContextType contextType= registry.getContextType(id);
		Iterator<TemplateVariableResolver> iter= parent.resolvers();
		while (iter.hasNext())
			contextType.addResolver(iter.next());
	}

	/**
	 * Returns the template store for the java editor templates.
	 *
	 * @return the template store for the java editor templates
	 * @since 3.0
	 */
	public TemplateStore getTemplateStore() {
		if (fTemplateStore == null) {
			synchronized (this) {
				if (fTemplateStore != null) {
					return fTemplateStore;
				}

				final IPreferenceStore store= getPreferenceStore();
				ContributionTemplateStore templateStore = new ContributionTemplateStore(getTemplateContextRegistry(), store, TEMPLATES_KEY);
				try {
					templateStore.load();
				} catch (IOException e) {
					log(e);
				}

				fTemplateStore = templateStore;
				templateStore.startListeningForPreferenceChanges();
			}
		}
		return fTemplateStore;
	}

	/**
	 * Returns the template context type registry for the code generation
	 * templates.
	 *
	 * @return the template context type registry for the code generation
	 *         templates
	 * @since 3.0
	 */
	public ContextTypeRegistry getCodeTemplateContextRegistry() {
		if (fCodeTemplateContextTypeRegistry == null) {
			synchronized (this) {
				if (fCodeTemplateContextTypeRegistry != null) {
					return fCodeTemplateContextTypeRegistry;
				}
			}
			// Registry creation loads 3rd party extensions and shouldn't be executed with lock held
			ContributionContextTypeRegistry codeTemplateContextTypeRegistry = new ContributionContextTypeRegistry();
			synchronized (this) {
				if (fCodeTemplateContextTypeRegistry == null) {
					CodeTemplateContextType.registerContextTypes(codeTemplateContextTypeRegistry);
					fCodeTemplateContextTypeRegistry = codeTemplateContextTypeRegistry;
				}
			}
		}
		return fCodeTemplateContextTypeRegistry;
	}

	/**
	 * Returns the template store for the code generation templates.
	 *
	 * @return the template store for the code generation templates
	 * @since 3.0
	 */
	public TemplateStore getCodeTemplateStore() {
		if (fCodeTemplateStore == null) {
			synchronized (this) {
				if (fCodeTemplateStore != null) {
					return fCodeTemplateStore;
				}

				IPreferenceStore store= getPreferenceStore();
				ContributionTemplateStore templateStore = new ContributionTemplateStore(getCodeTemplateContextRegistry(), store, CODE_TEMPLATES_KEY);
				try {
					templateStore.load();
				} catch (IOException e) {
					log(e);
				}

				fCodeTemplateStore = templateStore;
				templateStore.startListeningForPreferenceChanges();
			}
		}

		return fCodeTemplateStore;
	}

	ImageDescriptorRegistry internalGetImageDescriptorRegistry() {
		ImageDescriptorRegistry result= fImageDescriptorRegistry;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fImageDescriptorRegistry == null) { // Second check (with locking)
				fImageDescriptorRegistry= new ImageDescriptorRegistry();
			}
			return fImageDescriptorRegistry;
		}
	}

	/**
	 * Returns a combined preference store, this store is read-only.
	 *
	 * @return the combined preference store
	 *
	 * @since 3.0
	 */
	public IPreferenceStore getCombinedPreferenceStore() {
		if (fCombinedPreferenceStore == null) {
			synchronized (this) {
				if (fCombinedPreferenceStore != null) {
					return fCombinedPreferenceStore;
				}
			}
			
			// Block below may init other bundles and shouldn't be executed with lock held
			IPreferenceStore generalTextStore= EditorsUI.getPreferenceStore();
			ChainedPreferenceStore store = new ChainedPreferenceStore(new IPreferenceStore[] { getPreferenceStore(), new PreferencesAdapter(getJavaCorePluginPreferences()), generalTextStore });
			
			synchronized (this) {
				if (fCombinedPreferenceStore == null) {
					fCombinedPreferenceStore = store;
				}
			}
		}
		return fCombinedPreferenceStore;
	}

	/**
	 * Flushes the instance scope of this plug-in.
	 *
	 * @since 3.7
	 */
	public static void flushInstanceScope() {
		try {
			InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN).flush();
		} catch (BackingStoreException e) {
			log(e);
		}
	}

	/**
	 * Returns the registry of the extensions to the
	 * <code>org.eclipse.jdt.ui.javaFoldingStructureProvider</code> extension point.
	 *
	 * @return the registry of contributed <code>IJavaFoldingStructureProvider</code>
	 * @since 3.0
	 */
	public JavaFoldingStructureProviderRegistry getFoldingStructureProviderRegistry() {
		JavaFoldingStructureProviderRegistry result= fFoldingStructureProviderRegistry;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fFoldingStructureProviderRegistry == null) { // Second check (with locking)
				fFoldingStructureProviderRegistry= new JavaFoldingStructureProviderRegistry();
			}
			return fFoldingStructureProviderRegistry;
		}
	}

	/**
	 * Returns the save participant registry.
	 *
	 * @return the save participant registry, not null
	 * @since 3.3
	 */
	public SaveParticipantRegistry getSaveParticipantRegistry() {
		SaveParticipantRegistry result= fSaveParticipantRegistry;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fSaveParticipantRegistry == null) { // Second check (with locking)
				fSaveParticipantRegistry= new SaveParticipantRegistry();
			}
			return fSaveParticipantRegistry;
		}
	}

	public CleanUpRegistry getCleanUpRegistry() {
		CleanUpRegistry result= fCleanUpRegistry;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fCleanUpRegistry == null) { // Second check (with locking)
				fCleanUpRegistry= new CleanUpRegistry();
			}
			return fCleanUpRegistry;
		}
	}

	/**
	 * Returns the Java content assist history.
	 *
	 * @return the Java content assist history
	 * @since 3.2
	 */
	public ContentAssistHistory getContentAssistHistory() {
		if (fContentAssistHistory == null) {
			synchronized (this) {
				if (fContentAssistHistory != null) {
					return fContentAssistHistory;
				}
				try {
					fContentAssistHistory= ContentAssistHistory.load(getPluginPreferences(), PreferenceConstants.CODEASSIST_LRU_HISTORY);
				} catch (CoreException x) {
					log(x);
				}
				if (fContentAssistHistory == null) {
					fContentAssistHistory= new ContentAssistHistory();
				}
			}
		}

		return fContentAssistHistory;
	}

	/**
	 * Returns a section in the Java plugin's dialog settings. If the section doesn't exist yet, it is created.
	 *
	 * @param name the name of the section
	 * @return the section of the given name
	 * @since 3.2
	 */
	public IDialogSettings getDialogSettingsSection(String name) {
		IDialogSettings dialogSettings= getDialogSettings();
		IDialogSettings section= dialogSettings.getSection(name);
		if (section == null) {
			section= dialogSettings.addNewSection(name);
		}
		return section;
	}

	/**
	 * Returns the descriptors for the class path attribute configuration extension point
	 *
	 * @return access to the descriptors for the class path attribute configuration extension point
	 * @since 3.3
	 */
	public ClasspathAttributeConfigurationDescriptors getClasspathAttributeConfigurationDescriptors() {
		ClasspathAttributeConfigurationDescriptors result= fClasspathAttributeConfigurationDescriptors;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fClasspathAttributeConfigurationDescriptors == null) { // Second check (with locking)
				fClasspathAttributeConfigurationDescriptors= new ClasspathAttributeConfigurationDescriptors();
			}
			return fClasspathAttributeConfigurationDescriptors;
		}
	}

	/**
	 * Returns the image registry that keeps its images on the local file system.
	 *
	 * @return the image registry
	 * @since 3.4
	 */
	public ImagesOnFileSystemRegistry getImagesOnFSRegistry() {
		ImagesOnFileSystemRegistry result= fImagesOnFSRegistry;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fImagesOnFSRegistry == null) { // Second check (with locking)
				fImagesOnFSRegistry= new ImagesOnFileSystemRegistry();
			}
			return fImagesOnFSRegistry;
		}
	}

	/**
	 * Returns the content assist additional info focus affordance string.
	 *
	 * @return the affordance string which is <code>null</code> if the
	 *			preference is disabled
	 *
	 * @see EditorsUI#getTooltipAffordanceString()
	 * @since 3.4
	 */
	public static final String getAdditionalInfoAffordanceString() {
		if (!EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE))
			return null;

		return JavaUIMessages.JavaPlugin_additionalInfo_affordance;
	}

	/**
	 * Returns the bundles for a given bundle name and version range,
	 * regardless whether the bundle is resolved or not.
	 *
	 * @param bundleName the bundle name
	 * @param version the version of the bundle, or <code>null</code> for all bundles
	 * @return the bundles of the given name belonging to the given version range
	 * @since 3.10
	 */
	public Bundle[] getBundles(String bundleName, String version) {
		Bundle[] bundles= Platform.getBundles(bundleName, version);
		if (bundles != null)
			return bundles;

		// Accessing unresolved bundle
		ServiceReference<PackageAdmin> serviceRef= fBundleContext.getServiceReference(PackageAdmin.class);
		PackageAdmin admin= fBundleContext.getService(serviceRef);
		bundles= admin.getBundles(bundleName, version);
		if (bundles != null && bundles.length > 0)
			return bundles;
		return null;
	}

	@Override
	public void optionsChanged(DebugOptions options) {
		DEBUG_AST_PROVIDER= options.getBooleanOption("org.eclipse.jdt.ui/debug/ASTProvider", false); //$NON-NLS-1$
		DEBUG_BREADCRUMB_ITEM_DROP_DOWN= options.getBooleanOption("org.eclipse.jdt.ui/debug/BreadcrumbItemDropDown", false); //$NON-NLS-1$
		DEBUG_RESULT_COLLECTOR= options.getBooleanOption("org.eclipse.jdt.ui/debug/ResultCollector", false); //$NON-NLS-1$
	}

	/**
	 * Add only 'initializeCodeAssistCategoryDisabled(String)' calls here for
	 * proposal category id of *NEW* features meant to be disabled by deafult.
	 *
	 * Eg. initializeCodeAssistCategoryDisabled("org.eclipse.jdt.ui.javaPostfixProposalCategory"); //$NON-NLS-1$
	 *
	 * The call must be added here in addition to setting default disablement in
	 * {@link PreferenceConstants} CODEASSIST_EXCLUDED_CATEGORIES and CODEASSIST_CATEGORY_ORDER.
	 * This will only work correctly for newly added proposal categories for a given release.
	 */
	private static void disableNewCodeAssistCategoryPreferences() {
		// Eg. initializeCodeAssistCategoryDisabled("org.eclipse.jdt.ui.javaPostfixProposalCategory"); //$NON-NLS-1$
	}

	/**
	 * Disable (by default) the given category id for both the default content
	 * assist list (CODEASSIST_EXCLUDED_CATEGORIES) and the cycling content
	 * assist list (CODEASSIST_CATEGORY_ORDER)
	 *
	 * @param id The category id for the proposal feature to disable by default
	 */
	@SuppressWarnings("unused")
	private static void initializeCodeAssistCategoryDisabled(String id) {
		// If preference migrated, nothing to do
		if (isCodeAssistMigrated(id)) {
			return;
		}

		String currPrefExcludedValue= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.CODEASSIST_EXCLUDED_CATEGORIES);
		Set<String> disabled= new HashSet<>();
		StringTokenizer tok= new StringTokenizer(currPrefExcludedValue, "\0");  //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			disabled.add(tok.nextToken());
		}

		// preference not migrated, and not in user preferences
		if (!disabled.isEmpty() && !disabled.contains(id)) {
			String newPrefExcludedValue= currPrefExcludedValue + id + "\0"; //$NON-NLS-1$
			PreferenceConstants.getPreferenceStore().setValue(PreferenceConstants.CODEASSIST_EXCLUDED_CATEGORIES, newPrefExcludedValue);

			// retrieve the id=rank to add from CODEASSIST_CATEGORY_ORDER from the default preferences
			String defPrefOrderValue= PreferenceConstants.getPreferenceStore().getDefaultString(PreferenceConstants.CODEASSIST_CATEGORY_ORDER);
			tok= new StringTokenizer(defPrefOrderValue, "\0"); //$NON-NLS-1$
			while (tok.hasMoreTokens()) {
				StringTokenizer inner= new StringTokenizer(tok.nextToken(), ":"); //$NON-NLS-1$
				String key= inner.nextToken();
				int rank= Integer.parseInt(inner.nextToken());
				if (id.equals(key)) {
					String currPrefOrderValue= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.CODEASSIST_CATEGORY_ORDER);
					String newPreferenceOrderValue= currPrefOrderValue + id + ":" + rank + "\0"; //$NON-NLS-1$ //$NON-NLS-2$
					PreferenceConstants.getPreferenceStore().setValue(PreferenceConstants.CODEASSIST_CATEGORY_ORDER, newPreferenceOrderValue);
				}
			}
		}

		// set as migrated
		setCodeAssistMigrated(id);
	}

	private static boolean isCodeAssistMigrated(String id) {
		String key= CODE_ASSIST_MIGRATED + "_" + id; //$NON-NLS-1$
		boolean res= Platform.getPreferencesService().getBoolean(JavaPlugin.getPluginId(), key, false, null);
		return res;
	}

	private static void setCodeAssistMigrated(String id) {
		String key= CODE_ASSIST_MIGRATED + "_" + id; //$NON-NLS-1$
		IEclipsePreferences preferences= InstanceScope.INSTANCE.getNode(JavaPlugin.getPluginId());
		preferences.putBoolean(key, true);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			JavaPlugin.log(e);
		}
	}

	/**
	 * Add the new default type filters in old workspaces that already have non-default type
	 * filters. Only do this once, so that users have a way to opt-out if they don't want the new
	 * filters.
	 */
	private void setTypeFilterPreferences() {
		Set<String> enabledFiltersToAdd= new LinkedHashSet<>();
		enabledFiltersToAdd.add("com.sun.*"); //$NON-NLS-1$
		enabledFiltersToAdd.add("sun.*"); //$NON-NLS-1$
		enabledFiltersToAdd.add("jdk.*"); //$NON-NLS-1$
		enabledFiltersToAdd.add("io.micrometer.shaded.*"); //$NON-NLS-1$
		enabledFiltersToAdd.add("java.awt.List"); //$NON-NLS-1$

		Set<String> disabledFiltersToAdd= new LinkedHashSet<>();
		disabledFiltersToAdd.add("java.rmi.*"); //$NON-NLS-1$
		disabledFiltersToAdd.add("org.graalvm.*"); //$NON-NLS-1$
		disabledFiltersToAdd.add("java.awt.*"); //$NON-NLS-1$

		// default value - enabled
		Set<String> defaultEnabled= new LinkedHashSet<>();
		String defaultEnabledString= PreferenceConstants.getPreferenceStore().getDefaultString(PreferenceConstants.TYPEFILTER_ENABLED);
		defaultEnabled.addAll(Arrays.asList(defaultEnabledString.split(";"))); //$NON-NLS-1$
		defaultEnabled.addAll(enabledFiltersToAdd);
		String newDefaultEnabledString= defaultEnabled.stream().collect(Collectors.joining(";")); //$NON-NLS-1$
		PreferenceConstants.getPreferenceStore().setDefault(PreferenceConstants.TYPEFILTER_ENABLED, newDefaultEnabledString);

		// default value - disabled
		Set<String> defaultDisabled= new LinkedHashSet<>();
		String defaultDisabledString= PreferenceConstants.getPreferenceStore().getDefaultString(PreferenceConstants.TYPEFILTER_DISABLED);
		defaultDisabled.addAll(Arrays.asList(defaultDisabledString.split(";"))); //$NON-NLS-1$
		defaultDisabled.addAll(disabledFiltersToAdd);
		String newDefaultDisabledString= defaultDisabled.stream().collect(Collectors.joining(";")); //$NON-NLS-1$
		PreferenceConstants.getPreferenceStore().setDefault(PreferenceConstants.TYPEFILTER_DISABLED, newDefaultDisabledString);

		if (isTypeFilterMigrated()) {
			return;
		}

		// current values
		Set<String> currentEnabled= new LinkedHashSet<>();
		String currentEnabledString= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.TYPEFILTER_ENABLED);
		currentEnabled.addAll(Arrays.asList(currentEnabledString.split(";"))); //$NON-NLS-1$
		currentEnabled.remove("org.graalvm.*"); //$NON-NLS-1$
		currentEnabled.remove("java.awt.*"); //$NON-NLS-1$

		Set<String> currentDisabled= new LinkedHashSet<>();
		String currentDisabledString= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.TYPEFILTER_DISABLED);
		currentDisabled.addAll(Arrays.asList(currentDisabledString.split(";"))); //$NON-NLS-1$

		enabledFiltersToAdd.removeAll(currentEnabled);
		enabledFiltersToAdd.removeAll(currentDisabled);

		disabledFiltersToAdd.removeAll(currentEnabled);
		disabledFiltersToAdd.removeAll(currentDisabled);

		if (!enabledFiltersToAdd.isEmpty()) {
			currentEnabledString = currentEnabled.stream().collect(Collectors.joining(";")); //$NON-NLS-1$
			String newEnabledString= currentEnabledString + ";" + enabledFiltersToAdd.stream().collect(Collectors.joining(";")); //$NON-NLS-1$ //$NON-NLS-2$
			PreferenceConstants.getPreferenceStore().setValue(PreferenceConstants.TYPEFILTER_ENABLED, newEnabledString);
		}

		if (!disabledFiltersToAdd.isEmpty()) {
			currentDisabledString = currentDisabled.stream().collect(Collectors.joining(";")); //$NON-NLS-1$
			String newDisabledString= currentDisabledString + ";" + disabledFiltersToAdd.stream().collect(Collectors.joining(";")); //$NON-NLS-1$ //$NON-NLS-2$
			PreferenceConstants.getPreferenceStore().setValue(PreferenceConstants.TYPEFILTER_DISABLED, newDisabledString);
		}

		// set as migrated
		setTypeFilterMigrated();
	}

	private boolean isTypeFilterMigrated() {
		return Platform.getPreferencesService().getBoolean(JavaPlugin.getPluginId(), TYPEFILTER_MIGRATED, false, null);
	}

	private void setTypeFilterMigrated() {
		IEclipsePreferences preferences= InstanceScope.INSTANCE.getNode(JavaPlugin.getPluginId());
		preferences.putBoolean(TYPEFILTER_MIGRATED, true);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			JavaPlugin.log(e);
		}
	}
}
