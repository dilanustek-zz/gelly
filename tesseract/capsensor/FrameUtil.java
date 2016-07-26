package capsensor;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

/**
 * Created by dilanustek on 7/26/16.
 */
public class FrameUtil {

    protected static JFrame frame = new JFrame();

    protected static JPanel[][] panelHolder = new JPanel[AppSettings.numRows][AppSettings.numColumns];
    protected static final String frameTitle = "Capacitive Sensor - Readout Matrix";

    // GUI Component variables
    private static HashMap<String, Component> componentMap;
    private static final String matrixName = "GridMatrix";
    private static final String debugTextName = "DebugText";
    private static GridBagConstraints c_settings = new GridBagConstraints();
    private static GridBagConstraints c_title = new GridBagConstraints();
    private static GridBagConstraints c_matrix =  new GridBagConstraints();
    private static GridBagConstraints c_debug = new GridBagConstraints();

    /**
     * Function set for managing components in the frame
     */
    private static void createComponentMap() {
        componentMap = new HashMap<String, Component>();
        Component[] componentList = frame.getContentPane().getComponents();
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

    protected static void addComponentsToFrame() {
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
        frame.getContentPane().add(headerPanel, c_title);

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
        frame.getContentPane().add(new SettingsPanel(), c_settings);

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
        frame.getContentPane().add(matrix, c_matrix);

        // Add debug text to frame
        c_debug.gridx = 0;
        c_debug.gridy = AppSettings.numRows+1;
        c_debug.weightx = 10;
        c_debug.weighty = 1;
        c_debug.gridwidth = AppSettings.numColumns + 1;
        c_debug.ipady = 20;
        c_debug.anchor = GridBagConstraints.LAST_LINE_START;
        c_debug.insets = new Insets(5,10,5,10);
        JLabel debugTextComponent = new JLabel(CapMatrix.debugText);
        debugTextComponent.setName(debugTextName);
        frame.getContentPane().add(debugTextComponent, c_debug);

        createComponentMap();
    }

    // Generic update command for filling entire grid matrix with current capacitance values or graphs
    protected static void updateMatrix() {
        // Clear matrix contents
        if(componentMap.containsKey(matrixName)) {
            frame.getContentPane().remove(getComponentByName(matrixName));
//			frame.getContentPane().remove(getComponentByName(debugTextName));
        }

        // Add matrix to frame
        GridMatrix matrix = new GridMatrix(panelHolder);
        matrix.setName(matrixName);
        frame.getContentPane().add(matrix, c_matrix);
        componentMap.replace(matrixName, matrix);

        // Add debug text to frame
        updateDebugText();

        // Repaint components to frame
        frame.getContentPane().revalidate();
        frame.getContentPane().repaint();
    }

    protected static void updateDebugText() {
        JLabel debugTextComponent = (JLabel) getComponentByName(debugTextName);
        debugTextComponent.setText(CapMatrix.debugText);
    }
}
