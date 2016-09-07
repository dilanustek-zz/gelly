package capsensor;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AppSettings {
	// Grid matrix parameters
	public static int numRows = 4;
	public static int numColumns = 4;
	protected static int numDataPts = AppSettings.numColumns * AppSettings.numRows;

	public static String[] gridAddress = {"0110","0100","0101","0111","0010","0000","0001","0011","1010","1000","1001","1011","1110","1100","1101","1111"};
	/*public static String[] gridAddress = {"1111","1011","0011","0111",
											"1101","1001","0001","0101",
											"1110","1010","0010","0110",
											"0000","1100","0100","1000"};*/


	// Grid object parameters
	public static final int widthRect = 200;
	public static final int heightRect = 150;

	// Color parameters
	public static final Color COLOR_HOVER = Color.green;
	public static final Color COLOR_PRESS = Color.red;
	public static final Color COLOR_DEFAULT = Color.white;
	
	// Arduino serial parameters
	public static String serialPortNumber = "/dev/cu.usbmodem1411";
	
	// File IO parameters
	public static final String fileDirectory = new String();
	public static final String fileName = "/Users/dilanustek/Documents/OneDrive/research/code/gelly/out/artifacts/gelly_jar";
	public static final String csvFile = "/Users/dilanustek/Documents/OneDrive/research/code/logs/newlog.csv";

	private static int tmpRow = -1;
	private static int tmpColumn = -1;
	private static String tmpSerialPort = new String();
	private static ArrayList<String> tmpGridAddress = new ArrayList<String>();
	private static Float tmpThresholdHover;
	private static Float tmpThresholdPress;
	private static Float tmpThresholdRelease;
	private static final String tagRow = "Number of rows:";
	private static final String tagColumn = "Number of columns:";
	private static final String tagSerialPort = "Arduino serial port:";
	private static final String tagGridAddress = "Grid matrix addresses:";
	private static final String tagThresholdPress = "Relative ratio for press:";
	private static final String tagThresholdRelease = "Relative ratio for release:";
	private static final String patternRowString = "Number of rows:\\s*(\\d+)";
	private static final String patternColumnString = "Number of columns:\\s*(\\d+)";
	private static final String patternSerialPortString = "Arduino serial port:\\s*(.+)";
//	private static final String patternGridString = "Grid matrix addresses:";
	private static final String patternGridObject = "\\[(\\d+)\\]";
	private static String patternGridAddressString = "\\[(\\d+)\\]\\[(\\d+)\\]\\[(\\d+)\\]\\[(\\d+)\\]";
	private static final String patternThresholdHover = "Relative ratio for hover:\\s*(.+)";
	private static final String patternThresholdPress = "Relative ratio for press:\\s*(.+)";
	private static final String patternThresholdRelease = "Relative ratio for release:\\s*(.+)";
	static final HashMap<String,Integer> rowMap = new HashMap<String,Integer>();
	
	// Touch threshold values
	public static Float relDiffPress = -0.05f;
	public static Float relDiffRelease = -0.005f;
	public static Float relDiffHover= relDiffPress.floatValue() * 0.5f;

	// Filtering parameters
	public static final float slewRateIncrement = 0.005f;
	public static int slewRateSampleCounter = 0;
	public static int slewRateSetCounter = 0;
	public static final int slewRateSetsPerUpdate = 4;

	// GUI Max and Min Range
	protected static double minRange = 1.5;
	protected static double maxRange = 2.5;


	private static void prepareMap() {
		rowMap.clear();
		rowMap.put(tagRow, 1);
		rowMap.put(tagColumn, 2);
		rowMap.put(tagSerialPort, 3);
		rowMap.put(tagThresholdPress, 5);
		rowMap.put(tagThresholdRelease, 6);
		rowMap.put(tagGridAddress, 7);
	}
	
	public static void readFile(String filePath) throws IOException {
		prepareMap();
		File file = new File(filePath);
		if(file.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			int lineNumber = 1;
			Pattern pattern;
			Matcher matcher;
			while((line = br.readLine()) != null) {
				if(line.length() <= 0)
					continue;
				line = line.trim();
				if(lineNumber == rowMap.get(tagRow)) {
					pattern = Pattern.compile(patternRowString);
					matcher = pattern.matcher(line);
					if(matcher.find() && matcher.groupCount() == 1) {
						tmpRow = Integer.parseInt(matcher.group(1));
						numRows = (tmpRow > 0 ? tmpRow : numRows);
						System.out.println("Row read: " + tmpRow);
//						rowMap.put(tagThresholds, rowMap.get(tagGridAddress) + 1 + tmpRow);
					}
				} else if(lineNumber == rowMap.get(tagColumn)) {
					pattern = Pattern.compile(patternColumnString);
					matcher = pattern.matcher(line);
					if (matcher.find() && matcher.groupCount() == 1) {
						tmpColumn = Integer.parseInt(matcher.group(1));
						numColumns = (tmpColumn > 0 ? tmpColumn : numColumns);
						System.out.println("Column read: " + tmpColumn);
						generateGridPattern();
					}
				} else if(lineNumber == rowMap.get(tagSerialPort)) {
					pattern = Pattern.compile(patternSerialPortString);
					matcher = pattern.matcher(line);
					if (matcher.find() && matcher.groupCount() == 1) {
//						tmpSerialPort = stripSpaces(matcher.group(1).trim().toUpperCase());
						tmpSerialPort = stripSpaces(matcher.group(1).trim());
						if(tmpSerialPort.contains("COM") && tmpSerialPort.length() > 3)
							serialPortNumber = tmpSerialPort.toUpperCase();
						else
							serialPortNumber = tmpSerialPort;
						System.out.println("Serial port read: " + tmpSerialPort);
					} else {
						System.out.println("No serial port value read.");
					}
				} else if(lineNumber == rowMap.get(tagThresholdPress)) {
					pattern = Pattern.compile(patternThresholdPress);
					matcher = pattern.matcher(line);
					if (matcher.find() && matcher.groupCount() == 1) {
						tmpThresholdPress= Float.parseFloat(matcher.group(1).trim());
						AppSettings.relDiffPress= tmpThresholdPress != null ? tmpThresholdPress/100f : AppSettings.relDiffPress;
//						System.out.println("Press threshold read: " + matcher.group(1).trim());
						System.out.println("Press threshold read: " + AppSettings.relDiffPress);
					} else {
						System.out.println("No hover threshold value read.");
					}
				} else if(lineNumber == rowMap.get(tagThresholdRelease)) {
					pattern = Pattern.compile(patternThresholdRelease);
					matcher = pattern.matcher(line);
					if (matcher.find() && matcher.groupCount() == 1) {
						tmpThresholdRelease = Float.parseFloat(matcher.group(1).trim());
						AppSettings.relDiffRelease = tmpThresholdRelease != null ? tmpThresholdRelease/100f : AppSettings.relDiffRelease;
						System.out.println("Release threshold read: " + AppSettings.relDiffRelease);
					} else {
						System.out.println("No Release threshold value read.");
					}
				} else if(lineNumber == rowMap.get(tagGridAddress)) {
					if(line.equals(tagGridAddress)) {
						pattern = Pattern.compile(patternGridAddressString);
						int loopEnd = (tmpRow > 0 ? tmpRow : numRows);
						for(int i=0; i<loopEnd; i++) {
							line = (br.readLine()).trim();
							System.out.println(line);
							matcher = pattern.matcher(line);
//							System.out.println("Matches found: " + matcher.find() + "; Match count: " + matcher.groupCount());
//							System.out.println(matcher.find() && (matcher.groupCount() == numColumns) ? true:false);
							if(matcher.find() && matcher.groupCount() == numColumns) {
								for(int j=0; j<matcher.groupCount(); j++) {
									tmpGridAddress.add(matcher.group(j+1));
//									System.out.println("Address read: " + matcher.group(j+1));
								}
							}
							if(tmpGridAddress.size() == numRows*numColumns) {
								gridAddress = tmpGridAddress.toArray(gridAddress);
								System.out.println(gridAddress);
							}
							lineNumber++;
						}
					}
				} 
				lineNumber++;
			}
			br.close();
		} else {
			System.out.println("File does not exist.");
		}
	}
	
	private static String stripSpaces(String originalString) {
		char[] charArray = originalString.toCharArray();
		StringBuilder strippedString = new StringBuilder();
		for(char c : charArray) {
			if(c != ' ')
				strippedString.append(c);				
		}
		return strippedString.toString();
	}
	
	private static void generateGridPattern() {
		patternGridAddressString = new String();
		for(int i=0; i<numColumns; i++) {
			patternGridAddressString += patternGridObject;
			System.out.println("New grid pattern string: " + patternGridAddressString);
		}
		
	}
}
