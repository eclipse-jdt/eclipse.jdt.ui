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
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;
import org.eclipse.jdt.internal.ui.util.TypeInfo;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;

/**
 * A dialog to select a type from a list of types.
 */
public class TypeSelectionDialog extends TwoPaneElementSelector {

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fStyle;
	
	/**
	 * Constructs a type selection dialog.
	 * @param parent  the parent shell.
	 * @param context the runnable context.
	 * @param scope   the java search scope.
	 * @param style   the widget style.
	 */
	public TypeSelectionDialog(Shell parent, IRunnableContext context,
		IJavaSearchScope scope, int style)
	{
		super(parent, new TypeInfoLabelProvider(0),
			new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_PACKAGE_ONLY + TypeInfoLabelProvider.SHOW_ROOT_POSTFIX));

		Assert.isNotNull(context);
		Assert.isNotNull(scope);

		fRunnableContext= context;
		fScope= scope;
		fStyle= style;
		
		setUpperListLabel(JavaUIMessages.getString("TypeSelectionDialog.upperLabel")); //$NON-NLS-1$
		setLowerListLabel(JavaUIMessages.getString("TypeSelectionDialog.lowerLabel")); //$NON-NLS-1$
	}

	public void create() {
		if (getFilter() == null)
			setFilter("A"); //$NON-NLS-1$
				
		super.create();
	}
	
	/**
	 * @see Window#open()
	 */
	public int open() {
		AllTypesSearchEngine engine= new AllTypesSearchEngine(JavaPlugin.getWorkspace());
		
		List typeList= engine.searchTypes(fRunnableContext, fScope, fStyle);
		
		if (typeList.isEmpty()) {
			String title= JavaUIMessages.getString("TypeSelectionDialog.notypes.title"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("TypeSelectionDialog.notypes.message"); //$NON-NLS-1$
			MessageDialog.openInformation(getShell(), title, message);
			return CANCEL;
		}
			
		TypeInfo[] typeRefs= (TypeInfo[])typeList.toArray(new TypeInfo[typeList.size()]);
		setElements(typeRefs);

		return super.open();
	}
	
	/**
	 * @see SelectionStatusDialog#computeResult()
	 */
	protected void computeResult() {
		TypeInfo ref= (TypeInfo) getLowerSelectedElement();

		if (ref == null)
			return;

		try {
			IType type= ref.resolveType(fScope);			
			if (type == null) {
				// not a class file or compilation unit
				String title= JavaUIMessages.getString("TypeSelectionDialog.errorTitle"); //$NON-NLS-1$
				String message= JavaUIMessages.getString("TypeSelectionDialog.errorMessage"); //$NON-NLS-1$
				MessageDialog.openError(getShell(), title, message);
				setResult(null);
			} else {
				List result= new ArrayList(1);
				result.add(type);
				setResult(result);
			}

		} catch (JavaModelException e) {
			String title= JavaUIMessages.getString("TypeSelectionDialog.errorTitle"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("TypeSelectionDialog.errorMessage"); //$NON-NLS-1$
			MessageDialog.openError(getShell(), title, message);
			setResult(null);
		}
	}
	
}