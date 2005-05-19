/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.jdt.internal.ui.dialogs;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.util.TypeInfo;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetFilterActionGroup;

public class TypeSelectionComponent extends Composite {
	
	private IDialogSettings fSettings;
	
	private ToolBar fToolBar;
	private ToolItem fToolItem;
	private MenuManager fMenuManager;
	private WorkingSetFilterActionGroup fFilterActionGroup;
	
	private Text fFilter;
	private String fInitialFilterText;
	private IJavaSearchScope fScope;
	private TypeInfoViewer fViewer;
	
	public static final int NONE= 0;
	public static final int CARET_BEGINNING= 1;
	public static final int FULL_SELECTION= 2;
	
	private static final String DIALOG_SETTINGS= "org.eclipse.jdt.internal.ui.dialogs.TypeSelectionComponent"; //$NON-NLS-1$
	private static final String SELECTION_QUALIFICATION= "selection_qualification"; //$NON-NLS-1$
	private static final String WORKINGS_SET_SETTINGS= "workingset_settings"; //$NON-NLS-1$
	
	private class QualifyAction extends Action {
		private boolean fFullyQualified;
		public QualifyAction(String text, boolean full) {
			super(text, IAction.AS_RADIO_BUTTON);
			fFullyQualified= full;
		}
		public void run() {
			fViewer.setQualificationStyle(fFullyQualified);
			fSettings.put(SELECTION_QUALIFICATION, fFullyQualified);
		}
	}
	
	public TypeSelectionComponent(Composite parent, int style, String message, boolean multi, IJavaSearchScope scope, int elementKind, String initialFilter) {
		super(parent, style);
		setFont(parent.getFont());
		fScope= scope;
		fInitialFilterText= initialFilter;
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		fSettings= settings.getSection(DIALOG_SETTINGS);
		if (fSettings == null) {
			fSettings= new DialogSettings(DIALOG_SETTINGS);
			settings.addSection(fSettings);
		}
		createContent(message, multi, elementKind);
	}
	
	public TypeInfo[] getSelection() {
		return fViewer.getSelection();
	}
	
	private void createContent(String message, boolean multi, int elementKind) {
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0; layout.marginHeight= 0;
		setLayout(layout);
		Font font= getFont();
		
		Control header= createHeader(this, font, message);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		header.setLayoutData(gd);
		
		fFilter= new Text(this, SWT.BORDER | SWT.FLAT);
		fFilter.setFont(font);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		fFilter.setLayoutData(gd);
		fFilter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				patternChanged((Text)e.widget);
			}
		});
		fFilter.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					fViewer.setFocus();
				}
			}
		});
		Label label= new Label(this, SWT.NONE);
		label.setFont(font);
		label.setText(JavaUIMessages.TypeSelectionComponent_label);
		label= new Label(this, SWT.RIGHT);
		label.setFont(font);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);
		fViewer= new TypeInfoViewer(this, multi ? SWT.MULTI : SWT.NONE, label, fScope, elementKind, fInitialFilterText);
		gd= new GridData(GridData.FILL_BOTH);
		PixelConverter converter= new PixelConverter(fViewer.getTable());
		gd.widthHint= converter.convertWidthInCharsToPixels(70);
		gd.heightHint= SWTUtil.getTableHeightHint(fViewer.getTable(), 10);
		gd.horizontalSpan= 2;
		fViewer.getTable().setLayoutData(gd);
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				saveFilterState();
			}
		});
	}

	public void addSelectionListener(SelectionListener listener) {
		fViewer.getTable().addSelectionListener(listener);
	}
	
	public void populate(int selectionMode) {
		boolean fully= fSettings.getBoolean(SELECTION_QUALIFICATION);
		fViewer.setQualificationStyle(fully);
		if (fInitialFilterText != null) {
			fFilter.setText(fInitialFilterText);
			switch(selectionMode) {
				case CARET_BEGINNING:
					fFilter.setSelection(0, 0);
					break;
				case FULL_SELECTION:
					fFilter.setSelection(0, fInitialFilterText.length());
					break;
			}
		}
		fViewer.reset();
		fFilter.setFocus();
	}
	
	private void patternChanged(Text text) {
		fViewer.setSearchPattern(text.getText());
	}
	
	private Control createHeader(Composite parent, Font font, String message) {
		Composite header= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0; layout.marginHeight= 0;
		header.setLayout(layout);
		header.setFont(font);
		Label label= new Label(header, SWT.NONE);
		label.setText(message);
		label.setFont(font);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);
		
		createViewMenu(header);
		return header;
	}
	
	private void createViewMenu(Composite parent) {
		fToolBar= new ToolBar(parent, SWT.FLAT);
		fToolItem= new ToolItem(fToolBar, SWT.PUSH, 0);

		GridData data= new GridData();
		data.horizontalAlignment= GridData.END;
		fToolBar.setLayoutData(data);

		fToolItem.setImage(JavaPluginImages.get(JavaPluginImages.IMG_ELCL_VIEW_MENU));
		fToolItem.setDisabledImage(JavaPluginImages.get(JavaPluginImages.IMG_DLCL_VIEW_MENU));
		fToolItem.setToolTipText(JavaUIMessages.TypeSelectionComponent_menu);
		fToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showViewMenu();
			}
		});
		
		fMenuManager= new MenuManager();
		fillViewMenu(fMenuManager);

		// ICommandService commandService= (ICommandService)PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		// IHandlerService handlerService= (IHandlerService)PlatformUI.getWorkbench().getAdapter(IHandlerService.class);
	}
	
	private void showViewMenu() {
		Menu menu = fMenuManager.createContextMenu(getShell());
		Rectangle bounds = fToolItem.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = fToolBar.toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}
	
	private void fillViewMenu(IMenuManager viewMenu) {
		QualifyAction full= new QualifyAction(JavaUIMessages.TypeSelectionComponent_fully_qualify, true);
		QualifyAction pack= new QualifyAction(JavaUIMessages.TypeSelectionComponent_package_qualify, false);
		boolean qualification= fSettings.getBoolean(SELECTION_QUALIFICATION);
		if (qualification) {
			full.setChecked(true);
		} else {
			pack.setChecked(true);
		}
		viewMenu.add(full);
		viewMenu.add(pack);
		if (fScope == null) {
			fFilterActionGroup= new WorkingSetFilterActionGroup(getShell(),
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						IWorkingSet ws= (IWorkingSet)event.getNewValue();
						IJavaSearchScope scope;
						if (ws == null) {
							scope= SearchEngine.createWorkspaceScope();
						} else {
							scope= JavaSearchScopeFactory.getInstance().createJavaSearchScope(ws, true);
						}
						fViewer.setSearchScope(scope, true);
					}
				});
			String setting= fSettings.get(WORKINGS_SET_SETTINGS);
			if (setting != null) {
				try {
					IMemento memento= XMLMemento.createReadRoot(new StringReader(setting));
					fFilterActionGroup.restoreState(memento);
				} catch (WorkbenchException e) {
				}
			}
			IWorkingSet ws= fFilterActionGroup.getWorkingSet();
			if (ws != null) {
				fScope= JavaSearchScopeFactory.getInstance().createJavaSearchScope(ws, true);
			} else {
				fScope= SearchEngine.createWorkspaceScope();
			}
			fFilterActionGroup.fillViewMenu(viewMenu);
		}
	}

	private void saveFilterState() {
		if (fFilterActionGroup != null) {
			XMLMemento memento= XMLMemento.createWriteRoot("workingSet"); //$NON-NLS-1$
			fFilterActionGroup.saveState(memento);
			fFilterActionGroup.dispose();
			StringWriter writer= new StringWriter();
			try {
				memento.save(writer);
				fSettings.put(WORKINGS_SET_SETTINGS, writer.getBuffer().toString());
			} catch (IOException e) {
				// don't do anythiung. Simply don't store the settings
			}
		}
	}
}