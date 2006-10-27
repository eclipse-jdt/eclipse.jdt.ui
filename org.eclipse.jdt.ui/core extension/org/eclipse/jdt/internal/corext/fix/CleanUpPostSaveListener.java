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
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.IPostSaveListener;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileManager;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;

public class CleanUpPostSaveListener implements IPostSaveListener {
	
	/**
	 * {@inheritDoc}
	 */
	public void saved(ICompilationUnit unit, IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
		
		try {
    		if (!ActionUtil.isOnBuildPath(unit))
    			return;

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
    		
    		do {
    			for (int i= 0; i < cleanUps.length; i++) {
	                cleanUps[i].checkPreConditions(unit.getJavaProject(), new ICompilationUnit[] {unit}, new SubProgressMonitor(monitor, 5));
                }
    			
    			Map options= RefactoringASTParser.getCompilerOptions(unit.getJavaProject());
    			for (int i= 0; i < cleanUps.length; i++) {
	                Map map= cleanUps[i].getRequiredOptions();
	                if (map != null) {
	                	options.putAll(map);
	                }
                }
    			
        		CompilationUnit ast= createAst(unit, options, new SubProgressMonitor(monitor, 10));
        		
        		List undoneCleanUps= new ArrayList();
    			CompilationUnitChange change= CleanUpRefactoring.calculateChange(ast, unit, cleanUps, undoneCleanUps);
    			if (change != null) {
        			change.setSaveMode(TextFileChange.LEAVE_DIRTY);
        			change.initializeValidationData(new NullProgressMonitor());
        			
        			PerformChangeOperation performChangeOperation= RefactoringUI.createUIAwareChangeOperation(change);
    				performChangeOperation.setUndoManager(RefactoringCore.getUndoManager(), getName());
    				performChangeOperation.setSchedulingRule(unit.getSchedulingRule());
    				
    				performChangeOperation.run(new SubProgressMonitor(monitor, 5));
    			}
    			
    			for (int i= 0; i < cleanUps.length; i++) {
	                cleanUps[i].checkPostConditions(new SubProgressMonitor(monitor, 1));
                }
    			
    			cleanUps= (ICleanUp[])undoneCleanUps.toArray(new ICleanUp[undoneCleanUps.size()]);
    		} while (cleanUps.length > 0);
		} finally {
			monitor.done();
		}
    }

	private CompilationUnit createAst(ICompilationUnit unit, Map options, IProgressMonitor monitor) {
        ASTParser parser= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
        parser.setResolveBindings(true);
        parser.setProject(unit.getJavaProject());
        parser.setSource(unit);
        parser.setCompilerOptions(options);
        
        return (CompilationUnit)parser.createAST(monitor);
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
