package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Searches and launches JUnit tests.
 */
public class LaunchTestAction implements IWorkbenchWindowActionDelegate, IExecutableExtension {
	
	private IWorkbenchWindow fWindow= null;
	private IEditorPart fEditor= null;
	private IStructuredSelection fSelection= null;
	private String fMode= ILaunchManager.RUN_MODE;
	
	/**
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		fWindow= window;
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		IType[] types= null;
		Object[] search= null;
		if (fSelection == null) {
			if (fEditor != null) {
				IEditorInput input= fEditor.getEditorInput();
				IJavaElement je= (IJavaElement)input.getAdapter(IJavaElement.class);
				if (je != null) {
					search= new Object[] {je};
				}
			}			
		} else {
			search= fSelection.toArray();
		}
		
		if (search != null) {
			try {
				types= TestSearchEngine.findTests(new ProgressMonitorDialog(fWindow.getShell()), search);
			} catch (InterruptedException e) {
				JUnitPlugin.log(e);
				return;
			} catch (InvocationTargetException e) {
				JUnitPlugin.log(e);
				return;
			}
			IType type= null;
			if (types.length == 0) {
				MessageDialog.openInformation(fWindow.getShell(), JUnitMessages.getString("LaunchTestAction.dialog.title"), JUnitMessages.getString("LaunchTestAction.message.notests")); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (types.length > 1) {
				type= chooseType(types);
			} else {
				type= types[0];
			}
			if (type != null) {
				launch(type);
			}
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fEditor= null;
		fSelection= null;
		boolean enabled= false;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection)selection;
			Iterator iter= ss.iterator();
			while (iter.hasNext()) {
				Object obj= iter.next();
				if (obj instanceof IAdaptable) {
					IJavaElement je= (IJavaElement)((IAdaptable)obj).getAdapter(IJavaElement.class);
					if (je != null) {
						enabled= true;
						fSelection= ss;
					}
				}
			}
		} else {
			IWorkbenchPage page= fWindow.getActivePage();
			if (page != null) {
				IEditorPart editor= page.getActiveEditor();
				if (editor != null) {
					String id= editor.getSite().getId();
					if (JavaUI.ID_CU_EDITOR.equals(id) || JavaUI.ID_CF_EDITOR.equals(id)) {
						enabled= true;
						fEditor= editor;
					}
				}
			}
		}
		action.setEnabled(enabled);
	}

	/**
	 * Prompts the user to select a type
	 * 
	 * @return the selected type or <code>null</code> if none.
	 */
	protected IType chooseType(IType[] types) {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(fWindow.getShell(), new JavaElementLabelProvider());
		dialog.setElements(types);
		dialog.setTitle(JUnitMessages.getString("LaunchTestAction.dialog.title2")); //$NON-NLS-1$
		if (getMode().equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(JUnitMessages.getString("LaunchTestAction.message.selectTestToRun"));  //$NON-NLS-1$
		} else {
			dialog.setMessage(JUnitMessages.getString("LaunchTestAction.message.selectTestToDebug")); //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		if (dialog.open() == dialog.OK) {
			return (IType)dialog.getFirstResult();
		}
		return null;
	}
	
	/**
	 * Launches a configuration for the given type
	 */
	protected void launch(IType type) {
		try { 
			ILaunchConfiguration config = findLaunchConfiguration(type);
			if (config != null) {
				config.launch(getMode(), null);
			}			
		} catch (CoreException e) {
			ErrorDialog.openError(fWindow.getShell(), JUnitMessages.getString("LaunchTestAction.message.launchFailed"), e.getMessage(), e.getStatus());  //$NON-NLS-1$
		}
	}
	
	/**
	 * Locate a configuration to relaunch for the given type.  If one cannot be found, create one.
	 * 
	 * @return a re-useable config or <code>null</code> if none
	 */
	protected ILaunchConfiguration findLaunchConfiguration(IType type) {
		ILaunchConfigurationType configType= getJavaLaunchConfigType();
		List candidateConfigs= Collections.EMPTY_LIST;
		try {
			ILaunchConfiguration[] configs= DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
			candidateConfigs= new ArrayList(configs.length);
			for (int i= 0; i < configs.length; i++) {
				ILaunchConfiguration config= configs[i];
				if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "").equals(type.getFullyQualifiedName())) { //$NON-NLS-1$
					if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(type.getJavaProject().getElementName())) { //$NON-NLS-1$
						candidateConfigs.add(config);
					}
				}
			}
		} catch (CoreException e) {
			JUnitPlugin.log(e);
		}
		
		// If there are no existing configs associated with the IType, create one.
		// If there is exactly one config associated with the IType, return it.
		// Otherwise, if there is more than one config associated with the IType, prompt the
		// user to choose one.
		int candidateCount= candidateConfigs.size();
		if (candidateCount < 1) {
			return createConfiguration(type);
		} else if (candidateCount == 1) {
			return (ILaunchConfiguration) candidateConfigs.get(0);
		} else {
			// Prompt the user to choose a config.  A null result means the user
			// cancelled the dialog, in which case this method returns null,
			// since cancelling the dialog should also cancel launching anything.
			ILaunchConfiguration config= chooseConfiguration(candidateConfigs);
			if (config != null) {
				return config;
			}
		}
		return null;
	}
	
	/**
	 * Show a selection dialog that allows the user to choose one of the specified
	 * launch configurations.  Return the chosen config, or <code>null</code> if the
	 * user cancelled the dialog.
	 */
	protected ILaunchConfiguration chooseConfiguration(List configList) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(fWindow.getShell(), labelProvider);
		dialog.setElements(configList.toArray());
		dialog.setTitle(JUnitMessages.getString("LaunchTestAction.message.selectConfiguration")); //$NON-NLS-1$
		if (getMode().equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(JUnitMessages.getString("LaunchTestAction.message.selectDebugConfiguration")); //$NON-NLS-1$
		} else {
			dialog.setMessage(JUnitMessages.getString("LaunchTestAction.message.selectRunConfiguration")); //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		int result= dialog.open();
		labelProvider.dispose();
		if (result == dialog.OK) {
			return (ILaunchConfiguration)dialog.getFirstResult();
		}
		return null;		
	}
	
	/**
	 * Create & return a new configuration based on the specified <code>IType</code>.
	 */
	protected ILaunchConfiguration createConfiguration(IType type) {
		ILaunchConfiguration config = null;
		try {
			ILaunchConfigurationType configType= getJavaLaunchConfigType();
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, type.getElementName() + " [" + type.getJavaProject().getElementName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, type.getFullyQualifiedName());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, type.getJavaProject().getElementName());
			wc.setAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
			wc.setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, (String)null);
			wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
			config= wc.doSave();		
		} catch (CoreException ce) {
			JUnitPlugin.log(ce);
		}
		return config;
	}
	
	/**
	 * Returns the local java launch config type
	 */
	protected ILaunchConfigurationType getJavaLaunchConfigType() {
		ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
		return lm.getLaunchConfigurationType(JUnitLaunchConfiguration.ID_JUNIT_APPLICATION);		
	}
	
	/**
	 * Returns the mode this action launches in
	 */
	protected String getMode() {
		return fMode;
	}
	
	/*
	 * @see IExecutableExtension#setInitializationData(IConfigurationElement, String, Object)
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		fMode= config.getAttribute("mode"); //$NON-NLS-1$
	}

}