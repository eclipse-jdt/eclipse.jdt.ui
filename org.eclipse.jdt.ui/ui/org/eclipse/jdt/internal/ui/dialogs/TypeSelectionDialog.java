/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.dialogs.FilteredList;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;

/**
 * A dialog to select a type from a list of types.
 */
public class TypeSelectionDialog extends TwoPaneElementSelector {

	private static class TypeFilterMatcher implements FilteredList.FilterMatcher {

		private StringMatcher fMatcher;
		private StringMatcher fQualifierMatcher;
		
		/*
		 * @see FilteredList.FilterMatcher#setFilter(String, boolean)
		 */
		public void setFilter(String pattern, boolean ignoreCase, boolean igoreWildCards) {
			int qualifierIndex= pattern.lastIndexOf("."); //$NON-NLS-1$

			// type			
			if (qualifierIndex == -1) {
				fQualifierMatcher= null;
				fMatcher= new StringMatcher(adjustPattern(pattern), ignoreCase, igoreWildCards);
				
			// qualified type
			} else {
				fQualifierMatcher= new StringMatcher(pattern.substring(0, qualifierIndex), ignoreCase, igoreWildCards);
				fMatcher= new StringMatcher(adjustPattern(pattern.substring(qualifierIndex + 1)), ignoreCase, igoreWildCards);
			}
		}

		/*
		 * @see FilteredList.FilterMatcher#match(Object)
		 */
		public boolean match(Object element) {
			if (!(element instanceof TypeInfo))
				return false;

			TypeInfo type= (TypeInfo) element;

			if (!fMatcher.match(type.getTypeName()))
				return false;

			if (fQualifierMatcher == null)
				return true;

			return fQualifierMatcher.match(type.getTypeContainerName());
		}
		
		private String adjustPattern(String pattern) {
			if (pattern.length() == 0)
				return pattern;			
			int pos= pattern.indexOf('*');
			if (pos == -1)
				return pattern + '*';
			else
				return pattern;
		}
	}
	
	/*
	 * A string comparator which is aware of obfuscated code
	 * (type names starting with lower case characters).
	 */
	private static class StringComparator implements Comparator {
	    public int compare(Object left, Object right) {
	     	String leftString= (String) left;
	     	String rightString= (String) right;
	     		     	
	     	if (Character.isLowerCase(leftString.charAt(0)) &&
	     		!Character.isLowerCase(rightString.charAt(0)))
	     		return +1;

	     	if (Character.isLowerCase(rightString.charAt(0)) &&
	     		!Character.isLowerCase(leftString.charAt(0)))
	     		return -1;
	     	
			int result= leftString.compareToIgnoreCase(rightString);			
			if (result == 0)
				result= leftString.compareTo(rightString);

			return result;
	    }
	}

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fElementKinds;
	
	/**
	 * Constructs a type selection dialog.
	 * @param parent  the parent shell.
	 * @param context the runnable context.
	 * @param elementKinds <code>IJavaSearchConstants.CLASS</code>, <code>IJavaSearchConstants.INTERFACE</code>
	 * or <code>IJavaSearchConstants.TYPE</code>
	 * @param scope   the java search scope.
	 */
	public TypeSelectionDialog(Shell parent, IRunnableContext context, int elementKinds, IJavaSearchScope scope) {
		super(parent, new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_TYPE_ONLY),
			new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_TYPE_CONTAINER_ONLY + TypeInfoLabelProvider.SHOW_ROOT_POSTFIX));

		Assert.isNotNull(context);
		Assert.isNotNull(scope);

		fRunnableContext= context;
		fScope= scope;
		fElementKinds= elementKinds;
		
		setUpperListLabel(JavaUIMessages.getString("TypeSelectionDialog.upperLabel")); //$NON-NLS-1$
		setLowerListLabel(JavaUIMessages.getString("TypeSelectionDialog.lowerLabel")); //$NON-NLS-1$
	}

	/*
	 * @see AbstractElementListSelectionDialog#createFilteredList(Composite)
	 */
 	protected FilteredList createFilteredList(Composite parent) {
 		FilteredList list= super.createFilteredList(parent);
 		
		fFilteredList.setFilterMatcher(new TypeFilterMatcher());
		fFilteredList.setComparator(new StringComparator());
		
		return list;
	}
	
	/*
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		final ArrayList typeList= new ArrayList();
		if (AllTypesCache.isCacheUpToDate()) {
			// run without progress monitor
			try {
				AllTypesCache.getTypes(fScope, fElementKinds, null, typeList);
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, JavaUIMessages.getString("TypeSelectionDialog.error2Title"), JavaUIMessages.getString("TypeSelectionDialog.error2Message")); //$NON-NLS-1$ //$NON-NLS-2$
				return CANCEL;
			}
		} else {
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						AllTypesCache.getTypes(fScope, fElementKinds, monitor, typeList);
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
				ExceptionHandler.handle(e, JavaUIMessages.getString("TypeSelectionDialog.error3Title"), JavaUIMessages.getString("TypeSelectionDialog.error3Message")); //$NON-NLS-1$ //$NON-NLS-2$
				return CANCEL;
			} catch (InterruptedException e) {
				// cancelled by user
				return CANCEL;
			}
		}
		
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
			ErrorDialog.openError(getShell(), title, message, e.getStatus());
			setResult(null);
		}
	}
	
}