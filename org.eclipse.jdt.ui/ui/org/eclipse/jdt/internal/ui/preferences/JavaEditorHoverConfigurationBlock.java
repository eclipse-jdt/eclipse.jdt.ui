/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.preferences;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaEditorTextHoverDescriptor;

/**
 * Configures Java Editor hover preferences.
 * 
 * @since 2.1
 */
class JavaEditorHoverConfigurationBlock {

	private final static int NO_HOVER_CONFIGURED_INDEX= -1;
	private final static int DEFAULT_HOVER_CONFIGURED_INDEX= -2;
	private final static String MODIFIER= "modifier"; //$NON-NLS-1$	
	private final static String HOVER= "hover"; //$NON-NLS-1$	
	

	private static class HoverConfig {
		
		private String fModifier;
		private int fStateMask;
		private int fHoverIndex;
		private String fHoverId;

		private HoverConfig(String modifier, int stateMask, String hoverId) {
			fModifier= modifier;
			fStateMask= stateMask;

			if (PreferenceConstants.EDITOR_NO_HOVER_CONFIGURED_ID.equals(hoverId))
				setHoverIndex(NO_HOVER_CONFIGURED_INDEX);
			else if (PreferenceConstants.EDITOR_DEFAULT_HOVER_CONFIGURED_ID.equals(hoverId))
				setHoverIndex(DEFAULT_HOVER_CONFIGURED_INDEX);
			else {
				// Find index
				int i= 0;
				Iterator iter= getContributedHovers().iterator();
				while (iter.hasNext()) {
					if (hoverId.equals(((JavaEditorTextHoverDescriptor)iter.next()).getId()))
						break;
					i++;
				}
				if (i < getContributedHovers().size())
					setHoverIndex(i);
				else
					setHoverIndex(NO_HOVER_CONFIGURED_INDEX);
			}
		}
		
		private void setHoverIndex(int hoverIndex) {
			fHoverIndex= hoverIndex;
			if (hoverIndex == NO_HOVER_CONFIGURED_INDEX)
				fHoverId= PreferenceConstants.EDITOR_NO_HOVER_CONFIGURED_ID;
			else if (hoverIndex == DEFAULT_HOVER_CONFIGURED_INDEX)
				fHoverId= PreferenceConstants.EDITOR_DEFAULT_HOVER_CONFIGURED_ID;
			else if (hoverIndex >= 0 && hoverIndex < getContributedHovers().size())
				fHoverId= ((JavaEditorTextHoverDescriptor)getContributedHovers().get(hoverIndex)).getId();
			else
				Assert.isLegal(false);
		}
	}

	private static class HoverConfigLabelProvider extends LabelProvider implements ITableLabelProvider {
		/*
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			HoverConfig config= (HoverConfig)element;
			switch (columnIndex) {
				case 0:
					return config.fModifier;
				case 1:
					if (config.fHoverIndex == NO_HOVER_CONFIGURED_INDEX)
						return JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.noHoverConfigured"); //$NON-NLS-1$
					else if (config.fHoverIndex == DEFAULT_HOVER_CONFIGURED_INDEX)
						return JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.defaultHover"); //$NON-NLS-1$
					else
						return ((JavaEditorTextHoverDescriptor)getContributedHovers().get(config.fHoverIndex)).getLabel();
				default :
					Assert.isLegal(false);
				return null;
			}
		}
		/* 
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	private static class HoverConfigContentProvider implements IStructuredContentProvider {
		/* 
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return (Object[])inputElement;
		}
		/* 
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		/*
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}
	}


	private IPreferenceStore fStore;
	private HoverConfig[] fHoverConfigs;
	private TableViewer fTableViewer;

	public JavaEditorHoverConfigurationBlock(IPreferenceStore store) {
		Assert.isNotNull(store);
		fStore= store;
	}

	private static List getContributedHovers() {
		return JavaEditorTextHoverDescriptor.getContributedHovers();
	}

	/**
	 * Creates page for hover preferences.
	 */
	public Control createControl(Composite parent) {

		Composite hoverComposite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 1;
		hoverComposite.setLayout(layout);

		final Table table= new Table(hoverComposite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		
		GridData data= new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);
				
		table.setHeaderVisible(true);
		table.setLinesVisible(true);		

		TableLayout tableLayout= new TableLayout();
		table.setLayout(tableLayout);

		TableColumn column1= new TableColumn(table, SWT.NONE);		
		column1.setText(JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.tableColumn.stateMask")); //$NON-NLS-1$

		TableColumn column2= new TableColumn(table, SWT.NONE);
		column2.setText(JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.tableColumn.hover")); //$NON-NLS-1$

		fTableViewer= new TableViewer(table);

		fTableViewer.setLabelProvider(new HoverConfigLabelProvider());
		fTableViewer.setContentProvider(new HoverConfigContentProvider());

		// Setup cell editing support
		final ComboBoxCellEditor comboBoxCellEditor= new ComboBoxCellEditor();
		comboBoxCellEditor.setStyle(SWT.READ_ONLY);
		fTableViewer.setCellEditors(new CellEditor [] {null, comboBoxCellEditor});
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (comboBoxCellEditor.getControl() == null & !table.isDisposed())
					comboBoxCellEditor.create(table);
				comboBoxCellEditor.setItems(getLabelsForHoverCombo());
			}
		});
		
		ICellModifier cellModifier = new ICellModifier() {
			public Object getValue(Object element, String property) {
				if (HOVER.equals(property)) {
					if (isDefaultRowSelected())
						return new Integer(((HoverConfig)element).fHoverIndex + 1);
					else
						return new Integer(((HoverConfig)element).fHoverIndex + 2);
				}
				return null;
			}
	
			public boolean canModify(Object element, String property) {
				return HOVER.equals(property);
			}
	
			public void modify(Object element, String property, Object value) {
				if (HOVER.equals(property)) {
					HoverConfig hoverConfig= (HoverConfig)((Item)element).getData();
					if (isDefaultRowSelected())
						hoverConfig.setHoverIndex(((Integer)value).intValue() - 1);
					else
						hoverConfig.setHoverIndex(((Integer)value).intValue() - 2);
					fTableViewer.refresh(hoverConfig);
				}
			}
		};
		fTableViewer.setCellModifier(cellModifier);
		fTableViewer.setColumnProperties(new String[] {MODIFIER, HOVER});
		
		setTableInput();

        configureTableResizing(hoverComposite, table, column1, column2);

		return hoverComposite;
	}

	private void setTableInput() {
		fHoverConfigs= new HoverConfig[8];
		
		String label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.defaultHover"); //$NON-NLS-1$
		fHoverConfigs[0]= new HoverConfig(label, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK, fStore.getString(PreferenceConstants.EDITOR_DEFAULT_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.none"); //$NON-NLS-1$
		fHoverConfigs[1]= new HoverConfig(label, SWT.NONE, fStore.getString(PreferenceConstants.EDITOR_NONE_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.ctrl"); //$NON-NLS-1$		
		fHoverConfigs[2]= new HoverConfig(label, SWT.CTRL, fStore.getString(PreferenceConstants.EDITOR_CTRL_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.shift"); //$NON-NLS-1$		
		fHoverConfigs[3]= new HoverConfig(label, SWT.SHIFT, fStore.getString(PreferenceConstants.EDITOR_SHIFT_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.ctrlShift"); //$NON-NLS-1$		
		fHoverConfigs[4]= new HoverConfig(label, SWT.CTRL | SWT.SHIFT, fStore.getString(PreferenceConstants.EDITOR_CTRL_SHIFT_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.ctrlAlt"); //$NON-NLS-1$		
		fHoverConfigs[5]= new HoverConfig(label, SWT.CTRL | SWT.ALT, fStore.getString(PreferenceConstants.EDITOR_CTRL_ALT_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.altShift"); //$NON-NLS-1$		
		fHoverConfigs[6]= new HoverConfig(label, SWT.ALT | SWT.SHIFT, fStore.getString(PreferenceConstants.EDITOR_ALT_SHIFT_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.ctrlAltShift"); //$NON-NLS-1$		
		fHoverConfigs[7]= new HoverConfig(label, SWT.CTRL | SWT.ALT | SWT.SHIFT, fStore.getString(PreferenceConstants.EDITOR_CTRL_ALT_SHIFT_HOVER));

		fTableViewer.setInput(fHoverConfigs);
	}

	void initializeFields() {
		setTableInput();
	}

	void performOk() {
		fStore.setValue(PreferenceConstants.EDITOR_DEFAULT_HOVER, fHoverConfigs[0].fHoverId);
		fStore.setValue(PreferenceConstants.EDITOR_NONE_HOVER, fHoverConfigs[1].fHoverId);
		fStore.setValue(PreferenceConstants.EDITOR_CTRL_HOVER, fHoverConfigs[2].fHoverId);
		fStore.setValue(PreferenceConstants.EDITOR_SHIFT_HOVER, fHoverConfigs[3].fHoverId);
		fStore.setValue(PreferenceConstants.EDITOR_CTRL_SHIFT_HOVER, fHoverConfigs[4].fHoverId);
		fStore.setValue(PreferenceConstants.EDITOR_CTRL_ALT_HOVER, fHoverConfigs[5].fHoverId);
		fStore.setValue(PreferenceConstants.EDITOR_ALT_SHIFT_HOVER, fHoverConfigs[6].fHoverId);
		fStore.setValue(PreferenceConstants.EDITOR_CTRL_ALT_SHIFT_HOVER, fHoverConfigs[7].fHoverId);
	}

	/**
	 * Correctly resizes the table so no phantom columns appear.
	 */
    private void configureTableResizing(final Composite parent, final Table table, final TableColumn column1, final TableColumn column2) {
        parent.addControlListener(new ControlAdapter() {
            public void controlResized(ControlEvent e) {
                Rectangle area= parent.getClientArea();
                Point preferredSize= table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                int width= area.width - 2 * table.getBorderWidth();
                if (preferredSize.y > area.height) {
                    // Subtract the scrollbar width from the total column width
                    // if a vertical scrollbar will be required
                    Point vBarSize = table.getVerticalBar().getSize();
                    width -= vBarSize.x;
                }
                Point oldSize= table.getSize();
                if (oldSize.x > width) {
                    // table is getting smaller so make the columns
                    // smaller first and then resize the table to
                    // match the client area width
                    column1.setWidth(width/2);
                    column2.setWidth(width - column1.getWidth());
                    table.setSize(width, area.height);
                } else {
                    // table is getting bigger so make the table
                    // bigger first and then make the columns wider
                    // to match the client area width
                    table.setSize(width, area.height);
                    column1.setWidth(width / 2);
                    column2.setWidth(width - column1.getWidth());
                 }
            }
        });
    }

	private boolean isDefaultRowSelected() {
		return fTableViewer.getTable().getSelectionIndex() == 0;
	}

	private String[] getLabelsForHoverCombo() {
		int additionalItemCount= 1;
		if (!isDefaultRowSelected())
			additionalItemCount++;

		String[] labels= new String[getContributedHovers().size() + additionalItemCount];
		
		if (!isDefaultRowSelected())
			labels[0]= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.defaultHover"); //$NON-NLS-1$

		labels[additionalItemCount - 1]= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.noHoverConfigured"); //$NON-NLS-1$

		for (int i= additionalItemCount; i < labels.length; i++)
			labels[i]= ((JavaEditorTextHoverDescriptor)getContributedHovers().get(i - additionalItemCount)).getLabel();
		
		return labels;
	}
}
