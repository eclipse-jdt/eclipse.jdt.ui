/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui;

import java.util.List;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.launcher.JavaApplicationLauncherDelegate;
import org.eclipse.jdt.internal.ui.launcher.LauncherLabelProvider;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;

/**
 * The main page in a <code>JavaApplicationWizard</code>. Presents the
 * user with a list of launchable elements - from which the user must choose.  
 */
public class JavaApplicationWizardPage extends WizardPage {

	private static final String PREFIX= "java_application_wizard_page.";
	private static final String DEBUG= PREFIX + "debug.description";
	private static final String RUN= PREFIX + "run.description";
	private static final String LAUNCHER= PREFIX + "launcher";
	private static final String SELECT_ELEMENTS= PREFIX + "select_elements";
	private static final String SELECT_ERROR_ELEMENTS= PREFIX + "select_error_elements";
	private static final String PATTERN_LABEL= PREFIX + "pattern_label";

	/**
	 * Viewer for the elements to launch
	 */
	protected TableViewer fElementsList;

	/**
	 * A text field to perform pattern matching
	 */
	protected Text fPatternText;

	/**
	 * The filtered array
	 */
	protected Object[] fFilteredElements;

	/**
	 * The selection from which to determine the elements to launch
	 */
	protected Object[] fElements;

	protected String fMode;

	protected JavaApplicationLauncherDelegate fLauncher;

	/**
	 * A content provider for the elements list
	 */
	class ElementsContentProvider implements IStructuredContentProvider {

		/**
		 * @see IContentProvider#inputChanged
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			return fElements;
		}
	}

	class PatternFilter extends ViewerFilter {
		protected StringMatcher fMatcher= null;

		/**
		 * @see ViewerFilter
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (fMatcher == null) {
				return true;
			}
			ILabelProvider lp= (ILabelProvider) fElementsList.getLabelProvider();
			return fMatcher.match(lp.getText(element));
		}

		public void setPattern(String pattern) {
			fMatcher= new StringMatcher(pattern + "*", true, false);
		}

		/**
		 * Cache the filtered elements so we can single-select.
		 *
		 * @see ViewerFilter
		 */
		public Object[] filter(Viewer viewer, Object parent, Object[] input) {
			fFilteredElements= super.filter(viewer, parent, input);
			return fFilteredElements;
		}

	}

	class SimpleSorter extends ViewerSorter {
		/**
		 * @see ViewerSorter#isSorterProperty(Object, Object)
		 */
		public boolean isSorterProperty(Object element, Object property) {
			return true;
		}
	}

	/**
	 * Constructs a <code>JavaApplicationWizardPage</code> with the given launcher and pre-computed children
	 */
	public JavaApplicationWizardPage(Object[] elements, JavaApplicationLauncherDelegate launcher, String mode) {
		super(DebugUIUtils.getResourceString(PREFIX + "title"));
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_JAVA_LAUNCH);
		fElements= elements;
		fMode= mode;
		fLauncher= launcher;
	}

	/**
	 * Creates the contents of the page - two lists
	 * and a check box for setting the default launcher.
	 */
	public void createControl(Composite ancestor) {
		Composite root= new Composite(ancestor, SWT.NONE);
		GridLayout l= new GridLayout();
		l.numColumns= 1;
		l.makeColumnsEqualWidth= true;
		root.setLayout(l);

		createElementsGroup(root);

		//determine description
		if (fMode.equals(ILaunchManager.DEBUG_MODE)) {
			setDescription(DebugUIUtils.getResourceString(DEBUG));
		} else {
			setDescription(DebugUIUtils.getResourceString(RUN));
		}

		setPageComplete(false);
		setTitle(DebugUIUtils.getResourceString(PREFIX + "title"));
		setControl(root);
		WorkbenchHelp.setHelp(root, new DialogPageContextComputer(this, IJavaHelpContextIds.JAVA_APPLICATION_WIZARD_PAGE));				
	}

	public void createElementsGroup(Composite root) {
		Label elementsLabel= new Label(root, SWT.NONE);
		elementsLabel.setText(DebugUIUtils.getResourceString(PATTERN_LABEL));

		fPatternText= new Text(root, SWT.BORDER);
		fPatternText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		fElementsList= new TableViewer(root, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER) {
			protected void handleDoubleSelect(SelectionEvent event) {
				updateSelection(getSelection());
				if (getWizard().performFinish()) {
					((WizardDialog) getWizard().getContainer()).close();
				}
			}
		};

		Table list= fElementsList.getTable();

		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		gd.heightHint= 200;
		gd.grabExcessVerticalSpace= true;
		list.setLayoutData(gd);

		fElementsList.setContentProvider(new ElementsContentProvider());
		fElementsList.setLabelProvider(new LauncherLabelProvider());
		fElementsList.setSorter(new SimpleSorter());

		final PatternFilter filter= new PatternFilter();
		fElementsList.addFilter(filter);
		fPatternText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				filter.setPattern(((Text) (e.widget)).getText());
				fElementsList.refresh();
				if (fFilteredElements.length >= 1) {
					fElementsList.setSelection(new StructuredSelection(fFilteredElements[0]), true);
					setMessage(DebugUIUtils.getResourceString(SELECT_ELEMENTS));					
					setPageComplete(true);
					return;
				} else {
					setMessage(DebugUIUtils.getResourceString(SELECT_ERROR_ELEMENTS));
					setPageComplete(false);
				}
			}
		});

		fElementsList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				if (e.getSelection().isEmpty()) {
					setPageComplete(false);
				} else if (e.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection ss= (IStructuredSelection) e.getSelection();
					if (!ss.isEmpty()) {
						setPageComplete(true);
					}
				}
			}
		});

		fElementsList.setInput(fLauncher);
		initializeSettings();
	}

	/**
	 * Returns the selected elements to launch or <code>null</code> if
	 * no elements are selected.
	 */
	protected Object[] getElements() {
		ISelection s= fElementsList.getSelection();
		if (s.isEmpty()) {
			return null;
		}

		if (s instanceof IStructuredSelection) {
			return ((IStructuredSelection) s).toArray();
		}

		return null;
	}

	/**
	 * Convenience method to set the message line
	 */
	public void setMessage(String message) {
		super.setErrorMessage(null);
		super.setMessage(message);
	}

	/**
	 * Convenience method to set the error line
	 */
	public void setErrorMessage(String message) {
		super.setMessage(null);
		super.setErrorMessage(message);
	}

	/**
	 * Initialize the settings:<ul>
	 * <li>If there is only one element, select it
	 * <li>Put the cursor in the pattern text area
	 * </ul>
	 */
	protected void initializeSettings() {
		Runnable runnable= new Runnable() {
			public void run() {
				if (getControl().isDisposed()) {
					return;
				}
				if (fElements.length >= 1) {
					fElementsList.setSelection(new StructuredSelection(fElements[0]), true);
					setMessage(DebugUIUtils.getResourceString(SELECT_ELEMENTS));
					setPageComplete(true);				
				} else {										
					// no elements to select
					setErrorMessage(DebugUIUtils.getResourceString(SELECT_ERROR_ELEMENTS));
					setPageComplete(false);	
								
				}
				fPatternText.setFocus();		
			}
		};

		Display.getCurrent().asyncExec(runnable);
	}
}
