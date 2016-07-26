package capsensor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpringLayout;


public class SettingsPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	// Constant strings
	public static final String serialInitializeString = "Initialize Port";
	public static final String serialDisconnectString = "Disconnect Port";
	private static final String updateSettingsString = "Update Thresholds";
	
	// Serial port components	
	public static final JTextField comPortField = new JTextField(AppSettings.serialPortNumber);
	public static final JButton comPortButton = new JButton(serialInitializeString);

	// Display settings components
	public static final JRadioButton graphViewBtn = new JRadioButton("Graph View");
	public static final JRadioButton capViewBtn = new JRadioButton("Capacitance");
	public static final ButtonGroup displaySettings = new ButtonGroup();
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
	public static final JTextField threshHoverField = new JTextField(Float.toString(AppSettings.relDiffHover * 100f));
	public static final JTextField threshPressField = new JTextField(Float.toString(AppSettings.relDiffPress * 100f));
	public static final JTextField threshReleaseField = new JTextField(Float.toString(AppSettings.relDiffRelease * 100f));
	public static final JButton updateButton = new JButton(updateSettingsString);
		
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
						CapMatrix.initializeSerialConnection();
					} catch (IOException e1) {
						System.out.println("IO Error: " + e1);
					}
				} else {
					try {
						CapMatrix.endSerialConnection();
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
		capViewBtn.setSelected(true);
		capViewBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CapMatrix.displayMode = CapMatrix.CAPACITANCE_MODE;	
				CapMatrix.findTouchPoints();
			}			
		});
		graphViewBtn.setSelected(false);
		graphViewBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CapMatrix.displayMode = CapMatrix.GRAPH_MODE;
				CapMatrix.updateMatrix();
			}			
		});
		displaySettings.add(capViewBtn);
		displaySettings.add(graphViewBtn);		
		JPanel displaySettingsButtons = new JPanel();
		displaySettingsButtons.add(capViewBtn);
		displaySettingsButtons.add(graphViewBtn);
		
		displaySettingsList = new JComboBox<String>(displayOptions);
		displaySettingsList.setSelectedIndex(0);
		displaySettingsList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				JComboBox<String> cb = (JComboBox<String>)e.getSource();
				String selectedOption = (String)cb.getSelectedItem();
				if(selectedOption == displayCapString) {
					InterfaceFunct.changeDisplayMode(CapMatrix.CAPACITANCE_MODE);
				} else if(selectedOption == displayGraphString) {
					InterfaceFunct.changeDisplayMode(CapMatrix.GRAPH_MODE);
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
					InterfaceFunct.changeFilterMode(CapMatrix.FILTER_NONE);
				} else if(selectedOption == filterSrString) {
					InterfaceFunct.changeFilterMode(CapMatrix.FILTER_SR);
				} else if(selectedOption == filterLpLPtAvgString) {
					InterfaceFunct.changeFilterMode(CapMatrix.FILTER_LP);
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
					InterfaceFunct.changeTouchMode(CapMatrix.TOUCH_THRESHOLD);
				} else if(selectedOption == touch1PtMaxString) {
					InterfaceFunct.changeTouchMode(CapMatrix.TOUCH_1PTMAX);
				}
				
			}			
		});
		SettingGroupPanel touchSettingsPanel = new SettingGroupPanel("Touch Recognition Method", touchSettingsList);
		this.add(touchSettingsPanel);
		layout.putConstraint(SpringLayout.WEST, touchSettingsPanel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, touchSettingsPanel, 10, SpringLayout.SOUTH, filterSettingsPanel);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, touchSettingsPanel, 0, SpringLayout.HORIZONTAL_CENTER, this);
				
		// Initialize threshold fields
		JLabel threshHoverLabel = new JLabel("Hover Change Threshold (%):");
		this.add(threshHoverLabel);
		this.add(threshHoverField);
		layout.putConstraint(SpringLayout.WEST, threshHoverLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, threshHoverLabel, 15, SpringLayout.SOUTH, touchSettingsPanel);
		layout.putConstraint(SpringLayout.WEST, threshHoverField, 5, SpringLayout.EAST, threshHoverLabel);
		layout.putConstraint(SpringLayout.NORTH, threshHoverField, 15, SpringLayout.SOUTH, touchSettingsPanel);
		layout.putConstraint(SpringLayout.EAST, comPortField, 0, SpringLayout.EAST, threshHoverField);
		JLabel threshPressLabel = new JLabel("Press Change Threshold (%):");
		this.add(threshPressLabel);
		this.add(threshPressField);
		layout.putConstraint(SpringLayout.WEST, threshPressLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, threshPressLabel, 15, SpringLayout.SOUTH, threshHoverField);
		layout.putConstraint(SpringLayout.WEST, threshPressField, 5, SpringLayout.EAST, threshPressLabel);
		layout.putConstraint(SpringLayout.NORTH, threshPressField, 15, SpringLayout.SOUTH, threshHoverField);
		layout.putConstraint(SpringLayout.EAST, threshHoverField, 0, SpringLayout.EAST, threshPressField);
		JLabel threshReleaseLabel = new JLabel("Release Change Threshold (%):");
		this.add(threshReleaseLabel);
		this.add(threshReleaseField);
		layout.putConstraint(SpringLayout.WEST, threshReleaseLabel, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, threshReleaseLabel, 15, SpringLayout.SOUTH, threshPressField);
		layout.putConstraint(SpringLayout.WEST, threshReleaseField, 5, SpringLayout.EAST, threshReleaseLabel);
		layout.putConstraint(SpringLayout.NORTH, threshReleaseField, 15, SpringLayout.SOUTH, threshPressField);
		layout.putConstraint(SpringLayout.EAST, threshPressField, 0, SpringLayout.EAST, threshReleaseField);
		
		
		// Initialize update button
		updateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Float tmpThresholdPress = Float.valueOf(SettingsPanel.threshPressField.getText())/100f;
				if(tmpThresholdPress >= 0 && tmpThresholdPress != null) {
					AppSettings.relDiffPress = tmpThresholdPress;
					CapMatrix.debugText = "Settings updated.";
					CapMatrix.updateDebugText();
				} else {
					CapMatrix.debugText = "Invalid press threshold value - keeping previous value";
					CapMatrix.updateDebugText();
					return;
				}
				Float tmpThresholdRelease = Float.valueOf(SettingsPanel.threshReleaseField.getText())/100f;
				if(tmpThresholdRelease >= 0 && tmpThresholdRelease != null) {
					AppSettings.relDiffRelease = tmpThresholdRelease;
					CapMatrix.debugText = "Settings updated.";
					CapMatrix.updateDebugText();
				} else {
					CapMatrix.debugText = "Invalid release threshold value - keeping previous value";
					CapMatrix.updateDebugText();
					return;
				}
			}
		});
		this.add(updateButton);
		layout.putConstraint(SpringLayout.WEST, updateButton, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, updateButton, 10, SpringLayout.SOUTH, threshReleaseField);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, updateButton, 0, SpringLayout.HORIZONTAL_CENTER, this);
		
		// Initially disable panel
		disablePanel();
	}
	
	public static void enablePanel() {
		// Disable com port field
		comPortField.setEnabled(false);
		// Enable display settings radio group
		displaySettingsList.setEnabled(true);
		// Enable threshold input fields
		threshHoverField.setEnabled(true);
		threshPressField.setEnabled(true);
		threshReleaseField.setEnabled(true);
		// Enable filter settings radio group
		filterSettingsList.setEnabled(true);
		// Enable touch mode settings
		touchSettingsList.setEnabled(true);
		// Enable update button
		updateButton.setEnabled(true);
	}
	
	public static void disablePanel() {
		// Enable com port field
		comPortField.setEnabled(true);
		// Disable display settings radio group
		displaySettingsList.setEnabled(false);
		// Disable threshold input fields
		threshHoverField.setEnabled(false);
		threshPressField.setEnabled(false);
		threshReleaseField.setEnabled(false);
		// Disable filter settings radio group
		filterSettingsList.setEnabled(false);
		// Disable touch mode settings
		touchSettingsList.setEnabled(false);
		// Disable update button
		updateButton.setEnabled(false);
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
