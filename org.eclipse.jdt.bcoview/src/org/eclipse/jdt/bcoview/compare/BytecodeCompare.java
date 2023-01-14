/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.compare;

import java.lang.reflect.Field;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;
import org.eclipse.jdt.bcoview.preferences.BCOConstants;
import org.eclipse.jdt.bcoview.ui.actions.DefaultToggleAction;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;

import org.eclipse.ui.IReusableEditor;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.structuremergeviewer.Differencer;

public class BytecodeCompare extends CompareEditorInput {

	/** Stores reference to the element displayed on the left side of the viewer. */
	protected TypedElement left;

	/** Stores reference to the element displayed on the right side of the viewer. */
	protected TypedElement right;

	/** Action used in compare view/bytecode view to toggle asmifier mode on/off */
	protected Action toggleAsmifierModeAction;

	/** Action used in compare view/bytecode view to hide/show line info. */
	protected Action hideLineInfoAction;

	/** Action used in compare view/bytecode view to hide/show local variables. */
	protected Action hideLocalsAction;

	protected Action hideStackMapAction;

	protected Action expandStackMapAction;

	protected IReusableEditor myEditor;

	public BytecodeCompare(final TypedElement left, final TypedElement right) {
		super(new CompareConfiguration());
		this.left = left;
		this.right = right;
		toggleAsmifierModeAction = new DefaultToggleAction(BCOConstants.DIFF_SHOW_ASMIFIER_CODE, false) {
			@Override
			public void run(final boolean newState) {
				toggleMode(BCOConstants.F_SHOW_ASMIFIER_CODE, newState, newState);
			}
		};

		hideLineInfoAction = new DefaultToggleAction(BCOConstants.DIFF_SHOW_LINE_INFO, false) {
			@Override
			public void run(final boolean newState) {
				toggleMode(BCOConstants.F_SHOW_LINE_INFO, newState, toggleAsmifierModeAction.isChecked());
			}
		};

		hideLocalsAction = new DefaultToggleAction(BCOConstants.DIFF_SHOW_VARIABLES, false) {
			@Override
			public void run(final boolean newState) {
				toggleMode(BCOConstants.F_SHOW_VARIABLES, newState, toggleAsmifierModeAction.isChecked());
			}
		};

		hideStackMapAction = new DefaultToggleAction(BCOConstants.DIFF_SHOW_STACKMAP, false) {
			@Override
			public void run(final boolean newState) {
				toggleMode(BCOConstants.F_SHOW_STACKMAP, newState, toggleAsmifierModeAction.isChecked());
			}
		};

		expandStackMapAction = new DefaultToggleAction(BCOConstants.DIFF_EXPAND_STACKMAP, false) {
			@Override
			public void run(final boolean newState) {
				toggleMode(BCOConstants.F_EXPAND_STACKMAP, newState, toggleAsmifierModeAction.isChecked());
			}
		};
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor) throws InterruptedException {
		if (right == null || left == null) {
			return null;
		}

		try {
			initLabels();
			Differencer differencer = new Differencer();
			monitor.beginTask("Bytecode Outline: comparing...", 30); //$NON-NLS-1$
			IProgressMonitor sub = SubMonitor.convert(monitor, 10);
			try {
				sub.beginTask("Bytecode Outline: comparing...", 100); //$NON-NLS-1$
				return differencer.findDifferences(false, sub, null, null, left, right);
			} finally {
				sub.done();
			}
		} catch (OperationCanceledException e) {
			throw new InterruptedException(e.getMessage());
		} finally {
			monitor.done();
		}
	}

	/**
	 * Sets up the title and pane labels for the comparison view.
	 */
	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();

		cc.setLeftLabel(left.getName());
		cc.setLeftImage(left.getImage());

		cc.setRightLabel(right.getName());
		cc.setRightImage(right.getImage());

		setTitle("Bytecode compare: " + left.getElementName() + " - " + right.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public CompareViewerSwitchingPane getInputPane() {
		try {
			Field field = CompareEditorInput.class.getDeclaredField("fContentInputPane"); //$NON-NLS-1$
			field.setAccessible(true);
			Object object = field.get(this);
			if (object instanceof CompareViewerSwitchingPane) {
				return (CompareViewerSwitchingPane) object;
			}
		} catch (Exception e) {
			// ignore
			BytecodeOutlinePlugin.log(e, IStatus.ERROR);
		}

		// does not work after changing content: this is a bug in CompareEditorInput, because
		// navigator instance holds old (not up to date) instance of the input pane
//        ICompareNavigator navigator = getNavigator();
//        if(navigator instanceof CompareNavigator) {
//            CompareNavigator compareNavigator = (CompareNavigator) navigator;
//            try {
//                Method method = compareNavigator.getClass().getDeclaredMethod(
//                    "getPanes", null);
//                method.setAccessible(true);
//                Object object = method.invoke(compareNavigator, null);
//                if(object instanceof Object[]) {
//                    Object[] panes = (Object[]) object;
//                    if(panes.length == 4 && panes[3] instanceof CompareViewerSwitchingPane) {
//                        // there are 4 panels, last one is the input pane that we search for
//                        // see org.eclipse.compare.CompareEditorInput.getNavigator()
//                        return (CompareViewerSwitchingPane) panes[3];
//                    }
//                }
//            } catch (Exception e) {
//                // ignore.
//            }
//        }
		return null;
	}

	@Override
	public Control createContents(final Composite parent) {
		Object obj = parent.getData();
		if (obj == null) {
			obj = parent.getParent().getData();
		}
		// dirty hook on this place to get reference to editor
		// CompareEditor extends EditorPart implements IReusableEditor
		if (obj instanceof IReusableEditor) {
			myEditor = (IReusableEditor) obj;
		}

		Control control = super.createContents(parent);

		CompareViewerSwitchingPane inputPane = getInputPane();
		if (inputPane != null) {
			ToolBarManager toolBarManager2 = CompareViewerPane.getToolBarManager(inputPane);
			if (toolBarManager2 == null) {
				return control;
			}
			boolean separatorExist = false;
			if (toolBarManager2.find(hideLineInfoAction.getId()) == null) {
				if (!separatorExist) {
					separatorExist = true;
					toolBarManager2.insert(0, new Separator("bco")); //$NON-NLS-1$
				}
				toolBarManager2.insertBefore("bco", hideLineInfoAction); //$NON-NLS-1$
			}

			if (toolBarManager2.find(hideLocalsAction.getId()) == null) {
				if (!separatorExist) {
					separatorExist = true;
					toolBarManager2.insert(0, new Separator("bco")); //$NON-NLS-1$
				}
				toolBarManager2.insertBefore("bco", hideLocalsAction); //$NON-NLS-1$
			}

			if (toolBarManager2.find(hideStackMapAction.getId()) == null) {
				if (!separatorExist) {
					separatorExist = true;
					toolBarManager2.insert(0, new Separator("bco")); //$NON-NLS-1$
				}
				toolBarManager2.insertBefore("bco", hideStackMapAction); //$NON-NLS-1$
			}

			if (toolBarManager2.find(expandStackMapAction.getId()) == null) {
				if (!separatorExist) {
					separatorExist = true;
					toolBarManager2.insert(0, new Separator("bco")); //$NON-NLS-1$
				}
				toolBarManager2.insertBefore("bco", expandStackMapAction); //$NON-NLS-1$
			}

			if (toolBarManager2.find(toggleAsmifierModeAction.getId()) == null) {
				if (!separatorExist) {
					toolBarManager2.insert(0, new Separator("bco")); //$NON-NLS-1$
					separatorExist = true;
				}
				toolBarManager2.insertBefore("bco", toggleAsmifierModeAction); //$NON-NLS-1$
			}

			try {
				toolBarManager2.update(true);
				toolBarManager2.getControl().getParent().layout(true);
				toolBarManager2.getControl().getParent().update();
			} catch (NullPointerException e) {
				// ignore, i'm just curios why we need this code in 3.2 and expect
				// some unwanted side effects...
			}
		}
		return control;
	}

	protected void toggleMode(final int mode, final boolean value, final boolean isASMifierMode) {
		String contentType = isASMifierMode ? TypedElement.TYPE_ASM_IFIER : TypedElement.TYPE_BYTECODE;

		left.setMode(mode, value);
		left.setMode(BCOConstants.F_SHOW_ASMIFIER_CODE, isASMifierMode);
		left.setType(contentType);

		right.setMode(mode, value);
		right.setMode(BCOConstants.F_SHOW_ASMIFIER_CODE, isASMifierMode);
		right.setType(contentType);

		CompareUI.reuseCompareEditor(new BytecodeCompare(left, right), myEditor);
	}
}
