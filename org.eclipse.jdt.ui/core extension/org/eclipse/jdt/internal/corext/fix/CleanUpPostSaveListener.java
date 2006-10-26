/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.IPostSaveListener;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileManager;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

public class CleanUpPostSaveListener implements IPostSaveListener {
	
	/**
	 * {@inheritDoc}
	 */
	public void saved(ICompilationUnit unit, IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		
		monitor.beginTask(getName(), 1);
		
		try {
    		if (!ActionUtil.isOnBuildPath(unit))
    			return;
    		
    		final IResource resource= unit.getCorrespondingResource();
    		CleanUpRefactoring refactoring= new CleanUpRefactoring() {
    			public ISchedulingRule getSchedulingRule() {
    			    return resource;
    			}
    		};
    		
    		refactoring.addCompilationUnit(unit);
    		
    		refactoring.setLeaveFilesDirty(true);

    		IEclipsePreferences node= new InstanceScope().getNode(JavaUI.ID_PLUGIN);
    		String id= node.get(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, null);
    		if (id == null) {
    			id= new DefaultScope().getNode(JavaUI.ID_PLUGIN).get(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, null);
    			if (id == null) {
    				id= CleanUpProfileManager.DEFAULT_SAVE_PARTICIPANT_PROFILE;
    			}
    		}
    		Hashtable profilesTable= loadProfiles();
    		Profile selectedProfile= (Profile)profilesTable.get(id);
    		if (selectedProfile == null)
    			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, Messages.format(FixMessages.CleanUpPostSaveListener_could_not_find_profile_error, id)));
    		
    		Map settings= selectedProfile.getSettings();
    		
    		ICleanUp[] cleanUps= CleanUpRefactoring.createCleanUps(settings);
    		for (int i= 0; i < cleanUps.length; i++) {
	            refactoring.addCleanUp(cleanUps[i]);
            }
    		
    		Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    		RefactoringExecutionHelper helper= new RefactoringExecutionHelper(refactoring, IStatus.ERROR, false, shell, new BusyIndicatorRunnableContext());
    		try {
    	        helper.perform(false, true);
            } catch (InterruptedException e) {
            } catch (InvocationTargetException e) {
            	if (e.getCause() instanceof CoreException) {
            		throw (CoreException)e.getCause();
            	} else {
            		throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, Messages.format(FixMessages.CleanUpPostSaveListener_exception_error, getName()), e));
            	}
            }
		} finally {
			monitor.done();
		}
    }

	/**
     * {@inheritDoc}
     */
    public String getName() {
	    return FixMessages.CleanUpPostSaveListener_name;
    }
    
	/**
     * {@inheritDoc}
     */
    public String getId() {
	    return "org.eclipse.jdt.ui.postsavelistener.cleanup"; //$NON-NLS-1$
    }
	
	private Hashtable loadProfiles() {
		InstanceScope instanceScope= new InstanceScope();
		
        CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
		ProfileStore profileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);
		
		List list= null;
        try {
            list= profileStore.readProfiles(instanceScope);
        } catch (CoreException e1) {
            JavaPlugin.log(e1);
        }
        if (list == null)
        	list= new ArrayList();
        
		CleanUpProfileManager.addBuiltInProfiles(list, versioner);
		
		Hashtable profileIdsTable= new Hashtable();
		for (Iterator iterator= list.iterator(); iterator.hasNext();) {
            Profile profile= (Profile)iterator.next();
            profileIdsTable.put(profile.getID(), profile);
        }
     
		return profileIdsTable;
    }

}
