package capsensor;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JPanel;
import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.traces.Trace2DLtd;

public class ChartObject extends JPanel {
	private static final long serialVersionUID = 1L;
	private static Color traceColor = Color.BLUE;
	private Chart2D chart = new Chart2D();
	private ITrace2D trace = new Trace2DLtd(200);
	
	// Public constructors
	public ChartObject(ITrace2D _trace) {
		trace = _trace;
		trace.setColor(traceColor);
		chart.addTrace(trace);
		chart.setMinimumSize(new Dimension(AppSettings.widthRect, AppSettings.heightRect));
		chart.setPreferredSize(new Dimension(AppSettings.widthRect, AppSettings.heightRect));
		chart.setSize(getMinimumSize());
//		FlowLayoutCorrectMinimumSize cellLayout = new FlowLayoutCorrectMinimumSize(FlowLayoutCorrectMinimumSize.CENTER);
//		cellLayout.minimumLayoutSize(chart);
		
//		this.setLayout(new FlowLayout());
		
		// Add chart to panel
		this.add(chart);
		this.setOpaque(true);
		this.setBackground(AppSettings.COLOR_DEFAULT);
		this.setMinimumSize(new Dimension(AppSettings.widthRect, AppSettings.heightRect));
//		this.setSize(AppSettings.widthRect, AppSettings.heightRect);
	}
	
	
	public void paint(Color stateColor) {
		this.setBackground(stateColor);
	}
	
//	public Dimension getPreferredSize() {
//		return new Dimension(AppSettings.widthRect, AppSettings.heightRect);
//	}

}
