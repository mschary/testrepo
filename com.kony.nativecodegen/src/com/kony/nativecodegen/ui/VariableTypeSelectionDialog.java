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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.pat.tool.keditor.propertyDescriptor.TableLabelProvider;
import com.pat.tool.keditor.utils.SWTResourceUtil;

public class VariableTypeSelectionDialog extends Dialog {

	private static final String DELIMITER = ":";
	private static final String TYPE = "TYPE";
	private static final String VARIABLE = "VARIABLE";
	private static final String USAGE = "USAGE";

	private Properties variablesMap;
	private Map<Object, Object> backupMap = new HashMap<Object, Object>();
	private TableViewer viewer;
	private String message;
	private static List<String> variableTypes = new ArrayList<String>();

	static {
		variableTypes.add("number");
		variableTypes.add("boolean");
		variableTypes.add("string");
		variableTypes.add("luatable");
	}

	public VariableTypeSelectionDialog(Shell parentShell, Properties variablesMap, String message) {
		super(parentShell);
		setShellStyle(SWT.RESIZE | SWT.MODELESS);
		this.variablesMap = variablesMap;
		backupMap.putAll(variablesMap);
		this.message = message;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Variable Selection");
		Composite container = new Composite(parent, SWT.NONE);
		int heightHint = Math.min(variablesMap.size() * 25, Display.getDefault().getClientArea().height / 2);
		GridDataFactory.fillDefaults().grab(true, true).hint(500, heightHint).applyTo(container);
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
		tableColumnLayout.setColumnData(variableColumn, new ColumnWeightData(80, 300, true));
		tableColumnLayout.setColumnData(dataTypeColumn, new ColumnWeightData(10, 100, true));
		tableColumnLayout.setColumnData(usageColumn, new ColumnWeightData(10, 100, true));

		viewer.setInput(variablesMap);

		viewer.setColumnProperties(new String[] { VARIABLE, TYPE, USAGE });
		viewer.setCellModifier(getCellModifier());
		viewer.setCellEditors(new CellEditor[] { new TextCellEditor(table), new ComboBoxCellEditor(table, variableTypes.toArray(new String[0])), new TextCellEditor(table) });
		viewer.setSorter(new ViewerSorter());
		Label messageLabel = new Label(parent, SWT.WRAP);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(500, SWT.DEFAULT).applyTo(messageLabel);
		messageLabel.setText(message);
		Color blueColor = new Color(Display.getDefault(), new RGB(0, 0, 255));
		SWTResourceUtil.cleanupOnDispose(messageLabel, blueColor);
		messageLabel.setForeground(blueColor);
		
		return container;
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
		if (value == null) {
			return Boolean.TRUE.toString();
		}
		String[] split = value.split(DELIMITER);
		return split.length == 2 ? value.split(DELIMITER)[1] : Boolean.TRUE.toString();
	}

	public static void main(String[] args) {
		test();
	}

	public static void test() {
		Properties properties = new Properties();
		properties.put("Name", "String:true");
		properties.put("Age", "String");
		properties.put("Sex", "");
		properties.put("Id", "Number:true");
		properties.put("Address", "LuaTable:true");
		VariableTypeSelectionDialog dialog = new VariableTypeSelectionDialog(new Shell(), properties, "Select type for the above variables");
		dialog.open();
		System.out.println(properties);
	}

}
