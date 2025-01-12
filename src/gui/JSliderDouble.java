package gui;

import java.text.DecimalFormat;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JSlider;

@SuppressWarnings("serial")
public class JSliderDouble extends JSlider {

	private double from;
	private double to;
	private int resolution;
	private double currentValue;

	private int labelEvery;

	public JSliderDouble(double from, double to, double currentValue,
			int resolution, int labelEvery) {

		this.from = from;
		this.to = to;
		this.resolution = resolution;
		this.currentValue = currentValue;
		this.labelEvery = labelEvery;

		initSlider();

	}// END: Constructor

	private void initSlider() {

		DecimalFormat df = new DecimalFormat("#.#");

		setOrientation(HORIZONTAL);

		int min = (int) (from * resolution);
		setMinimum(min);

		int max = (int) (to * resolution);
		setMaximum(max);

		int curr = (int) (currentValue * resolution);
		setValue(curr);

		double delta = (to - from) / (resolution - 1);
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		for (int i = 0; i < resolution; i++) {

			if (i % labelEvery == 0) {

				double value = (from + (i+1) * delta);
				int tick = (int) (value * resolution);
				String label = df.format(value);

				labelTable.put(new Integer(tick), new JLabel(label));
			}
		}

		setLabelTable(labelTable);

	}// END: initSlider

//	public void setLabelEvery() {
//		this.labelEvery = 
//	}
	
	public double getDoubleValue() {
		double value = (double) this.getValue() / (double) resolution;
		return value;
	}// END: getDoubleValue

}// END: class