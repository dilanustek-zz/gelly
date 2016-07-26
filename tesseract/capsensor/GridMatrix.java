package capsensor;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;


public class GridMatrix extends JPanel {
	private static final long serialVersionUID = 1L;
	private JPanel panel;
	
	public GridMatrix() {
		// Initialize panel
		panel = this;
//		panel.setLayout(new GridBagLayout());
		GridLayout layout = new GridLayout(AppSettings.numRows, AppSettings.numColumns);
		layout.setHgap(5);
		layout.setVgap(5);
		panel.setLayout(layout);
		// Initialize empty panel holder
		JPanel[][] panelHolder = new JPanel[AppSettings.numRows][AppSettings.numColumns];
		for(int i=0; i<AppSettings.numRows; i++) {
			for(int j=0; j<AppSettings.numColumns; j++) {
				panelHolder[i][j] = new GridObject();
				panelHolder[i][j].setBorder(BorderFactory.createLineBorder(Color.black, 1));
			}
		}	
		// Add matrix to frame
		for(int i=0; i<AppSettings.numRows; i++) {
			for(int j=0; j<AppSettings.numColumns; j++) {
				panel.add(panelHolder[i][j]);
			}
		}
	}
	
	public GridMatrix(JPanel[][] panelHolder) {
		// Initialize panel
		panel = this;
//		panel.setLayout(new GridBagLayout());
		panel.setLayout(new GridLayout(AppSettings.numRows, AppSettings.numColumns));
		// Add matrix to frame
		for(int i=0; i<AppSettings.numRows; i++) {
			for(int j=0; j<AppSettings.numColumns; j++) {
				panel.add(panelHolder[i][j]);
			}
		}
	}

}
