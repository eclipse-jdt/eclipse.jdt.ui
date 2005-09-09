/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.jface.text.Assert;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.text.java.CompletionProposalCategory;
import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerRegistry;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * 	
 * @since 3.2
 */
final class CodeAssistConfigurationBlockInProgress extends OptionsConfigurationBlock {

	private static final int LIMIT= 0xffff;
	private static final String COLON= ":"; //$NON-NLS-1$
	private static final String SEPARATOR= "\0"; //$NON-NLS-1$
	private static final String DASH= "-"; //$NON-NLS-1$
	
	private static final Key PREF_EXCLUDED_CATEGORIES= getJDTUIKey(PreferenceConstants.CODEASSIST_EXCLUDED_CATEGORIES);
	private static final Key PREF_CATEGORY_ORDER= getJDTUIKey(PreferenceConstants.CODEASSIST_CATEGORY_ORDER);
	
	private static Key[] getAllKeys() {
		return new Key[] {
				PREF_EXCLUDED_CATEGORIES,
				PREF_CATEGORY_ORDER,
		};
	}

	private final class ComputerLabelProvider extends LabelProvider implements ITableLabelProvider {

		/*
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			return ((ModelElement) element).getColumnImage(columnIndex);
		}

		/*
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			return ((ModelElement) element).getColumnLabel(columnIndex);
		}
	}

	private final Comparator fCategoryComparator= new Comparator() {
		private int getRank(Object o) {
			return ((ModelElement) o).getRank();
		}

		public int compare(Object o1, Object o2) {
			return getRank(o1) - getRank(o2);
		}
	};
	
	private static abstract class ModelElement {
		String getColumnLabel(int column) {
			return null;
		}
		abstract int getRank();
		Image getColumnImage(int column) {
			return null;
		}
		boolean getIncluded() {
			return false;
		}
		boolean isRealCategory() {
			return false;
		}
		void setIncluded(boolean selection) {
		}
		String getId() {
			return null;
		}
		void update() {
		}
		void setLabel(String label) {
		}
	}
	private final class Category extends ModelElement {
		private final CompletionProposalCategory fCategory;
		private final Command fCommand;
		private final IParameter fParam;
		Category(CompletionProposalCategory category) {
			fCategory= category;
			ICommandService commandSvc= (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
			fCommand= commandSvc.getCommand("org.eclipse.jdt.ui.specific_content_assist.command"); //$NON-NLS-1$
			IParameter type;
			try {
				type= fCommand.getParameters()[0];
			} catch (NotDefinedException x) {
				Assert.isTrue(false);
				type= null;
			}
			fParam= type;
		}
		Image getColumnImage(int column) {
			if (column == 0)
				return CodeAssistConfigurationBlockInProgress.this.getImage(fCategory.getImageDescriptor());
			return super.getColumnImage(column);
		}
		String getColumnLabel(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return fCategory.getName().replaceAll("&", ""); //$NON-NLS-1$ //$NON-NLS-2$
				case 1:
					final Parameterization[] params= { new Parameterization(fParam, fCategory.getId()) };
					final ParameterizedCommand pCmd= new ParameterizedCommand(fCommand, params);
					String key= getKeyboardShortcut(pCmd);
					return key;
			}
			return super.getColumnLabel(columnIndex);
		}
		boolean isRealCategory() {
			return true;
		}
		boolean getIncluded() {
			return readInclusionPreference(fCategory);
		}
		void setIncluded(boolean included) {
			writeInclusionPreference(this, included);
		}
		String getId() {
			return fCategory.getId();
		}
		int getRank() {
			return readOrderPreference(fCategory);
		}
		void update() {
			fCategory.setIncluded(getIncluded());
			int rank= getRank();
			fCategory.setSortOrder(rank);
			fCategory.setSeparateCommand(rank < LIMIT);
		}
	}
	private static final class Separator extends ModelElement {
		private String fLabel= PreferencesMessages.CodeAssistConfigurationBlockInProgress_separator;
		String getColumnLabel(int index) {
			if (index == 0)
				return fLabel;
			return super.getColumnLabel(index);
		}
		int getRank() {
			return LIMIT;
		}
		void setLabel(String label) {
			fLabel= label;
		}
	}

	/** element type: {@link ModelElement}. */
	private final List fModel;
	private final Map fImages= new HashMap();

	private TableViewer fViewer;
	private Button fInclusionButton;
	private Button fUpButton;
	private Button fDownButton;
	private final Separator fSeparator= new Separator();
	
	CodeAssistConfigurationBlockInProgress(IStatusChangeListener statusListener, IWorkbenchPreferenceContainer container) {
		super(statusListener, null, getAllKeys(), container);
		fModel= fillModel();
	}

	private List fillModel() {
		CompletionProposalComputerRegistry registry= CompletionProposalComputerRegistry.getDefault();
		
		List categories= registry.getProposalCategories();
		List model= new ArrayList();
		for (Iterator it= categories.iterator(); it.hasNext();) {
			CompletionProposalCategory category= (CompletionProposalCategory) it.next();
			if (category.hasComputers()) {
				model.add(new Category(category));
			}
		}
		model.add(fSeparator);
		return model;
	}		

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		
		ScrolledComposite scrolled= new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		
		final Composite composite= new Composite(scrolled, SWT.NONE);
		RowLayout layout= createRowLayout(SWT.VERTICAL);
		layout.spacing= 20;
		layout.fill= true;
		composite.setLayout(layout);
		
		final ICommandService commandSvc= (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		final Command command= commandSvc.getCommand(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		ParameterizedCommand pCmd= new ParameterizedCommand(command, null);
		String key= getKeyboardShortcut(pCmd);
		if (key == null)
			key= PreferencesMessages.CodeAssistConfigurationBlockInProgress_no_shortcut;

		new Label(composite, SWT.NONE | SWT.WRAP).setText(MessageFormat.format(PreferencesMessages.CodeAssistConfigurationBlockInProgress_computer_description, new Object[] { key }));
		
		createControls(composite);
		
		Link link= new Link(composite, SWT.NONE | SWT.WRAP);
		link.setText(PreferencesMessages.CodeAssistConfigurationBlockInProgress_computer_link);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(getShell(), e.text, null, null);
			}
		});
		// limit the size of the Link as it would take all it can get
		link.setLayoutData(new RowData(300, SWT.DEFAULT));
		
		updateControls();
		if (fModel.size() > 0) {
			fViewer.getTable().select(0);
			handleTableSelection();
		}
		
		scrolled.setContent(composite);
		scrolled.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		return scrolled;
	}

	private void createControls(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		RowLayout layout= createRowLayout(SWT.HORIZONTAL);
		layout.fill= true;
		composite.setLayout(layout);
		
		createViewer(composite);
		createButtonList(composite);
	}

	private void createViewer(Composite composite) {
		fViewer= new TableViewer(composite, SWT.SINGLE | SWT.BORDER);
		Table table= fViewer.getTable();
		table.setHeaderVisible(false);
		table.setLinesVisible(false);
		
		TableColumn nameColumn= new TableColumn(table, SWT.NONE);
		TableColumn keyColumn= new TableColumn(table, SWT.NONE);
		
		fViewer.setContentProvider(new ArrayContentProvider());
		
		ComputerLabelProvider labelProvider= new ComputerLabelProvider();
		fViewer.setLabelProvider(labelProvider);
		fViewer.setInput(fModel);
		
		final int ICON_AND_CHECKBOX_WITH= 20;
		int minNameWidth= 100;
		int minKeyWidth= 5;
		for (int i= 0; i < fModel.size(); i++) {
			minNameWidth= Math.max(minNameWidth, computeWidth(table, labelProvider.getColumnText(fModel.get(i), 0)) + ICON_AND_CHECKBOX_WITH);
			minKeyWidth= Math.max(minKeyWidth, computeWidth(table, labelProvider.getColumnText(fModel.get(i), 1)));
		}
		
		String separatorLabel= PreferencesMessages.CodeAssistConfigurationBlockInProgress_separator;
		int baseLabelWidth= computeWidth(table, separatorLabel);
		StringBuffer buf= new StringBuffer(separatorLabel);
		int dashWidth= computeWidth(table, DASH);
		int additionalDashes= (minNameWidth - baseLabelWidth) / dashWidth;
		for (int i= 0; i < additionalDashes; i++) {
			buf.insert(0, DASH);
			buf.append(DASH);
		}
		fSeparator.setLabel(buf.toString());
		
		nameColumn.setWidth(minNameWidth);
		keyColumn.setWidth(minKeyWidth);
		
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleTableSelection();
			}
		});
		
	}

	private void createButtonList(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		RowLayout layout= createRowLayout(SWT.VERTICAL);
		layout.spacing= 20;
		composite.setLayout(layout);
		
		createUpDownControls(composite);
		createButtons(composite);
	}

	private void createUpDownControls(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		RowLayout layout= createRowLayout(SWT.VERTICAL);
		layout.fill= true;
		composite.setLayout(layout);
		
		fUpButton= new Button(composite, SWT.PUSH | SWT.CENTER);
		fUpButton.setText(PreferencesMessages.CodeAssistConfigurationBlockInProgress_Up);
		fUpButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index= getSelectionIndex();
				if (index > 0) {
					Object item= fModel.remove(index);
					fModel.add(index - 1, item);
					fViewer.refresh();
					handleTableSelection();
					writeOrderPreference();
				}
			}		
		});
		fUpButton.setLayoutData(new RowData(SWTUtil.getButtonWidthHint(fUpButton), SWT.DEFAULT));
		
		fDownButton= new Button(composite, SWT.PUSH | SWT.CENTER);
		fDownButton.setText(PreferencesMessages.CodeAssistConfigurationBlockInProgress_Down);
		fDownButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index= getSelectionIndex();
				if (index < fModel.size() - 1) {
					Object item= fModel.remove(index);
					fModel.add(index + 1, item);
					fViewer.refresh();
					handleTableSelection();
					writeOrderPreference();
				}
			}		
		});
		fDownButton.setLayoutData(new RowData(SWTUtil.getButtonWidthHint(fDownButton), SWT.DEFAULT));
	}

	private void createButtons(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		RowLayout layout= createRowLayout(SWT.VERTICAL);
		layout.spacing= 10;
		composite.setLayout(layout);
		
		fInclusionButton= new Button(composite, SWT.CHECK);
		fInclusionButton.setText(PreferencesMessages.CodeAssistConfigurationBlockInProgress_include);
		fInclusionButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ModelElement item= getSelectedItem();
				if (item != null) {
					item.setIncluded(fInclusionButton.getSelection());
				}
			}

		});
	}
	
	private void handleTableSelection() {
		ModelElement item= getSelectedItem();
		if (item != null) {
			fInclusionButton.setEnabled(item.isRealCategory());
			fInclusionButton.setSelection(item.getIncluded());
			
			fUpButton.setEnabled(getSelectionIndex() > 0);
			fDownButton.setEnabled(getSelectionIndex() < fModel.size() - 1);
		} else {
			fInclusionButton.setEnabled(false);
			fUpButton.setEnabled(false);
			fDownButton.setEnabled(false);
		}
	}
	
	private void writeInclusionPreference(ModelElement changed, boolean value) {
		StringBuffer buf= new StringBuffer();
		for (Iterator it= fModel.iterator(); it.hasNext();) {
			ModelElement item= (ModelElement) it.next();
			if (item.isRealCategory()) {
				boolean included= changed == item ? value : item.getIncluded();
				if (!included)
					buf.append(item.getId() + SEPARATOR);
			}
		}
		
		String newValue= buf.toString();
		String oldValue= setValue(PREF_EXCLUDED_CATEGORIES, newValue);
		validateSettings(PREF_EXCLUDED_CATEGORIES, oldValue, newValue);
	}
	
	private void writeOrderPreference() {
		StringBuffer buf= new StringBuffer();
		int plus= 0;
		int i= 0;
		for (Iterator it= fModel.iterator(); it.hasNext(); i++) {
			ModelElement item= (ModelElement) it.next();
			if (item.isRealCategory()) {
				int rank= i + plus;
				buf.append(item.getId() + COLON + rank + SEPARATOR);
			} else {
				plus= LIMIT;
			}
		}
		
		String newValue= buf.toString();
		String oldValue= setValue(PREF_CATEGORY_ORDER, newValue);
		validateSettings(PREF_CATEGORY_ORDER, oldValue, newValue);
	}
	

	private boolean readInclusionPreference(CompletionProposalCategory cat) {
		String[] ids= getTokens(getValue(PREF_EXCLUDED_CATEGORIES), SEPARATOR);
		for (int i= 0; i < ids.length; i++) {
			if (ids[i].equals(cat.getId()))
				return false;
		}
		return true;
	}
	
	private int readOrderPreference(CompletionProposalCategory cat) {
		String[] sortOrderIds= getTokens(getValue(PREF_CATEGORY_ORDER), SEPARATOR);
		for (int i= 0; i < sortOrderIds.length; i++) {
			String[] idAndRank= getTokens(sortOrderIds[i], COLON);
			if (idAndRank[0].equals(cat.getId()))
				return Integer.parseInt(idAndRank[1]);
		}
		return LIMIT + 1;
	}
	
	private ModelElement getSelectedItem() {
		return (ModelElement) ((IStructuredSelection) fViewer.getSelection()).getFirstElement();
	}
	
	private int getSelectionIndex() {
		return fViewer.getTable().getSelectionIndex();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#updateControls()
	 */
	protected void updateControls() {
		super.updateControls();
		
		Collections.sort(fModel, fCategoryComparator);
		fViewer.refresh();
		handleTableSelection();
	}
	
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#processChanges(org.eclipse.ui.preferences.IWorkbenchPreferenceContainer)
	 */
	protected boolean processChanges(IWorkbenchPreferenceContainer container) {
		for (Iterator it= fModel.iterator(); it.hasNext();) {
			ModelElement item= (ModelElement) it.next();
			item.update();
		}
		
		return super.processChanges(container);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#validateSettings(org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock.Key, java.lang.String, java.lang.String)
	 */
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getFullBuildDialogStrings(boolean)
	 */
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		// no builds triggered by our settings
		return null;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#dispose()
	 */
	public void dispose() {
		for (Iterator it= fImages.values().iterator(); it.hasNext();) {
			Image image= (Image) it.next();
			image.dispose();
		}
		
		super.dispose();
	}

	private RowLayout createRowLayout(int type) {
		RowLayout layout= new RowLayout(type);
		layout.wrap= false;
		layout.marginHeight= 0;
		layout.marginTop= 0;
		layout.marginBottom= 0;
		layout.marginWidth= 0;
		layout.marginLeft= 0;
		layout.marginRight= 0;
		layout.spacing= 5;
		return layout;
	}

	private int computeWidth(Control control, String name) {
		if (name == null)
			return 0;
		GC gc= new GC(control.getDisplay());
		try {
			gc.setFont(JFaceResources.getDialogFont());
			return gc.stringExtent(name).x + 10;
		} finally {
			gc.dispose();
		}
	}

	private String getKeyboardShortcut(ParameterizedCommand command) {
		final IBindingService bindingSvc= (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		final Binding[] bindings= bindingSvc.getBindings();
		for (int i= 0; i < bindings.length; i++) {
			Binding binding= bindings[i];
			if (command.equals(binding.getParameterizedCommand())) {
				TriggerSequence triggers= binding.getTriggerSequence();
				return triggers.format();
			}
		}
		return null;
	}
	
	private Image getImage(ImageDescriptor imgDesc) {
		if (imgDesc == null)
			return null;
		
		Image img= (Image) fImages.get(imgDesc);
		if (img == null) {
			img= imgDesc.createImage(false);
			fImages.put(imgDesc, img);
		}
		return img;
	}

}
