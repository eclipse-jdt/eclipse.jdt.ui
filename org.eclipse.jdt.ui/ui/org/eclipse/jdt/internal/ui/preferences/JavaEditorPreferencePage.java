/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.WorkbenchChainedTextFontFieldEditor;

import org.eclipse.jdt.ui.text.IJavaColorConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;


/*
 * The page for setting the editor options.
 */
public class JavaEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private static final String BOLD= "_bold"; //$NON-NLS-1$
	
	public final OverlayPreferenceStore.OverlayKey[] fKeys= new OverlayPreferenceStore.OverlayKey[] {
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, JavaSourceViewerConfiguration.PREFERENCE_TAB_WIDTH),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_MULTI_LINE_COMMENT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_MULTI_LINE_COMMENT + BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT + BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_KEYWORD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_KEYWORD + BOLD),
				
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_STRING),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_STRING + BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_DEFAULT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_DEFAULT + BOLD),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVADOC_KEYWORD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVADOC_KEYWORD + BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVADOC_TAG),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVADOC_TAG + BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVADOC_LINK),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVADOC_LINK + BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVADOC_DEFAULT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVADOC_DEFAULT + BOLD),
				
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, CompilationUnitEditor.MATCHING_BRACKETS_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, CompilationUnitEditor.MATCHING_BRACKETS),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, CompilationUnitEditor.CURRENT_LINE_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, CompilationUnitEditor.CURRENT_LINE),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, CompilationUnitEditor.PRINT_MARGIN_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, CompilationUnitEditor.PRINT_MARGIN_COLUMN),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, CompilationUnitEditor.PRINT_MARGIN),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractTextEditor.PREFERENCE_COLOR_FIND_SCOPE),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, CompilationUnitEditor.LINKED_POSITION_COLOR),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, CompilationUnitEditor.PROBLEM_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, CompilationUnitEditor.PROBLEM_INDICATION),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, CompilationUnitDocumentProvider.HANDLE_TEMPRARY_PROBELMS),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, CompilationUnitEditor.OVERVIEW_RULER),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, CompilationUnitEditor.SPACES_FOR_TABS),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.AUTOACTIVATION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, ContentAssistPreference.AUTOACTIVATION_DELAY),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.AUTOINSERT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.PROPOSALS_BACKGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.PROPOSALS_FOREGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.PARAMETERS_BACKGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.PARAMETERS_FOREGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVA),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVADOC),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.SHOW_VISIBLE_PROPOSALS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.ORDER_PROPOSALS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.CASE_SENSITIVITY),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.ADD_IMPORT)
		
	};
	
	private final String[][] fListModel= new String[][] {
		{ JavaUIMessages.getString("JavaEditorPreferencePage.multiLineComment"), IJavaColorConstants.JAVA_MULTI_LINE_COMMENT }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.singleLineComment"), IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.keywords"), IJavaColorConstants.JAVA_KEYWORD }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.strings"), IJavaColorConstants.JAVA_STRING }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.others"), IJavaColorConstants.JAVA_DEFAULT }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.javaDocKeywords"), IJavaColorConstants.JAVADOC_KEYWORD }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.javaDocHtmlTags"), IJavaColorConstants.JAVADOC_TAG }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.javaDocLinks"), IJavaColorConstants.JAVADOC_LINK }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.javaDocOthers"), IJavaColorConstants.JAVADOC_DEFAULT } //$NON-NLS-1$
	};
	
	private OverlayPreferenceStore fOverlayStore;
	private JavaTextTools fJavaTextTools;
	
	private Map fColorButtons= new HashMap();
	private SelectionListener fColorButtonListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			ColorEditor editor= (ColorEditor) e.widget.getData();
			PreferenceConverter.setValue(fOverlayStore, (String) fColorButtons.get(editor), editor.getColorValue());
		}
	};
	
	private Map fCheckBoxes= new HashMap();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};
	
	private Map fTextFields= new HashMap();
	private ArrayList fNumberFields= new ArrayList();
	private ModifyListener fTextFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			if (!e.widget.isDisposed())
				textChanged((Text) e.widget);
		}
	};
	
	private WorkbenchChainedTextFontFieldEditor fFontEditor;
	private List fList;
	private ColorEditor fForegroundColorEditor;
	private ColorEditor fBackgroundColorEditor;
	private Button fBackgroundDefaultRadioButton;
	private Button fBackgroundCustomRadioButton;
	private Button fBackgroundColorButton;
	private Button fBoldCheckBox;
	private SourceViewer fPreviewViewer;
	
	private Button fBracketHighlightButton;
	private Control fBracketHighlightColor;
	private Button fLineHighlightButton;
	private Control fLineHighlightColor;
	private Button fPrintMarginButton;
	private Control fPrintMarginColor;
	private Control fPrintMarginColumn;
	private Button fProblemIndicationButton;
	private Control fProblemIndicationColor;
	private Control fFindScopeColor;
	private Control fLinkedPositionColor;

	
	public JavaEditorPreferencePage() {
		setDescription(JavaUIMessages.getString("JavaEditorPreferencePage.description")); //$NON-NLS-1$
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		fOverlayStore= new OverlayPreferenceStore(getPreferenceStore(), fKeys);
	}
	
	public static void initDefaults(IPreferenceStore store) {
		
		/* 
		 * Ensure that the display is accessed only in the UI thread.
		 * Ensure that there are no side effects of switching the thread.
		 */
		final RGB[] rgbs= new RGB[3];
		final Display display= Display.getDefault();
		display.syncExec(new Runnable() {
			public void run() {
				Color c= display.getSystemColor(SWT.COLOR_GRAY);
				rgbs[0]= c.getRGB();
				c= display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
				rgbs[1]= c.getRGB();
				c= display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
				rgbs[2]= c.getRGB();
			}
		});
		
		
		/* 
		 * Go on in whatever thread this is. 
		 */
		store.setDefault(CompilationUnitEditor.MATCHING_BRACKETS, true);
		PreferenceConverter.setDefault(store, CompilationUnitEditor.MATCHING_BRACKETS_COLOR,  rgbs[0]);
		
		store.setDefault(CompilationUnitEditor.CURRENT_LINE, true);
		PreferenceConverter.setDefault(store, CompilationUnitEditor.CURRENT_LINE_COLOR, new RGB(225, 235, 224));
		
		store.setDefault(CompilationUnitEditor.PRINT_MARGIN, false);
		store.setDefault(CompilationUnitEditor.PRINT_MARGIN_COLUMN, 80);
		PreferenceConverter.setDefault(store, CompilationUnitEditor.PRINT_MARGIN_COLOR, new RGB(176, 180 , 185));

		PreferenceConverter.setDefault(store, AbstractTextEditor.PREFERENCE_COLOR_FIND_SCOPE, new RGB(185, 176 , 180));
		
		store.setDefault(CompilationUnitEditor.PROBLEM_INDICATION, true);
		PreferenceConverter.setDefault(store, CompilationUnitEditor.PROBLEM_INDICATION_COLOR, new RGB(255, 0 , 128));
		
		store.setDefault(CompilationUnitDocumentProvider.HANDLE_TEMPRARY_PROBELMS, true);
		
		store.setDefault(CompilationUnitEditor.OVERVIEW_RULER, true);
		
		WorkbenchChainedTextFontFieldEditor.startPropagate(store, JFaceResources.TEXT_FONT);
		
		PreferenceConverter.setDefault(store,  AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND, rgbs[1]);
		store.setDefault(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT, true);
		
		PreferenceConverter.setDefault(store,  AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, rgbs[2]);		
		store.setDefault(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT, true);
		
		store.setDefault(JavaSourceViewerConfiguration.PREFERENCE_TAB_WIDTH, 4);
		
		store.setDefault(CompilationUnitEditor.SPACES_FOR_TABS, false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_MULTI_LINE_COMMENT, new RGB(63, 127, 95));
		store.setDefault(IJavaColorConstants.JAVA_MULTI_LINE_COMMENT + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT, new RGB(63, 127, 95));
		store.setDefault(IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_KEYWORD, new RGB(127, 0, 85));
		store.setDefault(IJavaColorConstants.JAVA_KEYWORD + "_bold", true);
				
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_STRING, new RGB(42, 0, 255));
		store.setDefault(IJavaColorConstants.JAVA_STRING + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_DEFAULT, new RGB(0, 0, 0));
		store.setDefault(IJavaColorConstants.JAVA_DEFAULT + "_bold", false);

		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_KEYWORD, new RGB(127, 159, 191));
		store.setDefault(IJavaColorConstants.JAVADOC_KEYWORD + "_bold", true);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_TAG, new RGB(127, 127, 159));
		store.setDefault(IJavaColorConstants.JAVADOC_TAG + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_LINK, new RGB(63, 63, 191));
		store.setDefault(IJavaColorConstants.JAVADOC_LINK + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_DEFAULT, new RGB(63, 95, 191));
		store.setDefault(IJavaColorConstants.JAVADOC_DEFAULT + "_bold", false);		
		
		store.setDefault(ContentAssistPreference.AUTOACTIVATION, true);
		store.setDefault(ContentAssistPreference.AUTOACTIVATION_DELAY, 500);
		
		store.setDefault(ContentAssistPreference.AUTOINSERT, true);
		PreferenceConverter.setDefault(store, ContentAssistPreference.PROPOSALS_BACKGROUND, new RGB(254, 241, 233));
		PreferenceConverter.setDefault(store, ContentAssistPreference.PROPOSALS_FOREGROUND, new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, ContentAssistPreference.PARAMETERS_BACKGROUND, new RGB(254, 241, 233));
		PreferenceConverter.setDefault(store, ContentAssistPreference.PARAMETERS_FOREGROUND, new RGB(0, 0, 0));
		store.setDefault(ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVA, ".");
		store.setDefault(ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVADOC, "@");
		store.setDefault(ContentAssistPreference.SHOW_VISIBLE_PROPOSALS, true);
		store.setDefault(ContentAssistPreference.CASE_SENSITIVITY, false);
		store.setDefault(ContentAssistPreference.ORDER_PROPOSALS, false);
		store.setDefault(ContentAssistPreference.ADD_IMPORT, true);				
	}

	/*
	 * @see IWorkbenchPreferencePage#init()
	 */	
	public void init(IWorkbench workbench) {
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.JAVA_EDITOR_PREFERENCE_PAGE);
	}

	private void handleListSelection() {	
		int i= fList.getSelectionIndex();
		String key= fListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fForegroundColorEditor.setColorValue(rgb);		
		fBoldCheckBox.setSelection(fOverlayStore.getBoolean(key + BOLD));
	}
	
	private Control createColorPage(Composite parent) {
		
		Composite colorComposite= new Composite(parent, SWT.NULL);
		colorComposite.setLayout(new GridLayout());

		Composite backgroundComposite= new Composite(colorComposite, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		backgroundComposite.setLayout(layout);

		Label label= new Label(backgroundComposite, SWT.NULL);
		label.setText(JavaUIMessages.getString("JavaEditorPreferencePage.backgroundColor")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);

		SelectionListener backgroundSelectionListener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {				
				boolean custom= fBackgroundCustomRadioButton.getSelection();
				fBackgroundColorButton.setEnabled(custom);
				fOverlayStore.setValue(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT, !custom);
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		};

		fBackgroundDefaultRadioButton= new Button(backgroundComposite, SWT.RADIO | SWT.LEFT);
		fBackgroundDefaultRadioButton.setText(JavaUIMessages.getString("JavaEditorPreferencePage.systemDefault")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalSpan= 2;
		fBackgroundDefaultRadioButton.setLayoutData(gd);
		fBackgroundDefaultRadioButton.addSelectionListener(backgroundSelectionListener);

		fBackgroundCustomRadioButton= new Button(backgroundComposite, SWT.RADIO | SWT.LEFT);
		fBackgroundCustomRadioButton.setText(JavaUIMessages.getString("JavaEditorPreferencePage.custom")); //$NON-NLS-1$
		fBackgroundCustomRadioButton.addSelectionListener(backgroundSelectionListener);

		fBackgroundColorEditor= new ColorEditor(backgroundComposite);
		fBackgroundColorButton= fBackgroundColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		fBackgroundColorButton.setLayoutData(gd);

		label= new Label(colorComposite, SWT.LEFT);
		label.setText(JavaUIMessages.getString("JavaEditorPreferencePage.foreground")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite= new Composite(colorComposite, SWT.NULL);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		gd= new GridData(GridData.FILL_BOTH);
		editorComposite.setLayoutData(gd);		

		fList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL);
		gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(5);
		fList.setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NULL);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText(JavaUIMessages.getString("JavaEditorPreferencePage.color")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);

		fForegroundColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fForegroundColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);
		
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText(JavaUIMessages.getString("JavaEditorPreferencePage.bold")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);
		
		fBoldCheckBox= new Button(stylesComposite, SWT.CHECK);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		fBoldCheckBox.setLayoutData(gd);
		
		label= new Label(colorComposite, SWT.LEFT);
		label.setText(JavaUIMessages.getString("JavaEditorPreferencePage.preview")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Control previewer= createPreviewer(colorComposite);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(80);
		gd.heightHint= convertHeightInCharsToPixels(15);
		previewer.setLayoutData(gd);

		
		fList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				handleListSelection();
			}
		});
		
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fList.getSelectionIndex();
				String key= fListModel[i][1];
				
				PreferenceConverter.setValue(fOverlayStore, key, fForegroundColorEditor.getColorValue());
			}
		});

		fBackgroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				PreferenceConverter.setValue(fOverlayStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, fBackgroundColorEditor.getColorValue());					
			}
		});

		fBoldCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fList.getSelectionIndex();
				String key= fListModel[i][1];
				fOverlayStore.setValue(key + BOLD, fBoldCheckBox.getSelection());
			}
		});
				
		return colorComposite;
	}
	
	private Control createPreviewer(Composite parent) {
		
		fJavaTextTools= new JavaTextTools(fOverlayStore);
		
		fPreviewViewer= new SourceViewer(parent, null, SWT.V_SCROLL | SWT.H_SCROLL);
		fPreviewViewer.configure(new JavaSourceViewerConfiguration(fJavaTextTools, null));
		fPreviewViewer.getTextWidget().setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
		fPreviewViewer.setEditable(false);
		
		initializeViewerColors(fPreviewViewer);
		
		String content= loadPreviewContentFromFile("ColorSettingPreviewCode.txt"); //$NON-NLS-1$
		IDocument document= new Document(content);
		IDocumentPartitioner partitioner= fJavaTextTools.createDocumentPartitioner();
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		
		fPreviewViewer.setDocument(document);
		
		fOverlayStore.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String p= event.getProperty();
				if (p.equals(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND) ||
					p.equals(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT))
				{
					initializeViewerColors(fPreviewViewer);
				}
				
				fPreviewViewer.invalidateTextPresentation();
			}
		});
		
		return fPreviewViewer.getControl();
	}
	
	private Color fBackgroundColor;
	
	/**
	 * Initializes the given viewer's colors.
	 * 
	 * @param viewer the viewer to be initialized
	 */
	private void initializeViewerColors(ISourceViewer viewer) {
		
		IPreferenceStore store= fOverlayStore;
		if (store != null) {
			
			StyledText styledText= viewer.getTextWidget();
						
			// ---------- background color ----------------------
			Color color= store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)
				? null
				: createColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, styledText.getDisplay());
			styledText.setBackground(color);
				
			if (fBackgroundColor != null)
				fBackgroundColor.dispose();
				
			fBackgroundColor= color;
		}
	}

	/**
	 * Creates a color from the information stored in the given preference store.
	 * Returns <code>null</code> if there is no such information available.
	 */
	private Color createColor(IPreferenceStore store, String key, Display display) {
	
		RGB rgb= null;		
		
		if (store.contains(key)) {
			
			if (store.isDefault(key))
				rgb= PreferenceConverter.getDefaultColor(store, key);
			else
				rgb= PreferenceConverter.getColor(store, key);
		
			if (rgb != null)
				return new Color(display, rgb);
		}
		
		return null;
	}	
	
	// sets enabled flag for a control and all its sub-tree
	private static void setEnabled(Control control, boolean enable) {
		control.setEnabled(enable);
		if (control instanceof Composite) {
			Composite composite= (Composite) control;
			Control[] children= composite.getChildren();
			for (int i= 0; i < children.length; i++)
				setEnabled(children[i], enable);
		}
	}

	private Control createBehaviorPage(Composite parent) {

		Composite behaviorComposite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		behaviorComposite.setLayout(layout);
		
		
		String label= JavaUIMessages.getString("JavaEditorPreferencePage.textFont"); //$NON-NLS-1$
		addTextFontEditor(behaviorComposite, label, AbstractTextEditor.PREFERENCE_FONT);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.displayedTabWidth"); //$NON-NLS-1$
		addTextField(behaviorComposite, label, JavaSourceViewerConfiguration.PREFERENCE_TAB_WIDTH, 2, 0, true);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.insertSpaceForTabs"); //$NON-NLS-1$
		addCheckBox(behaviorComposite, label, CompilationUnitEditor.SPACES_FOR_TABS, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.showProblemsBeforeCompiling"); //$NON-NLS-1$
		addCheckBox(behaviorComposite, label, CompilationUnitDocumentProvider.HANDLE_TEMPRARY_PROBELMS, 0);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.showOverviewRuler"); //$NON-NLS-1$
		addCheckBox(behaviorComposite, label, CompilationUnitEditor.OVERVIEW_RULER, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.highlightMatchingBrackets"); //$NON-NLS-1$
		fBracketHighlightButton= addCheckBox(behaviorComposite, label, CompilationUnitEditor.MATCHING_BRACKETS, 0);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.matchingBracketsHighlightColor"); //$NON-NLS-1$
		fBracketHighlightColor= addColorButton(behaviorComposite, label, CompilationUnitEditor.MATCHING_BRACKETS_COLOR, 0);

		fBracketHighlightButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setEnabled(fBracketHighlightColor, fBracketHighlightButton.getSelection());
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.highlightCurrentLine"); //$NON-NLS-1$
		fLineHighlightButton= addCheckBox(behaviorComposite, label, CompilationUnitEditor.CURRENT_LINE, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.currentLineHighlightColor"); //$NON-NLS-1$
		fLineHighlightColor= addColorButton(behaviorComposite, label, CompilationUnitEditor.CURRENT_LINE_COLOR, 0);

		fLineHighlightButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setEnabled(fLineHighlightColor, fLineHighlightButton.getSelection());
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.highlightProblems"); //$NON-NLS-1$
		fProblemIndicationButton= addCheckBox(behaviorComposite, label, CompilationUnitEditor.PROBLEM_INDICATION, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.problemHighlightColor"); //$NON-NLS-1$
		fProblemIndicationColor= addColorButton(behaviorComposite, label, CompilationUnitEditor.PROBLEM_INDICATION_COLOR, 0);

		fProblemIndicationButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setEnabled(fProblemIndicationColor, fProblemIndicationButton.getSelection());
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.showPrintMargin"); //$NON-NLS-1$
		fPrintMarginButton= addCheckBox(behaviorComposite, label, CompilationUnitEditor.PRINT_MARGIN, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.printMarginColor"); //$NON-NLS-1$
		fPrintMarginColor= addColorButton(behaviorComposite, label, CompilationUnitEditor.PRINT_MARGIN_COLOR, 0);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.printMarginColumn"); //$NON-NLS-1$
		fPrintMarginColumn= addTextField(behaviorComposite, label, CompilationUnitEditor.PRINT_MARGIN_COLUMN, 4, 0, true);
		
		fPrintMarginButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled= fPrintMarginButton.getSelection();
				setEnabled(fPrintMarginColor, enabled);
				setEnabled(fPrintMarginColumn, enabled);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.findScopeColor"); //$NON-NLS-1$
		fFindScopeColor= addColorButton(behaviorComposite, label, AbstractTextEditor.PREFERENCE_COLOR_FIND_SCOPE, 0);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.linkedPositionColor"); //$NON-NLS-1$
		fLinkedPositionColor= addColorButton(behaviorComposite, label, CompilationUnitEditor.LINKED_POSITION_COLOR, 0);
		
		return behaviorComposite;
	}
	
	private Control createContentAssistPage(Composite parent) {

		Composite contentAssistComposite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		contentAssistComposite.setLayout(layout);
				
		String label= JavaUIMessages.getString("JavaEditorPreferencePage.insertSingleProposalsAutomatically"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, ContentAssistPreference.AUTOINSERT, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.showOnlyProposalsVisibleInTheInvocationContext"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, ContentAssistPreference.SHOW_VISIBLE_PROPOSALS, 0);
		
//		label= "Show only proposals with &matching cases";
//		addCheckBox(contentAssistComposite, label, ContentAssistPreference.CASE_SENSITIVITY, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.presentProposalsInAlphabeticalOrder"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, ContentAssistPreference.ORDER_PROPOSALS, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.enableAutoActivation"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, ContentAssistPreference.AUTOACTIVATION, 0);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.automaticallyAddImportInsteadOfQualifiedName"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, ContentAssistPreference.ADD_IMPORT, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.autoActivationDelay"); //$NON-NLS-1$
		addTextField(contentAssistComposite, label, ContentAssistPreference.AUTOACTIVATION_DELAY, 4, 0, true);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.autoActivationTriggersForJava"); //$NON-NLS-1$
		addTextField(contentAssistComposite, label, ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVA, 25, 0, false);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.autoActivationTriggersForJavaDoc"); //$NON-NLS-1$
		addTextField(contentAssistComposite, label, ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVADOC, 25, 0, false);
				
		label= JavaUIMessages.getString("JavaEditorPreferencePage.backgroundForCompletionProposals"); //$NON-NLS-1$
		addColorButton(contentAssistComposite, label, ContentAssistPreference.PROPOSALS_BACKGROUND, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.foregroundForCompletionProposals"); //$NON-NLS-1$
		addColorButton(contentAssistComposite, label, ContentAssistPreference.PROPOSALS_FOREGROUND, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.backgroundForMethodParameters"); //$NON-NLS-1$
		addColorButton(contentAssistComposite, label, ContentAssistPreference.PARAMETERS_BACKGROUND, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.foregroundForMethodParameters"); //$NON-NLS-1$
		addColorButton(contentAssistComposite, label, ContentAssistPreference.PARAMETERS_FOREGROUND, 0);
				
		return contentAssistComposite;
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		fOverlayStore.load();
		fOverlayStore.start();
		
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("JavaEditorPreferencePage.general")); //$NON-NLS-1$
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CFILE));
		item.setControl(createBehaviorPage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("JavaEditorPreferencePage.colors")); //$NON-NLS-1$
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CFILE));
		item.setControl(createColorPage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("JavaEditorPreferencePage.codeAssist")); //$NON-NLS-1$
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CFILE));
		item.setControl(createContentAssistPage(folder));
		
		initialize();
		
		return folder;
	}
	
	private void initialize() {
		
		fFontEditor.setPreferenceStore(getPreferenceStore());
		fFontEditor.setPreferencePage(this);
		fFontEditor.load();
		
		initializeFields();
		
		for (int i= 0; i < fListModel.length; i++)
			fList.add(fListModel[i][0]);
			
		fList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				fList.select(0);
				handleListSelection();
			}
		});
	}
	
	private void initializeFields() {
		
		Iterator e= fColorButtons.keySet().iterator();
		while (e.hasNext()) {
			ColorEditor c= (ColorEditor) e.next();
			String key= (String) fColorButtons.get(c);
			RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
			c.setColorValue(rgb);
		}
		
		e= fCheckBoxes.keySet().iterator();
		while (e.hasNext()) {
			Button b= (Button) e.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fOverlayStore.getBoolean(key));
		}
		
		e= fTextFields.keySet().iterator();
		while (e.hasNext()) {
			Text t= (Text) e.next();
			String key= (String) fTextFields.get(t);
			t.setText(fOverlayStore.getString(key));
		}
		
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
		fBackgroundColorEditor.setColorValue(rgb);		
		
		boolean default_= fOverlayStore.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
		fBackgroundDefaultRadioButton.setSelection(default_);
		fBackgroundCustomRadioButton.setSelection(!default_);
		fBackgroundColorButton.setEnabled(!default_);
		
		setEnabled(fBracketHighlightColor, fBracketHighlightButton.getSelection());
		setEnabled(fLineHighlightColor, fLineHighlightButton.getSelection());
	}
	
	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		fFontEditor.store();
		fOverlayStore.propagate();
		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		
		fFontEditor.loadDefault();
		
		fOverlayStore.loadDefaults();
		initializeFields();
		handleListSelection();
		
		super.performDefaults();
		
		fPreviewViewer.invalidateTextPresentation();
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		
		if (fJavaTextTools != null) {
			fJavaTextTools= null;
		}
		
		fFontEditor.setPreferencePage(null);
		fFontEditor.setPreferenceStore(null);
		
		if (fOverlayStore != null) {
			fOverlayStore.stop();
			fOverlayStore= null;
		}
		
		super.dispose();
	}
	
	private Control addColorButton(Composite parent, String label, String key, int indentation) {

		Composite composite= new Composite(parent, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		composite.setLayoutData(gd);
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);
				
		Label labelControl= new Label(composite, SWT.NONE);
		labelControl.setText(label);
		
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= indentation;
		labelControl.setLayoutData(gd);
		
		ColorEditor editor= new ColorEditor(composite);
		Button button= editor.getButton();
		button.setData(editor);
		
		gd= new GridData();
		gd.horizontalAlignment= GridData.END;
		button.setLayoutData(gd);
		button.addSelectionListener(fColorButtonListener);
		
		fColorButtons.put(editor, key);
		
		return composite;
	}
	
	private Button addCheckBox(Composite parent, String label, String key, int indentation) {		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 2;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);
		
		fCheckBoxes.put(checkBox, key);
		
		return checkBox;
	}
	
	private Control addTextField(Composite parent, String label, String key, int textLimit, int indentation, boolean isNumber) {
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		composite.setLayoutData(gd);
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);

		Label labelControl= new Label(composite, SWT.NONE);
		labelControl.setText(label);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= indentation;
		labelControl.setLayoutData(gd);
		
		Text textControl= new Text(composite, SWT.BORDER | SWT.SINGLE);		
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(textLimit + 1);
		gd.horizontalAlignment= GridData.END;
		textControl.setLayoutData(gd);
		textControl.setTextLimit(textLimit);
		textControl.addModifyListener(fTextFieldListener);
		fTextFields.put(textControl, key);
		if (isNumber)
			fNumberFields.add(textControl);
			
		return composite;
	}
	
	private void addTextFontEditor(Composite parent, String label, String key) {
		
		Composite editorComposite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		editorComposite.setLayout(layout);		
		fFontEditor= new WorkbenchChainedTextFontFieldEditor(key, label, editorComposite);
		fFontEditor.setChangeButtonText(JavaUIMessages.getString("JavaEditorPreferencePage.change")); //$NON-NLS-1$
				
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);
	}
	
	private String loadPreviewContentFromFile(String filename) {
		String line;
		String separator= System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer buffer= new StringBuffer(512);
		BufferedReader reader= null;
		try {
			reader= new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
			while ((line= reader.readLine()) != null) {
				buffer.append(line);
				buffer.append(separator);
			}
		} catch (IOException io) {
			JavaPlugin.log(io);
		} finally {
			if (reader != null) {
				try { reader.close(); } catch (IOException e) {}
			}
		}
		return buffer.toString();
	}
	
	private void textChanged(Text textControl) {
		String number= textControl.getText();
		IStatus status= validatePositiveNumber(number);
		if (!status.matches(IStatus.ERROR))
			fOverlayStore.setValue((String) fTextFields.get(textControl), number);
		updateStatus(status);
	}
	
	private IStatus validatePositiveNumber(String number) {
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(JavaUIMessages.getString("JavaEditorPreferencePage.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value < 0)
					status.setError(JavaUIMessages.getFormattedString("JavaEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				status.setError(JavaUIMessages.getFormattedString("JavaEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			}
		}
		return status;
	}
	
	private void updateStatus(IStatus status) {
		if (!status.matches(IStatus.ERROR)) {
			for (int i= 0; i < fNumberFields.size(); i++) {
				Text text= (Text) fNumberFields.get(i);
				IStatus s= validatePositiveNumber(text.getText());
				status= StatusUtil.getMoreSevere(s, status);
			}
		}	
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}
}


