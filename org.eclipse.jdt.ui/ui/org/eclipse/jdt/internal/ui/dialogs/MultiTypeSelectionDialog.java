/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;
import org.eclipse.jdt.internal.ui.util.TypeInfo;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

/**
 * A dialog to select a type from a list of types. The dialog allows
 * multiple selections.
 */
public class MultiTypeSelectionDialog extends ElementListSelectionDialog {

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fStyle;
	
	public MultiTypeSelectionDialog(Shell parent, IRunnableContext context,
		IJavaSearchScope scope, int style, boolean ignoreCase)
	{
		super(parent, new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_PACKAGE_POSTFIX),
			ignoreCase, true); 

		Assert.isNotNull(context);
		Assert.isNotNull(scope);

		fRunnableContext= context;
		fScope= scope;
		fStyle= style;
	}

	/*
	 * @private
	 */
	public int open() {
		AllTypesSearchEngine engine= new AllTypesSearchEngine(JavaPlugin.getWorkspace());
		List typesFound= engine.searchTypes(fRunnableContext, fScope, fStyle);

		if (typesFound.size() == 0)
			return CANCEL;
		
		setElements(typesFound);
		setInitialSelection("A"); //$NON-NLS-1$
		return super.open();
	}
	
	/*
	 * @private
	 */
	protected void computeResult() {
		List selection= getWidgetSelection();
		int size= selection.size();
		if (size == 0) {
			setResult(null);
			return;
		}
		
		List result= new ArrayList(size);
		if (result != null) {
			for (int i= 0; i < size; i++) {
				try {
					IType type= ((TypeInfo)selection.get(i)).resolveType(fScope);
					if (type == null) {
						String title= JavaUIMessages.getString("MultiTypeSelectionDialog.dialogTitle"); //$NON-NLS-1$
						String message= JavaUIMessages.getString("MultiTypeSelectionDialog.dialogMessage"); //$NON-NLS-1$
						MessageDialog.openError(getShell(), title, message);
						//XXX: java model
					} else {
						result.add(type);
					}
				} catch (JavaModelException e) {
					String title= JavaUIMessages.getString("MultiTypeSelectionDialog.errorTitle"); //$NON-NLS-1$
					String message= JavaUIMessages.getString("MultiTypeSelectionDialog.errorMessage"); //$NON-NLS-1$
					MessageDialog.openError(getShell(), title, message);
				}
			}
		}
		setResult(result);
	}
	
}