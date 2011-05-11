package com.kony.nativecodegen.ui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.kony.nativecodegen.actions.NativeCodeGenerationJob;
import com.pat.tool.keditor.propertyDescriptor.TableLabelProvider;

public class VariableTypeInferenceEditor extends EditorPart {

	public static final String ID = "com.kony.nativecodegen.ui.VariableTypeInferenceEditor";
	private static final String DELIMITER = ":";
	private static final String TYPE = "TYPE";
	private static final String VARIABLE = "VARIABLE";
	private static final String USAGE = "USAGE";
	private static final String LUA_EXTENSION = ".lua";

	private Properties variablesMap;
	private Map<Object, Object> backupMap = new HashMap<Object, Object>();
	private TableViewer viewer;
	private IFile file;
	private static List<String> variableTypes = new ArrayList<String>();
	
	private Job job;
	private boolean dirty;


	static {
		variableTypes.add("number");
		variableTypes.add("boolean");
		variableTypes.add("string");
		variableTypes.add("luatable");
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (input instanceof IFileEditorInput) {
			file = ((IFileEditorInput) input).getFile();
			variablesMap = new Properties();
			FileReader reader = null;
			try {
				reader = new FileReader(new File(file.getLocationURI()));
				variablesMap.load(reader);
			} catch (Exception e) {
				throw new PartInitException(e.getMessage(), e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						//ignore
					}
				}
			}
			backupMap.putAll(variablesMap);
		}
		setSite(site);
		setInput(input);
		setPartName(file.getProject().getName() + " (" + NativeCodeGenerationJob.getPlatformName(file.getName()) + ")");
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void createPartControl(Composite parent) {
		FormToolkit formToolkit = new FormToolkit(Display.getDefault());
		final Section section = formToolkit.createSection(parent, Section.TITLE_BAR | Section.DESCRIPTION | Section.EXPANDED);
		section.setText("Unresolved Variables for " + NativeCodeGenerationJob.getPlatformName(file.getName()));
		section.setDescription("There is a conflict in resolving types for below variables. Please select appropriate types and click run to proceed for native code generation.");
		GridDataFactory.fillDefaults().grab(true, true).applyTo(section);
	    GridLayoutFactory.fillDefaults().applyTo(section);
		
	    Composite container = formToolkit.createComposite(section, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
	    GridLayoutFactory.fillDefaults().margins(0, 5).numColumns(2).applyTo(container);
	  
	    Composite tableHolder = formToolkit.createComposite(container, SWT.BORDER);
	    section.setClient(container);
		createTable(tableHolder);
		Composite buttonComposite =  formToolkit.createComposite(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.END).grab(false, false).applyTo(buttonComposite);
	    GridLayoutFactory.fillDefaults().applyTo(buttonComposite);
	    
	    final Button removeButton = formToolkit.createButton(buttonComposite, "Remove", SWT.NONE);
	    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(removeButton);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				Object object = selection.getFirstElement();
				if (object != null) {
					variablesMap.remove(((Entry<String, String>) object).getKey());
					viewer.refresh();
				}
			}
		});
		removeButton.setEnabled(false);
	    
	    Button resetButton = formToolkit.createButton(buttonComposite, "Reset", SWT.NONE);
	    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(resetButton);
	    resetButton.setToolTipText("Discard changes made after opening the editor.");
		resetButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				variablesMap.clear();
				variablesMap.putAll(backupMap);
				viewer.refresh();
				doSave(null);
			}
		});
	    
	    
	    Button launchButton = formToolkit.createButton(buttonComposite, "Run", SWT.NONE);
	    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(launchButton);
	    launchButton.setToolTipText("Generate native code for " + getPartName());
		launchButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doSave(null);
				if (job == null) {
					String jobName = "Generation native code for " + NativeCodeGenerationJob.getPlatformName(file.getName());
					job = new NativeCodeGenerationJob(jobName, file.getProject().getName(), file.getName());
					job.setUser(true);
					setExecutionJob(job);
				}
				if (job.getState() != Job.RUNNING) {
					job.schedule();
				}
			}
		});
		
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				Object object = selection.getFirstElement();
				if (object != null) {
					removeButton.setEnabled(getUsageStatus(((Entry<String, String>) object).getValue()).equals(Boolean.FALSE.toString()));
				}
			}
		});
		
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				@SuppressWarnings("unchecked")
				Entry<String, String> entry = (Entry<String, String>) ((IStructuredSelection)selection).getFirstElement();
                String key = entry.getKey();
                String[] variableInfo = key.split(DELIMITER);
                revealVariableLocation(variableInfo);
			}
		});
	}

	private void revealVariableLocation(String[] variableInfo) {
        String fileName = variableInfo[0] + LUA_EXTENSION;
        String variableName = variableInfo[variableInfo.length - 2];
        String lineNumber = variableInfo[variableInfo.length - 1];
		try {
			IFile sourceFile = NativeCodeGenerationJob.getSourceFile(fileName, file.getName(), file.getProject());
			if (sourceFile != null) {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IEditorPart editor = IDE.openEditor(page, sourceFile);
				if (editor instanceof ITextEditor) {
					ITextEditor textEditor = (ITextEditor) editor;
					IDocumentProvider provider = textEditor.getDocumentProvider();
					IDocument document = provider.getDocument(editor.getEditorInput());
					try {
						int startOffset = document.getLineOffset(Integer.parseInt(lineNumber)- 1);
						FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(document);
						IRegion iRegion = finder.find(startOffset, variableName, true, false, true, false);
						textEditor.selectAndReveal(iRegion.getOffset(), iRegion.getLength());
					} catch (BadLocationException e) {
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void createTable(Composite tableHolder) {
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableHolder);
		viewer = new TableViewer(tableHolder, SWT.FULL_SELECTION);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		viewer.setContentProvider(getContentProvider());
		viewer.setLabelProvider(getLabelProvider());

		TableColumn variableColumn = new TableColumn(table, SWT.NONE);
		variableColumn.setText("Variable");
		TableColumn dataTypeColumn = new TableColumn(table, SWT.NONE);
		dataTypeColumn.setText("Type");
		TableColumn usageColumn = new TableColumn(table, SWT.NONE);
		usageColumn.setText("Used");
		usageColumn.setWidth(100);

		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableHolder.setLayout(tableColumnLayout);
		tableColumnLayout.setColumnData(variableColumn, new ColumnWeightData(80, 300, true));
		tableColumnLayout.setColumnData(dataTypeColumn, new ColumnWeightData(10, 50, true));
		tableColumnLayout.setColumnData(usageColumn, new ColumnWeightData(10, 50, true));

		viewer.setInput(variablesMap);

		viewer.setColumnProperties(new String[] { VARIABLE, TYPE, USAGE });
		viewer.setCellModifier(getCellModifier());
		viewer.setCellEditors(new CellEditor[] { new TextCellEditor(table), new ComboBoxCellEditor(table, variableTypes.toArray(new String[0])), new TextCellEditor(table) });
		viewer.setSorter(new ViewerSorter());
	}

	@Override
	public void setFocus() {
		
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(new File(file.getLocationURI()));
		    variablesMap.store(fileWriter, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fileWriter != null) {
				try {
					fileWriter.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
		
		try {
			file.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		dirty = false;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {
		
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private ICellModifier getCellModifier() {
		return new ICellModifier() {

			@Override
			public void modify(Object element, String property, Object value) {
				TableItem tableItem = (TableItem) element;
				Integer intValue = Integer.parseInt(value.toString());
				if (intValue == -1) {
					intValue++;
				}
				String newValue = variableTypes.get(intValue) + DELIMITER + getUsageStatus(variablesMap.get(tableItem.getText()).toString());
				variablesMap.put(tableItem.getText(), newValue);
				viewer.refresh();
				dirty = true;
				firePropertyChange(PROP_DIRTY);
			}

			@Override
			public Object getValue(Object element, String property) {
				Entry<String, String> entry = (Entry<String, String>) element;
				Object value = null;
				if (property.equals(property.equals(VARIABLE))) {
					value = entry.getKey();
				} else if (property.equals(TYPE)) {
					value = variableTypes.indexOf(entry.getValue().split(DELIMITER)[0]);
				} else {
					value = getUsageStatus(entry.getValue());
				}
				return value;
			}

			@Override
			public boolean canModify(Object element, String property) {
				return property.equals(TYPE);
			}
		};
	}

	private IContentProvider getContentProvider() {
		return new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			@Override
			public void dispose() {

			}

			@Override
			public Object[] getElements(Object inputElement) {
				Set<Entry<Object, Object>> entrySet = variablesMap.entrySet();
				return entrySet.toArray();
			}
		};
	}



	private TableLabelProvider getLabelProvider() {
		return new TableLabelProvider() {
			@SuppressWarnings("unchecked")
			@Override
			public String getColumnText(Object element, int columnIndex) {
				Entry<String, String> entry = (Entry<String, String>) element;
				String value = null;
				if (columnIndex == 0) {
					value = entry.getKey();
				} else if (columnIndex == 1) {
					value = entry.getValue().split(DELIMITER)[0];
				} else {
					value = getUsageStatus(entry.getValue());
				}
				return value;
			}
		};
	}

	private String getUsageStatus(String value) {
		if (value == null) {
			return Boolean.TRUE.toString();
		}
		String[] split = value.split(DELIMITER);
		return split.length == 2 ? value.split(DELIMITER)[1] : Boolean.TRUE.toString();
	}
	
	public void setExecutionJob(Job job) {
		if (!job.equals(this.job)) {
			Assert.isLegal(this.job == null);
			this.job = job;
		}
	}

}
