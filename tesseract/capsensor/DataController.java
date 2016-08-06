package capsensor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.ITrace2D;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dilanustek on 7/26/16.
 */
public class DataController {

    // Cell states
    protected static HashMap<Integer, Integer> cellStateMap = new HashMap<Integer, Integer>();
    //	private static HashMap<Integer, Integer> touchPoints = new HashMap<Integer, Integer>();
    protected static HashSet<Integer> touchPoints = new HashSet<Integer>();
    protected static final int STATE_IDLE = 0;
    protected static final int STATE_HOVER = 1;
    protected static final int STATE_PRESS = 2;
    // Matrix variables
    protected static HashMap<Integer,int[]> gridAddressMap = new HashMap<Integer, int[]>();

    // Capacitance thresholds
    protected static HashMap<Integer, Float> baselineThresholdMap = new HashMap<Integer, Float>();

    // Saved traces of capacitance values for graphing
    protected static ArrayList<ITrace2D> dataTrace = new ArrayList<ITrace2D>();
    //	private static ArrayList<ChartObject> chartsArray = new ArrayList<ChartObject>();
    protected static long startTime = System.currentTimeMillis();	// Used to help set the x-axis for graphing
    protected static ArrayList<Chart2D> chartsArray = new ArrayList<Chart2D>();

    // Data transmission variables
    protected static Boolean isDataTransmitting = false;
    protected static HashMap<Integer, Float> processedCap = new HashMap<Integer, Float>();
    protected static HashMap<Integer, Float> rawCap = new HashMap<Integer, Float>();
    protected static HashMap<Integer, Float> relCapDiff = new HashMap<Integer, Float>();
    protected static HashMap<Integer, Float> absCapDiff = new HashMap<Integer, Float>();

    // Regex patterns for parsing data from Arduino
    private static final String patternDataTransmission = "^\\((\\d+)\\)\\s*(\\d*\\.*\\d+)";
    private static final String patternDataBaseline = "^BASELINE\\s*\\((\\d+)\\)\\s*(\\d*\\.*\\d+)";

    // Baseline parameters
    private static int baselineCount = 0;

    // Touch flag to make sure baseline is not obtained during touch
    protected static boolean touchFlag = false;

    // Keep 4 cap values for each cell to average later
    private static Multimap<Integer, Float> fourCaps = ArrayListMultimap.create();

    /**
     * Functions for handling the commands received through serial
     */
    protected static void parseCommand(String command)
            throws IOException {

        Pattern pattern = Pattern.compile(patternDataTransmission);
        Matcher matcher = pattern.matcher(command);

        if (matcher.find()) {
            if (matcher.groupCount() == 2) {
                Integer gridAddress = Integer.parseInt(matcher.group(1),2);
                Float capValue = Float.parseFloat(matcher.group(2));
                if (gridAddress == 0)
                    System.out.println(gridAddress + "  " + capValue);

                   // if (relCapDiff != null && relCapDiff.size()==AppSettings.numDataPts) {
                    //    findTouchPoints();
                   // }
                // Check if command is for setting data baseline
                if (baselineThresholdMap.size() == AppSettings.numDataPts){
                    addCapacitancePoint(gridAddress, capValue);

                    // still a baseline point every 4 collections if there is no touch TODO + hover
                    if ((baselineCount >= 64) && touchPoints.isEmpty()){
                        addAveragedBaselinePoints();//  empty fourCaps
                    }

                } else if (baselineThresholdMap.size() < AppSettings.numDataPts){
                    addBaselinePoint(gridAddress, capValue);
                } else {
                    System.out.println("parse command error");
                    // TODO error
                }

            }
        }


    }


    private static void addAveragedBaselinePoints()
            throws IOException {
        float sum = 0;

        for (Object key: fourCaps.keys()){
            Collection<Float> c = fourCaps.get((Integer) key);

            for (Float cap : c){
                sum += cap;
            }
            addBaselinePoint((Integer)key, sum/c.size());
            sum = 0;
        }

        // reset to restart the count to baseline
        fourCaps.clear();
        baselineCount = 0;

        System.out.println("baseline map is = " + baselineThresholdMap.toString());

    }


    private static void addBaselinePoint(Integer gridAddress, Float capValue)
            throws IOException {
        // Raw Implementation
//		CapMatrix.baselineThresholdMap.put(gridAddress, capValue);

        // Implementation with max search algorithm
        if(gridAddress == 0) {
      //  if(baselineThresholdMap.size() == 0) {
            isDataTransmitting = true;
            switch(FilterUtil.filterMode) {
                case FilterUtil.FILTER_NONE:
                    // Implementation with no filter
                    baselineThresholdMap.put(gridAddress, capValue);
                    break;
                case FilterUtil.FILTER_SR:
                    // Implementation with SRF
                    FilterUtil.baselineSlewRateFilter(gridAddress, capValue);
                    break;
                case FilterUtil.FILTER_LP:
                    break; // TODO what happened here?
            }
            CSVUtil.writeCsv(gridAddress, capValue);

        }
        // Implementation with max search algorithm
        else if(isDataTransmitting) {
            switch(FilterUtil.filterMode) {
                case FilterUtil.FILTER_NONE:
                    // Implementation with no filter
                    baselineThresholdMap.put(gridAddress, capValue);
                    break;
                case FilterUtil.FILTER_SR:
                    // Implementation with SRF
                    FilterUtil.baselineSlewRateFilter(gridAddress, capValue);
                    break;
                case FilterUtil.FILTER_LP:
                    break;
            }
            if(gridAddress == (AppSettings.numDataPts-1)) {
                isDataTransmitting = false;
            }
            //CSVUtil.writeCsv(gridAddress, capValue); TODO: do we need to write it? -- or show it?
        }
//        else {
//            System.out.println("### KEY (" + gridAddress + ") NOT FOUND ###");
//        }
        CapMatrix.debugText = "Updating baseline values...";
        FrameUtil.updateDebugText();
    }

    private static void addCapacitancePoint(Integer gridAddress, Float capValue)
            throws IOException {


        if(gridAddressMap.containsKey(gridAddress) && baselineThresholdMap.containsKey(gridAddress)) {
            if(gridAddress == 0) {
                isDataTransmitting = true;
                switch(FilterUtil.filterMode) {
                    case FilterUtil.FILTER_NONE:
                        // Implementation with no filter
                        FilterUtil.applyNoFilter(gridAddress, capValue);
                        break;
                    case FilterUtil.FILTER_SR:
                        // Implementation with SRF
                        FilterUtil.applySlewRateFilter(gridAddress, capValue);
                        break;
                    case FilterUtil.FILTER_LP:
//						applyButterworthLpFilter(gridAddress, capValue);
                        FilterUtil.applyLPtAvgLpFilter(gridAddress, capValue);
                        break;
                }
                //Add to the fourCaps to keep for averaging later
                fourCaps.put(gridAddress, capValue);
                baselineCount++;

                CSVUtil.writeCsv(gridAddress, capValue);

            } else if(isDataTransmitting) {
                switch(FilterUtil.filterMode) {
                    case FilterUtil.FILTER_NONE:
                        // Implementation with no filter
                        FilterUtil.applyNoFilter(gridAddress, capValue);
                        if(gridAddress == (AppSettings.numDataPts-1)) {
                            isDataTransmitting = false;
                            // Implementation without SRF
                            findTouchPoints();
                        }
                        break;
                    case FilterUtil.FILTER_SR:
                        // Implementation with SRF
                        FilterUtil.applySlewRateFilter(gridAddress, capValue);
                        if(gridAddress == (AppSettings.numDataPts-1)) {
                            isDataTransmitting = false;
                            // Implementation with SRF
                            if(AppSettings.slewRateSetCounter%AppSettings.slewRateSetsPerUpdate == 0)
                                findTouchPoints();
                        }
                        break;
                    case FilterUtil.FILTER_LP:
                        // Implementation with Butterworth LP filter
//						applyButterworthLpFilter(gridAddress, capValue);
                        FilterUtil.applyLPtAvgLpFilter(gridAddress, capValue);
                        if(gridAddress == (AppSettings.numDataPts-1)) {
                            isDataTransmitting = false;
                            findTouchPoints();
                        }
                        break;
                }
                CapMatrix.debugText = "Parsing input capacitance...";

                //Add to the fourCaps to keep for averaging later
                fourCaps.put(gridAddress, capValue);
                baselineCount++;

                CSVUtil.writeCsv(gridAddress, capValue);
            }
            else {
                System.out.println("### KEY (" + gridAddress + ") NOT FOUND ###");
                CapMatrix.debugText = "### KEY (" + gridAddress + ") NOT FOUND ###";
            }
        } else {
            System.out.println("### Either grid map or baseline does not contain this key ### : " + gridAddress);
            CapMatrix.debugText = "### Either grid map or baseline does not contain this key ### : " + gridAddress;
        }
        FrameUtil.updateDebugText();

    }

    protected static void findTouchPoints() {
        switch(InterfaceFunct.touchMode) {
            case InterfaceFunct.TOUCH_THRESHOLD:
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

                        if(InterfaceFunct.displayMode == InterfaceFunct.CAPACITANCE_MODE) {
                            // If this gridAddress is a known touch point, we can continue to next grid address
                            if(touchPoints.contains(gridAddress)) {
                                FrameUtil.panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_PRESS);
//								continue;
                            }
                            // Otherwise, check for new touch points
                            else if(relCapDiff.get(gridAddress) < AppSettings.relDiffPress) {
                                FrameUtil.panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_PRESS);
                                touchPoints.add(gridAddress);
                            }
							/*else if(currentCapacitance.get(gridAddress) < baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffHover) {
								panelHolder[i][j] = new GridObject(gridAddress, currentCapacitance.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_HOVER);
							}*/
                            else {
                                FrameUtil.panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_DEFAULT);
                            }

                        } else if(InterfaceFunct.displayMode == InterfaceFunct.GRAPH_MODE) {
//							if(currentCapacitance.get(gridAddress) > baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffPress) {
                            // If this gridAddress is a known touch point, we can continue to next grid address

                            //if(gridAddress==0)
                                //System.out.println("rel cap diff= " + relCapDiff.get(gridAddress) + " -- press needed " + AppSettings.relDiffPress );

                            if(touchPoints.contains(gridAddress)) {
                                chartsArray.get(gridAddress).setBackground(AppSettings.COLOR_PRESS);
//								continue;
                                System.out.println("already a touch point exists at " + gridAddress);
                            }
                            // Otherwise, check for new touch points
                            else if(relCapDiff.get(gridAddress) < AppSettings.relDiffPress) {
                                chartsArray.get(gridAddress).setBackground(AppSettings.COLOR_PRESS);
                                System.out.println("NEW touch point at " + gridAddress);

                            }
							/*else if(currentCapacitance.get(gridAddress) < baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffHover) {

							}*/
                            else {
                                chartsArray.get(gridAddress).setBackground(AppSettings.COLOR_DEFAULT);
                            }
                            FrameUtil.panelHolder[i][j] = chartsArray.get(gridAddress);
                        }
                        FrameUtil.panelHolder[i][j].setBorder(BorderFactory.createLineBorder(Color.black, 1));
                    }
                }
                // Update matrix
                //if(InterfaceFunct.displayMode == InterfaceFunct.CAPACITANCE_MODE) {
                    FrameUtil.updateMatrix();
               // }
                // Update debug text
                CapMatrix.debugText = "Applying threshold touch recognition.";
                FrameUtil.updateDebugText();
//				System.out.println(debugText);
                break;
            case InterfaceFunct.TOUCH_1PTMAX:
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
                CapMatrix.debugText = "ABS (" + absDiffCoords[0] + "," + absDiffCoords[1] + ")" + maxAbsDiff + "    \n";
                // Find the coordinates of the max relative difference
                Float maxRelDiff = 0f;
                for(int i = 0; i < relCapDiff.size(); i++) {
                    if(relCapDiff.get(Integer.parseInt(AppSettings.gridAddress[i],2)) > maxRelDiff) {
                        gridAddress = Integer.parseInt(AppSettings.gridAddress[i],2);
                        maxRelDiff = relCapDiff.get(gridAddress);
                    }
                }
                int[] relDiffCoords = gridAddressMap.get(gridAddress);
                CapMatrix.debugText += "REL (" + relDiffCoords[0] + "," + relDiffCoords[1] + ")" + maxRelDiff;
                // Find average coordinates between the max relative and absolute coordinates
                int[] maxCoords = { Math.round((absDiffCoords[0] + relDiffCoords[0])/2), Math.round((absDiffCoords[1] + relDiffCoords[1])/2)};
                // Update matrix
                CapMatrix.debugText = "(" + maxCoords[0] + "," + maxCoords[1] + ") - AbsChange: " + maxAbsDiff + "; RelChange: " + maxRelDiff;
				/*if(displayMode == CAPACITANCE_MODE) {
					updateMatrix(maxCoords, maxRelDiff);
//					updateMatrix();
				}*/
                // Update the values of the panel holder
                for(int i=0; i<AppSettings.numColumns; i++) {
                    for(int j=0; j<AppSettings.numRows; j++) {
                        gridAddress = Integer.parseInt(AppSettings.gridAddress[((i)*AppSettings.numColumns) + (j)],2);
                        if(InterfaceFunct.displayMode == InterfaceFunct.CAPACITANCE_MODE) {
                            // Implementation for viewing capacitance readings
                            if(i == maxCoords[0] && j == maxCoords[1]) {
//								if(currentCapacitance.get(gridAddress) > baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffPress) {
                                if(relCapDiff.get(gridAddress) > AppSettings.relDiffPress) {
                                    FrameUtil.panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_PRESS);
                                }
								/*else if(currentCapacitance.get(gridAddress) < baselineThresholdMap.get(gridAddress) && relCapDiff.get(gridAddress) > relDiffHover) {
									panelHolder[i][j] = new GridObject(gridAddress, currentCapacitance.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_HOVER);
								}*/
                                else {
                                    FrameUtil.panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_DEFAULT);
                                }
                            } else {
                                FrameUtil.panelHolder[i][j] = new GridObject(gridAddress, processedCap.get(gridAddress) + " pF", relCapDiff.get(gridAddress)*100f + "%", AppSettings.COLOR_DEFAULT);
                            }
                        } else if(InterfaceFunct.displayMode == InterfaceFunct.GRAPH_MODE) {
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
                            FrameUtil.panelHolder[i][j] = chartsArray.get(gridAddress);

                        }
                        FrameUtil.panelHolder[i][j].setBorder(BorderFactory.createLineBorder(Color.black, 1));
                    }
                }
                // Update matrix
                if(InterfaceFunct.displayMode == InterfaceFunct.CAPACITANCE_MODE)
                    FrameUtil.updateMatrix();
                // Update debug text
                CapMatrix.debugText = "Applying 1ptMax touch recognition.";
                FrameUtil.updateDebugText();
//				System.out.println(debugText);
                break;
            case InterfaceFunct.TOUCH_RELMAX:
                break;
        }
    }
}
