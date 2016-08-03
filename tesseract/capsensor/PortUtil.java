package capsensor;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by dilanustek on 7/26/16.
 */
public class PortUtil {

    protected static SerialPort serialPort;

    //csv file writer
    protected static FileWriter writer;

    /**
     * Functions relating to serial port management
     */
    private static StringBuilder arduinoMessage = new StringBuilder();
    protected static void portInitialization()
            throws SerialPortException {
        CapMatrix.debugText = CapMatrix.DEBUG_WAIT_CALIBRATE;
        serialPort = new SerialPort(AppSettings.serialPortNumber);
        serialPort.openPort();
        serialPort.setParams(9600, 8, 1, SerialPort.PARITY_NONE);
        serialPort.addEventListener(new SerialPortEventListener() {
            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                if(serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() > 0) {
                    try {
                        byte buffer[] = serialPort.readBytes();
                        for(byte b:buffer) {
                            if((b == '\r' || b == '\n') && arduinoMessage.length() > 0) {
                                //System.out.println(arduinoMessage.toString().trim());
                                DataController.parseCommand(arduinoMessage.toString().trim());
                                arduinoMessage = new StringBuilder();
                            } else {
                                arduinoMessage.append((char) b);
                            }
                        }
                    } catch(SerialPortException e) {
                        System.out.println("Read Error: " + e);
                    } catch (IOException e) {
                        System.out.println("IO Error: " + e);
                    }
                }
            }
        });
    }

    protected static void initializeSerialConnection()
            throws IOException {
        try {
            portInitialization();

            writer = new FileWriter(AppSettings.csvFile);
            SettingsPanel.isConnected = true;
            SettingsPanel.comPortButton.setText(SettingsPanel.serialDisconnectString);
            SettingsPanel.enablePanel();
            DataController.startTime = System.currentTimeMillis();
        } catch(SerialPortException e) {
            System.out.println(e);
            CapMatrix.debugText = CapMatrix.DEBUG_COM_UNAVAILABLE;
            FrameUtil.updateDebugText();
            return;
        }
    }

    protected static void endSerialConnection()
            throws IOException {
        try {
            if(serialPort != null) {
                serialPort.closePort();
                SettingsPanel.isConnected = false;
                SettingsPanel.comPortButton.setText(SettingsPanel.serialInitializeString);
                SettingsPanel.disablePanel();
            }
            CapMatrix.debugText = "Disconnected from serial port.";
            FrameUtil.updateDebugText();

            writer.flush();//
            writer.close();

        } catch(SerialPortException e) {
            System.out.println(e);
            FrameUtil.frame = new JFrame();
            FrameUtil.frame.setTitle(FrameUtil.frameTitle);
            FrameUtil.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            FrameUtil.frame.add(new JLabel("Unable to close connection to Arduino...\nThe serial may be incorrect, not connected, or busy."));
            FrameUtil.frame.pack();
            FrameUtil.frame.setResizable(false);
            FrameUtil.frame.setVisible(true);
            CapMatrix.debugText = CapMatrix.DEBUG_COM_UNAVAILABLE;
            FrameUtil.addComponentsToFrame();
            FrameUtil.frame.pack();
            FrameUtil.frame.setResizable(false);
            FrameUtil.frame.setVisible(true);
            return;
        }
    }

}
