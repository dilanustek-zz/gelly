package capsensor;


import javax.swing.*;
import java.awt.*;

/**
 * Created by dilanustek on 7/25/16.
 */


public class InterfaceFunct {

    /**
     * Functions for interacting with the GUI elements
     */
    protected static void changeDisplayMode(int newMode) {
        CapMatrix.displayMode = newMode;
        if(CapMatrix.displayMode == CapMatrix.CAPACITANCE_MODE) {
//			findTouchPoints();
            CapMatrix.updateMatrix();
        } else if(CapMatrix.displayMode == CapMatrix.GRAPH_MODE) {
            // Update the values of the panel holder
            for(int i=0; i<AppSettings.numColumns; i++) {
                for(int j=0; j<AppSettings.numRows; j++) {
                    int gridAddress = Integer.parseInt(AppSettings.gridAddress[((i)*AppSettings.numColumns) + (j)],2);
                    if(CapMatrix.displayMode == CapMatrix.GRAPH_MODE) {
                        CapMatrix.panelHolder[i][j] = CapMatrix.chartsArray.get(gridAddress);
                    }
                    CapMatrix.panelHolder[i][j].setBorder(BorderFactory.createLineBorder(Color.black, 1));
                }
            }
            // Update matrix
            CapMatrix.updateMatrix();
        }
    }

    protected static void changeFilterMode(int newMode) {
        CapMatrix.filterMode = newMode;
        if(newMode == CapMatrix.FILTER_SR) {
            AppSettings.slewRateSampleCounter = 0;
            AppSettings.slewRateSetCounter = 0;
        } else if(newMode == CapMatrix.FILTER_LP) {
            CapMatrix.rawCap.clear();
        }
        if(CapMatrix.isDataTransmitting) {
            CapMatrix.isDataTransmitting = false;
        }
    }

    protected static void changeTouchMode(int newMode) {
        CapMatrix.touchMode = newMode;
        CapMatrix.touchPoints.clear();
        if(CapMatrix.isDataTransmitting) {
            CapMatrix.isDataTransmitting = false;
        }
    }

}
