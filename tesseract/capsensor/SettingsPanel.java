package capsensor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;


public class SettingsPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	// Constant strings
	public static final String serialInitializeString = "Initialize Port";
	public static final String serialDisconnectString = "Disconnect Port";
	private static final String updateString = "Update";
	private static final String updateBaselinesString = "Update Baselines";

	// Serial port components	
	public static final JTextField comPortField = new JTextField(AppSettings.serialPortNumber);
	public static final JButton comPortButton = new JButton(serialInitializeString);

	// Display settings components
	private static final String displayCapString = "Capacitance view";
	private static final String displayGraphString = "Graph view";
	private static final String[] displayOptions = {displayCapString, displayGraphString};
	private static JComboBox<String> displaySettingsList;
	
	// Filter settings components
	private static final String filterNoneString = "No filter";
	private static final String filterSrString = "Slew rate filter";
	private static final String filterLpLPtAvgString = "Low pass (L-point average) filter";
	private static final String filterLpButterString = "Low pass (Butterworth) filter";
	private static final String[] filterOptions = {filterNoneString, filterSrString, filterLpLPtAvgString};
	private static JComboBox<String> filterSettingsList;
	
	// Touch recognition methods
	private static final String touchThresholdString = "Threshold-based";
	private static final String touch1PtMaxString = "1PtMax - Abs/Rel max search";
	private static final String[] touchOptions = {touchThresholdString, touch1PtMaxString};
	private static JComboBox<String> touchSettingsList;
	
	// Threshold field components
	static DecimalFormat df = new DecimalFormat("#0.0");
	public static final JTextField threshPressField = new JTextField(df.format(AppSettings.relDiffPress * 100f));
	public static final JTextField threshReleaseField = new JTextField(df.format(AppSettings.relDiffRelease * 100f));
	public static final JButton updateThresholdButton = new JButton(updateString);

	// Min and max range of graphs
	public static final JTextField minRangeField = new JTextField();
	public static final JTextField maxRangeField = new JTextField();
	public static final JButton updateRangeButton = new JButton(updateString);

	// Update baselines button
	public static final JButton updateBaselinesButton = new JButton(updateBaselinesString);

	// System state variables
	public static Boolean isConnected = false;
	
	
	public SettingsPanel() {
		// Initialize layout
		this.setPreferredSize(new Dimension(300,AppSettings.heightRect*AppSettings.numRows));
		this.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		SpringLayout layout = new SpringLayout();
		this.setLayout(layout);
		
		// Initialize Panel Title
		JLabel panelLabel = new JLabel("Settings");
		panelLabel.setFont(new Font("Arial", Font.BOLD, 18));
		panelLabel.setHorizontalAlignment(JLabel.CENTER);
		panelLabel.setVerticalAlignment(JLabel.TOP);
		this.add(panelLabel);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, panelLabel, 0, SpringLayout.HORIZONTAL_CENTER, this);
		layout.putConstraint(SpringLayout.NORTH, panelLabel, 5, SpringLayout.NORTH, this);
		// Initialize com port field
		JLabel comPortLabel = new JLabel("COM PORT:");
		this.add(comPortLabel);
		this.add(comPortField);
		layout.putConstraint(SpringLayout.WEST, comPortLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, comPortLabel, 15, SpringLayout.SOUTH, panelLabel);
		layout.putConstraint(SpringLayout.WEST, comPortField, 5, SpringLayout.EAST, comPortLabel);
		layout.putConstraint(SpringLayout.NORTH, comPortField, 15, SpringLayout.SOUTH, panelLabel);
		layout.putConstraint(SpringLayout.EAST, this, 20, SpringLayout.EAST, comPortField);
		
		// Initialize com port button
		comPortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AppSettings.serialPortNumber = comPortField.getText();
				if(!SettingsPanel.isConnected) {
					try {
						PortUtil.initializeSerialConnection();
					} catch (IOException e1) {
						System.out.println("IO Error: " + e1);
					}
				} else {
					try {
						PortUtil.endSerialConnection();
					} catch (IOException e1) {
						System.out.println("IO Error: " + e1);
					}

				}				
			}			
		});
		this.add(comPortButton);
		layout.putConstraint(SpringLayout.WEST, comPortButton, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, comPortButton, 10, SpringLayout.SOUTH, comPortField);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, comPortButton, 0, SpringLayout.HORIZONTAL_CENTER, this);

		// Initialize display settings combo box
		displaySettingsList = new JComboBox<String>(displayOptions);
		displaySettingsList.setSelectedIndex(0);
		displaySettingsList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				JComboBox<String> cb = (JComboBox<String>)e.getSource();
				String selectedOption = (String)cb.getSelectedItem();
				if(selectedOption == displayCapString) {
					InterfaceFunct.changeDisplayMode(InterfaceFunct.CAPACITANCE_MODE);
				} else if(selectedOption == displayGraphString) {
					InterfaceFunct.changeDisplayMode(InterfaceFunct.GRAPH_MODE);
				}
				
			}			
		});

		SettingGroupPanel displaySettingsPanel = new SettingGroupPanel("Display Mode", displaySettingsList);		
		this.add(displaySettingsPanel);
		layout.putConstraint(SpringLayout.WEST, displaySettingsPanel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, displaySettingsPanel, 10, SpringLayout.SOUTH, comPortButton);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, displaySettingsPanel, 0, SpringLayout.HORIZONTAL_CENTER, this);
		
		// Initialize filter combo box		
		filterSettingsList = new JComboBox<String>(filterOptions);
		filterSettingsList.setSelectedIndex(0);
		filterSettingsList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				JComboBox<String> cb = (JComboBox<String>)e.getSource();
				String selectedOption = (String)cb.getSelectedItem();
				if(selectedOption == filterNoneString) {
					InterfaceFunct.changeFilterMode(FilterUtil.FILTER_NONE);
				} else if(selectedOption == filterSrString) {
					InterfaceFunct.changeFilterMode(FilterUtil.FILTER_SR);
				} else if(selectedOption == filterLpLPtAvgString) {
					InterfaceFunct.changeFilterMode(FilterUtil.FILTER_LP);
				}
				
			}			
		});
		SettingGroupPanel filterSettingsPanel = new SettingGroupPanel("Filter Mode", filterSettingsList);
		this.add(filterSettingsPanel);
		
		layout.putConstraint(SpringLayout.WEST, filterSettingsPanel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, filterSettingsPanel, 10, SpringLayout.SOUTH, displaySettingsPanel);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, filterSettingsPanel, 0, SpringLayout.HORIZONTAL_CENTER, this);

		// Initialize touch recognition combo box
		touchSettingsList = new JComboBox<String>(touchOptions);
		touchSettingsList.setSelectedIndex(0);
		touchSettingsList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				JComboBox<String> cb = (JComboBox<String>)e.getSource();
				String selectedOption = (String)cb.getSelectedItem();
				if(selectedOption == touchThresholdString) {
					InterfaceFunct.changeTouchMode(InterfaceFunct.TOUCH_THRESHOLD);
				} else if(selectedOption == touch1PtMaxString) {
					InterfaceFunct.changeTouchMode(InterfaceFunct.TOUCH_1PTMAX);
				}
				
			}			
		});
		SettingGroupPanel touchSettingsPanel = new SettingGroupPanel("Touch Recognition Method", touchSettingsList);
		this.add(touchSettingsPanel);
		layout.putConstraint(SpringLayout.WEST, touchSettingsPanel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, touchSettingsPanel, 10, SpringLayout.SOUTH, filterSettingsPanel);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, touchSettingsPanel, 0, SpringLayout.HORIZONTAL_CENTER, this);
				
		// Initialize threshold fields
		JLabel threshPressLabel = new JLabel("Touch Threshold (%):");
		this.add(threshPressLabel);
		this.add(threshPressField);
		layout.putConstraint(SpringLayout.WEST, threshPressLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, threshPressLabel, 15, SpringLayout.SOUTH, touchSettingsPanel);
		layout.putConstraint(SpringLayout.WEST, threshPressField, 5, SpringLayout.EAST, threshPressLabel);
		layout.putConstraint(SpringLayout.NORTH, threshPressField, 15, SpringLayout.SOUTH, touchSettingsPanel);
		layout.putConstraint(SpringLayout.EAST, comPortField, 0, SpringLayout.EAST, threshPressField);
		JLabel threshReleaseLabel = new JLabel("Release Change Threshold (%):");
		this.add(threshReleaseLabel);
		this.add(threshReleaseField);
		layout.putConstraint(SpringLayout.WEST, threshReleaseLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, threshReleaseLabel, 15, SpringLayout.SOUTH, threshPressField);
		layout.putConstraint(SpringLayout.WEST, threshReleaseField, 5, SpringLayout.EAST, threshReleaseLabel);
		layout.putConstraint(SpringLayout.NORTH, threshReleaseField, 15, SpringLayout.SOUTH, threshPressField);
		layout.putConstraint(SpringLayout.EAST, threshPressField, 0, SpringLayout.EAST, threshReleaseField);
		
		
		// Initialize update button
		updateThresholdButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Float tmpThresholdPress = Float.valueOf(SettingsPanel.threshPressField.getText())/100f;
				if(tmpThresholdPress != null) {
					AppSettings.relDiffPress = tmpThresholdPress;
					CapMatrix.debugText = "Settings updated.";
					FrameUtil.updateDebugText();
				} else {
					CapMatrix.debugText = "Invalid press threshold value - keeping previous value";
					FrameUtil.updateDebugText();
					return;
				}

				Float tmpThresholdRelease = Float.valueOf(SettingsPanel.threshReleaseField.getText())/100f;
				if(tmpThresholdRelease != null) {
					AppSettings.relDiffRelease = tmpThresholdRelease;
					CapMatrix.debugText = "Settings updated.";
					FrameUtil.updateDebugText();
				} else {
					CapMatrix.debugText = "Invalid release threshold value - keeping previous value";
					FrameUtil.updateDebugText();
					return;
				}
			}
		});
		this.add(updateThresholdButton);
		layout.putConstraint(SpringLayout.WEST, updateThresholdButton, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, updateThresholdButton, 10, SpringLayout.SOUTH, threshReleaseField);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, updateThresholdButton, 0, SpringLayout.HORIZONTAL_CENTER, this);



		// Initialize range fields
		JLabel minRangeLabel = new JLabel("Range min:");
		this.add(minRangeLabel);
		this.add(minRangeField);
		layout.putConstraint(SpringLayout.WEST, minRangeLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, minRangeLabel, 15, SpringLayout.SOUTH, updateThresholdButton);
		layout.putConstraint(SpringLayout.WEST, minRangeField, 5, SpringLayout.EAST, minRangeLabel);
		layout.putConstraint(SpringLayout.NORTH, minRangeField, 15, SpringLayout.SOUTH, updateThresholdButton);
		layout.putConstraint(SpringLayout.EAST, threshPressField, 0, SpringLayout.EAST, minRangeField);

		JLabel maxRangeLabel = new JLabel("Range max:");
		this.add(maxRangeLabel);
		this.add(maxRangeField);
		layout.putConstraint(SpringLayout.WEST, maxRangeLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, maxRangeLabel, 15, SpringLayout.SOUTH, minRangeField);
		layout.putConstraint(SpringLayout.WEST, maxRangeField, 5, SpringLayout.EAST, maxRangeLabel);
		layout.putConstraint(SpringLayout.NORTH, maxRangeField, 15, SpringLayout.SOUTH, minRangeField);
		layout.putConstraint(SpringLayout.EAST, minRangeField, 0, SpringLayout.EAST, maxRangeField);


		// Initialize update range button
		updateRangeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				try{
					AppSettings.minRange = Double.parseDouble(SettingsPanel.minRangeField.getText());
					CapMatrix.debugText = "Settings updated.";
					FrameUtil.updateDebugText();

					DataController.dataTrace.clear();
					DataController.chartsArray.clear();

					CapMatrix.prepareDataStructures();
					FrameUtil.updateMatrix();
				} catch (NumberFormatException n){
					CapMatrix.debugText = "Invalid min value - keeping previous value";
					FrameUtil.updateDebugText();
					return;
				}

				try{
					AppSettings.maxRange = Double.parseDouble(SettingsPanel.maxRangeField.getText());
					CapMatrix.debugText = "Settings updated.";
					FrameUtil.updateDebugText();

					DataController.dataTrace.clear();
					DataController.chartsArray.clear();

					CapMatrix.prepareDataStructures();
					FrameUtil.updateMatrix();
				} catch (NumberFormatException f) {
					CapMatrix.debugText = "Invalid max value - keeping previous value";
					FrameUtil.updateDebugText();
					return;
				}


			}
		});
		this.add(updateRangeButton);
		layout.putConstraint(SpringLayout.WEST, updateRangeButton, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, updateRangeButton, 10, SpringLayout.SOUTH, maxRangeField);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, updateRangeButton, 0, SpringLayout.HORIZONTAL_CENTER, this);


		// Initialize update range button
		updateBaselinesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				try{
					DataController.updateBaselines();
					FrameUtil.updateDebugText();
					FrameUtil.updateMatrix();
				} catch (IOException e1){
					CapMatrix.debugText = "Error updating baselines";
					FrameUtil.updateDebugText();
					return;
				}

			}
		});
		this.add(updateBaselinesButton);
		layout.putConstraint(SpringLayout.WEST, updateBaselinesButton, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, updateBaselinesButton, 10, SpringLayout.SOUTH, updateRangeButton);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, updateBaselinesButton, 0, SpringLayout.HORIZONTAL_CENTER, this);


		// Initially disable panel
		disablePanel();
	}
	
	public static void enablePanel() {
		// Disable com port field
		comPortField.setEnabled(false);
		// Enable display settings radio group
		displaySettingsList.setEnabled(true);
		// Enable threshold input fields
		threshPressField.setEnabled(true);
		threshReleaseField.setEnabled(true);
		// Enable filter settings radio group
		filterSettingsList.setEnabled(true);
		// Enable touch mode settings
		touchSettingsList.setEnabled(true);
		// Enable update button
		updateThresholdButton.setEnabled(true);
	}
	
	public static void disablePanel() {
		// Enable com port field
		comPortField.setEnabled(true);
		// Disable display settings radio group
		displaySettingsList.setEnabled(false);
		// Disable threshold input fields
		threshPressField.setEnabled(false);
		threshReleaseField.setEnabled(false);
		// Disable filter settings radio group
		filterSettingsList.setEnabled(false);
		// Disable touch mode settings
		touchSettingsList.setEnabled(false);
		// Disable update button
		updateThresholdButton.setEnabled(false);
	}
	
}

class SettingGroupPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private JPanel interiorPanel = new JPanel();
	
	public SettingGroupPanel(String labelString, JPanel radioGroup) {
//		this.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		this.setBorder(BorderFactory.createTitledBorder(labelString));
//		BorderLayout groupLayout = new BorderLayout();
//		this.setLayout(groupLayout);
		this.setLayout(new GridLayout(1,0));
		// Initialize label component
//		JLabel groupLabel = new JLabel(labelString);
//		groupLabel.setHorizontalAlignment(JLabel.CENTER);
		// Add components to panel
//		this.add(groupLabel, BorderLayout.PAGE_START);
//		this.add(radioGroup, BorderLayout.PAGE_END);
//		this.add(groupLabel);
		this.add(radioGroup);
	}
	
	public SettingGroupPanel(String labelString, Component settingsBlock) {
		this.setBorder(BorderFactory.createTitledBorder(labelString));
		this.setLayout(new GridLayout(1,0));
		this.add(interiorPanel);
		interiorPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		interiorPanel.setLayout(new GridLayout(1,0));
		interiorPanel.add(settingsBlock);
	}
}
