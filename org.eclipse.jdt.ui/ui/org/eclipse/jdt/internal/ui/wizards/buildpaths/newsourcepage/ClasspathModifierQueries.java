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
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.ExtendedNewFolderDialog;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ExclusionInclusionDialog;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.OutputLocationDialog;

/**
 */
public class ClasspathModifierQueries {

    /**
     * Query to get information about whether the project should be removed as
     * source folder and update build folder to <code>outputLocation</code>
     */
    public static abstract class IOutputFolderQuery {
        protected IPath fDesiredOutputLocation;
        
        /**
         * Constructor gets the desired output location
         * of the project
         * 
         * @param outputLocation desired output location for the
         * project. It is possible that the desired output location 
         * equals the current project's output location (for example if 
         * it is not intended to change the output locatio at this time).
         */
        public IOutputFolderQuery(IPath outputLocation) {
            if (outputLocation != null)
                fDesiredOutputLocation= outputLocation.makeAbsolute();
        }
        
        /**
         * Getter for the desired output location.
         * 
         * @return the project's desired output location
         */
        public IPath getDesiredOutputLocation() {
            return fDesiredOutputLocation;
        }
        
        /**
         * Get the output location that was determined by the 
         * query for the project. Note that this output location 
         * does not have to be the same as the desired output location 
         * that is passed to the constructor.
         * 
         * This method is only intended to be called if <code>doQuery</code> 
         * has been executed successfully and had return <code>true</code> to 
         * indicate that changes were accepted.
         *
         *@return the effective output location
         */
        public abstract IPath getOutputLocation();
        
        /**
         * Find out wheter the project should be removed from the classpath 
         * or not.
         * 
         * This method is only intended to be called if <code>doQuery</code> 
         * has been executed successfully and had return <code>true</code> to 
         * indicate that changes were accepted.
         * 
         * @return <code>true</code> if the project should be removed from 
         * the classpath, <code>false</code> otherwise.
         */
        public abstract boolean removeProjectFromClasspath();
        
        /**
         * Query to get information about whether the project should be removed as
         * source folder and update build folder to <code>outputLocation</code>.
         * 
         * There are several situations for setting up a project where it is not possible 
         * to have the project folder itself as output folder. Therefore, the query asks in the 
         * first place for changing the output folder. Additionally, it also can be usefull to 
         * remove the project from the classpath. This information can be retrieved by calling 
         * <code>removeProjectFromClasspath()</code>.
         * 
         * Note: if <code>doQuery</code> returns false, the started computation will stop immediately.
         * There is no additional dialog that informs the user about this abort. Therefore it is important 
         * that the query informs the users about the consequences of not allowing to change the output  
         * folder.
         * 
         * @param editingOutputFolder <code>true</code> if currently an output folder is changed, 
         * <code>false</code> otherwise. This information can be usefull to generate an appropriate 
         * message to ask the user for an action.
         * @param project the java project
         * @return <code>true</code> if the execution was successfull (e.g. not aborted) and 
         * the caller should execute additional steps as setting the output location for the project or (optionally) 
         * removing the project from the classpath, <code>false</code> otherwise.
         * @throws JavaModelException if the output location of the project could not be retrieved
         */
        public abstract boolean doQuery(final boolean editingOutputFolder, final IJavaProject project) throws JavaModelException;
        
    }

    public static interface IInclusionExclusionQuery {
        /**
         * Query to get information about the
         * inclusion and exclusion filters of
         * an element.
         * 
         * While executing <code>doQuery</code>,
         * these filter might change.
         * 
         * On calling <code>getInclusionPatter</code>
         * or <code>getExclusionPattern</code> it
         * is expected to get the new and updated
         * filters back.
         * 
         * @param element the element to get the
         * information from
         * @param focusOnExcluded
         * @return <code>true</code> if changes
         * have been accepted and <code>getInclusionPatter</code>
         * or <code>getExclusionPattern</code> can
         * be called.
         */
        boolean doQuery(CPListElement element, boolean focusOnExcluded);
        
        /**
         * Can only be called after <code>
         * doQuery</code> has been executed and
         * has returned <code>true</code>
         * 
         * @return the inclusion filters
         */
        public IPath[] getInclusionPattern();
        
        /**
         * Can only be called after <code>
         * doQuery</code> has been executed and
         * has returned <code>true</code>
         *
         * @return the exclusion filters
         */
        public IPath[] getExclusionPattern();
    }

    /**
     * Query to get information about the output
     * location that should be used for a 
     * given element.
     */
    public static interface IOutputLocationQuery {
        /**
         * Query to get information about the output
         * location that should be used for a 
         * given element.
         * 
         * @param element the element to get
         * an output location for
         * @return <code>true</code> if the output
         * location has changed, <code>false</code>
         * otherwise.
         */
        boolean doQuery(CPListElement element);
        
        /**
         * Gets the new output location.
         * 
         * May only be called after having
         * executed <code>doQuery</code> which
         * must have returned <code>true</code>
         * 
         * @return the new output location, can be <code>null</code>
         */
        public IPath getOutputLocation();
        
        /**
         * Get a query for information about whether the project should be removed as
         * source folder and update build folder
         * 
         * @param outputLocation output location for the
         * project
         * @return query giving information about output and source folders
         * @throws JavaModelException
         * 
         * @see IOutputFolderQuery
         */
        public IOutputFolderQuery getOutputFolderQuery(IPath outputLocation) throws JavaModelException;
    }

    /**
     * Query to create a folder.
     */
    public static interface IFolderCreationQuery {
        /**
         * Query to create a folder.
         * 
         * Clients need to implement their own
         * initialization methods so that
         * <code>doQuery</code> can be executed
         * safely on behalf of this
         * initialization
         * 
         * @return <code>true</code> if the operation
         * was successful (e.g. no cancelled), <code>
         * false</code> otherwise
         */
        boolean doQuery();
        
        /**
         * Find out whether a source folder is about
         * to be created or a normal folder which
         * is not on the classpath (and therefore
         * might have to be excluded).
         * 
         * Should only be called after having executed
         * <code>doQuery</code>, because otherwise
         * it might not be sure if a result exists or
         * not.
         * 
         * @return <code>true</code> if a source
         * folder should be created, <code>false
         * </code> otherwise
         */
        boolean isSourceFolder();
        
        /**
         * Get the newly created folder.
         * This method is only valid after having
         * called <code>doQuery</code>.
         * 
         * @return the created folder of type
         * <code>IFolder</code>
         */
        IFolder getCreatedFolder();
    }

    /**
     * A default folder query that can be used for this class.
     * The query is used to get information about whether the project should be removed as
     * source folder and update build folder to <code>outputLocation</code>
     * 
     * @param shell shell if there is any or <code>null</code>
     * @param outputLocation the desired project's output location
     * @return an <code>IOutputFolderQuery</code> that can be executed
     * 
     * @see ClasspathModifierQueries.IOutputFolderQuery
     * @see org.eclipse.jdt.internal.corext.buildpath.AddToClasspathOperation
     */
    public static IOutputFolderQuery getDefaultFolderQuery(final Shell shell, IPath outputLocation) {
        return new IOutputFolderQuery(outputLocation) {
            private final IPath[] fOutputLocation= new IPath[1];
            private boolean removeProject;
            public boolean doQuery(final boolean editingOutputFolder, final IJavaProject project) throws JavaModelException {
                final boolean[] result= new boolean[1];
                removeProject= false;
                fOutputLocation[0]= project.getOutputLocation();
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {                        
                        Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
                        
                        IPath newOutputFolder= null;
                        
                        String message;
                        if (fDesiredOutputLocation.segmentCount() == 1) {
                            String outputFolderName= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME);
                            newOutputFolder= fDesiredOutputLocation.append(outputFolderName);
                            if (editingOutputFolder)
                                message= NewWizardMessages.getFormattedString("ClasspathModifier.ChangeOutputLocationDialog.project.messageOutputFolder", newOutputFolder); //$NON-NLS-1$
                            else
                                message= NewWizardMessages.getFormattedString("ClasspathModifier.ChangeOutputLocationDialog.project.outputLocation", newOutputFolder); //$NON-NLS-1$
                        } else {
                            newOutputFolder= fDesiredOutputLocation;
                            if (editingOutputFolder && newOutputFolder != null) {
                                fOutputLocation[0]= newOutputFolder;
                                result[0]= true;
                                return; // show no dialog
                            }
                            message= NewWizardMessages.getString("ClasspathModifier.ChangeOutputLocationDialog.project.message"); //$NON-NLS-1$
                            removeProject= true;
                        }
                        String title= NewWizardMessages.getString("ClasspathModifier.ChangeOutputLocationDialog.title"); //$NON-NLS-1$$
                        MessageDialog dialog;
                        dialog= new MessageDialog(sh, title, null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
                        int code= dialog.open();
                        boolean ok= code == IDialogConstants.OK_ID;
                        if (ok) {
                            if (newOutputFolder != null) {
                                fOutputLocation[0]= newOutputFolder;
                            }
                        }
                        result[0]= ok;
                    }
                });
                return result[0];
            }
            public IPath getOutputLocation() {
                return fOutputLocation[0];
            }
            public boolean removeProjectFromClasspath() {
                return removeProject;
            }
        };
    }

    /**
     * A default query for inclusion and exclusion filters.
     * The query is used to get information about the
     * inclusion and exclusion filters of an element.
     * 
     * @param shell shell if there is any or <code>null</code>
     * @return an <code>IInclusionExclusionQuery</code> that can be executed
     * 
     * @see ClasspathModifierQueries.IInclusionExclusionQuery
     * @see org.eclipse.jdt.internal.corext.buildpath.EditOperation
     */
    public static IInclusionExclusionQuery getDefaultInclusionExclusionQuery(final Shell shell) {
        return new IInclusionExclusionQuery() {
            final boolean[] result= new boolean[1];
            final IPath[][] inclusionPattern= new IPath[1][1];
            final IPath[][] exclusionPattern= new IPath[1][1];
            public boolean doQuery(final CPListElement element, final boolean focusOnExcluded) {
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
                        ExclusionInclusionDialog dialog= new ExclusionInclusionDialog(sh, element, focusOnExcluded);
                        result[0]= dialog.open() == Window.OK;
                        inclusionPattern[0]= dialog.getInclusionPattern();
                        exclusionPattern[0]= dialog.getExclusionPattern();
                    }
                });
                return result[0];
            }
            
            public IPath[] getInclusionPattern() {
                return inclusionPattern[0];
            }
            
            public IPath[] getExclusionPattern() {
                return exclusionPattern[0];
            }
        };
    }

    /**
     * A default query for the output location.
     * The query is used to get information about the output location 
     * that should be used for a given element.
     * 
     * @param shell shell if there is any or <code>null</code>
     * @param projectOutputLocation the projects desired output location
     * @param classpathList a list of <code>CPListElement</code>s which represents the 
     * current list of source folders
     * @return an <code>IOutputLocationQuery</code> that can be executed
     * 
     * @see ClasspathModifierQueries.IOutputLocationQuery
     * @see org.eclipse.jdt.internal.corext.buildpath.CreateOutputFolderOperation
     * @see org.eclipse.jdt.internal.corext.buildpath.EditOperation
     */
    public static IOutputLocationQuery getDefaultOutputLocationQuery(final Shell shell, final IPath projectOutputLocation, final List classpathList) {
        return new IOutputLocationQuery() {
            final boolean[] result= new boolean[1];
            final IPath[] path= new IPath[1];
            public boolean doQuery(final CPListElement element) {
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
                        OutputLocationDialog dialog= new OutputLocationDialog(sh, element, classpathList);
                        result[0]= dialog.open() == Window.OK;
                        path[0]= dialog.getOutputLocation();
                    }
                });
                return result[0];
            }
            
            public IPath getOutputLocation() {
                return path[0];
            }
            
            public ClasspathModifierQueries.IOutputFolderQuery getOutputFolderQuery(IPath p) {
                return getDefaultFolderQuery(shell, projectOutputLocation);
            }
        };
    }

    /**
     * Query to create a folder.
     * 
     * The default query shows a dialog which allows
     * the user to specify the new folder that should
     * be created.
     * 
     * @param shell shell if there is any or <code>null</code>
     * @param selection an object of type <code>IFolder</code>
     * or <code>IJavaElement</code> which should become the direct
     * parent of the new folder.
     * @param type must be one of the one of the constants of
     * <code>PackageExplorerActionGroup</code> to
     * indicate the type of the selected parent folder
     * @return an <code>IFolderCreationQuery</code> showing a dialog
     * to create a folder.
     * 
     * @see ClasspathModifierQueries.IFolderCreationQuery
     * @see ExtendedNewFolderDialog
     */
    public static IFolderCreationQuery getDefaultFolderCreationQuery(final Shell shell, final Object selection, final int type) {
        return new IFolderCreationQuery() {
            final boolean[] isSourceFolder= {false};
            final IFolder[] folder= {null};
            
            public boolean doQuery() {
                final boolean[] isOK= {false};
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
                        IContainer container;
                        
                        if (selection instanceof IFolder)
                            container= (IFolder)selection;
                        else {
                            IJavaElement javaElement= (IJavaElement)selection;
                            IJavaProject project= javaElement.getJavaProject();
                            container= project.getProject();
                            if (!(selection instanceof IJavaProject)) {
                                container= container.getFolder(javaElement.getPath().removeFirstSegments(1));
                            }
                        }
                        
                        ExtendedNewFolderDialog dialog= new ExtendedNewFolderDialog(sh, container, type);
                        isOK[0]= dialog.open() == Window.OK;
                        if (isOK[0]) {
                            folder[0]= dialog.getCreatedFolder();
                            isSourceFolder[0]= dialog.isSourceFolder();
                        }
                    }
                });
                return isOK[0];
            }
            
            public boolean isSourceFolder() {
                return isSourceFolder[0];
            }
    
            public IFolder getCreatedFolder() {
                return folder[0];
            }
            
        };
    }
}
