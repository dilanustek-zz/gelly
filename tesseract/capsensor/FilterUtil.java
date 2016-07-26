package capsensor;


/**
 * Created by dilanustek on 7/26/16.
 */
public class FilterUtil {

    // Filtering parameters
    protected static int numDataPts = AppSettings.numColumns * AppSettings.numRows;
    protected static final int FILTER_NONE = 0;
    protected static final int FILTER_SR = 1;
    protected static final int FILTER_LP = 2;
    protected static int filterMode = FILTER_NONE;

    /**
     * Functions relating to data filtering
     */
    protected static void baselineSlewRateFilter(Integer gridAddress, Float capValue) {
        // If no value is currently stored for this grid address, add this value to the current reading
        if(!DataController.baselineThresholdMap.containsKey(gridAddress))
            DataController.baselineThresholdMap.put(gridAddress, capValue);
        if(capValue > DataController.baselineThresholdMap.get(gridAddress)) {
            DataController.baselineThresholdMap.put(gridAddress, DataController.baselineThresholdMap.get(gridAddress) + AppSettings.slewRateIncrement);
        } else if(capValue < DataController.baselineThresholdMap.get(gridAddress)) {
            DataController.baselineThresholdMap.put(gridAddress, DataController.baselineThresholdMap.get(gridAddress) - AppSettings.slewRateIncrement);
        }
    }

    protected static void applyNoFilter(Integer gridAddress, Float capValue) {
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
        CapMatrix.debugText = "Applying no filter.";
        FrameUtil.updateDebugText();
//		System.out.println(debugText);
        DataController.processedCap.put(gridAddress, capValue);
        DataController.absCapDiff.put(gridAddress, Math.abs(DataController.baselineThresholdMap.get(gridAddress) - capValue));
//		relCapDiff.put(gridAddress, Math.abs((CapMatrix.baselineThresholdMap.get(gridAddress) - capValue))/CapMatrix.baselineThresholdMap.get(gridAddress));
        DataController.relCapDiff.put(gridAddress, (capValue - DataController.baselineThresholdMap.get(gridAddress))/DataController.baselineThresholdMap.get(gridAddress));
        // Add data point to graph
//		dataTrace.get(gridAddress).addPoint(((double) System.currentTimeMillis() - startTime), capValue);
        DataController.dataTrace.get(gridAddress).addPoint(System.currentTimeMillis(), capValue);
    }

    protected static void applySlewRateFilter(Integer gridAddress, Float capValue) {
        // Update debug text
        CapMatrix.debugText = "Applying slew rate filter.";
        FrameUtil.updateDebugText();
//		System.out.println(debugText);
        // If no value is currently stored for this grid address, add this value to the current reading
        if(!DataController.processedCap.containsKey(gridAddress))
            DataController.processedCap.put(gridAddress, capValue);
        // Apply SRF
        if(capValue > DataController.processedCap.get(gridAddress)) {
            DataController.processedCap.put(gridAddress, DataController.processedCap.get(gridAddress) + AppSettings.slewRateIncrement);
        } else if(capValue < DataController.processedCap.get(gridAddress)) {
            DataController.processedCap.put(gridAddress, DataController.processedCap.get(gridAddress) - AppSettings.slewRateIncrement);
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
            DataController.absCapDiff.put(gridAddress,
                    Math.abs(DataController.baselineThresholdMap.get(gridAddress) - capValue));
//			relCapDiff.put(gridAddress, Math.abs((CapMatrix.baselineThresholdMap.get(gridAddress) -
// capValue))/CapMatrix.baselineThresholdMap.get(gridAddress));
            DataController.relCapDiff.put(gridAddress, (capValue - DataController.baselineThresholdMap.get(gridAddress))
                    /DataController.baselineThresholdMap.get(gridAddress));
        }
        // Add data point to graph
//		dataTrace.get(gridAddress).addPoint(((double) System.currentTimeMillis() - startTime), capValue);
        DataController.dataTrace.get(gridAddress).addPoint(System.currentTimeMillis(), capValue);

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
    protected static void applyButterworthLpFilter(Integer gridAddress, Float capValue) {
        // Update debug text
        CapMatrix.debugText = "Applying Butterworth LP filter.";
        FrameUtil.updateDebugText();

        // If no value is currently stored for this grid address, add this value to the raw
        float newOutput = capValue;
        if(!DataController.rawCap.containsKey(gridAddress)) {
            DataController.rawCap.put(gridAddress, capValue);
            DataController.processedCap.put(gridAddress, capValue);
//			return;
        } else if(!DataController.processedCap.containsKey(gridAddress)) {
            DataController.processedCap.put(gridAddress, capValue);
//			return;
        }
        // Apply Butterworth LP filter
        else {
            float coefficientA = 0.8125f;
            newOutput = capValue + DataController.rawCap.get(gridAddress) + (coefficientA * DataController.processedCap.get(gridAddress));
            DataController.processedCap.put(gridAddress, newOutput);
            System.out.println("Past value: " + DataController.rawCap.get(gridAddress) + "\nCoefficient A: " + coefficientA
                    + "\ny[n-1]: " + DataController.processedCap.get(gridAddress) + "\nAy[n-1]: " + (coefficientA * DataController.processedCap.get(gridAddress)));
        }
        System.out.println("New processed output:" + newOutput + "\n");
        // Add data point to graph
//		dataTrace.get(gridAddress).addPoint(((double) System.currentTimeMillis() - startTime), newOutput);
        DataController.dataTrace.get(gridAddress).addPoint(System.currentTimeMillis(), capValue);

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
    protected static void applyLPtAvgLpFilter(Integer gridAddress, Float capValue) {
        // Update debug text
        CapMatrix.debugText = "Applying IIR L-point average LP filter.";
        System.out.println("Applying IIR L-point average low pass filter");
        FrameUtil.updateDebugText();

        // If no value is currently stored for this grid address, add this value to the raw
        float newOutput = capValue;
        if(!DataController.processedCap.containsKey(gridAddress)) {
            DataController.processedCap.put(gridAddress, capValue);
        }
        // Apply L-point LP filter
        else {
            float LPtChange = (capValue - DataController.processedCap.get(gridAddress))/L_filter;
            newOutput = DataController.processedCap.get(gridAddress) + LPtChange;
            DataController.processedCap.put(gridAddress, newOutput);
            System.out.println("Sum of " + DataController.processedCap.get(gridAddress) + " and " + LPtChange);
        }
        System.out.println("New processed output:" + newOutput + "\n");
        DataController.absCapDiff.put(gridAddress,
                Math.abs(DataController.baselineThresholdMap.get(gridAddress) - newOutput));
        DataController.relCapDiff.put(gridAddress, (newOutput - DataController.baselineThresholdMap.get(gridAddress))
                /DataController.baselineThresholdMap.get(gridAddress));
        // Add data point to graph
//		dataTrace.get(gridAddress).addPoint(((double) System.currentTimeMillis() - startTime), newOutput);
        DataController.dataTrace.get(gridAddress).addPoint(System.currentTimeMillis(), capValue);

    }

}
