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
package org.eclipse.jdt.bcoview.ui.actions;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;

public class OpenAction extends BytecodeAction {

    @Override
    public void run(IAction action) {
        // always only one element!
        IJavaElement[] resources = getSelectedResources();

        // select one from input dialog
        IJavaElement element2 = selectJavaElement();
        if (element2 == null) {
            return;
        }
        try {
            exec(resources[0], element2);
        } catch (Exception e) {
            BytecodeOutlinePlugin.error("Failed to run Compare: "
                + e.getMessage(), e);
        }
    }

    private IJavaElement selectJavaElement() {
        IContainer input = ResourcesPlugin.getWorkspace().getRoot();

        OpenClassFileDialog dialog = new OpenClassFileDialog(
            shell, input, IResource.FILE);

        int resultCode = dialog.open();
        if (resultCode != IDialogConstants.OK_ID) {
            return null;
        }

        Object[] result = dialog.getResult();
        if (result == null || result.length == 0
            || !(result[0] instanceof IFile)) {
            return null;
        }
        return JavaCore.create((IFile) result[0]);
    }

    /**
     * @author Andrei
     */
    private static final class OpenClassFileDialog extends ResourceListSelectionDialog {

        /**
         * @param parentShell
         * @param container
         * @param typesMask
         */
        public OpenClassFileDialog(Shell parentShell, IContainer container,
            int typesMask) {
            super(parentShell, container, typesMask);
            setTitle("Bytecode compare");
            setMessage("Please select class file to compare");
        }

        /**
         * Extends the super's filter to exclude derived resources.
         * @since 3.0
         */
        @Override
        protected boolean select(IResource resource) {
            if (resource == null) {
                return false;
            }
            String fileExtension = resource.getFileExtension();
            return super.select(resource)
                && ("java".equals(fileExtension) || "class"
                    .equals(fileExtension));
        }
    }

}
