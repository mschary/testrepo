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
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IContentProvider;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;

import com.kony.nativecodegen.actions.NativeCodeGenerationJob;
import com.pat.tool.keditor.propertyDescriptor.TableLabelProvider;
import com.pat.tool.keditor.utils.SWTResourceUtil;

public class VariableTypeInferenceEditor extends EditorPart {

	public static final String ID = "com.kony.nativecodegen.ui.VariableTypeInferenceEditor";
	private static final String DELIMITER = ":";
	private static final String TYPE = "TYPE";
	private static final String VARIABLE = "VARIABLE";
	private static final String USAGE = "USAGE";

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
		Composite container = formToolkit.createComposite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
	    GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		
		final Section section = formToolkit.createSection(container, Section.TITLE_BAR | Section.DESCRIPTION | Section.EXPANDED);
		section.setText("Unresolved Variables");
		section.setDescription("Specify variable types for the below variables.");
		GridDataFactory.fillDefaults().grab(true, true).applyTo(section);
	    GridLayoutFactory.fillDefaults().applyTo(section);
	    Composite tableHolder = formToolkit.createComposite(section, SWT.BORDER);
	    section.setClient(tableHolder);
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
		resetButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				variablesMap.clear();
				variablesMap.putAll(backupMap);
				viewer.refresh();
				doSave(null);
			}
		});
	    
	    
	    Button launchButton = formToolkit.createButton(buttonComposite, "Launch", SWT.NONE);
	    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(launchButton);
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
		
		String message = "Note: Press launch button to generate the native code";
		Label messageLabel = formToolkit.createLabel(container, message, SWT.WRAP);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(2, 1).applyTo(messageLabel);
		Color blueColor = new Color(Display.getDefault(), new RGB(0, 0, 255));
		SWTResourceUtil.cleanupOnDispose(messageLabel, blueColor);
		messageLabel.setForeground(blueColor);
		
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
		} catch (IOException e) {
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
