package com.kony.nativecodegen.ui;

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

import com.pat.tool.keditor.widgets.RichClientChannel;

/**
 * Dialog allow to choose the mobile platform to generate
 * native code for respective platforms.
 * 
 * Supported platform so far (Iphone,Android,Blackberry and Windows)
 * 
 * @author Rakesh
 */

public class NativeCodePlatformSelectionDialog extends TrayDialog {
	
	public static final int IPHONE_SELECTED = 1;
	public static final int ANDROID_SELECTED = 2;
	public static final int BB_SELECTED = 4;
	public static final int WINDOWS_SELECTED = 8;
	
	private Button iphoneBtn,androidBtn,bBtn,windowsBtn,selectAllBtn;
	
	private int selectionState;

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
		client.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		
		client.setLayout(new GridLayout());
		
		Label label = new Label(client,SWT.CHECK);
		label.setText("Please tick the platform below to generate native code");
		label.setLayoutData(new GridData());
		
		GridData gridData = new GridData();
		gridData.horizontalIndent = 5;
		selectAllBtn = new Button(client,SWT.CHECK);
		selectAllBtn.setText("Select &All");
		selectAllBtn.setLayoutData(gridData);
		
		gridData = new GridData();
		gridData.horizontalIndent = 25;
		iphoneBtn = new Button(client,SWT.CHECK);
		iphoneBtn.setText(RichClientChannel.IPHONE.getDisplayName());
		iphoneBtn.setImage(RichClientChannel.IPHONE.getImage());
		iphoneBtn.setLayoutData(gridData);
		iphoneBtn.addSelectionListener(selectionListener);
		
		androidBtn = new Button(client,SWT.CHECK);
		androidBtn.setText(RichClientChannel.ANDROID.getDisplayName());
		androidBtn.setImage(RichClientChannel.ANDROID.getImage());
		androidBtn.setLayoutData(gridData);
		androidBtn.addSelectionListener(selectionListener);

		
		bBtn = new Button(client,SWT.CHECK);
		bBtn.setText(RichClientChannel.BLACKBERRY.getDisplayName());
		bBtn.setImage(RichClientChannel.BLACKBERRY.getImage());
		bBtn.setLayoutData(gridData);
		bBtn.addSelectionListener(selectionListener);

		
		/*windowsBtn = new Button(client,SWT.CHECK);
		windowsBtn.setText(RichClientChannel.WINMOBILE.getDisplayName());
		windowsBtn.setImage(RichClientChannel.WINMOBILE.getImage());
		windowsBtn.setLayoutData(gridData);*/
		
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
				iphoneBtn.setSelection(selected);
				androidBtn.setSelection(selected);
				bBtn.setSelection(selected);
				//windowsBtn.setSelection(selected);
				getButton(IDialogConstants.OK_ID).setEnabled(selected);
			}
		});
		

		return parent;
	}
	
	private SelectionListener selectionListener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
		     selectAllBtn.setSelection(iphoneBtn.getSelection() && androidBtn.getSelection() && bBtn.getSelection());	
			 getButton(IDialogConstants.OK_ID).setEnabled(iphoneBtn.getSelection() || androidBtn.getSelection() || bBtn.getSelection());
		}
	};
	
	public int getSelectedState(){
		return selectionState;
	}
	
	@Override
	protected void okPressed() {
		if(iphoneBtn.getSelection())
			selectionState |= IPHONE_SELECTED;
		if(androidBtn.getSelection())
			selectionState |= ANDROID_SELECTED;
		if(bBtn.getSelection())
			selectionState |= BB_SELECTED;
		/*if(windowsBtn.getSelection())
			selectionState |= WINDOWS_SELECTED;*/
		super.okPressed();
	}
	

}
