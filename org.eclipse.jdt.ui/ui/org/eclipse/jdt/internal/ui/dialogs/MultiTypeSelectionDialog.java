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
package org.eclipse.jdt.internal.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.TypeInfo;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;

/**
 * A dialog to select a type from a list of types. The dialog allows
 * multiple selections.
 */
public class MultiTypeSelectionDialog extends ElementListSelectionDialog {

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fElementKinds;
	
	/**
	 * Constructs an instance of <code>MultiTypeSelectionDialog</code>.
	 * @param parent  the parent shell.
	 * @param context the context.
	 * @param elementKinds IJavaSearchConstants.CLASS, IJavaSearchConstants.INTERFACE
	 * or IJavaSearchConstants.TYPE
	 * @param scope   the java search scope.
	 */
	public MultiTypeSelectionDialog(Shell parent, IRunnableContext context, int elementKinds, IJavaSearchScope scope)
	{
		super(parent, new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_PACKAGE_POSTFIX)); 
			
		setMultipleSelection(true);

		Assert.isNotNull(context);
		Assert.isNotNull(scope);

		fRunnableContext= context;
		fScope= scope;
		fElementKinds= elementKinds;
	}

	/*
	 * @see Window#open()
	 */
	public int open() {
		
		final ArrayList typesFound= new ArrayList();
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				AllTypesCache.getTypes(fScope, fElementKinds, monitor, typesFound);
				if (monitor.isCanceled()) {
					throw new InterruptedException();
				}
			}
		};
		
		try {
			fRunnableContext.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, JavaUIMessages.getString("MultiTypeSelectionDialog.error2Title"), JavaUIMessages.getString("MultiTypeSelectionDialog.error2Message")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InterruptedException e) {
			// cancelled by user
			return CANCEL;
		}

		setElements(typesFound.toArray());
		
		return super.open();
	}
	
	/*
	 * @see SelectionStatusDialog#computeResult()
	 */
	protected void computeResult() {
		List selection= Arrays.asList(getSelectedElements()); // XXX inefficient
		int size= selection.size();
		if (size == 0) {
			setResult(null);
			return;
		}
		
		List result= new ArrayList(size);
		if (result != null) {
			for (int i= 0; i < size; i++) {
				try {
					TypeInfo typeInfo= (TypeInfo)selection.get(i);
					IType type= typeInfo.resolveType(fScope);
					if (type == null) {
						String title= JavaUIMessages.getString("MultiTypeSelectionDialog.dialogTitle"); //$NON-NLS-1$
						String message= JavaUIMessages.getFormattedString("MultiTypeSelectionDialog.dialogMessage", typeInfo.getPath()); //$NON-NLS-1$
						MessageDialog.openError(getShell(), title, message);
					} else {
						result.add(type);
					}
				} catch (JavaModelException e) {
					String title= JavaUIMessages.getString("MultiTypeSelectionDialog.errorTitle"); //$NON-NLS-1$
					String message= JavaUIMessages.getString("MultiTypeSelectionDialog.errorMessage"); //$NON-NLS-1$
					ErrorDialog.openError(getShell(), title, message, e.getStatus());
				}
			}
		}
		setResult(result);
	}
	
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.MULTI_TYPE_SELECTION_DIALOG);
	}


}
