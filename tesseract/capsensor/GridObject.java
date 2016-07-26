
package capsensor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JPanel;


public class GridObject extends JPanel {
	private static final long serialVersionUID = 1L;
	private String gridText = new String();
	private Integer gridAddress = 0;
	
	private Color gridColor; 
	
	// Public constructor
	public GridObject() {
		gridText = "null";
		addCell(gridText);
	}
	public GridObject(Integer _gridAddress, String _gridLabel) {
		gridAddress = _gridAddress;
		gridText = _gridLabel;
		addCell(gridText);
	}
	
	public GridObject(Integer _gridAddress, String _gridLabel, Color _gridColor) {
		gridAddress = _gridAddress;
		gridText = _gridLabel;
		gridColor = _gridColor;
		addCell(gridText);
	}
	
	public GridObject(Integer _gridAddress, String _gridLabel, String _gridRelative, Color _gridColor) {
		gridAddress = _gridAddress;
		gridText = _gridLabel + "<br>" + _gridRelative;
		gridColor = _gridColor;
		addCell(gridText);
	}
	
	private void addCell(String cellText) {
		JLabel cellLabel = new JLabel("<html>" + cellText + "</html");
		cellLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		cellLabel.setHorizontalAlignment(JLabel.CENTER);
		cellLabel.setVerticalAlignment(JLabel.CENTER);
		cellLabel.setPreferredSize(new Dimension(AppSettings.widthRect, AppSettings.heightRect));
		this.add(cellLabel);
		this.setMinimumSize(new Dimension(AppSettings.widthRect, AppSettings.heightRect));
		this.setOpaque(true);
		if(gridColor != null)
			this.setBackground(gridColor);
		else
			this.setBackground(computeColor());
	}
	
	/*public void paint(Graphics g) {
		super.paint(g);
		// Draw the background color
		g.drawRect(AppSettings.xPosRect, AppSettings.yPosRect, AppSettings.widthRect, AppSettings.heightRect);
		if(gridColor != null)
			g.setColor(gridColor);
		else
			g.setColor(computeColor());
		g.fillRect(AppSettings.xPosFill, AppSettings.yPosFill, AppSettings.widthFill, AppSettings.heightFill);
		// Draw the object text
		g.setColor(Color.black);
		g.setFont(new Font("Arial", Font.BOLD, 14));
		// Calculate coordinates for centering text in grid object
		FontMetrics font = g.getFontMetrics();
		int px = (int)(AppSettings.widthRect*scale/2 - font.getStringBounds(gridLabel, g).getWidth()/2);
		int py = (int)(AppSettings.heightRect*scale/2 + (font.getAscent() - font.getDescent() + font.getLeading())/2);
		g.drawString(gridLabel, px, py);
	}*/
	
	/*public Dimension getPreferredSize() {
		return new Dimension(AppSettings.widthRect, AppSettings.heightRect);
	}*/
	
	private Color computeColor() {
		String capacitancePattern = "(\\d*.\\d*) pF";
		Pattern pattern = Pattern.compile(capacitancePattern);
		Matcher matcher = pattern.matcher(gridText);
		if (matcher.find() && matcher.groupCount() == 1) {
			Float capValue = Float.parseFloat(matcher.group(1));
			// Check that the grid address is valid
			if (!DataController.baselineThresholdMap.containsKey(gridAddress))
				return AppSettings.COLOR_DEFAULT;
			// If relative difference between capacitance and reference thresholds exceeds the defined relDiffPress, this is a press
			if (capValue >= DataController.baselineThresholdMap.get(gridAddress)*AppSettings.relDiffPress) {
				return AppSettings.COLOR_PRESS;
			}
			// If relative difference between capacitance and reference thresholds is below the defined relDiffHover, this is a hover
			else if (capValue <= DataController.baselineThresholdMap.get(gridAddress)*(1f - AppSettings.relDiffHover)) {
				return AppSettings.COLOR_HOVER;
			}
			// Otherwise, use default color
			else {
				return AppSettings.COLOR_DEFAULT;
			}
//			if()
		} else {
			return AppSettings.COLOR_DEFAULT;
		}
	}
	
}
