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
import org.eclipse.jdt.internal.ui.util.TypeRef;
import org.eclipse.jdt.internal.ui.util.TypeRefLabelProvider;

/**
 * A dialog to select a type from a list of types. The dialog allows
 * multiple selections.
 */
public class MultiTypeSelectionDialog extends ElementListSelectionDialog {

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fStyle;
	
	private final static String PREFIX= "type_selector.";
	private final static String NO_MAPPING_PREFIX= PREFIX+"no_mapping.";
		
	public MultiTypeSelectionDialog(Shell parent, IRunnableContext context, IJavaSearchScope scope, int style, boolean ignoreCase) {
		super(parent, new TypeRefLabelProvider(TypeRefLabelProvider.SHOW_PACKAGE_POSTFIX), ignoreCase, true); 
		fRunnableContext= context;
		Assert.isNotNull(fRunnableContext);
		fScope= scope;
		Assert.isNotNull(fScope);
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
		setInitialSelection("A");				
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
					IType type= ((TypeRef)selection.get(i)).resolveType(fScope);
					if (type == null) {
						String title= JavaPlugin.getResourceString(NO_MAPPING_PREFIX+"title");
						String message= JavaPlugin.getResourceString(NO_MAPPING_PREFIX+"message");
						MessageDialog.openError(getShell(), title, message);
						//XXX: java model
					} else {
						result.add(type);
					}
				} catch (JavaModelException e) {
					String title= JavaPlugin.getResourceString(NO_MAPPING_PREFIX+"title");
					String message= JavaPlugin.getResourceString(NO_MAPPING_PREFIX+"message");
					MessageDialog.openError(getShell(), title, message);
				}
			}
		}
		setResult(result);
	}
	
}