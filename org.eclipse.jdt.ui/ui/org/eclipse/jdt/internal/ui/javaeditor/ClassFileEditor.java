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

package org.eclipse.jdt.internal.ui.javaeditor;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.IWidgetTokenKeeper;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.IClassFileDisassembler;
import org.eclipse.jdt.core.util.IClassFileReader;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentDialog;

/**
 * Java specific text editor.
 */
public class ClassFileEditor extends JavaEditor implements ClassFileDocumentProvider.InputChangeListener {

		/** The horizontal scroll increment. */
		private static final int HORIZONTAL_SCROLL_INCREMENT= 10;
		/** The vertical scroll increment. */
		private static final int VERTICAL_SCROLL_INCREMENT= 10;
	
		/**
		 * A form to attach source to a class file.
		 */
		private class SourceAttachmentForm implements IPropertyChangeListener {
		
			private final IClassFile fFile;
			private ScrolledComposite fScrolledComposite;
			private Color fBackgroundColor;
			private Color fForegroundColor;
			private Color fSeparatorColor;
			private List fBannerLabels= new ArrayList();
			private List fHeaderLabels= new ArrayList();
			private Font fFont;
			
			/**
			 * Creates a source attachment form for a class file. 
			 */
			public SourceAttachmentForm(IClassFile file) {
				fFile= file;
			}
		
			/**
			 * Returns the package fragment root of this file.
			 */
			private IPackageFragmentRoot getPackageFragmentRoot(IClassFile file) {
		
				IJavaElement element= file.getParent();
				while (element != null && element.getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT)
					element= element.getParent();
		
				return (IPackageFragmentRoot) element;		
			}
			
			/**
			 * Creates the control of the source attachment form.
			 */
			public Control createControl(Composite parent) {
		
				Display display= parent.getDisplay();
				fBackgroundColor= display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
				fForegroundColor= display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
				fSeparatorColor= new Color(display, 152, 170, 203);
		
				JFaceResources.getFontRegistry().addListener(this);
		
				fScrolledComposite= new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
				fScrolledComposite.setAlwaysShowScrollBars(false);
				fScrolledComposite.setExpandHorizontal(true);
				fScrolledComposite.setExpandVertical(true);
				fScrolledComposite.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						JFaceResources.getFontRegistry().removeListener(SourceAttachmentForm.this);
						fScrolledComposite= null;
						fSeparatorColor.dispose();
						fSeparatorColor= null;				
						fBannerLabels.clear();
						fHeaderLabels.clear();
						if (fFont != null) {
							fFont.dispose();
							fFont= null;
						}
					}
				});
				
				fScrolledComposite.addControlListener(new ControlListener() {
					public void controlMoved(ControlEvent e) {}

					public void controlResized(ControlEvent e) {
						Rectangle clientArea = fScrolledComposite.getClientArea();
						
						ScrollBar verticalBar= fScrolledComposite.getVerticalBar();
						verticalBar.setIncrement(VERTICAL_SCROLL_INCREMENT);
						verticalBar.setPageIncrement(clientArea.height - verticalBar.getIncrement());
		
						ScrollBar horizontalBar= fScrolledComposite.getHorizontalBar();
						horizontalBar.setIncrement(HORIZONTAL_SCROLL_INCREMENT);
						horizontalBar.setPageIncrement(clientArea.width - horizontalBar.getIncrement());
					}
				});
	
				Composite composite= createComposite(fScrolledComposite);
				composite.setLayout(new GridLayout());
		
				createTitleLabel(composite, JavaEditorMessages.getString("SourceAttachmentForm.title")); //$NON-NLS-1$
				createLabel(composite, null);
				createLabel(composite, null);
		
				createHeadingLabel(composite, JavaEditorMessages.getString("SourceAttachmentForm.heading")); //$NON-NLS-1$
		
				Composite separator= createCompositeSeparator(composite);
				GridData data= new GridData(GridData.FILL_HORIZONTAL);
				data.heightHint= 2;
				separator.setLayoutData(data);
				
				try {
					IPackageFragmentRoot root= getPackageFragmentRoot(fFile);
					if (root != null) {
						createSourceAttachmentControls(composite, root);
					}					
				} catch (JavaModelException e) {
					String title= JavaEditorMessages.getString("SourceAttachmentForm.error.title"); //$NON-NLS-1$
					String message= JavaEditorMessages.getString("SourceAttachmentForm.error.message"); //$NON-NLS-1$
					ExceptionHandler.handle(e, fScrolledComposite.getShell(), title, message);				
				}	
				
				separator= createCompositeSeparator(composite);
				data= new GridData(GridData.FILL_HORIZONTAL);
				data.heightHint= 2;
				separator.setLayoutData(data);

				StyledText styledText= createCodeView(composite);
				data= new GridData(GridData.FILL_BOTH);
				styledText.setLayoutData(data);
				updateCodeView(styledText, fFile);
				
				fScrolledComposite.setContent(composite);
				fScrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				
				return fScrolledComposite;
			}
			
			private void createSourceAttachmentControls(Composite composite, IPackageFragmentRoot root) throws JavaModelException {
				IClasspathEntry entry= root.getRawClasspathEntry();	
				IPath containerPath= null;
				IJavaProject jproject= root.getJavaProject();
				
				if (entry == null || !root.isArchive()) {
					createLabel(composite, JavaEditorMessages.getFormattedString("SourceAttachmentForm.message.noSource", fFile.getElementName())); //$NON-NLS-1$
					return;
				}
				
				if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
					containerPath= entry.getPath();
					IClasspathEntry entry2= SourceAttachmentDialog.getClasspathEntryToEdit(jproject, containerPath, root.getPath());
					if (entry2 == null) {
						IClasspathContainer container= JavaCore.getClasspathContainer(entry.getPath(), root.getJavaProject());							
						String containerName= container == null ? entry.getPath().toString() : container.getDescription();
						createLabel(composite, JavaEditorMessages.getFormattedString("SourceAttachmentForm.message.containerEntry", containerName));  //$NON-NLS-1$
						return;
					}
					entry= entry2;
				}
				
				
				Button button;

				IPath path= entry.getSourceAttachmentPath();			
				if (path == null || path.isEmpty()) {			
					createLabel(composite, JavaEditorMessages.getFormattedString("SourceAttachmentForm.message.noSourceAttachment", root.getElementName())); //$NON-NLS-1$
					createLabel(composite, JavaEditorMessages.getString("SourceAttachmentForm.message.pressButtonToAttach")); //$NON-NLS-1$
					createLabel(composite, null);

					button= createButton(composite, JavaEditorMessages.getString("SourceAttachmentForm.button.attachSource"));		 //$NON-NLS-1$

				} else {
					createLabel(composite, JavaEditorMessages.getFormattedString("SourceAttachmentForm.message.noSourceInAttachment", fFile.getElementName())); //$NON-NLS-1$
					createLabel(composite, JavaEditorMessages.getString("SourceAttachmentForm.message.pressButtonToChange")); //$NON-NLS-1$
					createLabel(composite, null);

					button= createButton(composite, JavaEditorMessages.getString("SourceAttachmentForm.button.changeAttachedSource")); //$NON-NLS-1$
				}

				button.addSelectionListener(getButtonListener(entry, containerPath, jproject));
			}
			
			private SelectionListener getButtonListener(final IClasspathEntry entry, final IPath containerPath, final IJavaProject jproject) {
				return new SelectionListener() {
					public void widgetSelected(SelectionEvent event) {				
						try {
							SourceAttachmentDialog dialog= new SourceAttachmentDialog(fScrolledComposite.getShell(), entry, containerPath, jproject, true);
							if (dialog.open() == Window.OK)
								verifyInput(getEditorInput());
	
						} catch (CoreException e) {
							String title= JavaEditorMessages.getString("SourceAttachmentForm.error.title"); //$NON-NLS-1$
							String message= JavaEditorMessages.getString("SourceAttachmentForm.error.message"); //$NON-NLS-1$
							ExceptionHandler.handle(e, fScrolledComposite.getShell(), title, message);				
						}
					}
	
					public void widgetDefaultSelected(SelectionEvent e) {}
				};
			}	
			
		
			/*
			 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
			 */
			public void propertyChange(PropertyChangeEvent event) {		
		
				for (Iterator iterator = fBannerLabels.iterator(); iterator.hasNext();) {
					Label label = (Label) iterator.next();
					label.setFont(JFaceResources.getBannerFont());
				}
		
				for (Iterator iterator = fHeaderLabels.iterator(); iterator.hasNext();) {
					Label label = (Label) iterator.next();
					label.setFont(JFaceResources.getHeaderFont());
				}
		
				Control control= fScrolledComposite.getContent();
				fScrolledComposite.setMinSize(control.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				fScrolledComposite.setContent(control);
				
				fScrolledComposite.layout(true);
				fScrolledComposite.redraw();
			}
		
			// --- copied from org.eclipse.update.ui.forms.internal.FormWidgetFactory
		
			private Composite createComposite(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				composite.setBackground(fBackgroundColor);
		//		composite.addMouseListener(new MouseAdapter() {
		//			public void mousePressed(MouseEvent e) {
		//				((Control) e.widget).setFocus();
		//			}
		//		});
				return composite;
			}
		
			private Composite createCompositeSeparator(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				composite.setBackground(fSeparatorColor);
				return composite;
			}
			
			private StyledText createCodeView(Composite parent) {
				int styles= SWT.MULTI | SWT.FULL_SELECTION;
				StyledText styledText= new StyledText(parent, styles);
				styledText.setBackground(fBackgroundColor);
				styledText.setForeground(fForegroundColor);
				styledText.setEditable(false);
				return styledText;
			}
			
			private Label createLabel(Composite parent, String text) {
				Label label = new Label(parent, SWT.NONE);
				if (text != null)
					label.setText(text);
				label.setBackground(fBackgroundColor);
				label.setForeground(fForegroundColor);
				return label;
			}
		
			private Label createTitleLabel(Composite parent, String text) {
				Label label = new Label(parent, SWT.NONE);
				if (text != null)
					label.setText(text);
				label.setBackground(fBackgroundColor);
				label.setForeground(fForegroundColor);
				label.setFont(JFaceResources.getHeaderFont());
				fHeaderLabels.add(label);
				return label;
			}
		
			private Label createHeadingLabel(Composite parent, String text) {
				Label label = new Label(parent, SWT.NONE);
				if (text != null)
					label.setText(text);
				label.setBackground(fBackgroundColor);
				label.setForeground(fForegroundColor);
				label.setFont(JFaceResources.getBannerFont());
				fBannerLabels.add(label);
				return label;
			}
			
			private Button createButton(Composite parent, String text) {
				Button button = new Button(parent, SWT.FLAT);
				button.setBackground(fBackgroundColor);
				button.setForeground(fForegroundColor);
				if (text != null)
					button.setText(text);
		//		button.addFocusListener(visibilityHandler);
				return button;
			}
			
			private void updateCodeView(StyledText styledText, IClassFile classFile) {
				String content= null;
				int flags= IClassFileReader.FIELD_INFOS | IClassFileReader.METHOD_INFOS | IClassFileReader.SUPER_INTERFACES;
				IClassFileReader classFileReader= ToolFactory.createDefaultClassFileReader(classFile, flags);					
				if (classFileReader != null) {
					IClassFileDisassembler disassembler= ToolFactory.createDefaultClassFileDisassembler();
					content= disassembler.disassemble(classFileReader, "\n"); //$NON-NLS-1$
				}
				styledText.setText(content == null ? "" : content); //$NON-NLS-1$
			}	
		}
		
		/**
		 *  Updater that takes care of minimizing changes of the editor input.
		 */
		private class InputUpdater implements Runnable {
			
			/** Has the runnable already been posted? */
			private boolean fPosted= false;
			/** Editor input */
			private IClassFileEditorInput fClassFileEditorInput;
			
			
			public InputUpdater() {
			}
			
			/*
			 * @see Runnable#run()
			 */
			public void run() {
				
				IClassFileEditorInput input;
				synchronized (this) {
					input= fClassFileEditorInput;
				}
				
				try {
					
					if (getSourceViewer() != null)
						setInput(input);				
				
				} finally {
					synchronized (this) {
						fPosted= false;
					}
				}
			}
			
			/**
			 * Posts this runnable into the event queue if not already there.
			 * 
			 * @param input the input to be set when executed
			 */
			public void post(IClassFileEditorInput input) {
				
				synchronized(this) {
					if (fPosted) {
						if (input != null && input.equals(fClassFileEditorInput))
							fClassFileEditorInput= input;
						return;
					}
				}
				
				if (input != null && input.equals(getEditorInput())) {	
					ISourceViewer viewer= getSourceViewer();
					if (viewer != null) {
						StyledText textWidget= viewer.getTextWidget();
						if (textWidget != null && !textWidget.isDisposed()) {
							synchronized (this) {
								fPosted= true;
								fClassFileEditorInput= input;
							}
							textWidget.getDisplay().asyncExec(this);
						}
					}
				}
			}
		}	

	
	
	
	private StackLayout fStackLayout;
	private Composite fParent;

	private Composite fViewerComposite;
	private Control fSourceAttachmentForm;
	
	private InputUpdater fInputUpdater= new InputUpdater();
	
	/**
	 * Default constructor.
	 */
	public ClassFileEditor() {
		super();
		setDocumentProvider(JavaPlugin.getDefault().getClassFileDocumentProvider());
		setEditorContextMenuId("#ClassFileEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#ClassFileRulerContext"); //$NON-NLS-1$
		setOutlinerContextMenuId("#ClassFileOutlinerContext"); //$NON-NLS-1$
		// don't set help contextId, we install our own help context
	}
	
	/*
	 * @see AbstractTextEditor#createActions()
	 */
	protected void createActions() {
		super.createActions();
		
		setAction(ITextEditorActionConstants.SAVE, null);
		setAction(ITextEditorActionConstants.REVERT_TO_SAVED, null);
		
		/*
		 * 1GF82PL: ITPJUI:ALL - Need to be able to add bookmark to classfile
		 *
		 *  // replace default action with class file specific ones
		 *
		 *	setAction(ITextEditorActionConstants.BOOKMARK, new AddClassFileMarkerAction("AddBookmark.", this, IMarker.BOOKMARK, true)); //$NON-NLS-1$
		 *	setAction(ITextEditorActionConstants.ADD_TASK, new AddClassFileMarkerAction("AddTask.", this, IMarker.TASK, false)); //$NON-NLS-1$
		 *	setAction(ITextEditorActionConstants.RULER_MANAGE_BOOKMARKS, new ClassFileMarkerRulerAction("ManageBookmarks.", getVerticalRuler(), this, IMarker.BOOKMARK, true)); //$NON-NLS-1$
		 *	setAction(ITextEditorActionConstants.RULER_MANAGE_TASKS, new ClassFileMarkerRulerAction("ManageTasks.", getVerticalRuler(), this, IMarker.TASK, true)); //$NON-NLS-1$
		 */
	}
	
	/*
	 * @see JavaEditor#getElementAt(int)
	 */
	protected IJavaElement getElementAt(int offset) {
		if (getEditorInput() instanceof IClassFileEditorInput) {
			try {
				IClassFileEditorInput input= (IClassFileEditorInput) getEditorInput();
				return input.getClassFile().getElementAt(offset);
			} catch (JavaModelException x) {
			}
		}
		return null;
	}
	
	/*
	 * @see JavaEditor#getCorrespondingElement(IJavaElement)
	 */
	protected IJavaElement getCorrespondingElement(IJavaElement element) {
		if (getEditorInput() instanceof IClassFileEditorInput) {
			IClassFileEditorInput input= (IClassFileEditorInput) getEditorInput();
			IJavaElement parent= element.getAncestor(IJavaElement.CLASS_FILE);
			if (input.getClassFile().equals(parent))
				return element;
		}
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor#getInputElement()
	 */
	protected IJavaElement getInputJavaElement() {
		if (getEditorInput() == null)
			return null;
		
		return ((IClassFileEditorInput)getEditorInput()).getClassFile();
	}
	
	/*
	 * @see IEditorPart#saveState(IMemento)
	 */
	public void saveState(IMemento memento) {
	}
	
	/*
	 * @see JavaEditor#setOutlinePageInput(JavaOutlinePage, IEditorInput)
	 */
	protected void setOutlinePageInput(JavaOutlinePage page, IEditorInput input) {
		if (page != null && input instanceof IClassFileEditorInput) {
			IClassFileEditorInput cfi= (IClassFileEditorInput)input;
			IClassFile cf= cfi.getClassFile();
			if (cf != null && cf.exists())
				page.setInput(cf);
			else
				page.setInput(null);
		}
	}
	
	/*
	 * 1GEPKT5: ITPJUI:Linux - Source in editor for external classes is editable
	 * Removed methods isSaveOnClosedNeeded and isDirty.
	 * Added method isEditable.
	 */
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#isEditable()
	 */
	public boolean isEditable() {
		return false;
	}
	
	/**
	 * Translates the given editor input into an <code>ExternalClassFileEditorInput</code>
	 * if it is a file editor input representing an external class file.
	 * 
	 * @param input the editor input to be transformed if necessary
	 * @return the transformed editor input
	 */
	protected IEditorInput transformEditorInput(IEditorInput input) {
		
		if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) input).getFile();
			IClassFileEditorInput classFileInput= new ExternalClassFileEditorInput(file);
			if (classFileInput.getClassFile() != null)
				input= classFileInput;
		}
		
		return input;
	}
	
	/*
	 * @see AbstractTextEditor#doSetInput(IEditorInput)
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {

		input= transformEditorInput(input);
		if (!(input instanceof IClassFileEditorInput))
			throw new CoreException(JavaUIStatus.createError(
				IJavaModelStatusConstants.INVALID_RESOURCE_TYPE, 
				JavaEditorMessages.getString("ClassFileEditor.error.invalid_input_message"), //$NON-NLS-1$
				null)); //$NON-NLS-1$

		JavaModelException e= probeInputForSource(input);
		if (e != null) {
			IClassFileEditorInput classFileEditorInput= (IClassFileEditorInput) input;
			IClassFile file= classFileEditorInput.getClassFile();
			IJavaProject javaProject= file.getJavaProject();
			if (!javaProject.exists() || !javaProject.isOnClasspath(file)) {
				throw new CoreException(JavaUIStatus.createError(
					IJavaModelStatusConstants.INVALID_RESOURCE, 
					JavaEditorMessages.getString("ClassFileEditor.error.classfile_not_on_classpath"), //$NON-NLS-1$
					null)); //$NON-NLS-1$
			} else {
				throw e;
			}
		}

		IDocumentProvider documentProvider= getDocumentProvider();
		if (documentProvider instanceof ClassFileDocumentProvider)
			((ClassFileDocumentProvider) documentProvider).removeInputChangeListener(this);

		super.doSetInput(input);

		documentProvider= getDocumentProvider();
		if (documentProvider instanceof ClassFileDocumentProvider)
			((ClassFileDocumentProvider) documentProvider).addInputChangeListener(this);

		verifyInput(getEditorInput());
	}

	/*
	 * @see IWorkbenchPart#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {

		fParent= new Composite(parent, SWT.NONE);
		fStackLayout= new StackLayout();
		fParent.setLayout(fStackLayout);

		fViewerComposite= new Composite(fParent, SWT.NONE);
		fViewerComposite.setLayout(new FillLayout());

		super.createPartControl(fViewerComposite);

		fStackLayout.topControl= fViewerComposite;
		fParent.layout();
		
		try {
			verifyInput(getEditorInput());
		} catch (CoreException e) {
			String title= JavaEditorMessages.getString("ClassFileEditor.error.title"); //$NON-NLS-1$
			String message= JavaEditorMessages.getString("ClassFileEditor.error.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, fParent.getShell(), title, message);
		}
	}

	private JavaModelException probeInputForSource(IEditorInput input) {
		if (input == null)
			return null;

		IClassFileEditorInput classFileEditorInput= (IClassFileEditorInput) input;
		IClassFile file= classFileEditorInput.getClassFile();
			
		try {
			file.getSourceRange();
		} catch (JavaModelException e) {
			return e;			
		}

		return null;
	}

	/**
	 * Checks if the class file input has no source attached. If so, a source attachment form is shown.
	 */
	private void verifyInput(IEditorInput input) throws CoreException {

		if (fParent == null || input == null)
			return;

		IClassFileEditorInput classFileEditorInput= (IClassFileEditorInput) input;
		IClassFile file= classFileEditorInput.getClassFile();
		
		// show source attachment form if no source found
		if (file.getSourceRange() == null) {

			// dispose old source attachment form
			if (fSourceAttachmentForm != null)
				fSourceAttachmentForm.dispose();

			SourceAttachmentForm form= new SourceAttachmentForm(file);
			fSourceAttachmentForm= form.createControl(fParent);

			fStackLayout.topControl= fSourceAttachmentForm;
			fParent.layout();

		// show source viewer
		} else {
		
			if (fSourceAttachmentForm != null) {
				fSourceAttachmentForm.dispose();
				fSourceAttachmentForm= null;

				fStackLayout.topControl= fViewerComposite;		
				fParent.layout();
			}
		}		
	}
	
	/*
	 * @see ClassFileDocumentProvider.InputChangeListener#inputChanged(IClassFileEditorInput)
	 */
	public void inputChanged(IClassFileEditorInput input) {
		fInputUpdater.post(input);
	}
	
	/*
	 * @see JavaEditor#createJavaSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected ISourceViewer createJavaSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		return new JavaSourceViewer(parent, ruler, null, false, styles) {
			
			public boolean requestWidgetToken(IWidgetTokenKeeper requester) {
				if (WorkbenchHelp.isContextHelpDisplayed())
					return false;
				return super.requestWidgetToken(requester);
			}
			
			public boolean requestWidgetToken(IWidgetTokenKeeper requester, int priority) {
				if (WorkbenchHelp.isContextHelpDisplayed())
					return false;
				return super.requestWidgetToken(requester, priority);
			}
		};	
	}
	
	/*
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=18510
		IDocumentProvider documentProvider= getDocumentProvider();
		if (documentProvider instanceof ClassFileDocumentProvider)
			((ClassFileDocumentProvider) documentProvider).removeInputChangeListener(this);
		super.dispose();
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
		super.setFocus();

		if (fSourceAttachmentForm != null && !fSourceAttachmentForm.isDisposed())
			fSourceAttachmentForm.setFocus();
	}

}
