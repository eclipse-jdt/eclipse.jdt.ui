package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.swt.widgets.Shell;

public class HistoryListAction {

	
	private class HistoryListDialog extends StatusDialog {
		
		private ListDialogField fHistoryList;
		
		private HistoryListDialog(Shell shell) {
			super(shell);
			
			String[] buttonLabels= new String[] { 
				/* 0 */ TypeHierarchyMessages.getString("HistoryListDialog.up.button"), //$NON-NLS-1$
				/* 1 */ TypeHierarchyMessages.getString("HistoryListDialog.down.button"), //$NON-NLS-1$
				/* 2 */ null,
				/* 3 */ TypeHierarchyMessages.getString("HistoryListDialog.remove.button") //$NON-NLS-1$
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
			fHistoryList.setUpButtonIndex(0);
			fHistoryList.setDownButtonIndex(1);
			fHistoryList.setRemoveButtonIndex(3);
		}
		
		private void doSelectionChanged() {
		}
		
		
		
	}


}

