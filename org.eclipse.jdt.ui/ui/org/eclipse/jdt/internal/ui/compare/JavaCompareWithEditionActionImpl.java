/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

import java.util.ResourceBundle;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.compare.*;


/**
 * Provides "Replace from local history" for Java elements.
 */
class JavaCompareWithEditionActionImpl extends JavaHistoryActionImpl {
	
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.CompareWithEditionAction"; //$NON-NLS-1$
	
	
	JavaCompareWithEditionActionImpl() {
		super(false);
	}	
	
	public void run(ISelection selection) {
		
		String errorTitle= CompareMessages.CompareWithHistory_title; 
		String errorMessage= CompareMessages.CompareWithHistory_internalErrorMessage; 
				
		IMember input= getEditionElement(selection);
		if (input == null) {
			String invalidSelectionMessage= CompareMessages.CompareWithHistory_invalidSelectionMessage; 
			MessageDialog.openInformation(getShell(), errorTitle, invalidSelectionMessage);
			return;
		}
		
		IFile file= getFile(input);
		if (file == null) {
			MessageDialog.openError(getShell(), errorTitle, errorMessage);
			return;
		}
		
		boolean inEditor= beingEdited(file);
		if (inEditor)
			input= (IMember) getWorkingCopy(input);

		// get a TextBuffer
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(file);

			ITypedElement target= new JavaTextBufferNode(file, buffer, inEditor);

			ITypedElement[] editions= buildEditions(target, file);

			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(getShell(), bundle);
			d.setHelpContextId(IJavaHelpContextIds.COMPARE_ELEMENT_WITH_HISTORY_DIALOG);
			d.setCompareMode(true);
			Image image= JavaCompareUtilities.getImage(input);
			d.setEditionTitleImage(image);
			d.selectEdition(target, editions, input);
			if (image != null && !image.isDisposed())
				image.dispose();
		} catch(CoreException ex) {
			ExceptionHandler.handle(ex, getShell(), errorTitle, errorMessage);
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}
}

