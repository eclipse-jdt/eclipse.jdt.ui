package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class HistoryListAction extends Action {
	
	private class HistoryListDialog extends StatusDialog {
		
		private ListDialogField fHistoryList;
		private IStatus fHistoryStatus;
		private IType fResult;
		
		private HistoryListDialog(Shell shell, IType[] elements) {
			super(shell);
			setTitle(TypeHierarchyMessages.getString("HistoryListDialog.title"));
			
			String[] buttonLabels= new String[] { 
				/* 0 */ TypeHierarchyMessages.getString("HistoryListDialog.remove.button") //$NON-NLS-1$
			};
					
			IListAdapter adapter= new IListAdapter() {
				public void customButtonPressed(DialogField field, int index) {
				}
				public void selectionChanged(DialogField field) {
					doSelectionChanged();
				}
			};
		
			JavaElementLabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION);
			
			fHistoryList= new ListDialogField(adapter, buttonLabels, labelProvider);
			fHistoryList.setLabelText(TypeHierarchyMessages.getString("HistoryListDialog.label")); //$NON-NLS-1$
			fHistoryList.setRemoveButtonIndex(0);
			fHistoryList.setElements(Arrays.asList(elements));
			
			ISelection sel;
			if (elements.length > 0) {
				sel= new StructuredSelection(elements[0]);
			} else {
				sel= new StructuredSelection();
			}
			
			fHistoryList.selectElements(sel);
		}
		
		/*
		 * @see Dialog#createDialogArea(Composite)
		 */
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			int minimalWidth= convertWidthInCharsToPixels(80);
			LayoutUtil.doDefaultLayout(composite, new DialogField[] { fHistoryList }, true, minimalWidth, 0, SWT.DEFAULT, SWT.DEFAULT);	
			fHistoryList.getTableViewer().addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent event) {
					if (fHistoryStatus.isOK()) {
						okPressed();
					}
				}
			});
			return composite;
		}
		
		private void doSelectionChanged() {
			StatusInfo status= new StatusInfo();
			List selected= fHistoryList.getSelectedElements();
			if (selected.size() != 1) {
				status.setError("");
				fResult= null;
			} else {
				fResult= (IType) selected.get(0);
			}
			fHistoryStatus= status;
			updateStatus(status);	
		}
				
		public IType getResult() {
			return fResult;
		}
		
		public IType[] getRemaining() {
			List elems= fHistoryList.getElements();
			return (IType[]) elems.toArray(new IType[elems.size()]);
		}	
		
	}
	
	private TypeHierarchyViewPart fView;
	
	public HistoryListAction(TypeHierarchyViewPart view) {
		fView= view;
		setText(TypeHierarchyMessages.getString("HistoryListAction.label"));
		JavaPluginImages.setLocalImageDescriptors(this, "history_list.gif"); //$NON-NLS-1$
	}
	
		
	/*
	 * @see IAction#run()
	 */
	public void run() {
		IType[] historyEntries= fView.getHistoryEntries();
		HistoryListDialog dialog= new HistoryListDialog(JavaPlugin.getActiveWorkbenchShell(), historyEntries);
		if (dialog.open() == dialog.OK) {
			fView.setHistoryEntries(dialog.getRemaining());
			fView.setInput(dialog.getResult());
		}
	}

}

