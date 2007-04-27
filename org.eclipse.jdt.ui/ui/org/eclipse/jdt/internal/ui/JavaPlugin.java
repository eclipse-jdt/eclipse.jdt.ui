/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;


import java.io.IOException;
import java.util.LinkedHashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ConfigurationElementSorter;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.themes.IThemeManager;

import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;

import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaDocContextType;
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
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings;
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
import org.eclipse.jdt.internal.ui.viewsupport.ProblemMarkerManager;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ClasspathAttributeConfigurationDescriptors;

import org.osgi.framework.BundleContext;

/**
 * Represents the java plug-in. It provides a series of convenience methods such as
 * access to the workbench, keeps track of elements shared by all editors and viewers
 * of the plug-in such as document providers and find-replace-dialogs.
 */
public class JavaPlugin extends AbstractUIPlugin {
	
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
	/**
	 * The key to store whether the legacy templates have been migrated 
	 * @since 3.0
	 */
	private static final String TEMPLATES_MIGRATION_KEY= "org.eclipse.jdt.ui.text.templates_migrated"; //$NON-NLS-1$
	/**
	 * The key to store whether the legacy code templates have been migrated 
	 * @since 3.0
	 */
	private static final String CODE_TEMPLATES_MIGRATION_KEY= "org.eclipse.jdt.ui.text.code_templates_migrated"; //$NON-NLS-1$
	
	private static JavaPlugin fgJavaPlugin;
	
	private static LinkedHashMap fgRepeatedMessages= new LinkedHashMap(20, 0.75f, true) {
		private static final long serialVersionUID= 1L;
		protected boolean removeEldestEntry(java.util.Map.Entry eldest) {
			return size() >= 20;
		}
	};
	
	/** 
	 * The template context type registry for the java editor. 
	 * @since 3.0
	 */
	private ContextTypeRegistry fContextTypeRegistry;
	/** 
	 * The code template context type registry for the java editor. 
	 * @since 3.0
	 */
	private ContextTypeRegistry fCodeTemplateContextTypeRegistry;
	
	/**
	 * The template store for the java editor. 
	 * @since 3.0
	 */
	private TemplateStore fTemplateStore;
	/**
	 * The coded template store for the java editor. 
	 * @since 3.0
	 */
	private TemplateStore fCodeTemplateStore;
	
	/**
	 * Default instance of the appearance type filters.
	 * @since 3.0
	 */
	private TypeFilter fTypeFilter;


	private WorkingCopyManager fWorkingCopyManager;
	
	/**
	 * @deprecated
	 */
	private org.eclipse.jdt.core.IBufferFactory fBufferFactory;
	private ICompilationUnitDocumentProvider fCompilationUnitDocumentProvider;
	private ClassFileDocumentProvider fClassFileDocumentProvider;
	private JavaTextTools fJavaTextTools;
	private ProblemMarkerManager fProblemMarkerManager;
	private ImageDescriptorRegistry fImageDescriptorRegistry;
	
	private MembersOrderPreferenceCache fMembersOrderPreferenceCache;
	private IPropertyChangeListener fFontPropertyChangeListener;
	
	/**
	 * Property change listener on this plugin's preference store.
	 * 
	 * @since 3.0
	 */
	private IPropertyChangeListener fPropertyChangeListener;
	
	private JavaEditorTextHoverDescriptor[] fJavaEditorTextHoverDescriptors;
		
	/**
	 * The AST provider.
	 * @since 3.0
	 */
	private ASTProvider fASTProvider;
	
	/**
	 * The combined preference store.
	 * @since 3.0
	 */
	private IPreferenceStore fCombinedPreferenceStore;
	
	/**
	 * The extension point registry for the <code>org.eclipse.jdt.ui.javaFoldingStructureProvider</code>
	 * extension point.
	 * 
	 * @since 3.0
	 */
	private JavaFoldingStructureProviderRegistry fFoldingStructureProviderRegistry;

	/**
	 * The shared Java properties file document provider.
	 * @since 3.1
	 */
	private IDocumentProvider fPropertiesFileDocumentProvider;

	/**
	 * Content assist history.
	 * 
	 * @since 3.2
	 */
	private ContentAssistHistory fContentAssistHistory;
	
	/**
	 * The save participant registry.
	 * 
	 * @since 3.3
	 */
	private SaveParticipantRegistry fSaveParticipantRegistry;
	
	/**
	 * The descriptors from the 'classpathAttributeConfiguration' extension point.
	 * 
	 * @since 3.3
	 */
	private ClasspathAttributeConfigurationDescriptors fClasspathAttributeConfigurationDescriptors;
	
	private FormToolkit fDialogsFormToolkit;
	
	/**
	 * Theme listener.
	 * @since 3.3
	 */
	private IPropertyChangeListener fThemeListener;

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
		 IWorkbenchWindow window= getActiveWorkbenchWindow();
		 if (window != null) {
		 	return window.getShell();
		 }
		 return null;
	}
	
	/**
	 * @deprecated Use EditorUtility.getDirtyEditors() instead.
	 */
	public static IEditorPart[] getDirtyEditors() {
		return EditorUtility.getDirtyEditors();
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
			long last= ((Long) fgRepeatedMessages.get(message)).longValue();
			writeToLog= now - last > 5000;
		}
		fgRepeatedMessages.put(message, new Long(now));
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
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		WorkingCopyOwner.setPrimaryBufferProvider(new WorkingCopyOwner() {
			public IBuffer createBuffer(ICompilationUnit workingCopy) {
				ICompilationUnit original= workingCopy.getPrimary();
				IResource resource= original.getResource();
				if (resource instanceof IFile)
					return new DocumentAdapter(workingCopy, (IFile) resource);
				return DocumentAdapter.NULL;
			}
		});

		ensurePreferenceStoreBackwardsCompatibility();
		
		// Initialize AST provider
		getASTProvider();
		
		new InitializeAfterLoadJob().schedule();
		
		// make sure is loaded too for org.eclipse.jdt.core.manipulation
		// can be removed if JavaElementPropertyTester is moved down to jdt.core (bug 127085)
		JavaManipulation.class.toString();
		
		fThemeListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (IThemeManager.CHANGE_CURRENT_THEME.equals(event.getProperty())) {
					new JavaUIPreferenceInitializer().initializeDefaultPreferences();
					SemanticHighlightings.initDefaults(getPreferenceStore());
				}
			}
		};
		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fThemeListener);
		
	}

	/* package */ static void initializeAfterLoad(IProgressMonitor monitor) {
		OpenTypeHistory.getInstance().checkConsistency(monitor);
	}
	
	/** @deprecated */
	private static IPreferenceStore getDeprecatedWorkbenchPreferenceStore() {
		return PlatformUI.getWorkbench().getPreferenceStore();
	}
	
	/** @deprecated */
	private static final String DEPRECATED_EDITOR_TAB_WIDTH= PreferenceConstants.EDITOR_TAB_WIDTH;
	
	/** @deprecated */
	private static final String DEPRECATED_REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD= PreferenceConstants.REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD;
	
	/** @deprecated */
	private static final String DEPRECATED_CODEASSIST_ORDER_PROPOSALS= PreferenceConstants.CODEASSIST_ORDER_PROPOSALS;
	
	/**
	 * Installs backwards compatibility for the preference store.
	 */
	private void ensurePreferenceStoreBackwardsCompatibility() {

		IPreferenceStore store= getPreferenceStore();
		
		// must add here to guarantee that it is the first in the listener list
		fMembersOrderPreferenceCache= new MembersOrderPreferenceCache();
		fMembersOrderPreferenceCache.install(store);
		
		
		/*
		 * Installs backwards compatibility: propagate the Java editor font from a
		 * pre-2.1 plug-in to the Platform UI's preference store to preserve
		 * the Java editor font from a pre-2.1 workspace. This is done only
		 * once.
		 */
		String fontPropagatedKey= "fontPropagated"; //$NON-NLS-1$
		if (store.contains(JFaceResources.TEXT_FONT) && !store.isDefault(JFaceResources.TEXT_FONT)) {
			if (!store.getBoolean(fontPropagatedKey))
				PreferenceConverter.setValue(
						getDeprecatedWorkbenchPreferenceStore(), PreferenceConstants.EDITOR_TEXT_FONT, PreferenceConverter.getFontDataArray(store, JFaceResources.TEXT_FONT));
		}
		store.setValue(fontPropagatedKey, true);

		/*
		 * Backwards compatibility: set the Java editor font in this plug-in's
		 * preference store to let older versions access it. Since 2.1 the
		 * Java editor font is managed by the workbench font preference page.
		 */
		PreferenceConverter.putValue(store, JFaceResources.TEXT_FONT, JFaceResources.getFontRegistry().getFontData(PreferenceConstants.EDITOR_TEXT_FONT));

		fFontPropertyChangeListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (PreferenceConstants.EDITOR_TEXT_FONT.equals(event.getProperty()))
					PreferenceConverter.putValue(getPreferenceStore(), JFaceResources.TEXT_FONT, JFaceResources.getFontRegistry().getFontData(PreferenceConstants.EDITOR_TEXT_FONT));
			}
		};
		JFaceResources.getFontRegistry().addListener(fFontPropertyChangeListener);
		
		/*
		 * Backwards compatibility: propagate the Java editor tab width from a
		 * pre-3.0 plug-in to the new preference key. This is done only once.
		 */
		final String oldTabWidthKey= DEPRECATED_EDITOR_TAB_WIDTH;
		final String newTabWidthKey= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH;
		String tabWidthPropagatedKey= "tabWidthPropagated"; //$NON-NLS-1$
		if (store.contains(oldTabWidthKey) && !store.isDefault(oldTabWidthKey)) {
			if (!store.getBoolean(tabWidthPropagatedKey))
				store.setValue(newTabWidthKey, store.getInt(oldTabWidthKey));
		}
		store.setValue(tabWidthPropagatedKey, true);

		/*
		 * Backwards compatibility: set the Java editor tab width in this plug-in's
		 * preference store with the old key to let older versions access it.
		 * Since 3.0 the tab width is managed by the extended text editor and
		 * uses a new key.
		 */
		store.putValue(oldTabWidthKey, store.getString(newTabWidthKey));

		fPropertyChangeListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (newTabWidthKey.equals(event.getProperty())) {
					IPreferenceStore prefStore= getPreferenceStore();
					prefStore.putValue(oldTabWidthKey, prefStore.getString(newTabWidthKey));
				}
			}
		};
		store.addPropertyChangeListener(fPropertyChangeListener);
		
		/*
		 * Backward compatibility for the refactoring preference key. 
		 */
//		store.setValue(
//			PreferenceConstants.REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD, 
//			RefactoringCore.getConditionCheckingFailedSeverity());
		
		// The commented call above triggers the eager loading of the LTK core plug-in
		// Since the condition checking failed severity is guaranteed to be of RefactoringStatus.SEVERITY_WARNING,
		// we directly insert the inlined value of this constant
		store.setToDefault(DEPRECATED_REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD);
		
		if (!store.getBoolean(JavaDocLocations.PREF_JAVADOCLOCATIONS_MIGRATED)) {
			JavaDocLocations.migrateToClasspathAttributes();
		}
		
		FormatterProfileStore.checkCurrentOptionsVersion();
		
		/*
		 * Backward compatibility: migrate "alphabetic ordering" preference to point the sorter
		 * preference to the alphabetic sorter.
		 */
		String proposalOrderMigrated= "proposalOrderMigrated"; //$NON-NLS-1$

		if (store.contains(DEPRECATED_CODEASSIST_ORDER_PROPOSALS)) {
			if (!store.getBoolean(proposalOrderMigrated)) {
				boolean alphabetic= store.getBoolean(DEPRECATED_CODEASSIST_ORDER_PROPOSALS);
				if (alphabetic)
					store.setValue(PreferenceConstants.CODEASSIST_SORTER, "org.eclipse.jdt.ui.AlphabeticSorter"); //$NON-NLS-1$
			}
		}
		store.setValue(proposalOrderMigrated, true);

	}
	
	/**
	 * Uninstalls backwards compatibility for the preference store.
	 */
	private void uninstallPreferenceStoreBackwardsCompatibility() {
		JFaceResources.getFontRegistry().removeListener(fFontPropertyChangeListener);
		getPreferenceStore().removePropertyChangeListener(fPropertyChangeListener);
	}
	
	/*
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#createImageRegistry()
	 */
	protected ImageRegistry createImageRegistry() {
		return JavaPluginImages.getImageRegistry();
	}

	/*
	 * @see org.eclipse.core.runtime.Plugin#stop
	 */
	public void stop(BundleContext context) throws Exception {
		try {
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
			
			if (fTypeFilter != null) {
				fTypeFilter.dispose();
				fTypeFilter= null;
			}
			
			if (fContentAssistHistory != null) {
				ContentAssistHistory.store(fContentAssistHistory, getPluginPreferences(), PreferenceConstants.CODEASSIST_LRU_HISTORY);
				fContentAssistHistory= null;
			}
			
			uninstallPreferenceStoreBackwardsCompatibility();
			
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
			
			SpellCheckEngine.shutdownInstance();
			
			QualifiedTypeNameHistory.getDefault().save();
			
			// must add here to guarantee that it is the first in the listener list

			OpenTypeHistory.shutdown();
		} finally {	
			super.stop(context);
		}
	}
		
	private IWorkbenchPage internalGetActivePage() {
		IWorkbenchWindow window= getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
		return window.getActivePage();
	}
	
	/**
	 * @deprecated
	 */
	public synchronized org.eclipse.jdt.core.IBufferFactory getBufferFactory() {
		if (fBufferFactory == null)
			fBufferFactory= new org.eclipse.jdt.internal.ui.javaeditor.CustomBufferFactory();
		return fBufferFactory;
	}
	
	public synchronized ICompilationUnitDocumentProvider getCompilationUnitDocumentProvider() {
		if (fCompilationUnitDocumentProvider == null)
			fCompilationUnitDocumentProvider= new CompilationUnitDocumentProvider();
		return fCompilationUnitDocumentProvider;
	}
	
	/**
	 * Returns the shared document provider for Java properties files
	 * used by this plug-in instance.  
	 * 
	 * @return the shared document provider for Java properties files
	 * @since 3.1
	 */
	public synchronized IDocumentProvider getPropertiesFileDocumentProvider() {
		if (fPropertiesFileDocumentProvider == null)
			fPropertiesFileDocumentProvider= new PropertiesFileDocumentProvider();
		return fPropertiesFileDocumentProvider;
	}
	
	public synchronized ClassFileDocumentProvider getClassFileDocumentProvider() {
		if (fClassFileDocumentProvider == null)
			fClassFileDocumentProvider= new ClassFileDocumentProvider();
		return fClassFileDocumentProvider;
	}

	public synchronized WorkingCopyManager getWorkingCopyManager() {
		if (fWorkingCopyManager == null) {
			ICompilationUnitDocumentProvider provider= getCompilationUnitDocumentProvider();
			fWorkingCopyManager= new WorkingCopyManager(provider);
		}
		return fWorkingCopyManager;
	}
		
	public synchronized ProblemMarkerManager getProblemMarkerManager() {
		if (fProblemMarkerManager == null)
			fProblemMarkerManager= new ProblemMarkerManager();
		return fProblemMarkerManager;
	}	
	
	public synchronized JavaTextTools getJavaTextTools() {
		if (fJavaTextTools == null)
			fJavaTextTools= new JavaTextTools(getPreferenceStore(), JavaCore.getPlugin().getPluginPreferences());
		return fJavaTextTools;
	}
	
	/**
	 * Returns the AST provider.
	 * 
	 * @return the AST provider
	 * @since 3.0
	 */
	public synchronized ASTProvider getASTProvider() {
		if (fASTProvider == null)
			fASTProvider= new ASTProvider();
		
		return fASTProvider;
	}
		
	public synchronized MembersOrderPreferenceCache getMemberOrderPreferenceCache() {
		// initialized on startup
		return fMembersOrderPreferenceCache;
	}
	
	
	public synchronized TypeFilter getTypeFilter() {
		if (fTypeFilter == null)
			fTypeFilter= new TypeFilter();
		return fTypeFilter;
	}	
	
	public FormToolkit getDialogsFormToolkit() {
		if (fDialogsFormToolkit == null) {
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
	public synchronized JavaEditorTextHoverDescriptor[] getJavaEditorTextHoverDescriptors() {
		if (fJavaEditorTextHoverDescriptors == null) {
			fJavaEditorTextHoverDescriptors= JavaEditorTextHoverDescriptor.getContributedHovers();
			ConfigurationElementSorter sorter= new ConfigurationElementSorter() {
				/*
				 * @see org.eclipse.ui.texteditor.ConfigurationElementSorter#getConfigurationElement(java.lang.Object)
				 */
				public IConfigurationElement getConfigurationElement(Object object) {
					return ((JavaEditorTextHoverDescriptor)object).getConfigurationElement();
				}
			};
			sorter.sort(fJavaEditorTextHoverDescriptors);
		
			// Move Best Match hover to front
			for (int i= 0; i < fJavaEditorTextHoverDescriptors.length - 1; i++) {
				if (PreferenceConstants.ID_BESTMATCH_HOVER.equals(fJavaEditorTextHoverDescriptors[i].getId())) {
					JavaEditorTextHoverDescriptor hoverDescriptor= fJavaEditorTextHoverDescriptors[i];
					for (int j= i; j > 0; j--)
						fJavaEditorTextHoverDescriptors[j]= fJavaEditorTextHoverDescriptors[j-1];
					fJavaEditorTextHoverDescriptors[0]= hoverDescriptor;
					break;
				}
				
			}
		}
		
		return fJavaEditorTextHoverDescriptors;
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
		if (fContextTypeRegistry == null) {
			ContributionContextTypeRegistry registry= new ContributionContextTypeRegistry();
			registry.addContextType(JavaContextType.NAME);
			registry.addContextType(JavaDocContextType.NAME);

			fContextTypeRegistry= registry;
		}

		return fContextTypeRegistry;
	}
	
	/**
	 * Returns the template store for the java editor templates.
	 * 
	 * @return the template store for the java editor templates
	 * @since 3.0
	 */
	public TemplateStore getTemplateStore() {
		if (fTemplateStore == null) {
			final IPreferenceStore store= getPreferenceStore();
			boolean alreadyMigrated= store.getBoolean(TEMPLATES_MIGRATION_KEY);
			if (alreadyMigrated)
				fTemplateStore= new ContributionTemplateStore(getTemplateContextRegistry(), store, TEMPLATES_KEY);
			else {
				fTemplateStore= new CompatibilityTemplateStore(getTemplateContextRegistry(), store, TEMPLATES_KEY, getOldTemplateStoreInstance());
				store.setValue(TEMPLATES_MIGRATION_KEY, true);
			}

			try {
				fTemplateStore.load();
			} catch (IOException e) {
				log(e);
			}
			fTemplateStore.startListeningForPreferenceChanges();
		}
		
		return fTemplateStore;
	}
	
	/**
	 * @deprecated Indirection added to avoid deprecated warning on file
	 */
	private org.eclipse.jdt.internal.corext.template.java.Templates getOldTemplateStoreInstance() {
		return org.eclipse.jdt.internal.corext.template.java.Templates.getInstance();
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
			fCodeTemplateContextTypeRegistry= new ContributionContextTypeRegistry();
			
			CodeTemplateContextType.registerContextTypes(fCodeTemplateContextTypeRegistry);
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
			IPreferenceStore store= getPreferenceStore();
			boolean alreadyMigrated= store.getBoolean(CODE_TEMPLATES_MIGRATION_KEY);
			if (alreadyMigrated)
				fCodeTemplateStore= new ContributionTemplateStore(getCodeTemplateContextRegistry(), store, CODE_TEMPLATES_KEY);
			else {
				fCodeTemplateStore= new CompatibilityTemplateStore(getCodeTemplateContextRegistry(), store, CODE_TEMPLATES_KEY, getOldCodeTemplateStoreInstance());
				store.setValue(CODE_TEMPLATES_MIGRATION_KEY, true);
			}

			try {
				fCodeTemplateStore.load();
			} catch (IOException e) {
				log(e);
			}
			
			fCodeTemplateStore.startListeningForPreferenceChanges();
			
			// compatibility / bug fixing code for duplicated templates
			// TODO remove for 3.0
			CompatibilityTemplateStore.pruneDuplicates(fCodeTemplateStore, true);
			
		}
		
		return fCodeTemplateStore;
	}
	
	/**
	 * @deprecated Indirection added to avoid deprecated warning on file
	 */
	private org.eclipse.jdt.internal.corext.template.java.CodeTemplates getOldCodeTemplateStoreInstance() {
		return org.eclipse.jdt.internal.corext.template.java.CodeTemplates.getInstance();
	}
	
	private synchronized ImageDescriptorRegistry internalGetImageDescriptorRegistry() {
		if (fImageDescriptorRegistry == null)
			fImageDescriptorRegistry= new ImageDescriptorRegistry();
		return fImageDescriptorRegistry;
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
			IPreferenceStore generalTextStore= EditorsUI.getPreferenceStore(); 
			fCombinedPreferenceStore= new ChainedPreferenceStore(new IPreferenceStore[] { getPreferenceStore(), new PreferencesAdapter(JavaCore.getPlugin().getPluginPreferences()), generalTextStore });
		}
		return fCombinedPreferenceStore;
	}
	
	/**
	 * Returns the registry of the extensions to the <code>org.eclipse.jdt.ui.javaFoldingStructureProvider</code>
	 * extension point.
	 * 
	 * @return the registry of contributed <code>IJavaFoldingStructureProvider</code>
	 * @since 3.0
	 */
	public synchronized JavaFoldingStructureProviderRegistry getFoldingStructureProviderRegistry() {
		if (fFoldingStructureProviderRegistry == null)
			fFoldingStructureProviderRegistry= new JavaFoldingStructureProviderRegistry();
		return fFoldingStructureProviderRegistry;
	}
	
	/**
	 * Returns the save participant registry.
	 * 
	 * @return the save participant registry, not null
	 * @since 3.3
	 */
	public synchronized SaveParticipantRegistry getSaveParticipantRegistry() {
		if (fSaveParticipantRegistry == null)
			fSaveParticipantRegistry= new SaveParticipantRegistry();
		return fSaveParticipantRegistry;
	}

	/**
	 * Returns the Java content assist history.
	 * 
	 * @return the Java content assist history
	 * @since 3.2
	 */
	public ContentAssistHistory getContentAssistHistory() {
		if (fContentAssistHistory == null) {
			try {
				fContentAssistHistory= ContentAssistHistory.load(getPluginPreferences(), PreferenceConstants.CODEASSIST_LRU_HISTORY);
			} catch (CoreException x) {
				log(x);
			}
			if (fContentAssistHistory == null)
				fContentAssistHistory= new ContentAssistHistory();
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
	 * 	@since 3.3
	 */
	public ClasspathAttributeConfigurationDescriptors getClasspathAttributeConfigurationDescriptors() {
		if (fClasspathAttributeConfigurationDescriptors == null) {
			fClasspathAttributeConfigurationDescriptors= new ClasspathAttributeConfigurationDescriptors();
		}
		return fClasspathAttributeConfigurationDescriptors;
	}
	
}
