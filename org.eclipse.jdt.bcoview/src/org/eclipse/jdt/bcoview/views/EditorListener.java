/*******************************************************************************
 * Copyright (c) 2018 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.views;

import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

public class EditorListener implements ISelectionListener, IFileBufferListener,
    IPartListener2 {
    volatile protected BytecodeOutlineView view;

    EditorListener(BytecodeOutlineView view){
        this.view = view;
    }

    /**
     * clean view reference
     */
    public void dispose(){
        this.view = null;
    }

    /**
     * @param part
     * @param selection
     *
     */
    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if(!(selection instanceof ITextSelection)){
            if(selection instanceof IStructuredSelection){
                IStructuredSelection ssel = (IStructuredSelection) selection;
                if(ssel.isEmpty()){
                    return;
                }
                if(ssel.getFirstElement() instanceof IJavaElement){
                    /*
                     * this may be selection in outline view. If so, the editor selection
                     * would be changed but no event would be sent :(
                     * So we just delay the call and wait for new selection in editor
                     */
                    Display display = Display.getDefault();
                    // fork
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if(view != null) {
                                view.checkOpenEditors(true);
                            }
                        }
                    });
                }
            }
            return;
        }
        view.handleSelectionChanged(part, selection);
    }

    /**
     * @see org.eclipse.core.filebuffers.IFileBufferListener#dirtyStateChanged(org.eclipse.core.filebuffers.IFileBuffer, boolean)
     */
    @Override
    public void dirtyStateChanged(IFileBuffer buffer, final boolean isDirty) {
        if(!view.isLinkedWithEditor()){
            return;
        }
        if(isSupportedBuffer(buffer)){
            // first call set only view flag - cause
            view.handleBufferIsDirty(isDirty);

            // second call will really refresh view
            if(!isDirty){
                // this one will be called in UI thread after some delay, because we need
                // to wait until the bytecode will be written on disk
                final Runnable runnable2 = new Runnable() {
                    @Override
                    public void run() {
                        view.handleBufferIsDirty(isDirty);
                    }
                };
                // this one will be called in UI thread ASAP and allow us to leave
                // current (probably non-UI) thread
                Runnable runnable1 = new Runnable() {
                    @Override
                    public void run() {
                        Display display = Display.getCurrent();
                        display.timerExec(1000, runnable2);
                    }
                };
                Display display = Display.getDefault();
                // fork
                display.asyncExec(runnable1);
            }
        }
    }

    private static boolean isSupportedBuffer(IFileBuffer buffer) {
        String fileExtension = buffer.getLocation().getFileExtension();
        // TODO export to properties
        return "java".equals(fileExtension);// || "groovy".equals(fileExtension);  //$NON-NLS-1$
    }

    /**
     * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
        view.handlePartHidden(partRef.getPart(false));
    }

    /**
     * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
        view.handlePartHidden(partRef.getPart(false));
    }

    /**
     * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
        view.handlePartVisible(partRef.getPart(false));
    }

    /**
     * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
        view.handlePartVisible(partRef.getPart(false));
    }


    /**
     * @see org.eclipse.core.filebuffers.IFileBufferListener#bufferDisposed(org.eclipse.core.filebuffers.IFileBuffer)
     */
    @Override
    public void bufferDisposed(IFileBuffer buffer) {
        // is not used here
    }

    /**
     * @see org.eclipse.core.filebuffers.IFileBufferListener#bufferCreated(org.eclipse.core.filebuffers.IFileBuffer)
     */
    @Override
    public void bufferCreated(IFileBuffer buffer) {
        // is not used here
    }

    /**
     * @see org.eclipse.core.filebuffers.IFileBufferListener#bufferContentAboutToBeReplaced(org.eclipse.core.filebuffers.IFileBuffer)
     */
    @Override
    public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {
        // is not used here
    }

    /**
     * @see org.eclipse.core.filebuffers.IFileBufferListener#bufferContentReplaced(org.eclipse.core.filebuffers.IFileBuffer)
     */
    @Override
    public void bufferContentReplaced(IFileBuffer buffer) {
        // is not used here
    }

    /**
     * @see org.eclipse.core.filebuffers.IFileBufferListener#stateChanging(org.eclipse.core.filebuffers.IFileBuffer)
     */
    @Override
    public void stateChanging(IFileBuffer buffer) {
        // is not used here
    }

    /**
     * @see org.eclipse.core.filebuffers.IFileBufferListener#stateValidationChanged(org.eclipse.core.filebuffers.IFileBuffer, boolean)
     */
    @Override
    public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {
        // is not used here
    }

    /**
     * @see org.eclipse.core.filebuffers.IFileBufferListener#underlyingFileMoved(org.eclipse.core.filebuffers.IFileBuffer, org.eclipse.core.runtime.IPath)
     */
    @Override
    public void underlyingFileMoved(IFileBuffer buffer, IPath path) {
        //is not used here
    }

    /**
     * @see org.eclipse.core.filebuffers.IFileBufferListener#underlyingFileDeleted(org.eclipse.core.filebuffers.IFileBuffer)
     */
    @Override
    public void underlyingFileDeleted(IFileBuffer buffer) {
        //is not used here
    }

    /**
     * @see org.eclipse.core.filebuffers.IFileBufferListener#stateChangeFailed(org.eclipse.core.filebuffers.IFileBuffer)
     */
    @Override
    public void stateChangeFailed(IFileBuffer buffer) {
        //is not used here
    }

    /**
     * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
        // is not used here
    }

    /**
     * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
        // is not used here
    }

    /**
     * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
        // is not used here
    }

    /**
     * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
        // is not used here
    }





}
