/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.java.hover;

import java.util.Iterator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Geometry;

import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaAnnotationIterator;


/**
 * Abstract super class for annotation hovers.
 *
 * @since 3.0
 */
public abstract class AbstractAnnotationHover extends AbstractJavaEditorTextHover {

	/**
	 * An annotation info contains information about an {@link Annotation}
	 * It's used as input for the {@link AbstractAnnotationHover.AnnotationInformationControl}
	 * 
	 * @since 3.4
	 */
	private static class AnnotationInfo {
		public final Annotation annotation;
		public final Position position;
		public final IDocument document;

		public AnnotationInfo(Annotation annotation, Position position, IDocument document) {
			this.annotation= annotation;
			this.position= position;
			this.document= document;
		}
	}

	/**
	 * The annotation information control shows informations about a given
	 * {@link AbstractAnnotationHover.AnnotationInfo}. It can also show a toolbar
	 * and a list of {@link ICompletionProposal}s.
	 * 
	 * @since 3.4
	 */
	private class AnnotationInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {

		private final DefaultMarkerAnnotationAccess fMarkerAnnotationAccess;
		private Control fFocusControl;
		private Image fAnnotationImage;
		private Image fScaledAnnotationImage;
		private AnnotationInfo fInput;
		private Composite fParent;

		public AnnotationInformationControl(Shell parentShell, int shellStyle, String statusFieldText) {
			super(parentShell, shellStyle, statusFieldText);
			
			fMarkerAnnotationAccess= new DefaultMarkerAnnotationAccess();
		}

		public AnnotationInformationControl(Shell parentShell, int shellStyle, ToolBarManager toolBarManager) {
			super(parentShell, shellStyle, toolBarManager);
			
			fMarkerAnnotationAccess= new DefaultMarkerAnnotationAccess();
		}
		
		/*
		 * @see org.eclipse.jface.text.IInformationControl#setInformation(java.lang.String)
		 */
		public void setInformation(String information) {
			//replaced by IInformationControlExtension2#setInput
		}

		/*
		 * @see org.eclipse.jface.text.IInformationControlExtension2#setInput(java.lang.Object)
		 */
		public void setInput(Object input) {
			Assert.isLegal(input instanceof AnnotationInfo);
			fInput= (AnnotationInfo)input;
			
			deferredCreateToolbar(getToolBarManager());
			deferredCreateContent(fParent);
		}
		
		private AnnotationInfo getAnnotationInfo() {
			return fInput;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover.AbstractInformationControl#setFocus()
		 */
		public void setFocus() {
			super.setFocus();
			if (fFocusControl != null)
				fFocusControl.setFocus();
		}

		/*
		 * @see org.eclipse.jface.text.AbstractInformationControl#createContent(org.eclipse.swt.widgets.Composite)
		 */
		protected void createContent(Composite parent) {
			fParent= new Composite(parent, SWT.NONE);
			fParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			GridLayout layout= new GridLayout(1, false);
			layout.verticalSpacing= 0;
			fParent.setLayout(layout);
		}
		
		/*
		 * @see org.eclipse.jface.text.AbstractInformationControl#computeSizeHint()
		 */
		public Point computeSizeHint() {
			Point preferedSize= getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

			Point constrains= getSizeConstraints();
			if (constrains == null)
				return preferedSize;
				
			return Geometry.min(preferedSize, constrains);
		}

		/**
		 * Creates the toolbar actions, if a toolbar if available. This
		 * is called after the input has been set.
		 * 
		 * @param toolBarManager the tool bar manager or <code>null</code>
		 */
		private void deferredCreateToolbar(ToolBarManager toolBarManager) {
			if (toolBarManager == null)
				return;

			fillToolBar(toolBarManager, fInput.annotation, fInput.position, fInput.document);
			toolBarManager.update(true);
		}

		/**
		 * Create content of the hover. This is called after
		 * the input has been set.
		 * 
		 * @param parent the composite containing the content to create
		 */
		protected void deferredCreateContent(Composite parent) {
			createAnnotationInformation(parent, getAnnotationInfo().annotation);

			ICompletionProposal[] proposals= getCompletionProposals(getAnnotationInfo().annotation, getAnnotationInfo().position);
			if (proposals.length > 0)
				createCompletionProposalsControl(parent, getAnnotationInfo().document, proposals);

			setColor(parent, parent.getForeground(), parent.getBackground());
		}

		private void setColor(Control control, Color foreground, Color background) {
			control.setForeground(foreground);
			control.setBackground(background);

			if (control instanceof Composite) {
				Control[] children= ((Composite) control).getChildren();
				for (int i= 0; i < children.length; i++) {
					setColor(children[i], foreground, background);
				}
			}
		}

		private void createAnnotationInformation(Composite parent, final Annotation annotation) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			GridLayout layout= new GridLayout(2, false);
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			composite.setLayout(layout);

			final Canvas canvas= new Canvas(composite, SWT.NONE);
			GridData gridData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
			gridData.widthHint= 16;
			gridData.heightHint= 16;
			canvas.setLayoutData(gridData);
			canvas.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					fMarkerAnnotationAccess.paint(annotation, e.gc, canvas, new Rectangle(0, 0, 16, 16));
				}
			});
			
			Text text= new Text(composite, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			text.setText(annotation.getText());
		}

		private void createCompletionProposalsControl(Composite parent, final IDocument document, ICompletionProposal[] proposals) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			GridLayout layout2= new GridLayout(2, false);
			layout2.marginHeight= 2;
			layout2.marginWidth= 2;
			layout2.verticalSpacing= 2;
			composite.setLayout(layout2);

			Label seperator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			GridData gridData= new GridData(SWT.FILL, SWT.CENTER, true, false);
			gridData.horizontalSpan= 2;
			seperator.setLayoutData(gridData);

			Label quickFixImage= new Label(composite, SWT.NONE);
			quickFixImage.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
			quickFixImage.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_QUICK_FIX));

			Label quickFixLabel= new Label(composite, SWT.NONE);
			quickFixLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
			String text;
			if (proposals.length == 1) {
				text= JavaHoverMessages.AbstractAnnotationHover_message_singleQuickFix;
			} else {
				text= Messages.format(JavaHoverMessages.AbstractAnnotationHover_message_multipleQuickFix, new Object[] { String.valueOf(proposals.length) });
			}
			quickFixLabel.setText(text);

			createCompletionProposalsList(composite, proposals, document);
		}

		private void createCompletionProposalsList(Composite parent, ICompletionProposal[] proposals, final IDocument document) {
			final ScrolledComposite scrolledComposite= new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
			GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
			gridData.horizontalSpan= 2;
			scrolledComposite.setLayoutData(gridData);
			scrolledComposite.setExpandVertical(false);
			scrolledComposite.setExpandHorizontal(false);

			Composite composite= new Composite(scrolledComposite, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			GridLayout layout= new GridLayout(3, false);
			layout.verticalSpacing= 2;
			composite.setLayout(layout);

			final Link[] links= new Link[proposals.length];
			for (int i= 0; i < proposals.length; i++) {
				Label indent= new Label(composite, SWT.NONE);
				GridData gridData1= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
				gridData1.widthHint= 0;
				indent.setLayoutData(gridData1);

				links[i]= createCompletionProposalLink(composite, proposals[i], document);
			}

			scrolledComposite.setContent(composite);
			Point contentSize= composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			composite.setSize(contentSize);
			Point scrollSize= scrolledComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			gridData.heightHint= contentSize.y - (scrollSize.y - contentSize.y);

			fFocusControl= links[0];
			for (int i= 0; i < links.length; i++) {
				final int index= i;
				final Link link= links[index];
				link.addKeyListener(new KeyListener() {
					public void keyPressed(KeyEvent e) {
						switch (e.keyCode) {
							case SWT.ARROW_DOWN:
								if (index + 1 < links.length) {
									links[index + 1].setFocus();
								}
								break;
							case SWT.ARROW_UP:
								if (index > 0) {
									links[index - 1].setFocus();
								}
								break;
							default:
								break;
						}
					}

					public void keyReleased(KeyEvent e) {
					}
				});

				link.addFocusListener(new FocusListener() {
					public void focusGained(FocusEvent e) {
						int currentPosition= scrolledComposite.getOrigin().y;
						int hight= scrolledComposite.getSize().y;
						int linkPosition= link.getLocation().y;

						if (linkPosition < currentPosition) {
							if (linkPosition < 10)
								linkPosition= 0;

							scrolledComposite.setOrigin(0, linkPosition);
						} else if (linkPosition + 20 > currentPosition + hight) {
							scrolledComposite.setOrigin(0, linkPosition - hight + link.getSize().y);
						}
					}

					public void focusLost(FocusEvent e) {
					}
				});
			}
		}

		private Link createCompletionProposalLink(Composite parent, final ICompletionProposal proposal, final IDocument document) {
			Label proposalImage= new Label(parent, SWT.NONE);
			proposalImage.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
			Image image= proposal.getImage();
			if (image != null) {
				proposalImage.setImage(image);

				proposalImage.addMouseListener(new MouseListener() {

					public void mouseDoubleClick(MouseEvent e) {
					}

					public void mouseDown(MouseEvent e) {
					}

					public void mouseUp(MouseEvent e) {
						if (e.button == 1) {
							apply(proposal, document);
						}
					}

				});
			}

			Link proposalLink= new Link(parent, SWT.WRAP);
			proposalLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
			proposalLink.setText("<a>" + proposal.getDisplayString() + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			proposalLink.addSelectionListener(new SelectionAdapter() {
				/*
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				public void widgetSelected(SelectionEvent e) {
					apply(proposal, document);
				}
			});

			return proposalLink;
		}

		private void apply(ICompletionProposal proposal, IDocument document) {
			//Focus needs to be in the text viewer, otherwise linked mode does not work
			dispose();

			proposal.apply(document);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover.AbstractInformationControl#dispose()
		 */
		public void dispose() {
			super.dispose();

			if (fAnnotationImage != null) {
				fAnnotationImage.dispose();
				fAnnotationImage= null;
			}

			if (fScaledAnnotationImage != null) {
				fScaledAnnotationImage.dispose();
				fScaledAnnotationImage= null;
			}
		}
	}

	/**
	 * Presenter control creator.
	 *
	 * @since 3.4
	 */
	private final class PresenterControlCreator extends AbstractReusableInformationControlCreator {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#doCreateInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		public IInformationControl doCreateInformationControl(Shell parent) {
			return new AnnotationInformationControl(parent, SWT.RESIZE | SWT.TOOL, new ToolBarManager(SWT.FLAT));
		}
	}


	/**
	 * Hover control creator.
	 *
	 * @since 3.4
	 */
	private final class HoverControlCreator implements IInformationControlCreator {
		/*
		 * @see org.eclipse.jface.text.IInformationControlCreator#createInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		public IInformationControl createInformationControl(Shell parent) {
			return new AnnotationInformationControl(parent, SWT.TOOL, EditorsUI.getTooltipAffordanceString());
		}
	}
	
	/**
	 * Action to configure the annotation preferences.
	 * 
	 * @since 3.4
	 */
	private final class ConfigureAnnotationsAction extends Action {

		private final Annotation fAnnotation;

		public ConfigureAnnotationsAction(Annotation annotation) {
			super();
			fAnnotation= annotation;
			setImageDescriptor(JavaPluginImages.DESC_ELCL_CONFIGURE_ANNOTATIONS);
			setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CONFIGURE_ANNOTATIONS);
			setToolTipText(JavaHoverMessages.AbstractAnnotationHover_action_configureAnnotationPreferences);
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			
			Object data= null;
			AnnotationPreference preference= getAnnotationPreference(fAnnotation);
			if (preference != null)
				data= preference.getPreferenceLabel();
			
			PreferencesUtil.createPreferenceDialogOn(shell, "org.eclipse.ui.editors.preferencePages.Annotations", null, data).open(); //$NON-NLS-1$
		}
	}

	private final IPreferenceStore fStore= JavaPlugin.getDefault().getCombinedPreferenceStore();
	private final DefaultMarkerAnnotationAccess fAnnotationAccess= new DefaultMarkerAnnotationAccess();
	private final boolean fAllAnnotations;

	public AbstractAnnotationHover(boolean allAnnotations) {
		fAllAnnotations= allAnnotations;
	}

	/**
	 * Create completion proposals which can resolve the given annotation at
	 * the given position. Returns an empty array if no such proposals exist
	 * 
	 * @param annotation the annotation to resolve
	 * @param position the position of the annotation
	 * @return the proposals or an empty array
	 * @since 3.4
	 */
	protected ICompletionProposal[] getCompletionProposals(Annotation annotation, Position position) {
		return new ICompletionProposal[0];
	}
	
	/**
	 * Adds actions to the given toolbar. The actions can operate on the given annotation
	 * which is located in the given document at the given position.
	 * 
	 * @param manager the toolbar manager to add actions to
	 * @param annotation the annotation to operate on
	 * @param position the position of the annotation
	 * @param document the document containing the annotation
	 */
	protected void fillToolBar(ToolBarManager manager, Annotation annotation, Position position, IDocument document) {
		ConfigureAnnotationsAction configureAnnotationsAction= new ConfigureAnnotationsAction(annotation);
		manager.add(configureAnnotationsAction);
	}

	/*
	 * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover#getHoverInfo2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 * @since 3.4
	 */
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		IPath path;
		IAnnotationModel model;
		if (textViewer instanceof ISourceViewer) {
			path= null;
			model= ((ISourceViewer)textViewer).getAnnotationModel();
		} else {
			// Get annotation model from file buffer manager
			path= getEditorInputPath();
			model= getAnnotationModel(path);
		}
		if (model == null)
			return null;

		try {
			Iterator e= new JavaAnnotationIterator(model, true, fAllAnnotations);
			int layer= -1;
			Annotation annotation= null;
			Position position= null;
			while (e.hasNext()) {
				Annotation a= (Annotation) e.next();

				AnnotationPreference preference= getAnnotationPreference(a);
				if (preference == null || !(preference.getTextPreferenceKey() != null && fStore.getBoolean(preference.getTextPreferenceKey()) || (preference.getHighlightPreferenceKey() != null && fStore.getBoolean(preference.getHighlightPreferenceKey()))))
					continue;

				Position p= model.getPosition(a);

				int l= fAnnotationAccess.getLayer(a);

				if (l > layer && p != null && p.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
					String msg= a.getText();
					if (msg != null && msg.trim().length() > 0) {
						layer= l;
						annotation= a;
						position= p;
					}
				}
			}
			if (layer > -1)
				return new AnnotationInfo(annotation, position, textViewer.getDocument());

		} finally {
			try {
				if (path != null) {
					ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
					manager.disconnect(path, LocationKind.NORMALIZE, null);
				}
			} catch (CoreException ex) {
				JavaPlugin.log(ex.getStatus());
			}
		}

		return null;
	}

	/*
	 * @see ITextHoverExtension#getHoverControlCreator()
	 * @since 3.4
	 */
	public IInformationControlCreator getHoverControlCreator() {
		return new HoverControlCreator();
	}

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension2#getInformationPresenterControlCreator()
	 * @since 3.4
	 */
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return new PresenterControlCreator();
	}

	private IPath getEditorInputPath() {
		if (getEditor() == null)
			return null;

		IEditorInput input= getEditor().getEditorInput();
		if (input instanceof IStorageEditorInput) {
			try {
				return ((IStorageEditorInput)input).getStorage().getFullPath();
			} catch (CoreException ex) {
				JavaPlugin.log(ex.getStatus());
			}
		}
		return null;
	}

	private IAnnotationModel getAnnotationModel(IPath path) {
		if (path == null)
			return null;

		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(path, LocationKind.NORMALIZE, null);
		} catch (CoreException ex) {
			JavaPlugin.log(ex.getStatus());
			return null;
		}

		IAnnotationModel model= null;
		try {
			model= manager.getTextFileBuffer(path, LocationKind.NORMALIZE).getAnnotationModel();
			return model;
		} finally {
			if (model == null) {
				try {
					manager.disconnect(path, LocationKind.NORMALIZE, null);
				} catch (CoreException ex) {
					JavaPlugin.log(ex.getStatus());
				}
			}
		}
	}

	/**
	 * Returns the annotation preference for the given annotation.
	 *
	 * @param annotation the annotation
	 * @return the annotation preference or <code>null</code> if none
	 */
	private AnnotationPreference getAnnotationPreference(Annotation annotation) {

		if (annotation.isMarkedDeleted())
			return null;
		return EditorsUI.getAnnotationPreferenceLookup().getAnnotationPreference(annotation);
	}
}
