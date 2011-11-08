package com.kony.nativecodegen.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.pat.tool.keditor.editors.help.ContextSensitiveHelpConstants;
import com.pat.tool.keditor.editors.help.ContextSensitiveHelpUtil;
import com.pat.tool.keditor.utils.RendererHashMapData;
import com.pat.tool.keditor.widgets.IMobileChannel;
import com.pat.tool.keditor.widgets.RichClientChannel;
import com.pat.tool.keditor.widgets.TabletRichClientChannel;

/**
 * Dialog allow to choose the mobile platform to generate
 * native code for respective platforms.
 * 
 * Supported platform so far (Iphone,Android,Blackberry and Ipad)
 * 
 * @author Rakesh
 */

public class NativeCodePlatformSelectionDialog extends TrayDialog {
	
	public static final int IPHONE_SELECTED = 1;
	public static final int ANDROID_SELECTED = 2;
	public static final int BB_SELECTED = 4;
	public static final int IPAD_SELECTED = 8;
	private RendererHashMapData rendererHashMapData = new RendererHashMapData(false);
	
	private Button selectAllBtn;
	private List<Button> platformButtons = new ArrayList<Button>();

	public NativeCodePlatformSelectionDialog(Shell shell) {
		super(shell);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select Platform");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite client = new Composite(parent,SWT.NONE);
		client.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		client.setLayout(new GridLayout());
		
		Label label = new Label(client,SWT.CHECK);
		label.setText("Please tick the platform below to generate native code");
		label.setLayoutData(new GridData());
		
		GridData gridData = new GridData();
		gridData.horizontalIndent = 5;
		selectAllBtn = new Button(client,SWT.CHECK);
		selectAllBtn.setText("Select &All");
		selectAllBtn.setLayoutData(gridData);
		
		createButton(RichClientChannel.IPHONE, client);
		createButton(RichClientChannel.ANDROID, client);
		createButton(RichClientChannel.BLACKBERRY, client);
		createButton(TabletRichClientChannel.IPAD, client);
		
		Label lineLabel = new Label(client, SWT.NONE);
		lineLabel.setText("____________________________________________________" +
				"___________________________________________");
		
		Label noteLabel = new Label(client, SWT.NONE);
		noteLabel.setText("Note: It is recommended to perform project build, before invoking native code generation.");
		noteLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		
		
		selectAllBtn.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean selected = selectAllBtn.getSelection();
				for (Button button : platformButtons) {
					button.setSelection(selected);
				}
				okButton.setEnabled(selected);
			}
		});
		
		ContextSensitiveHelpUtil.hookHelp(parent,ContextSensitiveHelpConstants.GENERATE_NATIVE_CODE);

		return parent;
	}

	public void createButton(IMobileChannel mobileChannel, Composite client) {
		GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gridData.horizontalIndent = 25;
		Button button = new Button(client,SWT.CHECK);
		button.setText(mobileChannel.getDisplayName());
		button.setImage(mobileChannel.getImage());
		button.setLayoutData(gridData);
		button.addSelectionListener(selectionListener);
		button.setData(mobileChannel);
		platformButtons.add(button);
	}
	
	private SelectionListener selectionListener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			boolean isSelected = false;
			for (Button button : platformButtons) {
				if (button.getSelection()) {
					isSelected = true;
				}
			}
			selectAllBtn.setSelection(isSelected);
			okButton.setEnabled(isSelected);
		}
	};

	private Button okButton;
	
	@Override
	protected void okPressed() {
		for (Button button : platformButtons) {
			if (button.getSelection()) {
				IMobileChannel mobileChannel = ((IMobileChannel)button.getData());
				rendererHashMapData.put(mobileChannel, true);
			}
		}
		super.okPressed();
	}
	
	public RendererHashMapData getRendererHashMapData() {
		return rendererHashMapData;
	}
	

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected Control createContents(Composite parent) {
		// TODO Auto-generated method stub
		return super.createContents(parent);
	}
	

}
