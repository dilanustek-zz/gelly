package capsensor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


import info.monitorenter.gui.chart.*;
import info.monitorenter.gui.chart.labelformatters.LabelFormatterSimple;
import info.monitorenter.gui.chart.axis.AAxis;
import info.monitorenter.gui.chart.labelformatters.LabelFormatterDate;
import info.monitorenter.gui.chart.rangepolicies.RangePolicyFixedViewport;
import info.monitorenter.gui.chart.traces.Trace2DLtd;
import info.monitorenter.gui.chart.views.ChartPanel;
import info.monitorenter.util.Range;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * Java GUI for displaying the capacitance values read by capacitive sensor
 * 
 * @author Caleb Ng (2015) for Tesseract Technologies
 *
 */
public class CapMatrix extends JPanel {
	private static final long serialVersionUID = 1L;
	private static SerialPort serialPort;
	
	// UI variables
	private static final String frameTitle = "Capacitive Sensor - Readout Matrix";
	private static JFrame f = new JFrame();
	protected static String debugText = new String();
	private static final String DEBUG_COM_UNAVAILABLE = "Unable to connect to Arduino...\nThe serial may be incorrect, not connected, or busy.";
	private static final String DEBUG_WAIT_CALIBRATE = "Calibrating baseline...";
	private static final String DEBUG_START = "Application started.";
	private static JRadioButton rawReading = new JRadioButton("RAW");
	private static JRadioButton maxReading = new JRadioButton("MAX");
	private static JRadioButton srfReading = new JRadioButton("SLEW");
	private static JRadioButton capViewBtn = new JRadioButton("Capacitance");
	private static JRadioButton graphViewBtn = new JRadioButton("Graph View");
	private static ButtonGroup displaySettings = new ButtonGroup();

	
	// GUI Component variables
	private static HashMap<String, Component> componentMap;
	private static final String matrixName = "GridMatrix";
	private static final String debugTextName = "DebugText";
	private static GridBagConstraints c_settings = new GridBagConstraints();
	private static GridBagConstraints c_title = new GridBagConstraints();
	private static GridBagConstraints c_matrix =  new GridBagConstraints();
	private static GridBagConstraints c_debug = new GridBagConstraints();
	
	// Matrix variables
	private static HashMap<Integer,int[]> gridAddressMap = new HashMap<Integer, int[]>();
	
	// Data transmission variables
	protected static Boolean isDataTransmitting = false;
	protected static HashMap<Integer, Float> processedCap = new HashMap<Integer, Float>();
	protected static HashMap<Integer, Float> rawCap = new HashMap<Integer, Float>();
	protected static HashMap<Integer, Float> relCapDiff = new HashMap<Integer, Float>();
	protected static HashMap<Integer, Float> absCapDiff = new HashMap<Integer, Float>();
	
	// Capacitance thresholds
	protected static HashMap<Integer, Float> baselineThresholdMap = new HashMap<Integer, Float>();
	
	// Regex patterns for parsing data from Arduino
	protected static final String patternBeginTransmission = "BEGIN";
	protected static final String patternEndTransmission = "END";
	protected static final String patternDataTransmission = "^\\((\\d+)\\)\\s*(\\d*\\.*\\d+)";
	protected static final String patternDataBaseline = "^BASELINE\\s*\\((\\d+)\\)\\s*(\\d*\\.*\\d+)";
	
	// Cell states
	protected static HashMap<Integer, Integer> cellStateMap = new HashMap<Integer, Integer>();
//	private static HashMap<Integer, Integer> touchPoints = new HashMap<Integer, Integer>();
	protected static HashSet<Integer> touchPoints = new HashSet<Integer>();
	protected static final int STATE_IDLE = 0;
	protected static final int STATE_HOVER = 1;
	protected static final int STATE_PRESS = 2;

	// GUI Max and Min Range
	protected static double minRange = 0.5;
	protected static double maxRange = 1.5;

	// Filtering parameters
	private static int numDataPts = AppSettings.numColumns * AppSettings.numRows;
	protected static final int FILTER_NONE = 0;
	protected static final int FILTER_SR = 1;
	protected static final int FILTER_LP = 2;
	protected static int filterMode = FILTER_NONE;
	
	// Touch recognition variables
	protected static final int TOUCH_THRESHOLD = 0;
	protected static final int TOUCH_1PTMAX = 1;
	protected static final int TOUCH_RELMAX = 2;
	protected static int touchMode = TOUCH_THRESHOLD;

	protected static int displayMode = 0;
	protected static final int CAPACITANCE_MODE = 0;
	protected static final int GRAPH_MODE = 1;


	// Saved traces of capacitance values for graphing
	private static ArrayList<ITrace2D> dataTrace = new ArrayList<ITrace2D>();
//	private static ArrayList<ChartObject> chartsArray = new ArrayList<ChartObject>();
	private static long startTime = System.currentTimeMillis();	// Used to help set the x-axis for graphing	
	protected static ArrayList<Chart2D> chartsArray = new ArrayList<Chart2D>();

	protected static JPanel[][] panelHolder = new JPanel[AppSettings.numRows][AppSettings.numColumns];

	// Timing variables
	private static int indexRead = 0;
	private static DateFormat indexStart;
	private static DateFormat indexStop;
	private static int indexMax = 1000;

	//csv file writer
	private static FileWriter writer;


	public static void main(String[] a) {
		// Try reading application settings from file
		try {
			System.out.println("Reading file");
			AppSettings.readFile(AppSettings.fileName);
			numDataPts = AppSettings.numColumns * AppSettings.numRows;
		} catch (IOException e) {
			System.out.println("Unable to read settings from file.");
		}
		// Prepare GUI elements
		f = new JFrame();
		f.setTitle(frameTitle);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		f.setLayout(new GridLayout(numRows,numColumns));
		f.setLayout(new GridBagLayout());
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if(serialPort != null) {
					try {
						serialPort.closePort();
					} catch (SerialPortException e1) {
//						e1.printStackTrace();
					}
				}
				System.exit(0);
			}
		});
		debugText = DEBUG_START;
		prepareDataStructures();
		addComponentsToFrame();
		f.pack();
//		f.setResizable(false);
//		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);	
	}
	
	/**
	 * Function for preparing data structures for initial run-through
	 */
	private static void prepareDataStructures() {
		// Prepare hash map
		gridAddressMap = new HashMap<Integer, int[]>();
		for(int i=0; i<AppSettings.numRows; i++) {
			for(int j=0; j<AppSettings.numColumns; j++) {
				System.out.println("Adding address: " + Integer.parseInt(AppSettings.gridAddress[(i*AppSettings.numRows) + j],2));
//				gridAddressMap.put(AppSettings.gridAddress[(i*AppSettings.numRows) + j], new int[]{i,j});
				gridAddressMap.put(Integer.parseInt(AppSettings.gridAddress[(i*AppSettings.numRows) + j],2), new int[]{i,j});
			}
		}		
		// Prepare trace array
//		IAxis chartAxis = new IAxis(new LabelFormatterDate(DateFormat.getDateInstance()));
		for(int i=0; i<numDataPts; i++) {
			dataTrace.add(new Trace2DLtd(50));
			dataTrace.get(i).setColor(Color.BLUE);
			dataTrace.get(i).setName("");
//			chartsArray.add(new ChartObject(dataTrace.get(i)));
			chartsArray.add(new Chart2D());
			chartsArray.get(i).addTrace(dataTrace.get(i));
			chartsArray.get(i).setMinimumSize(new Dimension(AppSettings.widthRect, AppSettings.heightRect));
			chartsArray.get(i).setPreferredSize(new Dimension(AppSettings.widthRect, AppSettings.heightRect));
			// Set axis format
			IAxis xAxis = chartsArray.get(i).getAxisX();
			xAxis.setFormatter(new LabelFormatterDate(new SimpleDateFormat("HH:mm:ss")));
			xAxis.setTitle("Time");
//			chartAxis.setFormatter(new LabelFormatterDate(new SimpleDateFormat("HH:mm:ss:SSS")));
			IAxis yAxis = chartsArray.get(i).getAxisY();
			//yAxis.setTitle("Capacitance");

			//set constant Y axis range
			IRangePolicy rangePolicy = new RangePolicyFixedViewport(new Range(minRange, maxRange));
			chartsArray.get(i).getAxisY().setRangePolicy(rangePolicy);

		}
	}
	

	/**
	 * Function set for managing components in the frame
	 */
	private static void createComponentMap() {
		componentMap = new HashMap<String, Component>();
		Component[] componentList = f.getContentPane().getComponents();
		for(int i=0; i<componentList.length; i++) {
			componentMap.put(componentList[i].getName(), componentList[i]);
		}
	}
	
	private static Component getComponentByName(String componentName) {
		if(componentMap.containsKey(componentName)) {
			return (Component) componentMap.get(componentName);
		} else {
			return null;
		}		
	}
	
	private static void addComponentsToFrame() {
		// Initialize components
		for(int i=0; i<AppSettings.numRows; i++) {
			for(int j=0; j<AppSettings.numColumns; j++) {
//				panelHolder[i][j] = addCell("null","", AppSettings.COLOR_DEFAULT);
				panelHolder[i][j] = new GridObject();
//				panelHolder[i][j] = chartsArray.get((i*AppSettings.numColumns) + j);
				panelHolder[i][j].setBorder(BorderFactory.createLineBorder(Color.black, 1));
			}
		}
		
		// Add title label to frame
		c_title = new GridBagConstraints();
		c_title.fill = GridBagConstraints.BOTH;
		c_title.gridx = 0;
		c_title.gridy = 0;
		c_title.gridheight = 1;
		c_title.gridwidth = AppSettings.numColumns + 2;
		c_title.weightx = 10;
		c_title.weighty = 1;
		c_title.anchor = GridBagConstraints.PAGE_START;
		c_title.insets = new Insets(5,10,5,10);
		JPanel headerPanel = new JPanel();
		JLabel headerText = new JLabel(frameTitle);
		headerText.setFont(new Font("Arial", Font.BOLD, 22));
		headerPanel.add(headerText);
		f.getContentPane().add(headerPanel, c_title);
		
		// Add settings panel
		c_settings = new GridBagConstraints();
		c_settings.fill = GridBagConstraints.BOTH;
		c_settings.gridx = AppSettings.numColumns + 1;
		c_settings.gridy = 1;
		c_settings.gridheight = AppSettings.numRows;
		c_settings.gridwidth = 1;
		c_settings.weightx = 1;
		c_settings.weighty = 1;
		c_settings.insets = new Insets(5,10,5,10);
		f.getContentPane().add(new SettingsPanel(), c_settings);

		// Add matrix to frame
		c_matrix =  new GridBagConstraints();
		c_matrix.fill = GridBagConstraints.BOTH;
		c_matrix.gridheight = 1;
		c_matrix.gridwidth = 1;
		c_matrix.weightx = 10;
		c_matrix.weighty = 10;
		c_matrix.gridx = 0;
		c_matrix.gridy = 1;
		c_matrix.insets = new Insets(5,10,5,10);
		GridMatrix matrix = new GridMatrix(panelHolder);
		matrix.setName(matrixName);
		f.getContentPane().add(matrix, c_matrix);
		
		// Add debug text to frame
		c_debug.gridx = 0;
		c_debug.gridy = AppSettings.numRows+1;
		c_debug.weightx = 10;
		c_debug.weighty = 1;
		c_debug.gridwidth = AppSettings.numColumns + 1;
		c_debug.ipady = 20;
		c_debug.anchor = GridBagConstraints.LAST_LINE_START;
		c_debug.insets = new Insets(5,10,5,10);
		JLabel debugTextComponent = new JLabel(debugText);
		debugTextComponent.setName(debugTextName);
		f.getContentPane().add(debugTextComponent, c_debug);
		
		createComponentMap();		
	}

	// Generic update command for filling entire grid matrix with current capacitance values or graphs
	protected static void updateMatrix() {
		// Clear matrix contents
		if(componentMap.containsKey(matrixName)) {
			f.getContentPane().remove(getComponentByName(matrixName));
//			f.getContentPane().remove(getComponentByName(debugTextName));
		}

		// Add matrix to frame
		GridMatrix matrix = new GridMatrix(panelHolder);
		matrix.setName(matrixName);
		f.getContentPane().add(matrix, c_matrix);
		componentMap.replace(matrixName, matrix);

		// Add debug text to frame
		updateDebugText();

		// Repaint components to frame
		f.getContentPane().revalidate();
		f.getContentPane().repaint();
	}

	protected static void updateDebugText() {
		JLabel debugTextComponent = (JLabel) getComponentByName(debugTextName);
		debugTextComponent.setText(debugText);
	}
	
	/**
	 * Functions for handling the commands received through serial
	 */	
	private static void parseCommand(String command)
			throws IOException {
		// Check if command is for setting data baseline
		Pattern pattern = Pattern.compile(patternDataBaseline);
		Matcher matcher = pattern.matcher(command);
		if (matcher.find()) {
			if (matcher.groupCount() == 2) {
				Integer gridAddress = Integer.parseInt(matcher.group(1),2);
				Float capValue = Float.parseFloat(matcher.group(2));

				addBaselinePoint(gridAddress, capValue);
			} else {
				System.out.println("### FAILED TO SET THRESHOLD ###");
			}
		} 
		// If not for baseline, check if data corresponds to a capacitance reading
		else {
			pattern = Pattern.compile(patternDataTransmission);
			matcher = pattern.matcher(command);
			if (matcher.find()) {
				if (matcher.groupCount() == 2) {
					Integer gridAddress = Integer.parseInt(matcher.group(1),2);
					Float capValue = Float.parseFloat(matcher.group(2));

					addCapacitancePoint(gridAddress, capValue);

					// write to csv TODO

					List<String> list = new ArrayList<>();
					list.add(gridAddress.toString());
					list.add(capValue.toString());

					CSVUtil.writeLine(writer, list);
					writer.flush();
				}
			}
		}



	}
	
	private static void addBaselinePoint(Integer gridAddress, Float capValue) {
		// Raw Implementation
//		CapMatrix.baselineThresholdMap.put(gridAddress, capValue);
		
		// Implementation with max search algorithm
		if(gridAddress == 0) {
			isDataTransmitting = true;
			switch(filterMode) {
				case FILTER_NONE:
					// Implementation with no filter
					baselineThresholdMap.put(gridAddress, capValue);
					break;
				case FILTER_SR:
					// Implementation with SRF
					baselineSlewRateFilter(gridAddress, capValue);
					break;
				case FILTER_LP:
					break;
			}
		} 
		// Implementation with max search algorithm	
		else if(isDataTransmitting) {
			switch(filterMode) {
				case FILTER_NONE:
					// Implementation with no filter
					baselineThresholdMap.put(gridAddress, capValue);
					break;
				case FILTER_SR:
					// Implementation with SRF
					baselineSlewRateFilter(gridAddress, capValue);
					break;
				case FILTER_LP:
					break;
			}
			if(gridAddress == (numDataPts-1)) {
				isDataTransmitting = false;
			}
		}
		else {
			System.out.println("### KEY (" + gridAddress + ") NOT FOUND ###");
		}
		debugText = "Updating baseline values...";
		updateDebugText();
	}
	
	private static void addCapacitancePoint(Integer gridAddress, Float capValue) {

		//System.out.println("gridAdress contains: " + gridAddressMap.containsKey(gridAddress));
		//System.out.println("baselinethresholdmap contains: " + baselineThresholdMap.containsKey(gridAddress));


		if(gridAddressMap.containsKey(gridAddress) && baselineThresholdMap.containsKey(gridAddress)) {
			if(gridAddress == 0) {
				isDataTransmitting = true;
				switch(filterMode) {
					case FILTER_NONE:
						// Implementation with no filter
						applyNoFilter(gridAddress, capValue);
						break;
					case FILTER_SR:
						// Implementation with SRF
						applySlewRateFilter(gridAddress, capValue);
						break;
					case FILTER_LP:
//						applyButterworthLpFilter(gridAddress, capValue);
						applyLPtAvgLpFilter(gridAddress, capValue);
						break;
				}
			} else if(isDataTransmitting) {
				switch(filterMode) {
					case FILTER_NONE:
						// Implementation with no filter
						applyNoFilter(gridAddress, capValue);
						if(gridAddress == (numDataPts-1)) {
							isDataTransmitting = false;
							// Implementation without SRF
							findTouchPoints();
						}
						break;
					case FILTER_SR:
						// Implementation with SRF
						applySlewRateFilter(gridAddress, capValue);
						if(gridAddress == (numDataPts-1)) {
							isDataTransmitting = false;
							// Implementation with SRF
							if(AppSettings.slewRateSetCounter%AppSettings.slewRateSetsPerUpdate == 0)
								findTouchPoints();
						}
						break;
					case FILTER_LP:
						// Implementation with Butterworth LP filter
//						applyButterworthLpFilter(gridAddress, capValue);
						applyLPtAvgLpFilter(gridAddress, capValue);
						if(gridAddress == (numDataPts-1)) {
							isDataTransmitting = false;
							findTouchPoints();
						}
						break;
				}
				debugText = "Parsing input capacitance...";
			}
			else {
				System.out.println("### KEY (" + gridAddress + ") NOT FOUND ###");
				debugText = "### KEY (" + gridAddress + ") NOT FOUND ###";
			}
		} else {
			System.out.println("### Either grid map or baseline does not contain this key ### : " + gridAddress);
			debugText = "### Either grid map or baseline does not contain this key ### : " + gridAddress;
		}
		updateDebugText();
	}
	
	protected static void findTouchPoints() {
		switch(touchMode) {
			case TOUCH_THRESHOLD:
				// Check if existing touch points are still on
				Object tmpTouchSet[] = touchPoints.toArray();
				int numTouchPoints = touchPoints.size();
				for(int i=0; i<numTouchPoints; i++) {
					int gridAddress = (int) tmpTouchSet[i];

					if(relCapDiff.get(gridAddress) < AppSettings.relDiffRelease) {
						touchPoints.remove(gridAddress);
					}
				}
				// Check for new touch points - Update the values of the panel holder
				for(int i=0; i<AppSettings.numColumns; i++) {
					for(int j=0; j<AppSettings.numRows; j++) {
						Integer gridAddress = Integer.parseInt(AppSettings.gridAddress[((i)*AppSettings.numColumns) + (j)],2);
						if(displayMode == CAPACITANCE_MODE) {
							// If this gridAddress is a known touch point, we can continue to next grid address
							if(touchPoints.contains(gridAddress)) {
								panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_PRESS);
//								continue;
							}
							// Otherwise, check for new touch points
							else if(relCapDiff.get(gridAddress) > AppSettings.relDiffPress) {
								panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_PRESS);
								touchPoints.add(gridAddress);
							} 
							/*else if(currentCapacitance.get(gridAddress) < baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffHover) {
								panelHolder[i][j] = new GridObject(gridAddress, currentCapacitance.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_HOVER);
							}*/ 
							else {
								panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_DEFAULT);
							}
						} else if(displayMode == GRAPH_MODE) {
//							if(currentCapacitance.get(gridAddress) > baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffPress) {
							// If this gridAddress is a known touch point, we can continue to next grid address
							if(touchPoints.contains(gridAddress)) {
								chartsArray.get(gridAddress).setBackground(AppSettings.COLOR_PRESS);
//								continue;
							}
							// Otherwise, check for new touch points
							else if(relCapDiff.get(gridAddress) > AppSettings.relDiffPress) {
								chartsArray.get(gridAddress).setBackground(AppSettings.COLOR_PRESS);
							} 
							/*else if(currentCapacitance.get(gridAddress) < baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffHover) {
								
							}*/ 
							else {
								chartsArray.get(gridAddress).setBackground(AppSettings.COLOR_DEFAULT);
							}
							panelHolder[i][j] = chartsArray.get(gridAddress);							
						}
						panelHolder[i][j].setBorder(BorderFactory.createLineBorder(Color.black, 1));
					}
				}
				// Update matrix
				if(displayMode == CAPACITANCE_MODE)
					updateMatrix();
				// Update debug text
				debugText = "Applying threshold touch recognition.";
				updateDebugText();
//				System.out.println(debugText);
				break;
			case TOUCH_1PTMAX:
				// Find the coordinates of the max absolute difference - only suitable for one touch point
				Float maxAbsDiff = 0f;
				Integer gridAddress = 0;
				for(int i = 0; i < absCapDiff.size(); i++) {
					if(absCapDiff.get(Integer.parseInt(AppSettings.gridAddress[i],2)) > maxAbsDiff) {
						gridAddress = Integer.parseInt(AppSettings.gridAddress[i],2);
						maxAbsDiff = absCapDiff.get(gridAddress);
					}
				}
				int[] absDiffCoords = gridAddressMap.get(gridAddress);
				debugText = "ABS (" + absDiffCoords[0] + "," + absDiffCoords[1] + ")" + maxAbsDiff + "    \n";
				// Find the coordinates of the max relative difference
				Float maxRelDiff = 0f;
				for(int i = 0; i < relCapDiff.size(); i++) {
					if(relCapDiff.get(Integer.parseInt(AppSettings.gridAddress[i],2)) > maxRelDiff) {
						gridAddress = Integer.parseInt(AppSettings.gridAddress[i],2);
						maxRelDiff = relCapDiff.get(gridAddress);
					}
				}
				int[] relDiffCoords = gridAddressMap.get(gridAddress);
				debugText += "REL (" + relDiffCoords[0] + "," + relDiffCoords[1] + ")" + maxRelDiff;
				// Find average coordinates between the max relative and absolute coordinates
				int[] maxCoords = { Math.round((absDiffCoords[0] + relDiffCoords[0])/2), Math.round((absDiffCoords[1] + relDiffCoords[1])/2)};
				// Update matrix 
				debugText = "(" + maxCoords[0] + "," + maxCoords[1] + ") - AbsChange: " + maxAbsDiff + "; RelChange: " + maxRelDiff;
				/*if(displayMode == CAPACITANCE_MODE) {
					updateMatrix(maxCoords, maxRelDiff);
//					updateMatrix();
				}*/
				// Update the values of the panel holder
				for(int i=0; i<AppSettings.numColumns; i++) {
					for(int j=0; j<AppSettings.numRows; j++) {
						gridAddress = Integer.parseInt(AppSettings.gridAddress[((i)*AppSettings.numColumns) + (j)],2);
						if(displayMode == CAPACITANCE_MODE) {
							// Implementation for viewing capacitance readings
							if(i == maxCoords[0] && j == maxCoords[1]) {
//								if(currentCapacitance.get(gridAddress) > baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffPress) {
								if(relCapDiff.get(gridAddress) > AppSettings.relDiffPress) {
									panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_PRESS);
								} 
								/*else if(currentCapacitance.get(gridAddress) < baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffHover) {
									panelHolder[i][j] = new GridObject(gridAddress, currentCapacitance.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_HOVER);
								}*/ 
								else {
									panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_DEFAULT);
								}
							} else {
								panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_DEFAULT);
							}
						} else if(displayMode == GRAPH_MODE) {
							// Implementation for viewing capacitance graphs
							if(i == maxCoords[0] && j == maxCoords[1]) {
//								if(currentCapacitance.get(gridAddress) > baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffPress) {
								if(relCapDiff.get(gridAddress) > AppSettings.relDiffPress) {
									chartsArray.get(gridAddress).setBackground(AppSettings.COLOR_PRESS);
								} 
								/*else if(currentCapacitance.get(gridAddress) < baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffHover) {
									
								}*/ 
								else {
									chartsArray.get(gridAddress).setBackground(AppSettings.COLOR_DEFAULT);
								}
							} else {
								chartsArray.get(gridAddress).setBackground(AppSettings.COLOR_DEFAULT);
							}
							panelHolder[i][j] = chartsArray.get(gridAddress);
							
						}
						panelHolder[i][j].setBorder(BorderFactory.createLineBorder(Color.black, 1));
					}
				}
				// Update matrix
				if(displayMode == CAPACITANCE_MODE)
					updateMatrix();
				// Update debug text
				debugText = "Applying 1ptMax touch recognition.";
				updateDebugText();
//				System.out.println(debugText);
				break;
			case TOUCH_RELMAX:
				break;
		}
	}
	
	/**
	 * Functions relating to data filtering
	 */
	private static void baselineSlewRateFilter(Integer gridAddress, Float capValue) {
		// If no value is currently stored for this grid address, add this value to the current reading
		if(!baselineThresholdMap.containsKey(gridAddress))
			baselineThresholdMap.put(gridAddress, capValue);
		if(capValue > baselineThresholdMap.get(gridAddress)) {
			baselineThresholdMap.put(gridAddress, baselineThresholdMap.get(gridAddress) + AppSettings.slewRateIncrement);
		} else if(capValue < baselineThresholdMap.get(gridAddress)) {
			baselineThresholdMap.put(gridAddress, baselineThresholdMap.get(gridAddress) - AppSettings.slewRateIncrement);
		}
	}
	
	private static void applyNoFilter(Integer gridAddress, Float capValue) {
		//Old implementation with unprocessed updates to grid data
		/*if(gridAddressMap.containsKey(gridAddress)) {
			int[] coordinates = gridAddressMap.get(gridAddress);
			currentCapacitance.put(gridAddress, capValue);
			dataTrace.get(gridAddress).addPoint(((double) System.currentTimeMillis() - startTime), capValue);
//			panelHolder[coordinates[0]][coordinates[1]] = new GridObject(gridAddress,capValue + " pF");
//			f.getContentPane().add(panelHolder[coordinates[0]][coordinates[1]]);
			updateMatrix();
		}else {
				System.out.println("### KEY (" + gridAddress + ") NOT FOUND ###");
		} */
		// Update debug text
		debugText = "Applying no filter.";
		updateDebugText();
//		System.out.println(debugText);
		processedCap.put(gridAddress, capValue);
		absCapDiff.put(gridAddress, Math.abs(CapMatrix.baselineThresholdMap.get(gridAddress) - capValue));
//		relCapDiff.put(gridAddress, Math.abs((CapMatrix.baselineThresholdMap.get(gridAddress) - capValue))/CapMatrix.baselineThresholdMap.get(gridAddress));
		relCapDiff.put(gridAddress, (capValue - CapMatrix.baselineThresholdMap.get(gridAddress))/CapMatrix.baselineThresholdMap.get(gridAddress));
		// Add data point to graph
//		dataTrace.get(gridAddress).addPoint(((double) System.currentTimeMillis() - startTime), capValue);
		dataTrace.get(gridAddress).addPoint(System.currentTimeMillis(), capValue);
	}
	
	private static void applySlewRateFilter(Integer gridAddress, Float capValue) {
		// Update debug text
		debugText = "Applying slew rate filter.";
		updateDebugText();
//		System.out.println(debugText);
		// If no value is currently stored for this grid address, add this value to the current reading
		if(!processedCap.containsKey(gridAddress))
			processedCap.put(gridAddress, capValue);							
		// Apply SRF
		if(capValue > processedCap.get(gridAddress)) {
			processedCap.put(gridAddress, processedCap.get(gridAddress) + AppSettings.slewRateIncrement);
		} else if(capValue < processedCap.get(gridAddress)) {
			processedCap.put(gridAddress, processedCap.get(gridAddress) - AppSettings.slewRateIncrement);
		}
		AppSettings.slewRateSampleCounter++;
		if(AppSettings.slewRateSampleCounter%numDataPts == 0) {
			AppSettings.slewRateSetCounter++;
//			if(slewRateSetCounter%slewRateDataSets == 0) {
//				slewRateSetCounter = 0;
//			}
		}
		// If the number of data sets sampled equals slewRateDataSets, calculate the abs/rel difference
		if(AppSettings.slewRateSetCounter%AppSettings.slewRateSetsPerUpdate == 0) {
			absCapDiff.put(gridAddress, Math.abs(CapMatrix.baselineThresholdMap.get(gridAddress) - capValue));
//			relCapDiff.put(gridAddress, Math.abs((CapMatrix.baselineThresholdMap.get(gridAddress) - capValue))/CapMatrix.baselineThresholdMap.get(gridAddress));
			relCapDiff.put(gridAddress, (capValue - CapMatrix.baselineThresholdMap.get(gridAddress))/CapMatrix.baselineThresholdMap.get(gridAddress));
		}
		// Add data point to graph
//		dataTrace.get(gridAddress).addPoint(((double) System.currentTimeMillis() - startTime), capValue);
		dataTrace.get(gridAddress).addPoint(System.currentTimeMillis(), capValue);
		
	}
	
	/**
	 * Apply a Butterworth Low Pass filter using the following formula:
	 * 		y[n] = x[n] + x[n-1] + Ay[n-1]
	 * where:
	 * 		y[n] = output at time 'n'; the processed reading
	 * 		x[n] = input at time 'n'; x[n-1] = the raw reading value that is saved, x[n] = the capValue that is sent to this function
	 * 		A = filter coefficient (0<=A<1); common A values: 0.8125, 0.8750, 0.9375
	 * 	
	 */
	private static void applyButterworthLpFilter(Integer gridAddress, Float capValue) {
		// Update debug text
		debugText = "Applying Butterworth LP filter.";
		updateDebugText();
		
		// If no value is currently stored for this grid address, add this value to the raw
		float newOutput = capValue;
		if(!rawCap.containsKey(gridAddress)) {
			rawCap.put(gridAddress, capValue);
			processedCap.put(gridAddress, capValue);
//			return;
		} else if(!processedCap.containsKey(gridAddress)) {
			processedCap.put(gridAddress, capValue);
//			return;
		}			
		// Apply Butterworth LP filter
		else {
			float coefficientA = 0.8125f;
			newOutput = capValue + rawCap.get(gridAddress) + (coefficientA * processedCap.get(gridAddress));			
			processedCap.put(gridAddress, newOutput);
			System.out.println("Past value: " + rawCap.get(gridAddress) + "\nCoefficient A: " + coefficientA 
					+ "\ny[n-1]: " + processedCap.get(gridAddress) + "\nAy[n-1]: " + (coefficientA * processedCap.get(gridAddress)));
		}
		System.out.println("New processed output:" + newOutput + "\n");
		// Add data point to graph
//		dataTrace.get(gridAddress).addPoint(((double) System.currentTimeMillis() - startTime), newOutput);
		dataTrace.get(gridAddress).addPoint(System.currentTimeMillis(), capValue);
		
	}
	
	/**
	 * Apply an IIR L-point Average Low Pass filter using the following formula:
	 * 		y[n] = y[n-1] + (x[n]-y[n-1])/L
	 * where:
	 * 		y[n] = output at time 'n'; the processed reading
	 * 		x[n] = input at time 'n'; x[n] = the capValue that is sent to this function
	 * 		L = memory size of filter
	 * 	
	 */
	private static final int L_filter = 8;
	private static void applyLPtAvgLpFilter(Integer gridAddress, Float capValue) {
		// Update debug text
		debugText = "Applying IIR L-point average LP filter.";
		System.out.println("Applying IIR L-point average low pass filter");
		updateDebugText();
		
		// If no value is currently stored for this grid address, add this value to the raw
		float newOutput = capValue;
		if(!processedCap.containsKey(gridAddress)) {
			processedCap.put(gridAddress, capValue);
		}			
		// Apply L-point LP filter
		else {
			float LPtChange = (capValue - processedCap.get(gridAddress))/L_filter;
			newOutput = processedCap.get(gridAddress) + LPtChange;			
			processedCap.put(gridAddress, newOutput);
			System.out.println("Sum of " + processedCap.get(gridAddress) + " and " + LPtChange);
		}
		System.out.println("New processed output:" + newOutput + "\n");
		absCapDiff.put(gridAddress, Math.abs(CapMatrix.baselineThresholdMap.get(gridAddress) - newOutput));
		relCapDiff.put(gridAddress, (newOutput - CapMatrix.baselineThresholdMap.get(gridAddress))/CapMatrix.baselineThresholdMap.get(gridAddress));
		// Add data point to graph
//		dataTrace.get(gridAddress).addPoint(((double) System.currentTimeMillis() - startTime), newOutput);
		dataTrace.get(gridAddress).addPoint(System.currentTimeMillis(), capValue);
		
	}
	
	
	/**
	 * Functions relating to serial port management
	 */
	private static StringBuilder arduinoMessage = new StringBuilder();
	protected static void portInitialization()
			throws SerialPortException {
		debugText = DEBUG_WAIT_CALIBRATE;
		serialPort = new SerialPort(AppSettings.serialPortNumber);
		serialPort.openPort();
		serialPort.setParams(9600, 8, 1, SerialPort.PARITY_NONE);
		serialPort.addEventListener(new SerialPortEventListener() {
			@Override
			public void serialEvent(SerialPortEvent serialPortEvent) {
				if(serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() > 0) {
					try {
						byte buffer[] = serialPort.readBytes();
						for(byte b:buffer) {
							if((b == '\r' || b == '\n') && arduinoMessage.length() > 0) {
								System.out.println(arduinoMessage.toString().trim());
								parseCommand(arduinoMessage.toString().trim());
								arduinoMessage = new StringBuilder();
							} else {
								arduinoMessage.append((char) b);
							}
						}
					} catch(SerialPortException e) {
						System.out.println("Read Error: " + e);
					} catch (IOException e) {
						System.out.println("IO Error: " + e);
					}
				}				
			}			
		});
	}
	
	protected static void initializeSerialConnection()
			throws IOException {
		try {
			portInitialization();

			writer = new FileWriter(AppSettings.csvFile);
			SettingsPanel.isConnected = true;
			SettingsPanel.comPortButton.setText(SettingsPanel.serialDisconnectString);
			SettingsPanel.enablePanel();
			startTime = System.currentTimeMillis();
		} catch(SerialPortException e) {
			System.out.println(e);
			debugText = DEBUG_COM_UNAVAILABLE;
			updateDebugText();
			return;
		}
	}
	
	protected static void endSerialConnection()
			throws IOException {
		try {
			if(serialPort != null) {
				serialPort.closePort();
				SettingsPanel.isConnected = false;
				SettingsPanel.comPortButton.setText(SettingsPanel.serialInitializeString);
				SettingsPanel.disablePanel();
			}
			debugText = "Disconnected from serial port.";
			updateDebugText();

			writer.flush();//
			writer.close();

		} catch(SerialPortException e) {
			System.out.println(e);
			f = new JFrame();
			f.setTitle(frameTitle);
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.add(new JLabel("Unable to close connection to Arduino...\nThe serial may be incorrect, not connected, or busy."));
			f.pack();
			f.setResizable(false);
			f.setVisible(true);
			debugText = DEBUG_COM_UNAVAILABLE;
			addComponentsToFrame();
			f.pack();
			f.setResizable(false);
			f.setVisible(true);
			return;
		}
	}
}