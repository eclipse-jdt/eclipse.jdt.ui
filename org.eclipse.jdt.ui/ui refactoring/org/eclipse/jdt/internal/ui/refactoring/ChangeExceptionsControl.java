/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.ExceptionInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfo;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

/**
 * A special control to add and remove thrown exceptions.
 */
public class ChangeExceptionsControl extends Composite {
//TODO: cleanup, adapt NLS strings

	private static class ExceptionInfoContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			return removeMarkedAsDeleted((List) inputElement);
		}
		private ExceptionInfo[] removeMarkedAsDeleted(List exceptionInfos){
			List result= new ArrayList(exceptionInfos.size());
			for (Iterator iter= exceptionInfos.iterator(); iter.hasNext();) {
				ExceptionInfo info= (ExceptionInfo) iter.next();
				if (! info.isDeleted())
					result.add(info);
			}
			return (ExceptionInfo[]) result.toArray(new ExceptionInfo[result.size()]);
		}
		public void dispose() {
			// do nothing
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// do nothing
		}
	}

	private static class ExceptionInfoLabelProvider extends LabelProvider implements ITableLabelProvider {
		private Image fInterfaceImage;
			
		public ExceptionInfoLabelProvider() {
			super();
			fInterfaceImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS);
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return fInterfaceImage;
		}
		public String getColumnText(Object element, int columnIndex) {
			ExceptionInfo info= (ExceptionInfo) element;
			return info.getType().getFullyQualifiedName();
		}
	}

	private final IExceptionListChangeListener fListener;
	private final IJavaProject fProject;

	private TableViewer fTableViewer;
	private Button fAddButton;
	private Button fRemoveButton;
	private List fExceptionInfos;

	public ChangeExceptionsControl(Composite parent, int style, IExceptionListChangeListener listener, IJavaProject project) {
		super(parent, style);
		Assert.isNotNull(listener);
		fListener= listener;
		Assert.isNotNull(project);
		fProject= project;
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		setLayout(layout);

		createExceptionList(this);
		createButtonComposite(this);
	}

	public void setInput(List exceptionInfos) {
		Assert.isNotNull(exceptionInfos);
		fExceptionInfos= exceptionInfos;
		fTableViewer.setInput(fExceptionInfos);
		if (fExceptionInfos.size() > 0)
			fTableViewer.setSelection(new StructuredSelection(fExceptionInfos.get(0)));
	}

	private void createExceptionList(Composite parent) {
		final Table table= new Table(parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		fTableViewer= new TableViewer(table);
		fTableViewer.setUseHashlookup(true);
		fTableViewer.setContentProvider(new ExceptionInfoContentProvider());
		fTableViewer.setLabelProvider(new ExceptionInfoLabelProvider());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsEnabledState();
			}
		});
	}

	private ExceptionInfo[] getSelectedItems() {
		ISelection selection= fTableViewer.getSelection();
		if (selection == null)
			return new ExceptionInfo[0];

		if (!(selection instanceof IStructuredSelection))
			return new ExceptionInfo[0];

		List selected= ((IStructuredSelection) selection).toList();
		return (ExceptionInfo[]) selected.toArray(new ExceptionInfo[selected.size()]);
	}

	// ---- Button bar --------------------------------------------------------------------------------------

	private void createButtonComposite(Composite parent) {
		Composite buttonComposite= new Composite(parent, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		buttonComposite.setLayout(gl);

		fAddButton= createAddButton(buttonComposite);	
		fRemoveButton= createRemoveButton(buttonComposite);
		updateButtonsEnabledState();
	}

	private void updateButtonsEnabledState() {
		if (fRemoveButton != null)
			fRemoveButton.setEnabled(getTableSelectionCount() != 0);
	}

	private int getTableSelectionCount() {
		return getTable().getSelectionCount();
	}

	private int getTableItemCount() {
		return getTable().getItemCount();
	}

	private Table getTable() {
		return fTableViewer.getTable();
	}
	
	private Button createAddButton(Composite buttonComposite) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(RefactoringMessages.getString("ChangeExceptionsControl.buttons.add")); //$NON-NLS-1$
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.setEnabled(true);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doAddException();
			}
		});	
		return button;
	}

	private void doAddException() {
		IType newException= chooseException();
		if (newException == null)
			return;
		
		ExceptionInfo info= findExceptionInfo(newException);
		if (info != null) {
			if (info.isDeleted()) {
				info.markAsOld();
				fTableViewer.refresh();
			}
			fListener.exceptionListChanged();
			fTableViewer.getControl().setFocus();
			fTableViewer.setSelection(new StructuredSelection(info), true);
			return;
		}
		
		info= ExceptionInfo.createInfoForAddedException(newException);
		fExceptionInfos.add(info);
		fListener.exceptionListChanged();
		fTableViewer.refresh();
		fTableViewer.getControl().setFocus();
		int row= getTableItemCount() - 1;
		getTable().setSelection(row);
		updateButtonsEnabledState();

	}
	
	private IType chooseException() {
		IJavaElement[] elements= new IJavaElement[] { fProject.getJavaProject() };
		final IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
		
		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(),
				new ProgressMonitorDialog(getShell()), IJavaSearchConstants.CLASS, scope) {
			//TODO: workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=45438
			protected void updateOkState() {
				validateCurrentSelection();
			}
		};
		dialog.setTitle(RefactoringMessages.getString("ChangeExceptionsControl.choose.title")); //$NON-NLS-1$
		dialog.setMessage(RefactoringMessages.getString("ChangeExceptionsControl.choose.message")); //$NON-NLS-1$
		dialog.setFilter("*Exception*"); //$NON-NLS-1$
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection.length == 0)
					return JavaUIStatus.createError(IStatus.ERROR, "", null); //$NON-NLS-1$
				TypeInfo info= (TypeInfo) selection[0];
				try {
					IType type= info.resolveType(scope);
					return checkException(type);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
					return Status.OK_STATUS;
				}
			}
		});
		
		if (dialog.open() == Window.OK) {
			return (IType) dialog.getFirstResult();
		}
		return null;
	}
	
	private IStatus checkException(final IType type) throws JavaModelException {
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(new NullProgressMonitor());
		IType curr= type;
		while (curr != null) {
			String name= curr.getFullyQualifiedName();
			if ("java.lang.Throwable".equals(name)) //$NON-NLS-1$
				return Status.OK_STATUS;
			curr= hierarchy.getSuperclass(curr);
		}
		return JavaUIStatus.createError(IStatus.ERROR,
				RefactoringMessages.getString("ChangeExceptionsControl.not_exception"), null); //$NON-NLS-1$
	}
	
	private ExceptionInfo findExceptionInfo(IType exception) {
		for (Iterator iter= fExceptionInfos.iterator(); iter.hasNext(); ) {
			ExceptionInfo info= (ExceptionInfo) iter.next();
			if (info.getType().equals(exception))
				return info;
		}
		return null;
	}

	private Button createRemoveButton(Composite buttonComposite) {
		final Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(RefactoringMessages.getString("ChangeExceptionsControl.buttons.remove")); //$NON-NLS-1$
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index= getTable().getSelectionIndices()[0];
				ExceptionInfo[] selected= getSelectedItems();
				for (int i= 0; i < selected.length; i++) {
					if (selected[i].isAdded())
						fExceptionInfos.remove(selected[i]);
					else
						selected[i].markAsDeleted();	
				}
				restoreSelection(index);
			}
			private void restoreSelection(int index) {
				fTableViewer.refresh();
				fTableViewer.getControl().setFocus();
				int itemCount= getTableItemCount();
				if (itemCount != 0) {
					if (index >= itemCount)
						index= itemCount - 1;
					getTable().setSelection(index);
				}
				fListener.exceptionListChanged();
				updateButtonsEnabledState();
			}
		});	
		return button;
	}

}
