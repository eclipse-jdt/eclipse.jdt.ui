package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyLookupFactory;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardPage;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamingNameSuggestor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.refactoring.RenameSupport;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.refactoring.DelegateUIHelper;

public class RenameLinkedMode {

	private class FocusEditingSupport implements IEditingSupport {
		public boolean ownsFocusShell() {
			if (fPopup == null || fPopup.isDisposed())
				return false;
			Control focusControl= fPopup.getDisplay().getFocusControl();
			return focusControl != null && focusControl.getShell() == fPopup;
		}

		public boolean isOriginator(DocumentEvent event, IRegion subjectRegion) {
			return true;
		}
	}

	private class EditorSynchronizer implements ILinkedModeListener {
		public void left(LinkedModeModel model, int flags) {
			closePopup();
			if ( (flags & ILinkedModeListener.UPDATE_CARET) != 0) {
				doRename(fShowPreview);
			}
		}

		public void resume(LinkedModeModel model, int flags) {
		}

		public void suspend(LinkedModeModel model) {
		}
	}
	
	private class ExitPolicy implements IExitPolicy {
		public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
			fShowPreview= (event.stateMask & SWT.CTRL) != 0;
			return null; // don't change behavior; do actions in EditorSynchronizer
		}
	}
	
	private class PopupVisibilityManager implements IPartListener2, ControlListener {
		public void start() {
			fEditor.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
			fEditor.getViewer().getTextWidget().addControlListener(this);
			fEditor.getSite().getShell().addControlListener(this);
			fPopup.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					fEditor.getSite().getWorkbenchWindow().getPartService().removePartListener(PopupVisibilityManager.this);
					fEditor.getViewer().getTextWidget().removeControlListener(PopupVisibilityManager.this);
					fEditor.getSite().getShell().removeControlListener(PopupVisibilityManager.this);
					fPopup.removeDisposeListener(this);
				}
			});
		}
		
		public void stop() {
			fEditor.getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		}
		
		public void partActivated(IWorkbenchPartReference partRef) {
			IWorkbenchPart fPart= fEditor.getEditorSite().getPart();
			if (fPopup != null && ! fPopup.isDisposed() && partRef.getPart(false) == fPart) {
				fPopup.setVisible(true);
			}
		}

		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}

		public void partClosed(IWorkbenchPartReference partRef) {
		}

		public void partDeactivated(IWorkbenchPartReference partRef) {
			IWorkbenchPart fPart= fEditor.getEditorSite().getPart();
			if (fPopup != null && ! fPopup.isDisposed() && partRef.getPart(false) == fPart) {
				fPopup.setVisible(false);
			}
		}

		public void partHidden(IWorkbenchPartReference partRef) {
		}

		public void partInputChanged(IWorkbenchPartReference partRef) {
		}

		public void partOpened(IWorkbenchPartReference partRef) {
		}

		public void partVisible(IWorkbenchPartReference partRef) {
		}

		public void controlMoved(ControlEvent e) {
			setPopupLocation(fEditor.getViewer());
		}

		public void controlResized(ControlEvent e) {
			setPopupLocation(fEditor.getViewer());
		}
	}

	private static RenameLinkedMode fgActiveLinkedMode;
	
	private CompilationUnitEditor fEditor;
	private IJavaElement fJavaElement;

	private Shell fPopup;
	private LinkedPosition fNamePosition;
	
	private final IEditingSupport fFocusEditingSupport;

	private Point fOriginalSelection;
	private String fOriginalName;

	private LinkedModeModel fLinkedModeModel;
	private LinkedPositionGroup fLinkedPositionGroup;
	private boolean fShowPreview;


	public RenameLinkedMode(IJavaElement element, CompilationUnitEditor editor) {
		fEditor= editor;
		fJavaElement= element;
		fFocusEditingSupport= new FocusEditingSupport();
	}
	
	public static RenameLinkedMode getActiveLinkedMode() {
		return fgActiveLinkedMode;
	}
	
	public void start() {
		if (fgActiveLinkedMode != null) {
			// for safety; should already be handled in RenameJavaElementAction
			fgActiveLinkedMode.startFullDialog();
			return;
		}
		
		ISourceViewer viewer= fEditor.getViewer();
		IDocument document= viewer.getDocument();
		fOriginalSelection= viewer.getSelectedRange();
		int offset= fOriginalSelection.x;
		
		try {
			CompilationUnit root= JavaPlugin.getDefault().getASTProvider().getAST(getCompilationUnit(), ASTProvider.WAIT_YES, null);
			
			fLinkedPositionGroup= new LinkedPositionGroup();
			ASTNode selectedNode= NodeFinder.perform(root, fOriginalSelection.x, fOriginalSelection.y);
			if (! (selectedNode instanceof SimpleName)) {
				return; // TODO: show dialog
			}
			SimpleName nameNode= (SimpleName) selectedNode;
			
			fOriginalName= nameNode.getIdentifier();
			final int pos= nameNode.getStartPosition();
			ASTNode[] sameNodes= LinkedNodeFinder.findByNode(root, nameNode);
			
			//TODO: copied from LinkedNamesAssistProposal#apply(..):
			// sort for iteration order, starting with the node @ offset
			Arrays.sort(sameNodes, new Comparator() {
				public int compare(Object o1, Object o2) {
					return rank((ASTNode) o1) - rank((ASTNode) o2);
				}
				/**
				 * Returns the absolute rank of an <code>ASTNode</code>. Nodes
				 * preceding <code>pos</code> are ranked last.
				 *
				 * @param node the node to compute the rank for
				 * @return the rank of the node with respect to the invocation offset
				 */
				private int rank(ASTNode node) {
					int relativeRank= node.getStartPosition() + node.getLength() - pos;
					if (relativeRank < 0)
						return Integer.MAX_VALUE + relativeRank;
					else
						return relativeRank;
				}
			});
			for (int i= 0; i < sameNodes.length; i++) {
				ASTNode elem= sameNodes[i];
				LinkedPosition linkedPosition= new LinkedPosition(document, elem.getStartPosition(), elem.getLength(), i);
				if (i == 0)
					fNamePosition= linkedPosition;
				fLinkedPositionGroup.addPosition(linkedPosition);
			}
				
			fLinkedModeModel= new LinkedModeModel();
			fLinkedModeModel.addGroup(fLinkedPositionGroup);
			fLinkedModeModel.forceInstall();
			fLinkedModeModel.addLinkingListener(new EditorHighlightingSynchronizer(fEditor));
			fLinkedModeModel.addLinkingListener(new EditorSynchronizer());
            
			LinkedModeUI ui= new EditorLinkedModeUI(fLinkedModeModel, viewer);
			ui.setExitPosition(viewer, offset, 0, Integer.MAX_VALUE);
			ui.setExitPolicy(new ExitPolicy());
			ui.enter();
			
			viewer.setSelectedRange(fOriginalSelection.x, fOriginalSelection.y); // by default, full word is selected; restore original selection
			
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		openSecondaryPopup();
		fgActiveLinkedMode= this;
	}
	
	void doRename(boolean showPreview) {
		fLinkedModeModel.exit(ILinkedModeListener.NONE);
		closePopup();
		
		String oldName= fJavaElement.getElementName();
		try {
			String newName= fNamePosition.getContent();
			if (oldName.equals(newName))
				return;
			RenameSupport renameSupport= undoAndCreateRenameSupport(newName);
			if (renameSupport == null)
				return;
			
			Shell shell= fEditor.getSite().getShell();
			IWorkbenchWindow workbenchWindow= fEditor.getSite().getWorkbenchWindow();
			if (showPreview) {
				renameSupport.openPreview(shell, workbenchWindow);
			} else {
				renameSupport.perform(shell, workbenchWindow);
			}
			JavaModelUtil.reconcile(getCompilationUnit());
		} catch (CoreException ex) {
			JavaPlugin.log(ex);
		} catch (InterruptedException ex) {
			// canceling is OK
		} catch (InvocationTargetException ex) {
			JavaPlugin.log(ex);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}
	
	private RenameSupport undoAndCreateRenameSupport(String newName) throws CoreException {
		// Assumption: the linked mode model should be shut down by now.
		
		ISourceViewer viewer= fEditor.getViewer();
		final IDocument document= viewer.getDocument();
		
		try {
			if (! fOriginalName.equals(newName)) {
				fEditor.getSite().getWorkbenchWindow().run(false, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						LinkedPosition[] positions= fLinkedPositionGroup.getPositions();
						Arrays.sort(positions, new Comparator() {
							public int compare(Object o1, Object o2) {
								return ((LinkedPosition) o1).offset - ((LinkedPosition) o2).offset;
							}
						});
						int correction= 0;
						int originalLength= fOriginalName.length();
						for (int i= 0; i < positions.length; i++) {
							LinkedPosition position= positions[i];
							try {
								int length= position.getLength();
								document.replace(position.getOffset() + correction, length, fOriginalName);
								correction= correction - length + originalLength;
							} catch (BadLocationException e) {
								throw new InvocationTargetException(e);
							}
						}
					}
				});
			}
		} catch (InvocationTargetException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), ReorgMessages.RenameLinkedMode_error_saving_editor, e));
		} catch (InterruptedException e) {
			// cancelling is OK
			return null;
		} finally {
			JavaModelUtil.reconcile(getCompilationUnit());
		}
		
		viewer.setSelectedRange(fOriginalSelection.x, fOriginalSelection.y);
		
		RenameJavaElementDescriptor descriptor= createRenameDescriptor(fJavaElement, newName);
		RenameSupport renameSupport= RenameSupport.create(descriptor);
		return renameSupport;
	}

	private ICompilationUnit getCompilationUnit() {
		return (ICompilationUnit) EditorUtility.getEditorInputJavaElement(fEditor, false);
	}
	
	public void startFullDialog() {
		fLinkedModeModel.exit(ILinkedModeListener.NONE);
		closePopup();
		
		try {
			String newName= fNamePosition.getContent();
			RenameSupport renameSupport= undoAndCreateRenameSupport(newName);
			if (renameSupport != null)
				renameSupport.openDialog(fEditor.getSite().getShell());
		} catch (CoreException e) {
			JavaPlugin.log(e);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}
	
	/**
	 * @param javaElement
	 * @param newName
	 * @return a rename descriptor with current settings as used in the refactoring dialogs 
	 * @throws JavaModelException
	 */
	private RenameJavaElementDescriptor createRenameDescriptor(IJavaElement javaElement, String newName) throws JavaModelException {
		String contributionId;
		// see RefactoringExecutionStarter#createRenameSupport(..):
		int elementType= javaElement.getElementType();
		switch (elementType) {
			case IJavaElement.JAVA_PROJECT:
				contributionId= IJavaRefactorings.RENAME_JAVA_PROJECT;
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				contributionId= IJavaRefactorings.RENAME_SOURCE_FOLDER;
				break;
			case IJavaElement.PACKAGE_FRAGMENT:
				contributionId= IJavaRefactorings.RENAME_PACKAGE;
				break;
			case IJavaElement.COMPILATION_UNIT:
				contributionId= IJavaRefactorings.RENAME_COMPILATION_UNIT;
				break;
			case IJavaElement.TYPE:
				contributionId= IJavaRefactorings.RENAME_TYPE;
				break;
			case IJavaElement.METHOD:
				final IMethod method= (IMethod) javaElement;
				if (method.isConstructor())
					return createRenameDescriptor(method.getDeclaringType(), newName);
				else
					contributionId= IJavaRefactorings.RENAME_METHOD;
				break;
			case IJavaElement.FIELD:
				contributionId= IJavaRefactorings.RENAME_FIELD;
				break;
			case IJavaElement.TYPE_PARAMETER:
				contributionId= IJavaRefactorings.RENAME_TYPE_PARAMETER;
				break;
			case IJavaElement.LOCAL_VARIABLE:
				contributionId= IJavaRefactorings.RENAME_LOCAL_VARIABLE;
				break;
			default:
				return null;
		}
		
		RenameJavaElementDescriptor descriptor= (RenameJavaElementDescriptor) RefactoringCore.getRefactoringContribution(contributionId).createDescriptor();
		descriptor.setJavaElement(javaElement);
		descriptor.setNewName(newName);
		if (elementType != IJavaElement.PACKAGE_FRAGMENT_ROOT)
			descriptor.setUpdateReferences(true);
		
		IDialogSettings javaSettings= JavaPlugin.getDefault().getDialogSettings();
		IDialogSettings refactoringSettings= javaSettings.getSection(RefactoringWizardPage.REFACTORING_SETTINGS); //TODO: undocumented API
		if (refactoringSettings == null) {
			refactoringSettings= javaSettings.addNewSection(RefactoringWizardPage.REFACTORING_SETTINGS); 
		}
		
		switch (elementType) {
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
				descriptor.setDeprecateDelegate(refactoringSettings.getBoolean(DelegateUIHelper.DELEGATE_DEPRECATION));
				descriptor.setKeepOriginal(refactoringSettings.getBoolean(DelegateUIHelper.DELEGATE_UPDATING));
		}
		switch (elementType) {
			case IJavaElement.TYPE:
//			case IJavaElement.COMPILATION_UNIT: // TODO
				descriptor.setUpdateSimilarDeclarations(refactoringSettings.getBoolean(RenameRefactoringWizard.TYPE_UPDATE_SIMILAR_ELEMENTS));
				int strategy;
				try {
					strategy= refactoringSettings.getInt(RenameRefactoringWizard.TYPE_SIMILAR_MATCH_STRATEGY);
				} catch (NumberFormatException e) {
					strategy= RenamingNameSuggestor.STRATEGY_EXACT;
				}
				descriptor.setMatchStrategy(strategy);
		}
		switch (elementType) {
			case IJavaElement.PACKAGE_FRAGMENT:
				descriptor.setUpdateHierarchy(refactoringSettings.getBoolean(RenameRefactoringWizard.PACKAGE_RENAME_SUBPACKAGES));
		}
		switch (elementType) {
			case IJavaElement.PACKAGE_FRAGMENT:
			case IJavaElement.TYPE:
				String fileNamePatterns= refactoringSettings.get(RenameRefactoringWizard.QUALIFIED_NAMES_PATTERNS);
				if (fileNamePatterns != null && fileNamePatterns.length() != 0) {
					descriptor.setFileNamePatterns(fileNamePatterns);
					descriptor.setUpdateQualifiedNames(refactoringSettings.getBoolean(RenameRefactoringWizard.UPDATE_QUALIFIED_NAMES));
				}
		}
		switch (elementType) {
			case IJavaElement.PACKAGE_FRAGMENT:
			case IJavaElement.TYPE:
			case IJavaElement.FIELD:
				descriptor.setUpdateTextualOccurrences(refactoringSettings.getBoolean(RenameRefactoringWizard.UPDATE_TEXTUAL_MATCHES));
		}
		switch (elementType) {
			case IJavaElement.FIELD:
				descriptor.setRenameGetters(refactoringSettings.getBoolean(RenameRefactoringWizard.FIELD_RENAME_GETTER));
				descriptor.setRenameSetters(refactoringSettings.getBoolean(RenameRefactoringWizard.FIELD_RENAME_SETTER));
		}
		return descriptor;
	}

	private void closePopup() {
		fgActiveLinkedMode= null;
		if (fPopup != null) {
			if (!fPopup.isDisposed()) {
				fPopup.close();
			}
			fPopup= null;
		}
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.unregister(fFocusEditingSupport);
		}
	}

	public void openSecondaryPopup() {
		ISourceViewer viewer= fEditor.getViewer();
		
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.register(fFocusEditingSupport);
		}
		
		Shell workbenchShell= fEditor.getSite().getShell();
		final Display display= workbenchShell.getDisplay();
		
		fPopup= new Shell(workbenchShell, SWT.ON_TOP | SWT.NO_TRIM);
		GridLayout shellLayout= new GridLayout();
		shellLayout.marginWidth= 1;
		shellLayout.marginHeight= 1;
		fPopup.setLayout(shellLayout);
		fPopup.setBackground(viewer.getTextWidget().getForeground());
		
		createTable(fPopup);
		
		fPopup.pack();
		setPopupLocation(viewer);
		
		addMoveSupport(fPopup, fPopup);
		new PopupVisibilityManager().start();
		fPopup.addShellListener(new ShellAdapter() {
			public void shellDeactivated(ShellEvent e) {
				final Shell editorShell= fEditor.getSite().getShell();
				display.asyncExec(new Runnable() {
					// post to UI thread since editor shell only gets activated after popup has lost focus
					public void run() {
						Shell activeShell= display.getActiveShell();
						if (activeShell != editorShell) {
							fLinkedModeModel.exit(ILinkedModeListener.NONE);
						}
					}
				});
			}
		});
		
		fPopup.setVisible(true);
	}

	private void setPopupLocation(ISourceViewer sourceViewer) {
		if (fPopup == null || fPopup.isDisposed())
			return;
		
		StyledText eWidget= sourceViewer.getTextWidget();
		Rectangle eBounds= eWidget.getClientArea();
		Point eLowerRight= eWidget.toDisplay(eBounds.x + eBounds.width, eBounds.y + eBounds.height);
		Point pSize= fPopup.getSize();
		/*
		 * possible improvement: try not to cover selection:
		 */
		fPopup.setLocation(eLowerRight.x - pSize.x - 5, eLowerRight.y - pSize.y - 5);
	}

	private static void addMoveSupport(final Shell shell, final Control control) {
		control.addMouseListener(new MouseAdapter() {
			private MouseMoveListener fMoveListener;

			public void mouseDown(final MouseEvent downEvent) {
				final Point location= shell.getLocation();
				fMoveListener= new MouseMoveListener() {
					public void mouseMove(MouseEvent moveEvent) {
						Point down= control.toDisplay(downEvent.x, downEvent.y);
						Point move= control.toDisplay(moveEvent.x, moveEvent.y);
						location.x= location.x + move.x - down.x;
						location.y= location.y + move.y - down.y;
						shell.setLocation(location);
					}
				};
				control.addMouseMoveListener(fMoveListener);
			}
			
			public void mouseUp(MouseEvent e) {
				control.removeMouseMoveListener(fMoveListener);
				fMoveListener= null;
			}
		});
	}
	
	private Control createTable(Composite parent) {
		final Display display= parent.getDisplay();
		
		Composite table= new Composite(parent, SWT.NONE);
		GridLayout tableLayout= new GridLayout(2, false);
		tableLayout.marginHeight= 5; 
		tableLayout.marginWidth= 5;
		tableLayout.horizontalSpacing= 10;
		table.setLayout(tableLayout);
		table.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, true));
		
		Hyperlink refactorLink= new Hyperlink(table, SWT.NONE);
		refactorLink.setText(ReorgMessages.RenameLinkedMode_refactor_rename);
		refactorLink.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		refactorLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				display.asyncExec(new Runnable() { //TODO: workaround for 157196: [Forms] Hyperlink listener notification throws AIOOBE when listener removed in callback
					public void run() {
						doRename(false);
					}
				});
			}
		});
		Label refactorBinding= new Label(table, SWT.NONE);
		String refactorBindingText= KeyStroke.getInstance(KeyLookupFactory.getDefault().formalKeyLookup(IKeyLookup.CR_NAME)).format();
		refactorBinding.setText(refactorBindingText);
		
		Hyperlink previewLink= new Hyperlink(table, SWT.NONE);
		previewLink.setText(ReorgMessages.RenameLinkedMode_preview);
		previewLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				display.asyncExec(new Runnable() { //TODO: workaround for 157196: [Forms] Hyperlink listener notification throws AIOOBE when listener removed in callback
					public void run() {
						doRename(true);
					}
				});
			}
		});
		Label previewBinding= new Label(table, SWT.NONE);
		String previewBindingText= KeyStroke.getInstance(SWT.CTRL, KeyLookupFactory.getDefault().formalKeyLookup(IKeyLookup.CR_NAME)).format();
		previewBinding.setText(previewBindingText);
		
		Hyperlink openDialogLink= new Hyperlink(table, SWT.NONE);
		openDialogLink.setText(ReorgMessages.RenameLinkedMode_open_dialog);
		openDialogLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				display.asyncExec(new Runnable() { //TODO: workaround for 157196: [Forms] Hyperlink listener notification throws AIOOBE when listener removed in callback
					public void run() {
						startFullDialog();
					}
				});
			}
		});
		Label openDialogBinding= new Label(table, SWT.NONE);
		String openDialogBindingString= getOpenDialogBinding();
		if (openDialogBindingString != null)
			openDialogBinding.setText(openDialogBindingString); 
		
		HyperlinkGroup hyperlinkGroup= new HyperlinkGroup(display);
		hyperlinkGroup.add(refactorLink);
		hyperlinkGroup.add(previewLink);
		hyperlinkGroup.add(openDialogLink);
		hyperlinkGroup.setForeground(fEditor.getViewer().getTextWidget().getForeground());
		
		recursiveSetBackgroundColor(table, fEditor.getViewer().getTextWidget().getBackground());
		
		Point size= table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		fPopup.setSize(size);
		
		addMoveSupport(fPopup, table);
		addMoveSupport(fPopup, refactorBinding);
		addMoveSupport(fPopup, previewBinding);
		addMoveSupport(fPopup, openDialogBinding);
		
		return table;
	}
	
	private String getOpenDialogBinding() {
		IBindingService bindingService= (IBindingService)PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		if (bindingService == null)
			return null;
		return bindingService.getBestActiveBindingFormattedFor(IJavaEditorActionDefinitionIds.RENAME_ELEMENT);
	}
	
	private void recursiveSetBackgroundColor(Control control, Color color) {
		control.setBackground(color);
		if (control instanceof Composite) {
			Control[] children= ((Composite) control).getChildren();
			for (int i= 0; i < children.length; i++) {
				recursiveSetBackgroundColor(children[i], color);
			}
		}
	}

}
