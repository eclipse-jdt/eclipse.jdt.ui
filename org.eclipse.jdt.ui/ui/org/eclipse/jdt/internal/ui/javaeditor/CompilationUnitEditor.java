/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.IWidgetTokenKeeper;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.AddBlockCommentAction;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.actions.IndentAction;
import org.eclipse.jdt.internal.ui.actions.RemoveBlockCommentAction;
import org.eclipse.jdt.internal.ui.compare.LocalHistoryActionGroup;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner;
import org.eclipse.jdt.internal.ui.text.SmartBackspaceManager;
import org.eclipse.jdt.internal.ui.text.Symbols;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionAssistant;
import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener;



/**
 * Java specific text editor.
 */
public class CompilationUnitEditor extends JavaEditor implements IJavaReconcilingListener {

	/** 
	 * Text operation code for requesting correction assist to show correction
	 * proposals for the current position. 
	 */
	public static final int CORRECTIONASSIST_PROPOSALS= 50;

	/** 
	 * Text operation code for requesting common prefix completion. 
	 */
	public static final int CONTENTASSIST_COMPLETE_PREFIX= 60;

	
	interface ITextConverter {
		void customizeDocumentCommand(IDocument document, DocumentCommand command);
	}
	
	class AdaptedSourceViewer extends JavaSourceViewer  {
				
		private List fTextConverters;
		private boolean fIgnoreTextConverters= false;
		private JavaCorrectionAssistant fCorrectionAssistant;
		
		public AdaptedSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean showAnnotationsOverview, int styles, IPreferenceStore store) {
			super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles, store);
		}
				
		public IContentAssistant getContentAssistant() {
			return fContentAssistant;
		}
		
		/*
		 * @see ITextOperationTarget#doOperation(int)
		 */
		public void doOperation(int operation) {
		
			if (getTextWidget() == null)
				return;
			
			switch (operation) {
				case CONTENTASSIST_PROPOSALS:
					String msg= fContentAssistant.showPossibleCompletions();
					setStatusLineErrorMessage(msg);
					return;
				case CORRECTIONASSIST_PROPOSALS:
					msg= fCorrectionAssistant.showPossibleCompletions();
					setStatusLineErrorMessage(msg);
					return;
				case UNDO:
					fIgnoreTextConverters= true;
					super.doOperation(operation);
					fIgnoreTextConverters= false;
					return;
				case REDO:
					fIgnoreTextConverters= true;
					super.doOperation(operation);
					fIgnoreTextConverters= false;
					return;
			}
			
			super.doOperation(operation);
		}
		
		/*
		 * @see ITextOperationTarget#canDoOperation(int)
		 */
		public boolean canDoOperation(int operation) {
			if (operation == CORRECTIONASSIST_PROPOSALS)
				return isEditable();
			
			return super.canDoOperation(operation);
		}
		
		/*
		 * @see org.eclipse.jface.text.source.ISourceViewerExtension2#unconfigure()
		 * @since 3.0
		 */
		public void unconfigure() {
			if (fCorrectionAssistant != null) {
				fCorrectionAssistant.uninstall();
				fCorrectionAssistant= null;
			}
			super.unconfigure();
		}
		
		public void insertTextConverter(ITextConverter textConverter, int index) {
			throw new UnsupportedOperationException();
		}
		
		public void addTextConverter(ITextConverter textConverter) {
			if (fTextConverters == null) {
				fTextConverters= new ArrayList(1);
				fTextConverters.add(textConverter);
			} else if (!fTextConverters.contains(textConverter))
				fTextConverters.add(textConverter);
		}
		
		public void removeTextConverter(ITextConverter textConverter) {
			if (fTextConverters != null) {
				fTextConverters.remove(textConverter);
				if (fTextConverters.size() == 0)
					fTextConverters= null;
			}
		}
		
		/*
		 * @see TextViewer#customizeDocumentCommand(DocumentCommand)
		 */
		protected void customizeDocumentCommand(DocumentCommand command) {
			super.customizeDocumentCommand(command);
			if (!fIgnoreTextConverters && fTextConverters != null) {
				for (Iterator e = fTextConverters.iterator(); e.hasNext();)
					((ITextConverter) e.next()).customizeDocumentCommand(getDocument(), command);
			}
		}
				
		// http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
		public void updateIndentationPrefixes() {
			SourceViewerConfiguration configuration= getSourceViewerConfiguration();
			String[] types= configuration.getConfiguredContentTypes(this);
			for (int i= 0; i < types.length; i++) {
				String[] prefixes= configuration.getIndentPrefixes(this, types[i]);
				if (prefixes != null && prefixes.length > 0)
					setIndentPrefixes(prefixes, types[i]);
			}
		}
		
		/*
		 * @see IWidgetTokenOwner#requestWidgetToken(IWidgetTokenKeeper)
		 */
		public boolean requestWidgetToken(IWidgetTokenKeeper requester) {
			if (WorkbenchHelp.isContextHelpDisplayed())
				return false;
			return super.requestWidgetToken(requester);
		}
		
		/*
		 * @see IWidgetTokenOwnerExtension#requestWidgetToken(IWidgetTokenKeeper, int)
		 * @since 3.0
		 */
		public boolean requestWidgetToken(IWidgetTokenKeeper requester, int priority) {
			if (WorkbenchHelp.isContextHelpDisplayed())
				return false;
			return super.requestWidgetToken(requester, priority);
		}

		/*
		 * @see org.eclipse.jface.text.source.ISourceViewer#configure(org.eclipse.jface.text.source.SourceViewerConfiguration)
		 */
		public void configure(SourceViewerConfiguration configuration) {
			super.configure(configuration);
			fCorrectionAssistant= new JavaCorrectionAssistant(CompilationUnitEditor.this);
			fCorrectionAssistant.install(this);
		}
		
		/*
		 * @see org.eclipse.jface.text.source.SourceViewer#createFormattingContext()
		 * @since 3.0
		 */
		public IFormattingContext createFormattingContext() {
			IFormattingContext context= new CommentFormattingContext();
			
			Map preferences;
			IJavaElement inputJavaElement= getInputJavaElement();
			IJavaProject javaProject= inputJavaElement != null ? inputJavaElement.getJavaProject() : null;
			if (javaProject == null)
				preferences= new HashMap(JavaCore.getOptions());
			else
				preferences= new HashMap(javaProject.getOptions(true));
			
			context.storeToMap(PreferenceConstants.getPreferenceStore(), preferences, false);
			context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, preferences);
			
			return context;
		}
	}
	
	static class TabConverter implements ITextConverter {
		
		private int fTabRatio;
		private ILineTracker fLineTracker;
		
		public TabConverter() {
		} 
		
		public void setNumberOfSpacesPerTab(int ratio) {
			fTabRatio= ratio;
		}
		
		public void setLineTracker(ILineTracker lineTracker) {
			fLineTracker= lineTracker;
		}
		
		private int insertTabString(StringBuffer buffer, int offsetInLine) {
			
			if (fTabRatio == 0)
				return 0;
				
			int remainder= offsetInLine % fTabRatio;
			remainder= fTabRatio - remainder;
			for (int i= 0; i < remainder; i++)
				buffer.append(' ');
			return remainder;
		}
		
		public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
			String text= command.text;
			if (text == null)
				return;
				
			int index= text.indexOf('\t');
			if (index > -1) {
				
				StringBuffer buffer= new StringBuffer();
				
				fLineTracker.set(command.text);
				int lines= fLineTracker.getNumberOfLines();
				
				try {
						
						for (int i= 0; i < lines; i++) {
							
							int offset= fLineTracker.getLineOffset(i);
							int endOffset= offset + fLineTracker.getLineLength(i);
							String line= text.substring(offset, endOffset);
							
							int position= 0;
							if (i == 0) {
								IRegion firstLine= document.getLineInformationOfOffset(command.offset);
								position= command.offset - firstLine.getOffset();	
							}
							
							int length= line.length();
							for (int j= 0; j < length; j++) {
								char c= line.charAt(j);
								if (c == '\t') {
									position += insertTabString(buffer, position);
								} else {
									buffer.append(c);
									++ position;
								}
							}
							
						}
						
						command.text= buffer.toString();
						
				} catch (BadLocationException x) {
				}
			}
		}
	}
	
	private class ExitPolicy implements IExitPolicy {
		
		final char fExitCharacter;
		final char fEscapeCharacter;
		final Stack fStack;
		final int fSize;
		
		public ExitPolicy(char exitCharacter, char escapeCharacter, Stack stack) {
			fExitCharacter= exitCharacter;
			fEscapeCharacter= escapeCharacter;
			fStack= stack;
			fSize= fStack.size();
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI.ExitPolicy#doExit(org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager, org.eclipse.swt.events.VerifyEvent, int, int)
		 */
		public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
			
			if (event.character == fExitCharacter) {
				
				if (fSize == fStack.size() && !isMasked(offset)) {
					BracketLevel level= (BracketLevel) fStack.peek();
					if (level.fFirstPosition.offset > offset || level.fSecondPosition.offset < offset)
						return null;
					if (level.fSecondPosition.offset == offset && length == 0)
						// don't enter the character if if its the closing peer
						return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
				}
			}
			return null;
		}
			
		private boolean isMasked(int offset) {
			IDocument document= getSourceViewer().getDocument();
			try {
				return fEscapeCharacter == document.getChar(offset - 1);
			} catch (BadLocationException e) {
			}
			return false;
		}
	}
	
	private static class BracketLevel {
		int fOffset;
		int fLength;
		LinkedModeUI fUI;
		Position fFirstPosition;
		Position fSecondPosition;
	}
	
	/**
	 * Position updater that takes any changes at the borders of a position to not belong to the position.
	 * 
	 * @since 3.0
	 */
	private static class ExclusivePositionUpdater implements IPositionUpdater {

		/** The position category. */
		private final String fCategory;

		/**
		 * Creates a new updater for the given <code>category</code>.
		 * 
		 * @param category the new category.
		 */
		public ExclusivePositionUpdater(String category) {
			fCategory= category;
		}

		/*
		 * @see org.eclipse.jface.text.IPositionUpdater#update(org.eclipse.jface.text.DocumentEvent)
		 */
		public void update(DocumentEvent event) {

			int eventOffset= event.getOffset();
			int eventOldLength= event.getLength();
			int eventNewLength= event.getText() == null ? 0 : event.getText().length();
			int deltaLength= eventNewLength - eventOldLength;

			try {
				Position[] positions= event.getDocument().getPositions(fCategory);

				for (int i= 0; i != positions.length; i++) {

					Position position= positions[i];

					if (position.isDeleted())
						continue;

					int offset= position.getOffset();
					int length= position.getLength();
					int end= offset + length;

					if (offset >= eventOffset + eventOldLength) 
						// position comes
						// after change - shift
						position.setOffset(offset + deltaLength);
					else if (end <= eventOffset) {
						// position comes way before change -
						// leave alone
					} else if (offset <= eventOffset && end >= eventOffset + eventOldLength) {
						// event completely internal to the position - adjust length
						position.setLength(length + deltaLength);
					} else if (offset < eventOffset) {
						// event extends over end of position - adjust length
						int newEnd= eventOffset;
						position.setLength(newEnd - offset);
					} else if (end > eventOffset + eventOldLength) {
						// event extends from before position into it - adjust offset
						// and length
						// offset becomes end of event, length adjusted accordingly
						int newOffset= eventOffset + eventNewLength;
						position.setOffset(newOffset);
						position.setLength(end - newOffset);
					} else {
						// event consumes the position - delete it
						position.delete();
					}
				}
			} catch (BadPositionCategoryException e) {
				// ignore and return
			}
		}

		/**
		 * Returns the position category.
		 * 
		 * @return the position category
		 */
		public String getCategory() {
			return fCategory;
		}

	}
	
	private class BracketInserter implements VerifyKeyListener, ILinkedModeListener {
		
		private boolean fCloseBrackets= true;
		private boolean fCloseStrings= true;
		private boolean fCloseAngularBrackets= true;
		private final String CATEGORY= toString();
		private IPositionUpdater fUpdater= new ExclusivePositionUpdater(CATEGORY);
		private Stack fBracketLevelStack= new Stack();

		public void setCloseBracketsEnabled(boolean enabled) {
			fCloseBrackets= enabled;
		}

		public void setCloseStringsEnabled(boolean enabled) {
			fCloseStrings= enabled;
		}

		public void setCloseAngularBracketsEnabled(boolean enabled) {
			fCloseAngularBrackets= enabled;
		}

		private boolean isAngularIntroducer(String identifier) {
			return identifier.length() > 0
					&& (Character.isUpperCase(identifier.charAt(0)) 
							|| identifier.startsWith("final") //$NON-NLS-1$
							|| identifier.startsWith("public") //$NON-NLS-1$
							|| identifier.startsWith("public") //$NON-NLS-1$
							|| identifier.startsWith("protected") //$NON-NLS-1$
							|| identifier.startsWith("private")); //$NON-NLS-1$
		}
		
		/*
		 * @see org.eclipse.swt.custom.VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
		 */
		public void verifyKey(VerifyEvent event) {			

			// early pruning to slow down normal typing as little as possible
			if (!event.doit || getInsertMode() != SMART_INSERT)
				return;
			switch (event.character) {
				case '(':
				case '<':
				case '[':
				case '\'':
				case '\"':
					break;
				default:
					return;
			}
				
			final ISourceViewer sourceViewer= getSourceViewer();
			IDocument document= sourceViewer.getDocument();

			final Point selection= sourceViewer.getSelectedRange();
			final int offset= selection.x;
			final int length= selection.y;
			
			try {
				IRegion startLine= document.getLineInformationOfOffset(offset);
				IRegion endLine= document.getLineInformationOfOffset(offset + length);
				
				JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
				int nextToken= scanner.nextToken(offset + length, endLine.getOffset() + endLine.getLength());
				String next= nextToken == Symbols.TokenEOF ? null : document.get(offset, scanner.getPosition() - offset).trim();
				int prevToken= scanner.previousToken(offset - 1, startLine.getOffset());
				int prevTokenOffset= scanner.getPosition() + 1;
				String previous= prevToken == Symbols.TokenEOF ? null : document.get(prevTokenOffset, offset - prevTokenOffset).trim();
				
				switch (event.character) {
					case '(':
						if (!fCloseBrackets
								|| nextToken == Symbols.TokenLPAREN
								|| nextToken == Symbols.TokenIDENT 
								|| next != null && next.length() > 1)
							return;
						break;
						
					case '<':
						if (!(fCloseAngularBrackets && fCloseBrackets)
								|| nextToken == Symbols.TokenLESSTHAN 
								|| 		   prevToken != Symbols.TokenLBRACE	
										&& prevToken != Symbols.TokenRBRACE 
										&& prevToken != Symbols.TokenSEMICOLON
										&& prevToken != Symbols.TokenSYNCHRONIZED
										&& prevToken != Symbols.TokenSTATIC
										&& (prevToken != Symbols.TokenIDENT || !isAngularIntroducer(previous))
										&& prevToken != Symbols.TokenEOF)
							return;
						break;
						
					case '[':
						if (!fCloseBrackets
								|| nextToken == Symbols.TokenIDENT 
								|| next != null && next.length() > 1)
							return;
						break;
						
					case '\'':
					case '"':
						if (!fCloseStrings
								|| nextToken == Symbols.TokenIDENT 
								|| prevToken == Symbols.TokenIDENT
								|| next != null && next.length() > 1
								|| previous != null && previous.length() > 1)
							return;
						break;
						
					default:
						return;
				}
				
				ITypedRegion partition= TextUtilities.getPartition(document, IJavaPartitions.JAVA_PARTITIONING, offset, true);
				if (!IDocument.DEFAULT_CONTENT_TYPE.equals(partition.getType()))
					return;
				
				if (!validateEditorInputState())
					return;
				
				final char character= event.character;
				final char closingCharacter= getPeerCharacter(character);
				final StringBuffer buffer= new StringBuffer();
				buffer.append(character);
				buffer.append(closingCharacter);
				
				document.replace(offset, length, buffer.toString());
				
				
				BracketLevel level= new BracketLevel();
				fBracketLevelStack.push(level);
				
				LinkedPositionGroup group= new LinkedPositionGroup(); 
				group.addPosition(new LinkedPosition(document, offset + 1, 0, LinkedPositionGroup.NO_STOP));
				
				LinkedModeModel model= new LinkedModeModel();
				model.addLinkingListener(this);
				model.addGroup(group);
				model.forceInstall();
				
				level.fOffset= offset;
				level.fLength= 2;
				
				// set up position tracking for our magic peers
				if (fBracketLevelStack.size() == 1) {
					document.addPositionCategory(CATEGORY);
					document.addPositionUpdater(fUpdater);
				}
				level.fFirstPosition= new Position(offset, 1);
				level.fSecondPosition= new Position(offset + 1, 1);
				document.addPosition(CATEGORY, level.fFirstPosition);
				document.addPosition(CATEGORY, level.fSecondPosition);
				
				level.fUI= new EditorLinkedModeUI(model, sourceViewer);
				level.fUI.setSimpleMode(true);
				level.fUI.setExitPolicy(new ExitPolicy(closingCharacter, getEscapeCharacter(closingCharacter), fBracketLevelStack));
				level.fUI.setExitPosition(sourceViewer, offset + 2, 0, Integer.MAX_VALUE);
				level.fUI.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
				level.fUI.enter();
				
				
				IRegion newSelection= level.fUI.getSelectedRegion();
				sourceViewer.setSelectedRange(newSelection.getOffset(), newSelection.getLength());
				
				event.doit= false;
				
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			} catch (BadPositionCategoryException e) {
				JavaPlugin.log(e);
			}
		}
		
		/*
		 * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.text.link.LinkedModeModel, int)
		 */
		public void left(LinkedModeModel environment, int flags) {
			
			final BracketLevel level= (BracketLevel) fBracketLevelStack.pop();

			if (flags != ILinkedModeListener.EXTERNAL_MODIFICATION)
				return;

			// remove brackets
			final ISourceViewer sourceViewer= getSourceViewer();
			final IDocument document= sourceViewer.getDocument();
			if (document instanceof IDocumentExtension) {
				IDocumentExtension extension= (IDocumentExtension) document;
				extension.registerPostNotificationReplace(null, new IDocumentExtension.IReplace() {
					
					public void perform(IDocument d, IDocumentListener owner) {
						if ((level.fFirstPosition.isDeleted || level.fFirstPosition.length == 0) 
								&& !level.fSecondPosition.isDeleted 
								&& level.fSecondPosition.offset == level.fFirstPosition.offset)
						{
							try {
								document.replace(level.fSecondPosition.offset, 
												 level.fSecondPosition.length, 
												 null);
							} catch (BadLocationException e) {
								JavaPlugin.log(e);
							}
						}
						
						if (fBracketLevelStack.size() == 0) {
							document.removePositionUpdater(fUpdater);
							try {
								document.removePositionCategory(CATEGORY);
							} catch (BadPositionCategoryException e) {
								JavaPlugin.log(e);
							}
						}
					}
				});
			}
					

		}

		/*
		 * @see org.eclipse.jface.text.link.ILinkedModeListener#suspend(org.eclipse.jface.text.link.LinkedModeModel)
		 */
		public void suspend(LinkedModeModel environment) {
		}

		/*
		 * @see org.eclipse.jface.text.link.ILinkedModeListener#resume(org.eclipse.jface.text.link.LinkedModeModel, int)
		 */
		public void resume(LinkedModeModel environment, int flags) {
		}
	}
	
	/**
	 * Remembers data related to the current selection to be able to
	 * restore it later.
	 * 
	 * @since 3.0
	 */
	private class RememberedSelection {
		/** The remembered selection start. */
		private RememberedOffset fStartOffset= new RememberedOffset();
		/** The remembered selection end. */
		private RememberedOffset fEndOffset= new RememberedOffset();

		/**
		 * Remember current selection.
		 */
		public void remember() {
			/* https://bugs.eclipse.org/bugs/show_bug.cgi?id=52257
			 * This method may be called inside an asynchronous call posted
			 * to the UI thread, so protect against intermediate disposal
			 * of the editor.
			 */
			ISourceViewer viewer= getSourceViewer();
			if (viewer != null) {
				IRegion selection= getSignedSelection(viewer);
				int startOffset= selection.getOffset();
				int endOffset= startOffset + selection.getLength();
				
				fStartOffset.setOffset(startOffset);
				fEndOffset.setOffset(endOffset);
			}
		}

		/**
		 * Restore remembered selection.
		 */
		public void restore() {
			/* https://bugs.eclipse.org/bugs/show_bug.cgi?id=52257
			 * This method may be called inside an asynchronous call posted
			 * to the UI thread, so protect against intermediate disposal
			 * of the editor.
			 */
			if (getSourceViewer() == null)
				return;
			
			try {
				
				int startOffset, endOffset;
				int revealStartOffset, revealEndOffset; 
				if (showsHighlightRangeOnly()) {
					IJavaElement newStartElement= fStartOffset.getElement();
					startOffset= fStartOffset.getRememberedOffset(newStartElement);
					revealStartOffset= fStartOffset.getRevealOffset(newStartElement, startOffset);
					if (revealStartOffset == -1)
						startOffset= -1;
					
					IJavaElement newEndElement= fEndOffset.getElement();
					endOffset= fEndOffset.getRememberedOffset(newEndElement);
					revealEndOffset= fEndOffset.getRevealOffset(newEndElement, endOffset);
					if (revealEndOffset == -1)
						endOffset= -1;
				} else {
					startOffset= fStartOffset.getOffset();
					revealStartOffset= startOffset;
					endOffset= fEndOffset.getOffset();
					revealEndOffset= endOffset;
				}
				
				if (startOffset == -1) {
					startOffset= endOffset; // fallback to caret offset
					revealStartOffset= revealEndOffset;
				}
				
				if (endOffset == -1) {
					endOffset= startOffset; // fallback to other offset
					revealEndOffset= revealStartOffset;
				}
				
				IJavaElement element;
				if (endOffset == -1) {
					 // fallback to element selection
					element= fEndOffset.getElement();
					if (element == null)
						element= fStartOffset.getElement();
					if (element != null)
						setSelection(element);
					return;
				}
							
				if (isValidSelection(revealStartOffset, revealEndOffset - revealStartOffset) && isValidSelection(startOffset, endOffset - startOffset))
					selectAndReveal(startOffset, endOffset - startOffset, revealStartOffset, revealEndOffset - revealStartOffset);
			} finally {
				fStartOffset.clear();
				fEndOffset.clear();
			}
		}

		private boolean isValidSelection(int offset, int length) {
			IDocumentProvider provider= getDocumentProvider();
			if (provider != null) {
				IDocument document= provider.getDocument(getEditorInput());
				if (document != null) {
					int end= offset + length;
					int documentLength= document.getLength();
					return 0 <= offset  && offset <= documentLength && 0 <= end && end <= documentLength;
				}
			}
			return false;
		}
		
	}

	/**
	 * Remembers additional data for a given
	 * offset to be able restore it later.
	 * 
	 * @since 3.0
	 */
	private class RememberedOffset {
		/** Remembered line for the given offset */
		private int fLine;
		/** Remembered column for the given offset*/
		private int fColumn;
		/** Remembered Java element for the given offset*/
		private IJavaElement fElement;
		/** Remembered Java element line for the given offset*/
		private int fElementLine;
		
		/**
		 * Store visual properties of the given offset.  
		 * 
		 * @param offset Offset in the document
		 */
		public void setOffset(int offset) {
			try {
				IDocument document= getSourceViewer().getDocument();
				fLine= document.getLineOfOffset(offset);
				fColumn= offset - document.getLineOffset(fLine);
				fElement= getElementAt(offset, true);

				fElementLine= -1;
				if (fElement instanceof IMember) {
					ISourceRange range= ((IMember) fElement).getNameRange();
					if (range != null)
						fElementLine= document.getLineOfOffset(range.getOffset());
				}
				if (fElementLine == -1)
					fElementLine= document.getLineOfOffset(getOffset(fElement));
			} catch (BadLocationException e) {
				// should not happen
				JavaPlugin.log(e);
				clear();
			} catch (JavaModelException e) {
				// should not happen
				JavaPlugin.log(e.getStatus());
				clear();
			}
		}

		/**
		 * Return offset recomputed from stored visual properties.  
		 * 
		 * @return Offset in the document
		 */
		public int getOffset() {
			IJavaElement newElement= getElement();
			
			int offset= getRememberedOffset(newElement);
			
			if (offset != -1 && !containsOffset(newElement, offset) && (offset == 0 || !containsOffset(newElement, offset - 1)))
				return -1;
			
			return offset;
		}
		
		/**
		 * Return offset recomputed from stored visual properties.  
		 * 
		 * @param newElement Enclosing element
		 * @return Offset in the document
		 */
		public int getRememberedOffset(IJavaElement newElement) {
			try {
				if (newElement == null)
					return -1;
				
				IDocument document= getSourceViewer().getDocument();
				int newElementLine= -1;
				if (newElement instanceof IMember) {
					ISourceRange range= ((IMember) newElement).getNameRange();
					if (range != null)
						newElementLine= document.getLineOfOffset(range.getOffset());
				}
				if (newElementLine == -1)
					newElementLine= document.getLineOfOffset(getOffset(newElement));
				if (newElementLine == -1)
					return -1;

				int newLine= fLine + newElementLine - fElementLine;
				if (newLine < 0 || newLine >= document.getNumberOfLines())
					return -1;
				int maxColumn= document.getLineLength(newLine);
				String lineDelimiter= document.getLineDelimiter(newLine);
				if (lineDelimiter != null)
					maxColumn= maxColumn - lineDelimiter.length();
				int offset;
				if (fColumn > maxColumn)
					offset= document.getLineOffset(newLine) + maxColumn;
				else
					offset= document.getLineOffset(newLine) + fColumn;

				return offset;
			} catch (BadLocationException e) {
				// should not happen
				JavaPlugin.log(e);
				return -1;
			} catch (JavaModelException e) {
				// should not happen
				JavaPlugin.log(e.getStatus());
				return -1;
			}
		}
		
		/**
		 * Returns the offset used to reveal the given element based on the given selection offset.
		 * @param element the element
		 * @param offset the selection offset
		 * @return the offset to reveal the given element based on the given selection offset
		 */
		public int getRevealOffset(IJavaElement element, int offset) {
			if (element == null || offset == -1)
				return -1;

			if (containsOffset(element, offset)) {
				if (offset > 0) {
					IJavaElement alternateElement= getElementAt(offset, false);
					if (element.getHandleIdentifier().equals(alternateElement.getParent().getHandleIdentifier()))
						return offset - 1; // Solves test case 2 from https://bugs.eclipse.org/bugs/show_bug.cgi?id=47727#c3
				}
				return offset;
			} else if (offset > 0 && containsOffset(element, offset - 1))
				return offset - 1; // Solves test case 1 from https://bugs.eclipse.org/bugs/show_bug.cgi?id=47727#c3
			
			return -1;
		}

		/**
		 * Return Java element recomputed from stored visual properties.  
		 * 
		 * @return Java element
		 */
		public IJavaElement getElement() {
			if (fElement == null)
				return null;
			
			return findElement(fElement);
		}

		/**
		 * Clears the stored position 
		 */
		public void clear() {
			fLine= -1;
			fColumn= -1;
			fElement= null;
			fElementLine= -1;
		}
		
		/**
		 * Does the given Java element contain the given offset?
		 * @param element Java element
		 * @param offset Offset
		 * @return <code>true</code> iff the Java element contains the offset
		 */
		private boolean containsOffset(IJavaElement element, int offset) {
			int elementOffset= getOffset(element);
			int elementLength= getLength(element);
			return (elementOffset > -1 && elementLength > -1) ? (offset >= elementOffset && offset < elementOffset + elementLength) : false;
		}
		/**
		 * Returns the offset of the given Java element.
		 * 
		 * @param element	Java element
		 * @return Offset of the given Java element
		 */
		private int getOffset(IJavaElement element) {
			if (element instanceof ISourceReference) {
				ISourceReference sr= (ISourceReference) element;
				try {
					ISourceRange srcRange= sr.getSourceRange();
					if (srcRange != null)
						return srcRange.getOffset();
				} catch (JavaModelException e) {
				}
			}
			return -1;	
		}
		
		/**
		 * Returns the length of the given Java element.
		 * 
		 * @param element	Java element
		 * @return Length of the given Java element
		 */
		private int getLength(IJavaElement element) {
			if (element instanceof ISourceReference) {
				ISourceReference sr= (ISourceReference) element;
				try {
					ISourceRange srcRange= sr.getSourceRange();
					if (srcRange != null)
						return srcRange.getLength();
				} catch (JavaModelException e) {
				}
			}
			return -1;	
		}
		
		/**
		 * Returns the updated java element for the old java element.
		 * 
		 * @param element Old Java element
		 * @return Updated Java element
		 */
		private IJavaElement findElement(IJavaElement element) {
			
			if (element == null)
				return null;
			
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
			ICompilationUnit unit= manager.getWorkingCopy(getEditorInput());
			
			if (unit != null) {
				try {
					
					synchronized (unit) {
						unit.reconcile(ICompilationUnit.NO_AST, false, null, null);
					}
					IJavaElement[] findings= unit.findElements(element);
					if (findings != null && findings.length > 0)
						return findings[0];
				
				} catch (JavaModelException x) {
					JavaPlugin.log(x.getStatus());
					// nothing found, be tolerant and go on
				}
			}
			
			return null;
		}
		
	}
	
	/** Preference key for code formatter tab size */
	private final static String CODE_FORMATTER_TAB_SIZE= DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
	/** Preference key for inserting spaces rather than tabs */
	private final static String SPACES_FOR_TABS= DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR;
	/** Preference key for automatically closing strings */
	private final static String CLOSE_STRINGS= PreferenceConstants.EDITOR_CLOSE_STRINGS;
	/** Preference key for automatically closing brackets and parenthesis */
	private final static String CLOSE_BRACKETS= PreferenceConstants.EDITOR_CLOSE_BRACKETS;
	
	
	/** The editor's save policy */
	protected ISavePolicy fSavePolicy;
	/** Listener to annotation model changes that updates the error tick in the tab image */
	private JavaEditorErrorTickUpdater fJavaEditorErrorTickUpdater;
	/** The editor's tab converter */
	private TabConverter fTabConverter;
	/**
	 * The remembered selection.
	 * @since 3.0
	 */
	private RememberedSelection fRememberedSelection= new RememberedSelection();
	/** The bracket inserter. */
	private BracketInserter fBracketInserter= new BracketInserter();

	/** The standard action groups added to the menu */
	private GenerateActionGroup fGenerateActionGroup;
	private CompositeActionGroup fContextMenuGroup;
	
	/**
	 * Reconciling listeners.
	 * @since 3.0
	 */
	private ListenerList fReconcilingListeners= new ListenerList();

	/**
	 * Mutex for the reconciler. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=63898
	 * for a description of the problem.
	 * <p>
	 * TODO remove once the underlying problem is solved.
	 * </p>
	 */
	private final Object fReconcilerLock= new Object();

	
	
	/**
	 * Creates a new compilation unit editor.
	 */
	public CompilationUnitEditor() {
		super();
		setDocumentProvider(JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
		setEditorContextMenuId("#CompilationUnitEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#CompilationUnitRulerContext"); //$NON-NLS-1$
		setOutlinerContextMenuId("#CompilationUnitOutlinerContext"); //$NON-NLS-1$
		// don't set help contextId, we install our own help context
		fSavePolicy= null;
			
		fJavaEditorErrorTickUpdater= new JavaEditorErrorTickUpdater(this);
	}
	
	/*
	 * @see AbstractTextEditor#createActions()
	 */
	protected void createActions() {
		
		super.createActions();

		Action action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "CorrectionAssistProposal.", this, CORRECTIONASSIST_PROPOSALS); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.CORRECTION_ASSIST_PROPOSALS);		
		setAction("CorrectionAssistProposal", action); //$NON-NLS-1$
		markAsStateDependentAction("CorrectionAssistProposal", true); //$NON-NLS-1$
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.QUICK_FIX_ACTION);

		action= new ContentAssistAction(JavaEditorMessages.getResourceBundle(), "ContentAssistProposal.", this); //$NON-NLS-1$
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);		
		setAction("ContentAssistProposal", action); //$NON-NLS-1$
		markAsStateDependentAction("ContentAssistProposal", true); //$NON-NLS-1$
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.CONTENT_ASSIST_ACTION);

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ContentAssistContextInformation.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);	//$NON-NLS-1$
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);		
		setAction("ContentAssistContextInformation", action); //$NON-NLS-1$
		markAsStateDependentAction("ContentAssistContextInformation", true); //$NON-NLS-1$
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.PARAMETER_HINTS_ACTION);

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "Comment.", this, ITextOperationTarget.PREFIX); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.COMMENT);		
		setAction("Comment", action); //$NON-NLS-1$
		markAsStateDependentAction("Comment", true); //$NON-NLS-1$
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.COMMENT_ACTION);

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "Uncomment.", this, ITextOperationTarget.STRIP_PREFIX); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.UNCOMMENT);		
		setAction("Uncomment", action); //$NON-NLS-1$
		markAsStateDependentAction("Uncomment", true); //$NON-NLS-1$
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.UNCOMMENT_ACTION);

		action= new ToggleCommentAction(JavaEditorMessages.getResourceBundle(), "ToggleComment.", this); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.TOGGLE_COMMENT);		
		setAction("ToggleComment", action); //$NON-NLS-1$
		markAsStateDependentAction("ToggleComment", true); //$NON-NLS-1$
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.TOGGLE_COMMENT_ACTION);
		configureToggleCommentAction();

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "Format.", this, ISourceViewer.FORMAT); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);		
		setAction("Format", action); //$NON-NLS-1$
		markAsStateDependentAction("Format", true); //$NON-NLS-1$
		markAsSelectionDependentAction("Format", true); //$NON-NLS-1$		
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.FORMAT_ACTION);
		
		action= new AddBlockCommentAction(JavaEditorMessages.getResourceBundle(), "AddBlockComment.", this);  //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.ADD_BLOCK_COMMENT);		
		setAction("AddBlockComment", action); //$NON-NLS-1$
		markAsStateDependentAction("AddBlockComment", true); //$NON-NLS-1$
		markAsSelectionDependentAction("AddBlockComment", true); //$NON-NLS-1$		
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.ADD_BLOCK_COMMENT_ACTION);

		action= new RemoveBlockCommentAction(JavaEditorMessages.getResourceBundle(), "RemoveBlockComment.", this);  //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.REMOVE_BLOCK_COMMENT);		
		setAction("RemoveBlockComment", action); //$NON-NLS-1$
		markAsStateDependentAction("RemoveBlockComment", true); //$NON-NLS-1$
		markAsSelectionDependentAction("RemoveBlockComment", true); //$NON-NLS-1$		
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.REMOVE_BLOCK_COMMENT_ACTION);
		
		action= new IndentAction(JavaEditorMessages.getResourceBundle(), "Indent.", this, false); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.INDENT);		
		setAction("Indent", action); //$NON-NLS-1$
		markAsStateDependentAction("Indent", true); //$NON-NLS-1$
		markAsSelectionDependentAction("Indent", true); //$NON-NLS-1$
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.INDENT_ACTION);
		
		action= new IndentAction(JavaEditorMessages.getResourceBundle(), "Indent.", this, true); //$NON-NLS-1$
		setAction("IndentOnTab", action); //$NON-NLS-1$
		markAsStateDependentAction("IndentOnTab", true); //$NON-NLS-1$
		markAsSelectionDependentAction("IndentOnTab", true); //$NON-NLS-1$
		
		if (getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_TAB)) {
			// don't replace Shift Right - have to make sure their enablement is mutually exclusive
//			removeActionActivationCode(ITextEditorActionConstants.SHIFT_RIGHT);
			setActionActivationCode("IndentOnTab", '\t', -1, SWT.NONE); //$NON-NLS-1$
		}
		
		fGenerateActionGroup= new GenerateActionGroup(this, ITextEditorActionConstants.GROUP_EDIT);
		ActionGroup rg= new RefactorActionGroup(this, ITextEditorActionConstants.GROUP_EDIT);
		
		fActionGroups.addGroup(rg);
		fActionGroups.addGroup(fGenerateActionGroup);
		
		// We have to keep the context menu group separate to have better control over positioning
		fContextMenuGroup= new CompositeActionGroup(new ActionGroup[] {
			fGenerateActionGroup, 
			rg, 
			new LocalHistoryActionGroup(this, ITextEditorActionConstants.GROUP_EDIT)});
	}

	/*
	 * @see JavaEditor#getElementAt(int)
	 */
	protected IJavaElement getElementAt(int offset) {
		return getElementAt(offset, true);
	}
	
	/**
	 * Returns the most narrow element including the given offset.  If <code>reconcile</code>
	 * is <code>true</code> the editor's input element is reconciled in advance. If it is 
	 * <code>false</code> this method only returns a result if the editor's input element
	 * does not need to be reconciled.
	 * 
	 * @param offset the offset included by the retrieved element
	 * @param reconcile <code>true</code> if working copy should be reconciled
	 * @return the most narrow element which includes the given offset
	 */
	protected IJavaElement getElementAt(int offset, boolean reconcile) {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit unit= manager.getWorkingCopy(getEditorInput());
		
		if (unit != null) {
			try {
				if (reconcile) {
					synchronized (unit) {
						unit.reconcile(ICompilationUnit.NO_AST, false, null, null);
					}
					return unit.getElementAt(offset);
				} else if (unit.isConsistent())
					return unit.getElementAt(offset);
					
			} catch (JavaModelException x) {
				if (!x.isDoesNotExist())
				JavaPlugin.log(x.getStatus());
				// nothing found, be tolerant and go on
			}
		}
		
		return null;
	}
	
	/*
	 * @see JavaEditor#getCorrespondingElement(IJavaElement)
	 */
	protected IJavaElement getCorrespondingElement(IJavaElement element) {
		// TODO: With new working copy story: original == working copy.
		// Note that the previous code could result in a reconcile as side effect. Should check if that
		// is still required.
		return element;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor#getInputElement()
	 */
	protected IJavaElement getInputJavaElement() {
		return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(getEditorInput());
	}
	
	/*
	 * @see AbstractTextEditor#editorContextMenuAboutToShow(IMenuManager)
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);		
				
		ActionContext context= new ActionContext(getSelectionProvider().getSelection());
		fContextMenuGroup.setContext(context);
		fContextMenuGroup.fillContextMenu(menu);
		fContextMenuGroup.setContext(null);
	}
	
	/*
	 * @see JavaEditor#setOutlinePageInput(JavaOutlinePage, IEditorInput)
	 */
	protected void setOutlinePageInput(JavaOutlinePage page, IEditorInput input) {
		if (page != null) {
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
			page.setInput(manager.getWorkingCopy(input));
		}
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#performSave(boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
		IDocumentProvider p= getDocumentProvider();
		if (p instanceof ICompilationUnitDocumentProvider) {
			ICompilationUnitDocumentProvider cp= (ICompilationUnitDocumentProvider) p;
			cp.setSavePolicy(fSavePolicy);
		}
		try {
			super.performSave(overwrite, progressMonitor);
		} finally {
			if (p instanceof ICompilationUnitDocumentProvider) {
				ICompilationUnitDocumentProvider cp= (ICompilationUnitDocumentProvider) p;
				cp.setSavePolicy(null);
			}
		}
	}
	
	/*
	 * @see AbstractTextEditor#doSave(IProgressMonitor)
	 */
	public void doSave(IProgressMonitor progressMonitor) {
		
		IDocumentProvider p= getDocumentProvider();
		if (p == null) {
			// editor has been closed
			return;
		}
			
		if (p.isDeleted(getEditorInput())) {
			
			if (isSaveAsAllowed()) {
				
				/*
				 * 1GEUSSR: ITPUI:ALL - User should never loose changes made in the editors.
				 * Changed Behavior to make sure that if called inside a regular save (because
				 * of deletion of input element) there is a way to report back to the caller.
				 */
				 performSaveAs(progressMonitor);
			
			} else {
				
				/* 
				 * 1GF5YOX: ITPJUI:ALL - Save of delete file claims it's still there
				 * Missing resources.
				 */
				Shell shell= getSite().getShell();
				MessageDialog.openError(shell, JavaEditorMessages.getString("CompilationUnitEditor.error.saving.title1"), JavaEditorMessages.getString("CompilationUnitEditor.error.saving.message1")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
		} else {	
			
			setStatusLineErrorMessage(null);
			
			updateState(getEditorInput());
			validateState(getEditorInput());
			
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
			ICompilationUnit unit= manager.getWorkingCopy(getEditorInput());
			
			if (unit != null) {
				synchronized (unit) { 
					performSave(false, progressMonitor); 
				}
			} else 
				performSave(false, progressMonitor);
		}
	}
	
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	/**
	 * The compilation unit editor implementation of this  <code>AbstractTextEditor</code>
	 * method asks the user for the workspace path of a file resource and saves the document
	 * there. See http://dev.eclipse.org/bugs/show_bug.cgi?id=6295
	 * 
	 * @param progressMonitor the progress monitor
	 */
	protected void performSaveAs(IProgressMonitor progressMonitor) {
		
		Shell shell= getSite().getShell();
		IEditorInput input = getEditorInput();
		
		SaveAsDialog dialog= new SaveAsDialog(shell);
		
		IFile original= (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
		if (original != null)
			dialog.setOriginalFile(original);
			
		dialog.create();
		
			
		IDocumentProvider provider= getDocumentProvider();
		if (provider == null) {
			// editor has been programmatically closed while the dialog was open
			return;
		}
		
		if (provider.isDeleted(input) && original != null) {
			String message= JavaEditorMessages.getFormattedString("CompilationUnitEditor.warning.save.delete", new Object[] { original.getName() }); //$NON-NLS-1$
			dialog.setErrorMessage(null);
			dialog.setMessage(message, IMessageProvider.WARNING);
		}
			
		if (dialog.open() == Window.CANCEL) {
			if (progressMonitor != null)
				progressMonitor.setCanceled(true);
			return;
		}
			
		IPath filePath= dialog.getResult();
		if (filePath == null) {
			if (progressMonitor != null)
				progressMonitor.setCanceled(true);
			return;
		}
			
		IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
		IFile file= workspaceRoot.getFile(filePath);
		final IEditorInput newInput= new FileEditorInput(file);
		
		boolean success= false;
		try {
			
			provider.aboutToChange(newInput);
			getDocumentProvider().saveDocument(progressMonitor, newInput, getDocumentProvider().getDocument(getEditorInput()), true);
			success= true;
			
		} catch (CoreException x) {
			IStatus status= x.getStatus();
			if (status == null || status.getSeverity() != IStatus.CANCEL)
				ErrorDialog.openError(shell, JavaEditorMessages.getString("CompilationUnitEditor.error.saving.title2"), JavaEditorMessages.getString("CompilationUnitEditor.error.saving.message2"), x.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			provider.changed(newInput);
			if (success)
				setInput(newInput);
		}
		
		if (progressMonitor != null)
			progressMonitor.setCanceled(!success);
	}
	
	/*
	 * @see AbstractTextEditor#doSetInput(IEditorInput)
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		configureTabConverter();
		configureToggleCommentAction();
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor#installOverrideIndicator(boolean)
	 * @since 3.0
	 */
	protected void installOverrideIndicator(boolean provideAST) {
		super.installOverrideIndicator(provideAST);
		
		if (fOverrideIndicatorManager == null)
			return;
		
		addReconcileListener(fOverrideIndicatorManager);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor#uninstallOverrideIndicator()
	 * @since 3.0
	 */
	protected void uninstallOverrideIndicator() {
		if (fOverrideIndicatorManager != null)
			removeReconcileListener(fOverrideIndicatorManager);
		super.uninstallOverrideIndicator();
	}

	/**
	 * Configures the toggle comment action
	 * 
	 * @since 3.0
	 */
	private void configureToggleCommentAction() {
		IAction action= getAction("ToggleComment"); //$NON-NLS-1$
		if (action instanceof ToggleCommentAction) {
			ISourceViewer sourceViewer= getSourceViewer();
			SourceViewerConfiguration configuration= getSourceViewerConfiguration();
			((ToggleCommentAction)action).configure(sourceViewer, configuration);
		}
	}

	private void configureTabConverter() {
		if (fTabConverter != null) {
			IDocumentProvider provider= getDocumentProvider();
			if (provider instanceof ICompilationUnitDocumentProvider) {
				ICompilationUnitDocumentProvider cup= (ICompilationUnitDocumentProvider) provider;
				fTabConverter.setLineTracker(cup.createLineTracker(getEditorInput()));
			}
		}
	}
	
	private int getTabSize() {
		Preferences preferences= JavaCore.getPlugin().getPluginPreferences();
		return preferences.getInt(CODE_FORMATTER_TAB_SIZE);	
	}
	
	private void startTabConversion() {
		if (fTabConverter == null) {
			fTabConverter= new TabConverter();
			configureTabConverter();
			fTabConverter.setNumberOfSpacesPerTab(getTabSize());
			AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
			asv.addTextConverter(fTabConverter);
			// http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
			asv.updateIndentationPrefixes();
		}
	}
	
	private void stopTabConversion() {
		if (fTabConverter != null) {
			AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
			asv.removeTextConverter(fTabConverter);
			// http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
			asv.updateIndentationPrefixes();
			fTabConverter= null;
		}
	}
	
	private boolean isTabConversionEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return JavaCore.SPACE.equals(store.getString(SPACES_FOR_TABS));
	}
	
	public void dispose() {

		ISourceViewer sourceViewer= getSourceViewer();
		if (sourceViewer instanceof ITextViewerExtension)
			((ITextViewerExtension) sourceViewer).removeVerifyKeyListener(fBracketInserter);

		if (fJavaEditorErrorTickUpdater != null) {
			fJavaEditorErrorTickUpdater.dispose();
			fJavaEditorErrorTickUpdater= null;
		}
		
		if (fActionGroups != null) {
			fActionGroups.dispose();
			fActionGroups= null;
		}
			
		super.dispose();
	}
	
	/*
	 * @see AbstractTextEditor#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		
		super.createPartControl(parent);
					
		if (isTabConversionEnabled())
			startTabConversion();			
			
		IPreferenceStore preferenceStore= getPreferenceStore();
		boolean closeBrackets= preferenceStore.getBoolean(CLOSE_BRACKETS);
		boolean closeStrings= preferenceStore.getBoolean(CLOSE_STRINGS);
		boolean closeAngularBrackets= JavaCore.VERSION_1_5.compareTo(preferenceStore.getString(JavaCore.COMPILER_SOURCE)) <= 0;
		
		fBracketInserter.setCloseBracketsEnabled(closeBrackets);
		fBracketInserter.setCloseStringsEnabled(closeStrings);
		fBracketInserter.setCloseAngularBracketsEnabled(closeAngularBrackets);
		
		ISourceViewer sourceViewer= getSourceViewer();
		if (sourceViewer instanceof ITextViewerExtension)
			((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(fBracketInserter);
		
	}
	
	private static char getEscapeCharacter(char character) {
		switch (character) {
			case '"':
			case '\'':
				return '\\';
			default:
				return 0;
		}
	}
	
	private static char getPeerCharacter(char character) {
		switch (character) {
			case '(':
				return ')';
				
			case ')':
				return '(';
				
			case '<':
				return '>';
				
			case '>':
				return '<';
				
			case '[':
				return ']';

			case ']':
				return '[';
				
			case '"':
				return character;
				
			case '\'':
				return character;
			
			default:
				throw new IllegalArgumentException();
		}					
	}
	
	/*
	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		
		try {
			
			AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
			if (asv != null) {
					
				String p= event.getProperty();		
				
				if (CLOSE_BRACKETS.equals(p)) {
					fBracketInserter.setCloseBracketsEnabled(getPreferenceStore().getBoolean(p));
					return;	
				}

				if (CLOSE_STRINGS.equals(p)) {
					fBracketInserter.setCloseStringsEnabled(getPreferenceStore().getBoolean(p));
					return;
				}
				
				if (JavaCore.COMPILER_SOURCE.equals(p)) {
					boolean closeAngularBrackets= JavaCore.VERSION_1_5.compareTo(getPreferenceStore().getString(p)) <= 0;
					fBracketInserter.setCloseAngularBracketsEnabled(closeAngularBrackets);
				}
								
				if (SPACES_FOR_TABS.equals(p)) {
					if (isTabConversionEnabled())
						startTabConversion();
					else
						stopTabConversion();
					return;
				}
				
				if (PreferenceConstants.EDITOR_SMART_TAB.equals(p)) {
					if (getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_TAB)) {
						setActionActivationCode("IndentOnTab", '\t', -1, SWT.NONE); //$NON-NLS-1$
					} else {
						removeActionActivationCode("IndentOnTab"); //$NON-NLS-1$
					}
				}

				IContentAssistant c= asv.getContentAssistant();
				if (c instanceof ContentAssistant)
					ContentAssistPreference.changeConfiguration((ContentAssistant) c, getPreferenceStore(), event);

				if (CODE_FORMATTER_TAB_SIZE.equals(p)) {
					asv.updateIndentationPrefixes();
					if (fTabConverter != null)
						fTabConverter.setNumberOfSpacesPerTab(getTabSize());
				}
			}
				
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor#createJavaSourceViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler, org.eclipse.jface.text.source.IOverviewRuler, boolean, int)
	 */
	protected ISourceViewer createJavaSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean isOverviewRulerVisible, int styles, IPreferenceStore store) {
		return new AdaptedSourceViewer(parent, verticalRuler, overviewRuler, isOverviewRulerVisible, styles, store);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#aboutToBeReconciled()
	 * @since 3.0
	 */
	public void aboutToBeReconciled() {

		// Notify AST provider
		JavaPlugin.getDefault().getASTProvider().aboutToBeReconciled(getInputJavaElement());
		
		// Notify listeners
		Object[] listeners = fReconcilingListeners.getListeners();
		for (int i = 0, length= listeners.length; i < length; ++i)
			((IJavaReconcilingListener)listeners[i]).aboutToBeReconciled();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#reconciled(CompilationUnit, boolean, IProgressMonitor)
	 * @since 3.0
	 */
	public void reconciled(CompilationUnit ast, boolean forced, IProgressMonitor progressMonitor) {

		// Always notify AST provider
		JavaPlugin.getDefault().getASTProvider().reconciled(ast, getInputJavaElement());
		
		// Notify listeners
		Object[] listeners = fReconcilingListeners.getListeners();
		for (int i = 0, length= listeners.length; i < length; ++i)
			((IJavaReconcilingListener)listeners[i]).reconciled(ast, forced, progressMonitor);

		// Update Java Outline page selection
		if (!forced && !progressMonitor.isCanceled()) {
			Shell shell= getSite().getShell();
			if (shell != null && !shell.isDisposed()) {
				shell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						selectionChanged();
					}
				});
			}
		}
	}
	
	/**
	 * Tells whether this is the active editor in the active page.
	 *
	 * @return <code>true</code> if this is the active editor in the active page
	 * @see IWorkbenchPage#getActiveEditor
	 */
	protected final boolean isActiveEditor() {
		IWorkbenchWindow window= getSite().getWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		if (page == null)
			return false;
		IEditorPart activeEditor= page.getActiveEditor();
		return activeEditor != null && activeEditor.equals(this);
	}
	
	/**
	 * Adds the given listener.
	 * Has no effect if an identical listener was not already registered.
	 * 
	 * @param listener	The reconcile listener to be added
	 * @since 3.0
	 */
	final void addReconcileListener(IJavaReconcilingListener listener) {
		synchronized (fReconcilingListeners) {
			fReconcilingListeners.add(listener);
		}
	}
	
	/**
	 * Removes the given listener.
	 * Has no effect if an identical listener was not already registered.
	 * 
	 * @param listener	the reconcile listener to be removed
	 * @since 3.0
	 */
	final void removeReconcileListener(IJavaReconcilingListener listener) {
		synchronized (fReconcilingListeners) {
			fReconcilingListeners.remove(listener);
		}
	}
		
	protected void updateStateDependentActions() {
		super.updateStateDependentActions();
		fGenerateActionGroup.editorStateChanged();
	}
	
	/*
	 * @see AbstractTextEditor#rememberSelection()
	 */
	protected void rememberSelection() {
		fRememberedSelection.remember();
	}
	
	/*
	 * @see AbstractTextEditor#restoreSelection()
	 */
	protected void restoreSelection() {
		fRememberedSelection.restore();
	}
	
	/*
	 * @see AbstractTextEditor#canHandleMove(IEditorInput, IEditorInput)
	 */
	protected boolean canHandleMove(IEditorInput originalElement, IEditorInput movedElement) {
		
		String oldExtension= ""; //$NON-NLS-1$
		if (originalElement instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) originalElement).getFile();
			if (file != null) {
				String ext= file.getFileExtension();
				if (ext != null)
					oldExtension= ext;
			}
		}
		
		String newExtension= ""; //$NON-NLS-1$
		if (movedElement instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) movedElement).getFile();
			if (file != null)
				newExtension= file.getFileExtension();
		}
		
		return oldExtension.equals(newExtension);
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#isPrefQuickDiffAlwaysOn()
	 */
	protected boolean isPrefQuickDiffAlwaysOn() {
		// reestablishes the behavior from AbstractDecoratedTextEditor which was hacked by JavaEditor
		// to disable the change bar for the class file (attached source) java editor.
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.QUICK_DIFF_ALWAYS_ON);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class required) {
		if (SmartBackspaceManager.class.equals(required)) {
			if (getSourceViewer() instanceof JavaSourceViewer) {
				return ((JavaSourceViewer) getSourceViewer()).getBackspaceManager();
			}
		}

		return super.getAdapter(required);
	}

	/**
	 * Returns the mutex for the reconciler. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=63898
	 * for a description of the problem.
	 * <p>
	 * TODO remove once the underlying problem is solved.
	 * </p>
	 * @return the lock reconcilers may use to synchronize on
	 */
	public Object getReconcilerLock() {
		return fReconcilerLock;
	}
	
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor#createNavigationActions()
	 */
	protected void createNavigationActions() {
		super.createNavigationActions();
		
		final StyledText textWidget= getSourceViewer().getTextWidget();

		IAction action= new DeletePreviousSubWordAction();
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD);
		setAction(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD, action);
		textWidget.setKeyBinding(SWT.CTRL | SWT.BS, SWT.NULL);
		markAsStateDependentAction(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD, true);

		action= new DeleteNextSubWordAction();
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD);
		setAction(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD, action);
		textWidget.setKeyBinding(SWT.CTRL | SWT.DEL, SWT.NULL);
		markAsStateDependentAction(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD, true);
	}

}
