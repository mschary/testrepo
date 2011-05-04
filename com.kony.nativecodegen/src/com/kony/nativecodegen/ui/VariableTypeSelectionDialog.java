package com.kony.nativecodegen.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.pat.tool.keditor.propertyDescriptor.TableLabelProvider;

public class VariableTypeSelectionDialog extends Dialog {

	private static final String DELIMITER = ":";
	private static final String TYPE = "TYPE";
	private static final String VARIABLE = "VARIABLE";
	private static final String USAGE = "USAGE";

	private Properties variablesMap;
	private Map<Object, Object> backupMap = new HashMap<Object, Object>();
	private TableViewer viewer;
	private static List<String> variableTypes = new ArrayList<String>();

	static {
		variableTypes.add("Number");
		variableTypes.add("Boolean");
		variableTypes.add("String");
		variableTypes.add("LuaTable");
	}

	public VariableTypeSelectionDialog(Shell parentShell, Properties variablesMap) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.variablesMap = variablesMap;
		backupMap.putAll(variablesMap);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(450, 200).applyTo(container);
		viewer = new TableViewer(container, SWT.FULL_SELECTION);
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
		container.setLayout(tableColumnLayout);
		tableColumnLayout.setColumnData(variableColumn, new ColumnWeightData(40, 150, true));
		tableColumnLayout.setColumnData(dataTypeColumn, new ColumnWeightData(30, 100, true));
		tableColumnLayout.setColumnData(usageColumn, new ColumnWeightData(30, 100, true));

		viewer.setInput(variablesMap);

		viewer.setColumnProperties(new String[] { VARIABLE, TYPE, USAGE });
		viewer.setCellModifier(getCellModifier());
		viewer.setCellEditors(new CellEditor[] { new TextCellEditor(table), new ComboBoxCellEditor(table, variableTypes.toArray(new String[0])), new TextCellEditor(table) });
		viewer.setSorter(new ViewerSorter());

		return container;
	}

	@SuppressWarnings("unchecked")
	private ICellModifier getCellModifier() {
		return new ICellModifier() {
			@Override
			public void modify(Object element, String property, Object value) {
				TableItem tableItem = (TableItem) element;
				String newValue = variableTypes.get((Integer) value) + DELIMITER + getUsageStatus(variablesMap.get(tableItem.getText()).toString());
				variablesMap.put(tableItem.getText(), newValue);
				viewer.refresh();
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

	@SuppressWarnings("unchecked")
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CLOSE_ID) {
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			Object object = selection.getFirstElement();
			if (object != null) {
				variablesMap.remove(((Entry<String, String>) object).getKey());
				viewer.refresh();
			}
		} else {
			super.buttonPressed(buttonId);
		}
	}

	@Override
	protected void cancelPressed() {
		variablesMap.clear();
		variablesMap.putAll(backupMap);
		viewer.refresh();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Reset", true);
		createButton(parent, IDialogConstants.CLOSE_ID, "Remove", false);
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
		String[] split = value.split(DELIMITER);
		return split.length == 2 ? value.split(DELIMITER)[1] : Boolean.TRUE.toString();
	}

	public static void main(String[] args) {
		Properties properties = new Properties();
		properties.put("Name", "String:true");
		properties.put("Age", "String");
		properties.put("Sex", "Boolean:false");
		properties.put("Id", "Number:true");
		properties.put("Address", "LuaTable:true");
		VariableTypeSelectionDialog dialog = new VariableTypeSelectionDialog(null, properties);
		dialog.open();
		System.out.println(properties);
	}

}
