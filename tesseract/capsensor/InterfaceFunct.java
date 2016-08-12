package capsensor;


import javax.swing.*;
import javax.xml.crypto.Data;
import java.awt.*;

import static java.lang.Math.abs;

/**
 * Created by dilanustek on 7/25/16.
 */


public class InterfaceFunct {

    // Touch recognition variables
    protected static final int TOUCH_THRESHOLD = 0;
    protected static final int TOUCH_1PTMAX = 1;
    protected static final int TOUCH_RELMAX = 2;
    protected static int touchMode = TOUCH_THRESHOLD;

    protected static int displayMode = 0;
    protected static final int CAPACITANCE_MODE = 0;
    protected static final int GRAPH_MODE = 1;

    /**
     * Functions for interacting with the GUI elements
     */
    protected static void changeDisplayMode(int newMode) {
        displayMode = newMode;
        if(displayMode == CAPACITANCE_MODE) {
		//	DataController.findTouchPoints();
            FrameUtil.updateMatrix();
        } else if(displayMode == GRAPH_MODE) {
            // Update the values of the panel holder
            for(int i=0; i<AppSettings.numColumns; i++) {
                for(int j=0; j<AppSettings.numRows; j++) {
                    int gridAddress = Integer.parseInt(AppSettings.gridAddress[((i)*AppSettings.numColumns) + (j)],2);
                    if(displayMode == GRAPH_MODE) {
                        FrameUtil.panelHolder[i][j] = DataController.chartsArray.get(gridAddress);
                    }
                    FrameUtil.panelHolder[i][j].setBorder(BorderFactory.createLineBorder(Color.black, 1));
                }
            }
            // Update matrix
            FrameUtil.updateMatrix();
        }
    }

    protected static void changeFilterMode(int newMode) {
        FilterUtil.filterMode = newMode;
        if(newMode == FilterUtil.FILTER_SR) {
            AppSettings.slewRateSampleCounter = 0;
            AppSettings.slewRateSetCounter = 0;
        } else if(newMode == FilterUtil.FILTER_LP) {
            DataController.rawCap.clear();
        }
        if(DataController.isDataTransmitting) {
            DataController.isDataTransmitting = false;
        }
        System.out.println("change filter mode");
    }

    protected static void changeTouchMode(int newMode) {
        touchMode = newMode;
        DataController.touchPoints.clear();
        if(DataController.isDataTransmitting) {
            DataController.isDataTransmitting = false;
        }
        System.out.println("change touch mode");

    }

    /**
     * Function for calculating color
     */

    protected static Color calculateColor(int gridAddress){

        if (DataController.relCapDiff.get(gridAddress) < AppSettings.relDiffPress){
            //there is a touch
            DataController.touchPoints.add(gridAddress);
            return AppSettings.COLOR_PRESS;
        } else if (DataController.relCapDiff.get(gridAddress) > 0) {
            // increase of capacitance: no touch
            return AppSettings.COLOR_DEFAULT;
        } else {
            //interpolate
            int relDiff = (int) Math.ceil(AppSettings.relDiffPress * 100);
            int capDiff = (int) Math.ceil(DataController.relCapDiff.get(gridAddress) *100f);

            double unitDiff = -255/relDiff;

            return new Color(255, (int) (abs(relDiff - capDiff) * unitDiff), (int) (abs(relDiff - capDiff) * unitDiff));
        }

    }

}
