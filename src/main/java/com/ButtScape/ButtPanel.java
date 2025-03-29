package com.ButtScape;

import net.runelite.client.ui.PluginPanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ButtPanel extends PluginPanel {

    private final JButton connectButton;
    private JLabel errorLabel;
    private final JTextField expThresholdField;
    private final JSlider intensitySlider;
    private final JLabel intensityLabel;
    private final JTextField serverAddressField;

    private String serverAddress = "ws://localhost:12345";
    private int expThreshold = 10;
    private double vibrationIntensity = 0.5;

    public ButtPanel(Runnable connectAction) {
        super();
        setBorder(null); // Removes extra padding
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10,10,10,10);



        // EXP Threshold input
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        JLabel expThresholdLabel = new JLabel("EXP Threshold:");
        expThresholdField = new JTextField(String.valueOf(expThreshold));
        inputPanel.add(expThresholdLabel,gbc);
        gbc.gridx=1;
        gbc.weightx = 1.0;
        inputPanel.add(expThresholdField,gbc);


        //Vibration Intensity Slider
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.2;
        JLabel intensityTextLabel = new JLabel("Vibration Intensity:");
        intensitySlider = new JSlider(0,100,(int)(vibrationIntensity*100));
        intensityLabel = new JLabel(intensitySlider.getValue() + "%");

        intensitySlider.addChangeListener(e -> {
            int value = intensitySlider.getValue();
            intensityLabel.setText(value+"%");
        });
        inputPanel.add(intensityTextLabel,gbc);
        gbc.gridx=1;
        gbc.weightx = 1.0;
        inputPanel.add(intensitySlider,gbc);
        gbc.gridx=0;
        gbc.gridy=3;
        gbc.weightx = 0.2;
        inputPanel.add(new JLabel("Intensity Value:"),gbc);
        gbc.gridx=1;
        gbc.weightx = 1.0;
        inputPanel.add(intensityLabel,gbc);

        //Server Address Input
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.2;
        JLabel serverAddressLabel = new JLabel("Server Address:");
        serverAddressField = new JTextField(serverAddress, 10);
        inputPanel.add(serverAddressLabel,gbc);
        gbc.gridx=1;
        gbc.weightx = 1.0;
        inputPanel.add(serverAddressField,gbc);
        add(inputPanel, BorderLayout.CENTER);

        //Button Panel
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 3, 0, 3));


        // Apply Button
        JButton applyButton = new JButton("Apply Settings");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    expThreshold = Integer.parseInt(expThresholdField.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(ButtPanel.this, "Please enter a valid integer for EXP Threshold.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                vibrationIntensity = intensitySlider.getValue() / 100.0;
                serverAddress = serverAddressField.getText().trim();

                JOptionPane.showMessageDialog(ButtPanel.this, "Settings applied!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        buttonPanel.add(applyButton);

        // Create the reset button
        connectButton = new JButton("Connect to Intiface");
        connectButton.addActionListener(e -> connectAction.run());

        errorLabel = new JLabel(" "); // Initially no error message
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER); // Center the label
        buttonPanel.add(errorLabel, BorderLayout.CENTER);

        // Add button to the panel
        buttonPanel.add(connectButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    public void setErrorMessage(String message,Color color) {
        errorLabel.setForeground(color);
        SwingUtilities.invokeLater(() -> errorLabel.setText(message));
    }
    public int getExpThreshold() {
        return expThreshold;
    }

    public double getVibrationIntensity() {
        return vibrationIntensity;
    }

    public String getServerAddress(){ return serverAddress;}
}
