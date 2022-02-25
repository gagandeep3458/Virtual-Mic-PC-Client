package com.vmic;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.*;

public class MainForm {
    private JPanel mainPanel;
    private JButton startButton;
    private JButton stopButton;
    public JTextField textField;
    private JRadioButton mToSpeakerRadioButton;
    static AudioFormat format;
    static boolean isStreaming;
    static int port;
    static int sampleRate;
    static boolean isDriverPresent = false;

    DatagramSocket serverSocket;
    Thread networkingThread;
    Thread AudioThread;
    String localIpAddress;

    static boolean isRadioSelected;

    public MainForm() {

        port = 44456;
        sampleRate = 48000;
        format = new AudioFormat(sampleRate, 16, 1, true, false);

        textField.setText("Press Start to Receive Stream.");
        mToSpeakerRadioButton.setSelected(false);
        isRadioSelected = mToSpeakerRadioButton.isSelected();

        try {
            localIpAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        startButton.addActionListener(e -> {

            textField.setText("Use IP : " + localIpAddress + " to connect.");

            Mixer.Info[] mInfoList = AudioSystem.getMixerInfo();
            Mixer.Info mInfo = getMixerInfo(mInfoList);

            //Checking for VB-Driver
            if (mInfo != null) {

                isDriverPresent = true;

                isStreaming = true;
                //startButton.setText("Listening...");

                try {
                    if (serverSocket == null || !serverSocket.isBound()) {
                        serverSocket = new DatagramSocket(port);
                    }

                } catch (SocketException socketException) {
                    socketException.printStackTrace();
                }

                networkingThread = new Thread(() -> {

                    System.out.println();


                    byte[] receiveData = new byte[3840];

                    System.out.print("Listening on port : " + port);
                    while (isStreaming) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData,
                                receiveData.length);

                        if (serverSocket != null) {
                            try {
                                serverSocket.receive(receivePacket);
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }

                        System.out.println("Packet Received");

                        AudioThread = new Thread(() -> toSpeaker(receivePacket.getData(), mInfo));
                        AudioThread.start();
                    }
                });

                networkingThread.start();

            } else {
                textField.setText("Please Install VB-Driver.");
            }
        });

        //Stop Button
        stopButton.addActionListener(e -> {

            if (isDriverPresent) {
                textField.setText("Streaming Stopped.");
                startButton.setText("Start");
                isStreaming = false;
            }

        });

        //Radio button for Speaker Toggle
        mToSpeakerRadioButton.addActionListener(e -> {

            if (mToSpeakerRadioButton.isSelected()) {
                isRadioSelected = true;
                System.out.println("Selected.");

            } else {
                isRadioSelected = false;
                System.out.println("Not Selected.");
            }
        });
    }

    public static void main(String[] args) {

        JFrame jFrame = new JFrame("Virtual Mic");
        jFrame.setLocation(200, 200);
        jFrame.setPreferredSize(new Dimension(280, 100));
        jFrame.pack();
        jFrame.setResizable(false);
        jFrame.setContentPane(new MainForm().mainPanel);
        jFrame.setDefaultCloseOperation(jFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(true);
    }

    public static void toSpeaker(byte[] soundBytes, Mixer.Info mInfo) {

        try {
            Mixer mixer = AudioSystem.getMixer(mInfo);
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);

            SourceDataLine sourceDataLine;

            if (isRadioSelected) {
                sourceDataLine = AudioSystem.getSourceDataLine(format);

            } else {
                sourceDataLine = (SourceDataLine) mixer.getLine(lineInfo);
            }


            sourceDataLine.open();
            sourceDataLine.start();
            sourceDataLine.write(soundBytes, 0, soundBytes.length);
            sourceDataLine.drain();
            sourceDataLine.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Mixer.Info getMixerInfo(Mixer.Info[] mInfoList) {

        for (Mixer.Info mInfo : mInfoList) {
            if (mInfo.getName().equals("CABLE Input (VB-Audio Virtual Cable)")) {
                return mInfo;
            }
        }
        return null;
    }
}
