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

import java.util.ArrayList;
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
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.internal.ui.JavaUIMessages;


/**
 * @author dmegert
 *
 * To change this generated comment edit the template variable "typecomment":
 * Workbench>Preferences>Java>Templates.
 */
public class JavaEditorHoverPreferenceSubPage {

	private final static List fgJavaHovers= JavaEditorTextHoverDescriptor.getContributedHovers();
	private final static String NO_HOVER_CONFIGURED_ID= "noHoverConfiguredId"; //$NON-NLS-1$
	private final static int NO_HOVER_CONFIGURED_INDEX= -1;
	private final static String MODIFIER= "modifier"; //$NON-NLS-1$	
	private final static String HOVER= "hover"; //$NON-NLS-1$	
	
	// Preference store constants
	private final static String JavaEditor_DEFAULT_HOVER= "defaultHover"; //$NON-NLS-1$
	private final static String JavaEditor_CTRL_HOVER= "ctrlHover"; //$NON-NLS-1$
	private final static String JavaEditor_CTRL_ALT_HOVER= "ctrlAltHover"; //$NON-NLS-1$
	private final static String JavaEditor_CTRL_SHIFT_HOVER= "ctrlShiftHover"; //$NON-NLS-1$	
	private final static String JavaEditor_ALT_HOVER= "altHover"; //$NON-NLS-1$
	private final static String JavaEditor_SHIFT_HOVER= "shiftHover"; //$NON-NLS-1$
	private final static String JavaEditor_ALT_SHIFT_HOVER= "altShiftHover"; //$NON-NLS-1$


	private static class HoverConfig {
		
		private String fModifier;
		private int fStateMask;
		private int fHoverIndex;
		private String fHoverId;

		private HoverConfig(String modifier, int stateMask, String hoverId) {
			fModifier= modifier;
			fHoverId= hoverId;
			fStateMask= stateMask;
			
			// Find index
			int i= 0;
			Iterator iter= fgJavaHovers.iterator();
			while (iter.hasNext()) {
				if (hoverId.equals(((JavaEditorTextHoverDescriptor)iter.next()).getId()))
					break;
				i++;
			}
			if (i < fgJavaHovers.size())
				setHoverIndex(i);
			else
				setHoverIndex(NO_HOVER_CONFIGURED_INDEX); // no hover configured
		}
		
		private void setHoverIndex(int hoverIndex) {
			fHoverIndex= hoverIndex;
		}
	}

	private static class HoverConfigLabelProvider extends LabelProvider implements ITableLabelProvider {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			HoverConfig config= (HoverConfig)element;
			switch (columnIndex) {
				case 0:
					return config.fModifier;
				case 1:
					if (config.fHoverIndex == NO_HOVER_CONFIGURED_INDEX)
						return JavaUIMessages.getString("JavaEditorHoverPreferenceSubPage.noHoverConfigured"); //$NON-NLS-1$
					else
						return ((JavaEditorTextHoverDescriptor)fgJavaHovers.get(config.fHoverIndex)).getLabel();
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

	static class HoverConfigContentProvider implements IStructuredContentProvider {
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
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}
	}

	
	private IPreferenceStore fStore;
	private HoverConfig[] fHoverConfigs;

	public JavaEditorHoverPreferenceSubPage(IPreferenceStore store) {
		Assert.isNotNull(store);
		fStore= store;
	}

	/**
	 * Creates page for hover preferences.
	 */
	public Control createControl(Composite parent) {

		Composite hoverComposite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 1;
		hoverComposite.setLayout(layout);

		Table table= new Table(hoverComposite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		
		GridData data= new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);
				
		table.setHeaderVisible(true);
		table.setLinesVisible(true);		

		TableLayout tableLayout= new TableLayout();
		table.setLayout(tableLayout);

		TableColumn column1= new TableColumn(table, SWT.NONE);		
		column1.setText(JavaUIMessages.getString("JavaEditorHoverPreferenceSubPage.tableColumn.stateMask")); //$NON-NLS-1$

		TableColumn column2= new TableColumn(table, SWT.NONE);
		column2.setText(JavaUIMessages.getString("JavaEditorHoverPreferenceSubPage.tableColumn.hover")); //$NON-NLS-1$
		
		final TableViewer tableViewer= new TableViewer(table);

		tableViewer.setLabelProvider(new HoverConfigLabelProvider());
		tableViewer.setContentProvider(new HoverConfigContentProvider());


		// Setup cell editing support
		ComboBoxCellEditor comboEditor= new ComboBoxCellEditor(table, getLabelsForHoverCombo(), SWT.READ_ONLY);
		tableViewer.setCellEditors(new CellEditor [] {null, comboEditor});

		ICellModifier cellModifier = new ICellModifier() {
			public Object getValue(Object element, String property) {
				if (HOVER.equals(property)) {
					return new Integer(((HoverConfig)element).fHoverIndex + 1);
				}
				return null;
			}
	
			public boolean canModify(Object element, String property) {
				return HOVER.equals(property);
			}
	
			public void modify(Object element, String property, Object value) {
				if (HOVER.equals(property)) {
					HoverConfig hoverConfig= (HoverConfig)((Item)element).getData();
					hoverConfig.setHoverIndex(((Integer)value).intValue() - 1);
					tableViewer.refresh(hoverConfig);
				}
			}
		};
		tableViewer.setCellModifier(cellModifier);
		tableViewer.setColumnProperties(new String[] {MODIFIER, HOVER});

		fHoverConfigs= new HoverConfig[6];
		fHoverConfigs[0]= new HoverConfig("None", SWT.NONE, fStore.getString(JavaEditor_DEFAULT_HOVER));
		fHoverConfigs[1]= new HoverConfig("Ctrl", SWT.CTRL, fStore.getString(JavaEditor_CTRL_HOVER));
		fHoverConfigs[2]= new HoverConfig("Shift", SWT.SHIFT, fStore.getString(JavaEditor_SHIFT_HOVER));
		fHoverConfigs[3]= new HoverConfig("Ctrl + Shift", SWT.CTRL | SWT.SHIFT, fStore.getString(JavaEditor_CTRL_SHIFT_HOVER));
		fHoverConfigs[4]= new HoverConfig("Ctrl + Alt", SWT.CTRL | SWT.ALT, fStore.getString(JavaEditor_CTRL_ALT_HOVER));
		fHoverConfigs[5]= new HoverConfig("Alt + Shift", SWT.ALT | SWT.SHIFT, fStore.getString(JavaEditor_ALT_SHIFT_HOVER));
		tableViewer.setInput(fHoverConfigs);

        configureTableResizing(hoverComposite, table, column1, column2);

		return hoverComposite;
	}

	public void performOk() {
		fStore.setValue(JavaEditor_DEFAULT_HOVER, fHoverConfigs[0].fHoverId);
		fStore.setValue(JavaEditor_CTRL_HOVER, fHoverConfigs[1].fHoverId);
		fStore.setValue(JavaEditor_SHIFT_HOVER, fHoverConfigs[2].fHoverId);
		fStore.setValue(JavaEditor_CTRL_SHIFT_HOVER, fHoverConfigs[3].fHoverId);
		fStore.setValue(JavaEditor_CTRL_ALT_HOVER,  fHoverConfigs[4].fHoverId);
		fStore.setValue(JavaEditor_ALT_SHIFT_HOVER,  fHoverConfigs[5].fHoverId);
	}

	/**
	 * Correctly resizes the table so no phantom columns appear
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
    
	private static String[] getLabelsForHoverCombo() {
		String[] labels= new String[fgJavaHovers.size() + 1];
		labels[0]= JavaUIMessages.getString("JavaEditorHoverPreferenceSubPage.noHoverConfigured"); //$NON-NLS-1$
		for (int i= 1; i < labels.length; i++)
			labels[i]= ((JavaEditorTextHoverDescriptor)fgJavaHovers.get(i - 1)).getLabel();
		
		return labels;
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(JavaEditor_DEFAULT_HOVER, "org.eclipse.jdt.internal.ui.text.java.hover.JavaTypeHover"); //$NON-NLS-1$
		store.setDefault(JavaEditor_CTRL_HOVER, "org.eclipse.jdt.internal.ui.text.java.hover.JavaSourceHover"); //$NON-NLS-1$
		store.setDefault(JavaEditor_SHIFT_HOVER, NO_HOVER_CONFIGURED_ID);
		store.setDefault(JavaEditor_CTRL_SHIFT_HOVER, NO_HOVER_CONFIGURED_ID);
		store.setDefault(JavaEditor_CTRL_ALT_HOVER, NO_HOVER_CONFIGURED_ID);
		store.setDefault(JavaEditor_ALT_HOVER, NO_HOVER_CONFIGURED_ID);
		store.setDefault(JavaEditor_ALT_SHIFT_HOVER, NO_HOVER_CONFIGURED_ID);
	}
}
