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
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.jface.dialogs.Dialog;

/**
 * @author sib
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ModifyDialog extends Dialog {
	
	private final static String DIALOG_TITLE= "Code Formatter Settings";

	private final Map fWorkingValues;
	
		
	protected ModifyDialog(Shell parentShell, Map workingValues) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX );
		fWorkingValues= workingValues;
	}
	
	
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(DIALOG_TITLE);
	}
	
	
	protected Control createDialogArea(Composite parent) {
		
		GridData gd;
		
		final Composite composite= new Composite(parent, SWT.NONE);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(150);
		gd.heightHint= convertHeightInCharsToPixels(35);
		composite.setLayoutData(gd);
		composite.setLayout(new GridLayout(1, false));
		
		final TabFolder tabFolder= new TabFolder(composite, SWT.NONE);
		gd= new GridData(GridData.FILL_BOTH);
		tabFolder.setLayoutData(gd);
		tabFolder.setLayout(new FillLayout());

		addTabPage(tabFolder, "B&races", new BracesTabPage(fWorkingValues));
		addTabPage(tabFolder, "In&dentation", new IndentationTabPage(fWorkingValues));
		addTabPage(tabFolder, "Wh&ite Space", new WhiteSpaceTabPage(fWorkingValues));
		addTabPage(tabFolder, "Bla&nk Lines", new BlankLinesTabPage(fWorkingValues));
		addTabPage(tabFolder, "New &Lines", new NewLinesTabPage(fWorkingValues));
		addTabPage(tabFolder, "Con&trol Statements", new ControlStatementsTabPage(fWorkingValues));
		addTabPage(tabFolder, "Lin&e Wrapping", new LineWrappingTabPage(fWorkingValues));
		addTabPage(tabFolder, "Co&mments", new CommentsTabPage(fWorkingValues));
		addTabPage(tabFolder, "&Other", new GeneralSettingsTabPage(fWorkingValues));
		
		tabFolder.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {
				final TabItem t= (TabItem)e.item;
				final ModifyDialogTabPage page= (ModifyDialogTabPage)t.getData();
				page.updatePreview();
			}
		});
		return composite;
	}
	
	
	private final static void addTabPage(TabFolder tabFolder, String title, ModifyDialogTabPage tabPage) {
		final TabItem tabItem= new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(title);
		tabItem.setData(tabPage);
		tabItem.setControl(tabPage.createContents(tabFolder));
	}
	
	
}
