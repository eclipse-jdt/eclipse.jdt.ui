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
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;


/**
 * Configures Java Editor hover preferences.
 * 
 * @since 2.1
 */
public class JavaEditorHoverConfigurationBlock {

	private final static String NO_HOVER_CONFIGURED_ID= "noHoverConfiguredId"; //$NON-NLS-1$
	private final static int NO_HOVER_CONFIGURED_INDEX= -1;
	private final static String DEFAULT_HOVER_CONFIGURED_ID= "defaultHoverConfiguredId"; //$NON-NLS-1$
	private final static int DEFAULT_HOVER_CONFIGURED_INDEX= -2;
	private final static String MODIFIER= "modifier"; //$NON-NLS-1$	
	private final static String HOVER= "hover"; //$NON-NLS-1$	
	
	// Preference store constants
	final static String JAVA_EDITOR_HOVER= "JavaEditorHoverConfigurationBlock"; //$NON-NLS-1$
	final static String DEFAULT_HOVER= JAVA_EDITOR_HOVER + ".defaultHover"; //$NON-NLS-1$
	final static String NONE_HOVER= JAVA_EDITOR_HOVER + ".noneHover"; //$NON-NLS-1$
	final static String CTRL_HOVER= JAVA_EDITOR_HOVER + "ctrlHover"; //$NON-NLS-1$
	final static String SHIFT_HOVER= JAVA_EDITOR_HOVER + ".shiftHover"; //$NON-NLS-1$
	final static String CTRL_ALT_HOVER= JAVA_EDITOR_HOVER + "ctrlAltHover"; //$NON-NLS-1$
	final static String CTRL_ALT_SHIFT_HOVER= JAVA_EDITOR_HOVER + "ctrlAltShiftHover"; //$NON-NLS-1$
	final static String CTRL_SHIFT_HOVER= JAVA_EDITOR_HOVER + ".ctrlShiftHover"; //$NON-NLS-1$
	final static String ALT_SHIFT_HOVER= JAVA_EDITOR_HOVER + ".altShiftHover"; //$NON-NLS-1$


	private class HoverConfig {
		
		private String fModifier;
		private int fStateMask;
		private int fHoverIndex;
		private String fHoverId;

		private HoverConfig(String modifier, int stateMask, String hoverId) {
			fModifier= modifier;
			fStateMask= stateMask;
			
			// Find index
			int i= 0;
			Iterator iter= fContributedHovers.iterator();
			while (iter.hasNext()) {
				if (hoverId.equals(((JavaEditorTextHoverDescriptor)iter.next()).getId()))
					break;
				i++;
			}
			if (i < fContributedHovers.size())
				setHoverIndex(i);
			else
				setHoverIndex(NO_HOVER_CONFIGURED_INDEX);
		}
		
		private void setHoverIndex(int hoverIndex) {
			fHoverIndex= hoverIndex;
			if (hoverIndex != NO_HOVER_CONFIGURED_INDEX)	
				fHoverId= ((JavaEditorTextHoverDescriptor)fContributedHovers.get(hoverIndex)).getId();
			else 
				fHoverId= NO_HOVER_CONFIGURED_ID;
		}
	}

	private class HoverConfigLabelProvider extends LabelProvider implements ITableLabelProvider {
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
						return ((JavaEditorTextHoverDescriptor)fContributedHovers.get(config.fHoverIndex)).getLabel();
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
	private List fContributedHovers;

	public JavaEditorHoverConfigurationBlock(IPreferenceStore store) {
		Assert.isNotNull(store);
		fStore= store;
		fContributedHovers= JavaEditorTextHoverDescriptor.getContributedHovers();
	}

	public static IJavaEditorTextHover getTextHover(int stateMask) {
		String key= null;
		switch (stateMask) {
			case ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK:
				// no support for default hover yet
				return null;
//				key= DEFAULT_HOVER;
//				break;
			case SWT.NONE:
				key= NONE_HOVER;
				break;
			case SWT.CTRL:
				key= CTRL_HOVER;
				break;
			case SWT.SHIFT:
				key= SHIFT_HOVER;
				break;
			case SWT.CTRL | SWT.ALT:
				key= CTRL_ALT_HOVER;
				break;
			case SWT.CTRL | SWT.SHIFT:
				key= CTRL_SHIFT_HOVER;
				break;
			case SWT.CTRL | SWT.ALT | SWT.SHIFT:
				key= CTRL_ALT_SHIFT_HOVER;
				break;
			case SWT.ALT | SWT.SHIFT:
				key= ALT_SHIFT_HOVER;
				break;
			default :
				return null;
		}
		Iterator iter= JavaEditorTextHoverDescriptor.getContributedHovers().iterator();
		String id= JavaPlugin.getDefault().getPreferenceStore().getString(key);
		if (DEFAULT_HOVER_CONFIGURED_ID.equals(id))
			id= JavaPlugin.getDefault().getPreferenceStore().getString(id);
		if (id == null)
			return null;
		while (iter.hasNext()) {
			JavaEditorTextHoverDescriptor hoverDescriptor= (JavaEditorTextHoverDescriptor)iter.next();
			if (id.equals(hoverDescriptor.getId()))
				return hoverDescriptor.createTextHover();
		}
		return null;
	}

	public static boolean isAffectedBy(String property) {
		return	DEFAULT_HOVER.equals(property)
			|| NONE_HOVER.equals(property)
			|| CTRL_HOVER.equals(property)
			|| SHIFT_HOVER.equals(property)
			|| CTRL_ALT_HOVER.equals(property)
			|| CTRL_SHIFT_HOVER.equals(property)
			|| CTRL_ALT_SHIFT_HOVER.equals(property)
			|| ALT_SHIFT_HOVER.equals(property);
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
		column1.setText(JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.tableColumn.stateMask")); //$NON-NLS-1$

		TableColumn column2= new TableColumn(table, SWT.NONE);
		column2.setText(JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.tableColumn.hover")); //$NON-NLS-1$

		fTableViewer= new TableViewer(table);

		fTableViewer.setLabelProvider(new HoverConfigLabelProvider());
		fTableViewer.setContentProvider(new HoverConfigContentProvider());


		// Setup cell editing support
		ComboBoxCellEditor comboBoxCellEditor= new ComboBoxCellEditor(table, getLabelsForHoverCombo(), SWT.READ_ONLY);
		fTableViewer.setCellEditors(new CellEditor [] {null, comboBoxCellEditor});

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
		fHoverConfigs= new HoverConfig[7];
		
//		String label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.defaultHover"); //$NON-NLS-1$
//		fHoverConfigs[0]= new HoverConfig(label, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK, fStore.getString(DEFAULT_HOVER));

		String label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.none"); //$NON-NLS-1$
		fHoverConfigs[0]= new HoverConfig(label, SWT.NONE, fStore.getString(NONE_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.ctrl"); //$NON-NLS-1$		
		fHoverConfigs[1]= new HoverConfig(label, SWT.CTRL, fStore.getString(CTRL_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.shift"); //$NON-NLS-1$		
		fHoverConfigs[2]= new HoverConfig(label, SWT.SHIFT, fStore.getString(SHIFT_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.ctrlShift"); //$NON-NLS-1$		
		fHoverConfigs[3]= new HoverConfig(label, SWT.CTRL | SWT.SHIFT, fStore.getString(CTRL_SHIFT_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.ctrlAlt"); //$NON-NLS-1$		
		fHoverConfigs[4]= new HoverConfig(label, SWT.CTRL | SWT.ALT, fStore.getString(CTRL_ALT_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.altShift"); //$NON-NLS-1$		
		fHoverConfigs[5]= new HoverConfig(label, SWT.ALT | SWT.SHIFT, fStore.getString(ALT_SHIFT_HOVER));

		label= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.modifier.ctrlAltShift"); //$NON-NLS-1$		
		fHoverConfigs[6]= new HoverConfig(label, SWT.CTRL | SWT.ALT | SWT.SHIFT, fStore.getString(CTRL_ALT_SHIFT_HOVER));

		fTableViewer.setInput(fHoverConfigs);
	}

	void initializeFields() {
		setTableInput();
	}

	void performOk() {
//		fStore.setValue(DEFAULT_HOVER, fHoverConfigs[0].fHoverId);
		fStore.setValue(NONE_HOVER, fHoverConfigs[0].fHoverId);
		fStore.setValue(CTRL_HOVER, fHoverConfigs[1].fHoverId);
		fStore.setValue(SHIFT_HOVER, fHoverConfigs[2].fHoverId);
		fStore.setValue(CTRL_SHIFT_HOVER, fHoverConfigs[3].fHoverId);
		fStore.setValue(CTRL_ALT_HOVER, fHoverConfigs[4].fHoverId);
		fStore.setValue(ALT_SHIFT_HOVER, fHoverConfigs[5].fHoverId);
		fStore.setValue(CTRL_ALT_SHIFT_HOVER, fHoverConfigs[6].fHoverId);		
	}

	static void initDefaults(IPreferenceStore store) {
		store.setDefault(DEFAULT_HOVER, "org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover"); //$NON-NLS-1$
		store.setDefault(NONE_HOVER, "org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover"); //$NON-NLS-1$
		store.setDefault(CTRL_HOVER, "org.eclipse.jdt.internal.ui.text.java.hover.JavaSourceHover"); //$NON-NLS-1$
		store.setDefault(SHIFT_HOVER, "org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover"); //$NON-NLS-1$
		store.setDefault(CTRL_SHIFT_HOVER, "org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover"); //$NON-NLS-1$
		store.setDefault(CTRL_ALT_HOVER, "org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover"); //$NON-NLS-1$
		store.setDefault(ALT_SHIFT_HOVER, "org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover"); //$NON-NLS-1$
		store.setDefault(CTRL_ALT_SHIFT_HOVER, "org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover"); //$NON-NLS-1$
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

	private String[] getLabelsForHoverCombo() {
		String[] labels= new String[fContributedHovers.size() + 1];
		labels[0]= JavaUIMessages.getString("JavaEditorHoverConfigurationBlock.noHoverConfigured"); //$NON-NLS-1$
		for (int i= 1; i < labels.length; i++)
			labels[i]= ((JavaEditorTextHoverDescriptor)fContributedHovers.get(i - 1)).getLabel();
		
		return labels;
	}
}
