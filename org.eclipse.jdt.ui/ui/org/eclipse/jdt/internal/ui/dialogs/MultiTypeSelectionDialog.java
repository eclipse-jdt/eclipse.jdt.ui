/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.TypeInfo;
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
				try {
					AllTypesCache.getTypes(fScope, fElementKinds, monitor, typesFound);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
				if (monitor.isCanceled()) {
					throw new InterruptedException();
				}
			}
		};
		
		try {
			fRunnableContext.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, "Exception", "Unexpected exception. See log for details.");
		} catch (InterruptedException e) {
			// cancelled by user
			return CANCEL;
		}
		
		setFilter("A"); //$NON-NLS-1$
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
					IType type= ((TypeInfo)selection.get(i)).resolveType(fScope);
					if (type == null) {
						String title= JavaUIMessages.getString("MultiTypeSelectionDialog.dialogTitle"); //$NON-NLS-1$
						String message= JavaUIMessages.getString("MultiTypeSelectionDialog.dialogMessage"); //$NON-NLS-1$
						MessageDialog.openError(getShell(), title, message);
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