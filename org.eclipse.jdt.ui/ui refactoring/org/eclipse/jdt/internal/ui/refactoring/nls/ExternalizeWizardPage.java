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
package org.eclipse.jdt.internal.ui.refactoring.nls;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.nls.KeyValuePair;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;

class ExternalizeWizardPage extends UserInputWizardPage {

	private static final String[] PROPERTIES;
	private static final String[] fgTitles;
	private static final int STATE_PROP= 0; 
	private static final int VAL_PROP= 1; 
	private static final int KEY_PROP= 2; 
	private static final int SIZE= 3; //column counter
	private static final int ROW_COUNT= 5;
	
	public static final String PAGE_NAME= "NLSWizardPage1"; //$NON-NLS-1$
	static {
		PROPERTIES= new String[SIZE];
		PROPERTIES[STATE_PROP]= "task"; //$NON-NLS-1$
		PROPERTIES[KEY_PROP]= "key"; //$NON-NLS-1$
		PROPERTIES[VAL_PROP]= "value"; //$NON-NLS-1$
		
		fgTitles= new String[SIZE];
		fgTitles[STATE_PROP]= ""; //$NON-NLS-1$
		fgTitles[KEY_PROP]= NLSUIMessages.getString("ExternalizeWizard.key");  //$NON-NLS-1$
		fgTitles[VAL_PROP]= NLSUIMessages.getString("ExternalizeWizard.value");  //$NON-NLS-1$
	}
	
	private class CellModifier implements ICellModifier {
		
		/**
		 * @see ICellModifier#canModify(Object, String)
		 */
		public boolean canModify(Object element, String property) {		    
			if (property == null)
				return false;

			if (! (element instanceof NLSSubstitution))	
				return false;
				
			if (PROPERTIES[STATE_PROP].equals(property))
				return true;
			
			NLSSubstitution subs = (NLSSubstitution) element;			
			return subs.hasChanged() && (subs.getState() == NLSSubstitution.EXTERNALIZED);			    
		}
		
		/**
		 * @see ICellModifier#getValue(Object, String)
		 */
		public Object getValue(Object element, String property) {
			if (element instanceof NLSSubstitution) {
				NLSSubstitution substitution= (NLSSubstitution) element;
				if (PROPERTIES[KEY_PROP].equals(property))
					return substitution.fKey;
				if (PROPERTIES[VAL_PROP].equals(property))
				    return substitution.fValue;
				if (PROPERTIES[STATE_PROP].equals(property)){
					return new Integer(substitution.fState);
				}	
			}
			return null;
		}
		
		/**
		 * @see ICellModifier#modify(Object, String, Object)
		 */
		public void modify(Object element, String property, Object value) {
			if (element instanceof TableItem) {
				Object data= ((TableItem) element).getData();
				if (data instanceof NLSSubstitution) {
					NLSSubstitution substitution= (NLSSubstitution) data;
					if (PROPERTIES[KEY_PROP].equals(property)) {
						substitution.setKey((String) value);
						fTableViewer.update(substitution, new String[] { property });
						fTableViewer.refresh(true);
					}
					if (PROPERTIES[VAL_PROP].equals(property)) {						
					    substitution.setValue((String) value);
						fTableViewer.update(substitution, new String[] { property });
					}
					if (PROPERTIES[STATE_PROP].equals(property)) {
						substitution.setState(((Integer)value).intValue());
						if ((substitution.getState() == NLSSubstitution.EXTERNALIZED) && substitution.hasChanged()) {
						    substitution.generateKey(fSubstitutions, fPrefixField.getText());
						}
						fTableViewer.update(substitution, new String[] { property });
					}
				}
			}
		}
	}
	
	private class NLSSubstitutionLabelProvider extends LabelProvider implements ITableLabelProvider, IFontProvider {
	    
	    private Font bold;
	    private Font defaultFont;
	    
	    public NLSSubstitutionLabelProvider() {
	        FontRegistry fontRegistry = 
	            PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();	        
	        defaultFont = fontRegistry.defaultFont();	        
	        bold = fontRegistry.getBold(defaultFont.getFontData()[0].getName());        
	    }
	    
	    public String getColumnText(Object element, int columnIndex) {
	        String columnText = "";
			if (element instanceof NLSSubstitution) {
				NLSSubstitution substitution= (NLSSubstitution) element;
				if (columnIndex == KEY_PROP){
				    if (substitution.fState == NLSSubstitution.EXTERNALIZED) {
					    if (substitution.hasChanged()) {
					        columnText = substitution.getKeyWithPrefix(fPrefixField.getText());					        
					    } else {
					        columnText = substitution.getKey();
					    }
					}					 
				} else if ((columnIndex == VAL_PROP) && (substitution.fValue != null)) {			    
				    columnText = substitution.fValue;				    
				}
			}
			return columnText; 
		}
		
		public Image getColumnImage(Object element, int columnIndex) {
		    if ((columnIndex == STATE_PROP) && (element instanceof NLSSubstitution)) {
		        return getNLSImage((NLSSubstitution) element);
		    }
		    
			return null;
		}

		public Font getFont(Object element) {
            if (element instanceof NLSSubstitution) {
                NLSSubstitution substitution = (NLSSubstitution) element;
                if (substitution.hasChanged()) {
                    return bold;
                }                
            }
            return defaultFont;
        }
		
		private Image getNLSImage(NLSSubstitution sub){
		    Image image = getNLSImage(sub.getState());
		    if ((sub.fValue == null) && (sub.getKey() != null)) {
		        JavaElementImageDescriptor imageDescriptor = 
		            new JavaElementImageDescriptor(getNLSImageDescriptor(sub.getState()), JavaElementImageDescriptor.ERROR, JavaElementImageProvider.SMALL_SIZE);	        
		        return imageDescriptor.createImage();
		    } else if (sub.hasDuplicateKey(fSubstitutions, fPrefixField.getText())) {
		        JavaElementImageDescriptor imageDescriptor = 
		            new JavaElementImageDescriptor(getNLSImageDescriptor(sub.getState()), JavaElementImageDescriptor.WARNING, JavaElementImageProvider.SMALL_SIZE);	        
		        return imageDescriptor.createImage();	        
		    } else {
		        return image;	        
		    }	    
		}		
		
		// TODO: dry ???
		private Image getNLSImage(int task){
			switch (task){
				case NLSSubstitution.EXTERNALIZED:
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_NLS_TRANSLATE);
				case NLSSubstitution.IGNORED:	
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_NLS_NEVER_TRANSLATE);
				case NLSSubstitution.INTERNALIZED:
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_NLS_SKIP);	
				default:
					Assert.isTrue(false);	
					return null;
			}
		}
		
		// TODO: dry ???
		private ImageDescriptor getNLSImageDescriptor(int task){
			switch (task){
				case NLSSubstitution.EXTERNALIZED:
					return JavaPluginImages.DESC_OBJS_NLS_TRANSLATE;
				case NLSSubstitution.IGNORED:	
					return JavaPluginImages.DESC_OBJS_NLS_NEVER_TRANSLATE;
				case NLSSubstitution.INTERNALIZED:
					return JavaPluginImages.DESC_OBJS_NLS_SKIP;	
				default:
					Assert.isTrue(false);	
					return null;
			}
		}        
	}	
		
	private class NLSInputDialog extends StatusDialog implements IDialogFieldListener {
		private StringDialogField fKeyField;
		private StringDialogField fValueField;
		private DialogField fMessageField;
		
		public NLSInputDialog(Shell parent, String title, String message, NLSSubstitution substitution) {
			super(parent);				
			setTitle(title);
	
			fMessageField= new DialogField();
			fMessageField.setLabelText(message);
		
			fKeyField= new StringDialogField();
			fKeyField.setLabelText(NLSUIMessages.getString("ExternalizeWizard.NLSInputDialog.Enter_key")); //$NON-NLS-1$
			fKeyField.setDialogFieldListener(this);
				
			fValueField= new StringDialogField();
			fValueField.setLabelText(NLSUIMessages.getString("ExternalizeWizard.NLSInputDialog.Enter_value")); //$NON-NLS-1$
			fValueField.setDialogFieldListener(this);			
	
			fKeyField.setText(substitution.getKey());
			fValueField.setText(substitution.getValue());
		}
			
		public KeyValuePair getResult() {
			KeyValuePair res= new KeyValuePair(fKeyField.getText(), fValueField.getText());
			return res;
		}
					
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
				
			Composite inner= new Composite(composite, SWT.NONE);
			GridLayout layout= new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			layout.numColumns= 2;
			inner.setLayout(layout);
				
			fMessageField.doFillIntoGrid(inner, 2);
			fKeyField.doFillIntoGrid(inner, 2);
			fValueField.doFillIntoGrid(inner, 2);
				
			LayoutUtil.setHorizontalGrabbing(fKeyField.getTextControl(null));
			LayoutUtil.setWidthHint(fKeyField.getTextControl(null), convertWidthInCharsToPixels(45));
			LayoutUtil.setWidthHint(fValueField.getTextControl(null), convertWidthInCharsToPixels(45));
				
			fKeyField.postSetFocusOnDialogField(parent.getDisplay());
				
			applyDialogFont(composite);		
			return composite;
		}
			
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			// validate
			IStatus keyStatus= validateIdentifiers(getTokens(fKeyField.getText(), ","), true); //$NON-NLS-1$
			IStatus valueStatus= validateIdentifiers(getTokens(fValueField.getText(), ","), false); //$NON-NLS-1$
				
			updateStatus(StatusUtil.getMoreSevere(valueStatus, keyStatus));
		}		
		
		protected String[] getTokens(String text, String separator) {
			StringTokenizer tok= new StringTokenizer(text, separator); //$NON-NLS-1$
			int nTokens= tok.countTokens();
			String[] res= new String[nTokens];
			for (int i= 0; i < res.length; i++) {
				res[i]= tok.nextToken().trim();
			}
			return res;
		}	
			
		private IStatus validateIdentifiers(String[] values, boolean isKey) {
			for (int i= 0; i < values.length; i++) {
				String val= values[i];
				if (val.length() == 0) {
					if (isKey) {
						return new StatusInfo(IStatus.ERROR, NLSUIMessages.getString("ExternalizeWizard.NLSInputDialog.Error_empty_key")); //$NON-NLS-1$
					} else {
						return new StatusInfo(IStatus.ERROR, NLSUIMessages.getString("ExternalizeWizard.NLSInputDialog.Error_empty_value")); //$NON-NLS-1$
					}							
				}
				// validation so keys don't contain spaces
				if (isKey) {
					if (! validateKey(val))
						return new StatusInfo(IStatus.ERROR, NLSUIMessages.getFormattedString("ExternalizeWizard.NLSInputDialog.Error_invalid_key", val)); //$NON-NLS-1$
				}
			}
			return new StatusInfo();
		}		
		
		private boolean validateKey(String s) {
			for (int i= 0; i < s.length(); i++){
				if (Character.isWhitespace(s.charAt(i)))
					return false;
			}				
			return true;
		}
	}
	
	private Text fPrefixField;
	private Table fTable;
	private TableViewer fTableViewer;
	private SourceViewer fSourceViewer;
  
    private final ICompilationUnit fCu;
    private NLSSubstitution[] fSubstitutions;
    private String fDefaultPrefix;
    private Button fExternalizeButton;
    private Button fIgnoreButton;
    private Button fInternalizeButton;
    private Button fRevertButton;
	private Button fEditButton;
	
	private Label fWarningIcon;
	private Label fErrorIcon;
    private Label fWarningDesc;
    private Label fErrorDesc;
	
	public ExternalizeWizardPage(NLSRefactoring nlsRefactoring) {
		super(PAGE_NAME);
		fCu = nlsRefactoring.getCu();
		fSubstitutions = nlsRefactoring.getSubstitutions();		
		fDefaultPrefix = nlsRefactoring.getPrefixHint();		
	}	

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		
		Composite supercomposite= new Composite(parent, SWT.NONE);
		supercomposite.setLayout(new GridLayout());
		
		createKeyPrefixField(supercomposite);		
		
		SashForm composite= new SashForm(supercomposite, SWT.VERTICAL);
		
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 360;
		composite.setLayoutData(data);
				
		createTableViewer(composite);
		createSourceViewer(composite);
				
		composite.setWeights(new int[]{65, 45});
	 
		createStatusLabels(supercomposite);
		validateKeys();
		
		// promote control
		setControl(supercomposite);
		Dialog.applyDialogFont(supercomposite);
		WorkbenchHelp.setHelp(supercomposite, IJavaHelpContextIds.EXTERNALIZE_WIZARD_KEYVALUE_PAGE);
	}
	

    private void createTableViewer(Composite composite){
		createTableComposite(composite);
		
		/*
		 * Feature of CellEditors - double click is ignored.
		 * The workaround is to register my own listener and force the desired 
		 * behavior.
		 */
		fTableViewer= new TableViewer(fTable) {
				protected void hookControl(Control control) {
					super.hookControl(control);
					((Table) control).addMouseListener(new MouseAdapter() {
						public void mouseDoubleClick(MouseEvent e) {
							if (getTable().getSelection().length == 0)
								return;
							TableItem item= getTable().getSelection()[0];
							if (item.getBounds(STATE_PROP).contains(e.x, e.y)){
								List widgetSel= getSelectionFromWidget();
								if (widgetSel == null || widgetSel.size() != 1)
									return;
								NLSSubstitution s= (NLSSubstitution)widgetSel.get(0);
								Integer value= (Integer)getCellModifier().getValue(s, PROPERTIES[STATE_PROP]);
								int newValue= MultiStateCellEditor.getNextValue(NLSSubstitution.STATE_COUNT, value.intValue());
								getCellModifier().modify(item, PROPERTIES[STATE_PROP], new Integer(newValue));
							}	
						}
					});
				}
		};
		
		fTableViewer.setUseHashlookup(true);
		
		final CellEditor[] editors= createCellEditors();
		fTableViewer.setCellEditors(editors);		
		fTableViewer.setColumnProperties(PROPERTIES);
		fTableViewer.setCellModifier(new CellModifier());

		fTableViewer.setContentProvider(new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                return fSubstitutions;
            }
            public void dispose() {
            }
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
        });
		
		fTableViewer.setLabelProvider(new NLSSubstitutionLabelProvider());
		fTableViewer.setInput(new Object());
		
		fTableViewer.addDoubleClickListener(new IDoubleClickListener(){
			public void doubleClick(DoubleClickEvent event) {
				Set selected= getSelectedTableEntries();
				if (selected.size() != 1)
					return;
				NLSSubstitution substitution= (NLSSubstitution)selected.iterator().next();
				if ((substitution.hasChanged()) && (substitution.getState() == NLSSubstitution.EXTERNALIZED)) {
					openEditButton(event.getSelection());
				}
			}
		});
		
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ExternalizeWizardPage.this.selectionChanged(event);
			}
		});
	}
	
	private CellEditor[] createCellEditors() {
		final CellEditor editors[]= new CellEditor[SIZE];
		editors[STATE_PROP]= new MultiStateCellEditor(fTable, NLSSubstitution.STATE_COUNT, NLSSubstitution.DEFAULT);
		editors[KEY_PROP]= new TextCellEditor(fTable);
		editors[VAL_PROP]= new TextCellEditor(fTable);
		return editors;
	}

	private void createSourceViewer(Composite parent){
		Composite c= new Composite(parent, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
        c.setLayout(gl);
		
		Label l= new Label(c, SWT.NONE);
		l.setText(NLSUIMessages.getString("wizardPage.context")); //$NON-NLS-1$
		l.setLayoutData(new GridData());
		
		// source viewer
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();		
		int styles= SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION;
		fSourceViewer= new JavaSourceViewer(c, null, null, false, styles);
		fSourceViewer.configure(new JavaSourceViewerConfiguration(tools, null));
		fSourceViewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));

		try {
			
			String contents= fCu.getBuffer().getContents();
			IDocument document= new Document(contents);
			tools.setupJavaDocumentPartitioner(document);
			
			fSourceViewer.setDocument(document);
			fSourceViewer.setEditable(false);
			
			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.heightHint= convertHeightInCharsToPixels(10);
			gd.widthHint= convertWidthInCharsToPixels(40);
			fSourceViewer.getControl().setLayoutData(gd);
			
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, NLSUIMessages.getString("wizardPage.title"), NLSUIMessages.getString("wizardPage.exception")); //$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	private void createKeyPrefixField(Composite parent){
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		gl.marginWidth= 0;
		composite.setLayout(gl);
		
		Label l= new Label(composite, SWT.NONE);
		l.setText(NLSUIMessages.getString("wizardPage.common_prefix")); //$NON-NLS-1$
		l.setLayoutData(new GridData());
		
		fPrefixField= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fPrefixField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fPrefixField.setText(fDefaultPrefix);
		fPrefixField.selectAll();
		
		fPrefixField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {       
                fTableViewer.refresh(true);
            }		    
		});
	}	
	
	private void validateKeys() {
	    checkDuplicateKeys();
	    checkMissingKeys();
	}
	
    private void checkDuplicateKeys() {
        for (int i = 0; i < fSubstitutions.length; i++) {
            NLSSubstitution substitution = fSubstitutions[i];
            if (hasDuplicateKey(substitution)) {
                showWarningStatus(true);
                return;
            }            
        }
	    showWarningStatus(false);
    }
    
    private void checkMissingKeys() {
        for (int i = 0; i < fSubstitutions.length; i++) {
            NLSSubstitution substitution = fSubstitutions[i];
            if ((substitution.getValue() == null) && (substitution.getKey() != null)) {
                showErrorStatus(true);
                return;                
            }            
        }
	    showErrorStatus(false);
    }

    private boolean hasDuplicateKey(NLSSubstitution substitution) {
	    if (substitution.getState() == NLSSubstitution.EXTERNALIZED) {
	        if (substitution.hasChanged()) {
	            return substitution.hasDuplicateKey(fSubstitutions, fPrefixField.getText());	            
	        } else {
	            return substitution.hasDuplicateKey(fSubstitutions, "");
	        }
	    }
	    return false;
	}

	private void createStatusLabels(Composite parent){
		Composite labelComposite= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;		
		labelComposite.setLayout(gl);
		//labelComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fWarningIcon = new Label(labelComposite, SWT.NONE);		
		ImageDescriptor imageDescriptor = JavaPluginImages.DESC_OVR_WARNING;
		fWarningIcon.setImage(imageDescriptor.createImage());
		fWarningIcon.setLayoutData(new GridData());
		
		fWarningDesc = new Label(labelComposite, SWT.NONE);
        fWarningDesc.setText("Indication of duplicated keys.");		
		
		fErrorIcon = new Label(labelComposite, SWT.NONE);		
		imageDescriptor = JavaPluginImages.DESC_OVR_ERROR;
		fErrorIcon.setImage(imageDescriptor.createImage());
		fErrorIcon.setLayoutData(new GridData());
		
		fErrorDesc = new Label(labelComposite, SWT.NONE);
        fErrorDesc.setText("Indication of missing keys in the property file.");
	}
	
	private void showErrorStatus(boolean visible) {
	    fErrorIcon.setVisible(visible);
	    fErrorDesc.setVisible(visible);
	}
	
	private void showWarningStatus(boolean visible) {
	    fWarningIcon.setVisible(visible);
	    fWarningDesc.setVisible(visible);
	}
	
	private void createTableComposite(Composite parent) {
		Composite comp= new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout gl= new GridLayout();
		gl.marginWidth= 0;
		gl.marginHeight= 0;
		comp.setLayout(gl);
		
		Label l= new Label(comp, SWT.NONE);
		l.setText(NLSUIMessages.getString("wizardPage.strings_to_externalize")); //$NON-NLS-1$
		l.setLayoutData(new GridData());
		
		createTable(comp);
	}
	
	private void createTable(Composite parent){
		Composite c= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		gl.marginWidth= 0;
		gl.marginHeight= 0;
		c.setLayout(gl);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fTable= new Table(c, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.BORDER);
		GridData tableGD= new GridData(GridData.FILL_BOTH);
		tableGD.heightHint= SWTUtil.getTableHeightHint(fTable, ROW_COUNT);	
		//tableGD.widthHint= 40;
		fTable.setLayoutData(tableGD);
		
		fTable.setLinesVisible(true);
		
		TableLayout layout= new TableLayout();
		fTable.setLayout(layout);
		fTable.setHeaderVisible(true);
		
		ColumnLayoutData[] columnLayoutData= new ColumnLayoutData[SIZE];
		columnLayoutData[STATE_PROP]= new ColumnPixelData(20, false);
		columnLayoutData[KEY_PROP]= new ColumnWeightData(40, true);
		columnLayoutData[VAL_PROP]= new ColumnWeightData(40, true);
		
		for (int i= 0; i < fgTitles.length; i++) {
			TableColumn tc= new TableColumn(fTable, SWT.NONE, i);
			tc.setText(fgTitles[i]);
			layout.addColumnData(columnLayoutData[i]);
			tc.setResizable(columnLayoutData[i].resizable);
		}
		
		createButtonComposite(c);	
	}

	private void createButtonComposite(Composite parent){
		Composite buttonComp= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		buttonComp.setLayout(gl);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		fExternalizeButton = createTaskButton(buttonComp, "wizardPage.Externalize_Selected", new SelectionAdapter(){ //$NON-NLS-1$
		    public void widgetSelected(SelectionEvent e) {
		        setSelectedTasks(NLSSubstitution.EXTERNALIZED);
		    }
		});
		
		fIgnoreButton = createTaskButton(buttonComp, "wizardPage.Ignore_Selected", new SelectionAdapter(){ //$NON-NLS-1$
		    public void widgetSelected(SelectionEvent e) {
		        setSelectedTasks(NLSSubstitution.IGNORED);
		    }
		});
		
		fInternalizeButton = createTaskButton(buttonComp, "wizardPage.Internalize_Selected", new SelectionAdapter(){ //$NON-NLS-1$
		    public void widgetSelected(SelectionEvent e) {
		        setSelectedTasks(NLSSubstitution.INTERNALIZED);
		    }
		});
		
		fEditButton= createTaskButton(buttonComp, "ExternalizeWizardPage.Edit_key_and_value", new SelectionAdapter(){ //$NON-NLS-1$
		    public void widgetSelected(SelectionEvent e) {
		        openEditButton(fTableViewer.getSelection());
		    }
		});
		
		fRevertButton= createTaskButton(buttonComp, "wizardPage.Revert_Selected", new SelectionAdapter(){ //$NON-NLS-1$
		    public void widgetSelected(SelectionEvent e) {
		        revertStateOfSelection();
		    }

            private void revertStateOfSelection() {      
                Set selection = getSelectedTableEntries();
                for (Iterator iter = selection.iterator(); iter.hasNext();) {
                    NLSSubstitution substitution = (NLSSubstitution) iter.next();
                    substitution.setState(substitution.getOldState());                                        
                }
                fTableViewer.refresh();
                updateButtonStates((IStructuredSelection) fTableViewer.getSelection());
            }
		});
		
		fEditButton.setEnabled(false);
		buttonComp.pack();
	}
	
	private Button createTaskButton(Composite parent, String labelKey, SelectionAdapter adapter) {
	  Button button= new Button(parent, SWT.PUSH);
	  button.setText(NLSUIMessages.getString(labelKey)); //$NON-NLS-1$
	  button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	  SWTUtil.setButtonDimensionHint(button);
	  button.addSelectionListener(adapter);
	  return button;
	}  
	
	private void openEditButton(ISelection selection){
		try{
			Set selected= getSelectedTableEntries();
			Assert.isTrue(selected.size() == 1);
			NLSSubstitution substitution= (NLSSubstitution)selected.iterator().next();
			NLSInputDialog dialog= 
			    new NLSInputDialog(getShell(), 
			            NLSUIMessages.getString("ExternalizeWizard.NLSInputDialog.Title"),  //$NON-NLS-1$
			            NLSUIMessages.getString("ExternalizeWizard.NLSInputDialog.Label"),  //$NON-NLS-1$
			            substitution);
			if (dialog.open() == Window.CANCEL)
				return;
			KeyValuePair kvPair= dialog.getResult();
			substitution.setKey(kvPair.getKey());
			substitution.setValue(kvPair.getValue());
			fTableViewer.update(substitution, new String[] { PROPERTIES[KEY_PROP], PROPERTIES[VAL_PROP] });
		} finally{
			fTableViewer.refresh();
			fTableViewer.getControl().setFocus();
			fTableViewer.setSelection(selection);
		}
	}
	
	// TODO: why a set...there are no duplicate entries in selection
	private Set getSelectedTableEntries() {
		ISelection sel= fTableViewer.getSelection();
		if (sel instanceof IStructuredSelection) 
			return new HashSet(((IStructuredSelection)sel).toList());
		else		
			return new HashSet(0);
	}
		
	private void setSelectedTasks(int state){
		Assert.isTrue(state == NLSSubstitution.EXTERNALIZED 
				   || state == NLSSubstitution.IGNORED
				   || state == NLSSubstitution.INTERNALIZED);
		Set selected= getSelectedTableEntries();		
		String[] props= new String[] { PROPERTIES[STATE_PROP] };
		for (Iterator iter= selected.iterator(); iter.hasNext();) {
		    NLSSubstitution substitution = (NLSSubstitution) iter.next();
			substitution.setState(state);
			if ((substitution.getState() == NLSSubstitution.EXTERNALIZED) && substitution.hasChanged()) {
			    substitution.generateKey(fSubstitutions, fPrefixField.getText());
			}
		}
		fTableViewer.update(selected.toArray(), props);		
		fTableViewer.getControl().setFocus();
		updateButtonStates((IStructuredSelection) fTableViewer.getSelection());
	}
			
	private void selectionChanged(SelectionChangedEvent event) {
		ISelection s= event.getSelection();
		if (! (s instanceof IStructuredSelection)) 
			return;
		IStructuredSelection selection= (IStructuredSelection) s;
		updateButtonStates(selection);		
		
		updateSourceView(selection);			
	}
	
	private void updateSourceView(IStructuredSelection selection) {
        NLSSubstitution first= (NLSSubstitution) selection.getFirstElement();
		TextRegion region= first.fNLSElement.getPosition();
		fSourceViewer.setSelectedRange(region.getOffset(), region.getLength());
		fSourceViewer.revealRange(region.getOffset(), region.getLength());
    }

    private void updateButtonStates(IStructuredSelection selection){
	    fExternalizeButton.setEnabled(true);
	    fIgnoreButton.setEnabled(true);
	    fInternalizeButton.setEnabled(true);
	    fRevertButton.setEnabled(true);
	    
	    if (containsOnlyElementsOfSameState(NLSSubstitution.EXTERNALIZED, selection)) {
	        fExternalizeButton.setEnabled(false);
	    }
	    
	    if (containsOnlyElementsOfSameState(NLSSubstitution.IGNORED, selection)) {
	        fIgnoreButton.setEnabled(false);
	    }
	    
	    if (containsOnlyElementsOfSameState(NLSSubstitution.INTERNALIZED, selection)) {
	        fInternalizeButton.setEnabled(false);
	    }
	    
	    if (!containsElementsWithChange(selection)) {
	        fRevertButton.setEnabled(false);
	    }
	    
	    if (selection.size() == 1){
	        NLSSubstitution substitution= (NLSSubstitution) selection.getFirstElement();	
	        fEditButton.setEnabled(substitution.hasChanged() && (substitution.fState == NLSSubstitution.EXTERNALIZED));
	        
	    } else {	
	        fEditButton.setEnabled(false);			
	    }
	}

	private boolean containsElementsWithChange(IStructuredSelection selection) {
	    for (Iterator iter = selection.iterator(); iter.hasNext();) {
            NLSSubstitution	substitution = (NLSSubstitution) iter.next();
            if (substitution.hasChanged()) {
                return true;
            }            
        }
	    return false;	    
	}
	
	private boolean containsOnlyElementsOfSameState(int state, IStructuredSelection selection) {
	    for (Iterator iter = selection.iterator(); iter.hasNext();) {
            NLSSubstitution	substitution = (NLSSubstitution) iter.next();
            if (substitution.getState() != state) {
                return false;
            }            
        }
	    return true;	    
	}
	
	private void initializeRefactoring(){
		NLSRefactoring refactoring= (NLSRefactoring) getRefactoring();
		refactoring.setSubstitutionPrefix(fPrefixField.getText());
	}
		
	public boolean performFinish(){
		initializeRefactoring();
		
		//when finish is pressed on the first page - we want the settings from the
		//second page to be set to the refactoring object
		((ExternalizeWizardPage2)getWizard().getPage(ExternalizeWizardPage2.PAGE_NAME)).updateRefactoring();
		return super.performFinish();
	}
	
	public IWizardPage getNextPage() {
		initializeRefactoring();
		return super.getNextPage();
	}
	
	public void dispose(){
		//widgets will be disposed. only need to null'em		
		fPrefixField= null;		
		fSourceViewer= null;
		fTable= null;		
		fTableViewer= null;
		fEditButton= null;
		super.dispose();
	}	
}
