/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;import java.util.List;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.operation.IRunnableContext;import org.eclipse.jface.util.Assert;import org.eclipse.jdt.core.ElementChangedEvent;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IElementChangedListener;import org.eclipse.jdt.core.IField;import org.eclipse.jdt.core.IImportContainer;import org.eclipse.jdt.core.IImportDeclaration;import org.eclipse.jdt.core.IInitializer;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaElementDelta;import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.IPackageDeclaration;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.IWorkingCopy;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;import org.eclipse.jdt.internal.ui.util.TypeInfo;import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.internal.ui.JavaUIMessages;


/**
 * A dialog to select a type from a list of types.
 */
public class TypeSelectionDialog extends TwoPaneElementSelector {

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fStyle;
	
	/**
	 * Constructor.
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
	
	/**
	 * @private
	 */
	public int open() {
		AllTypesSearchEngine engine= new AllTypesSearchEngine(JavaPlugin.getWorkspace());
		
		List typeList= TypeCache.findTypes(engine, fStyle, fRunnableContext, fScope);
		
		if (typeList.isEmpty()) {
			String title= JavaUIMessages.getString("TypeSelectionDialog.notypes.title"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("TypeSelectionDialog.notypes.message"); //$NON-NLS-1$
			MessageDialog.openInformation(getShell(), title, message);
			return CANCEL;
		}
			
		TypeInfo[] typeRefs= (TypeInfo[])typeList.toArray(new TypeInfo[typeList.size()]);
		setElements(typeRefs);
		setInitialSelection("A"); //$NON-NLS-1$
		return super.open();
	}
	
	/**
	 * @private
	 */
	protected void computeResult() {
		TypeInfo ref= (TypeInfo) getWidgetSelection();

		if (ref == null)
			return;

		try {
			IType type= ref.resolveType(fScope);			
			if (type != null) {
				List result= new ArrayList(1);
				result.add(type);
				setResult(result);
				return;				
			}

		// not a class file or compilation unit
		} catch (JavaModelException e) {
		} finally {
			String title= JavaUIMessages.getString("TypeSelectionDialog.errorTitle"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("TypeSelectionDialog.errorMessage"); //$NON-NLS-1$
			MessageDialog.openError(getShell(), title, message);
			setResult(null);
		}
	}
	
}