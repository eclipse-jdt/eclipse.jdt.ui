package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ArrayList;
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
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class HistoryListAction extends Action {
	
	private class HistoryListDialog extends StatusDialog {
		
		private ListDialogField fHistoryList;
		private IStatus fHistoryStatus;
		private Object fResult;
		
		private HistoryListDialog(Shell shell, ArrayList elements) {
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
			fHistoryList.setElements(elements);
			
			ISelection sel;
			if (elements.size() > 0) {
				sel= new StructuredSelection(elements.get(0));
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
			LayoutUtil.doDefaultLayout(composite, new DialogField[] { fHistoryList }, true, 420, 0, SWT.DEFAULT, SWT.DEFAULT);	
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
				fResult= selected.get(0);
			}
			fHistoryStatus= status;
			updateStatus(status);	
		}
		
		public Object getResult() {
			return fResult;
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
		ArrayList elements= new ArrayList();
		int i= 0;
		while (true) {
			Object elem= fView.getHistoryEntry(i++);
			if (elem != null) {
				elements.add(elem);
			} else {
				break;
			}
		}
		HistoryListDialog dialog= new HistoryListDialog(JavaPlugin.getActiveWorkbenchShell(), elements);
		if (dialog.open() == dialog.OK) {
			int index= elements.indexOf(dialog.getResult());
			fView.gotoHistoryEntry(index);
		}
	}

}

