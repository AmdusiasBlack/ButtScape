package com.ButtScape;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;

import io.github.blackspherefollower.buttplug4j.client.*;
import io.github.blackspherefollower.buttplug4j.connectors.jetty.websocket.client.ButtplugClientWSClient;
import io.github.blackspherefollower.buttplug4j.protocol.ButtplugMessage;
import io.github.blackspherefollower.buttplug4j.protocol.messages.Error;


@Slf4j
@PluginDescriptor(
	name = "ButtScape"
)
public class ButtScapePlugin extends Plugin
{

	private static ButtplugClientWSClient connectionClient; //Client object that connects to Intiface
	private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(); //Scheduler to handle intiface commands
	private ButtPanel panel;
	private NavigationButton navButton;

	@Inject
	private ClientToolbar clientToolbar;

	public void connectDevice()  {
		try {
			System.out.println("Attempting to connect...");
			//If the client isn't already connected, don't make a new connection
			if (connectionClient != null && connectionClient.isConnected()) {
				System.out.println("Already connected.");
				return;
			} else if (connectionClient != null){
				connectionClient.disconnect();
				connectionClient = null;
				System.out.print("Previous connection cleared.");
			}
			String serverAddress = panel.getServerAddress();


			connectionClient = new ButtplugClientWSClient("Buttscape");;
			System.out.println("Client initialized..");
			connectionClient.setOnConnected(new IConnectedEvent() {
				@Override
				public void onConnected(ButtplugClient buttplugClient) {
					// Called on successful connection
					System.out.println("Connected");
					//SwingUtilities.invokeLater(() -> panel.setErrorMessage("Connected!", Color.GREEN));
				}
			});
			connectionClient.setErrorReceived(new IErrorEvent() {
				@Override
				public void errorReceived(Error error) {
					// Called for any errors - this includes those sent from Intiface
					// and also exceptions raised on other threads that may need handling
					System.out.println("Error received: " + error.getErrorMessage());
					SwingUtilities.invokeLater(() -> panel.setErrorMessage(error.getErrorMessage(), Color.RED));
					System.out.println("Nulling client variable..");
					connectionClient = null;
					System.out.println("Shutting down scheduler..");
					scheduler.shutdownNow();
					System.out.println("Reinstantiating scheduler..");
					scheduler = Executors.newSingleThreadScheduledExecutor();
					//Certain fail-states cause the connection client to freeze up, so we respond to all errors by nulling the client and resetting the scheduler.
				}
			});
			connectionClient.setScanningFinished(new IScanningEvent() {
				@Override
				public void scanningFinished() {
					// Called when Intiface stops scanning for devices
					System.out.println("Scanning finished");
				}
			});
			IDeviceEvent deviceEventHandler = new IDeviceEvent() {
				@Override
				public void deviceAdded(ButtplugClientDevice buttplugClientDevice) {
					// Called when Intiface connects to a device
					System.out.println("Device added: " + buttplugClientDevice.getName() + "(" + buttplugClientDevice.getDeviceIndex() + ")");
				}

				@Override
				public void deviceRemoved(long l) {
					// Called when a device disconnects from Intiface
					System.out.println("Device removed: " + l);
				}
			};
			connectionClient.setDeviceAdded(deviceEventHandler);
			connectionClient.setDeviceRemoved(deviceEventHandler);

			// Connect to Intiface (default address/port is hardcoded here but should be configurable)
			System.out.println("Connecting...");
			connectionClient.connect(new URI(serverAddress));
			SwingUtilities.invokeLater(() -> panel.setErrorMessage("Connected!", Color.GREEN));

			// Tell Intiface to start scanning for devices and wait 5 seconds
			connectionClient.startScanning();
			System.out.println("Scanning...");
			//Thread.sleep(5000);

			// List the connected devices
			for (ButtplugClientDevice dev : connectionClient.getDevices()) {
				System.out.println("Device: " + dev.getName() + "(" + dev.getDeviceIndex() + ")");
			}

		}catch (Exception e) {
			    System.out.println("Failed");
				log.error("Failed to Connect",e);
				//ButtplugClientWSClient buttclient = new ButtplugClientWSClient("Buttscape");
				System.out.println("Error: " + e.getMessage());
				//connectionClient.disconnect();
				connectionClient = null;
				scheduler.shutdownNow();
				scheduler = Executors.newSingleThreadScheduledExecutor();

		}
	}

	public void threadConnect(){
		scheduler.schedule(this::connectDevice,600, TimeUnit.MILLISECONDS);
		//Performing the connection within a thread enables us to better handle certain fail-states

	}

	public static void stopAllVibration(){
        try {
            connectionClient.stopAllDevices();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	public void sendVibrate() throws Exception{
		// Iterate over the devices and make them all vibrate at 50%
		// Commands are sent async, so collect an array of the Futures and join them after
		// all the vibration commands have been scheduled
		if (panel == null) {
			System.out.println("Panel not initialized.");
			return;
		}

		double intensity = panel.getVibrationIntensity();
		List<Future<ButtplugMessage>> commands = new LinkedList<>();
		for (ButtplugClientDevice dev : connectionClient.getDevices()) {
			if (dev.getScalarVibrateCount() > 0) {
				try {
					commands.add(dev.sendScalarVibrateCmd(intensity));
				} catch (ButtplugDeviceException e) {
					// This could be a "device disconnected" error or a "connection lost" error...
					System.out.println("Error: " + e.getMessage());
				}
				scheduler.schedule(ButtScapePlugin::stopAllVibration,500, TimeUnit.MILLISECONDS);
				//Stop all vibration after 1 game tick.

			}
		}
		// Join the command send threads
		for (Future<ButtplugMessage> cmd : commands) {
			try {
				cmd.get();
			} catch (Exception e) {
				// This could be a "device disconnected" error or a "connection lost" error...
				System.out.println("Error: " + e.getMessage());
			}
		}
	}

	@Inject
	private Client client;

	private final int[] previousXp = new int[Skill.values().length];

	@Inject
	private ButtscapeConfig config;

	@Override
	protected void startUp() throws Exception
	{
		panel = new ButtPanel(this::threadConnect);
		//ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
		BufferedImage bufferedImage = ImageIO.read(getClass().getResource("/icon.png"));

		navButton = NavigationButton.builder()
				.tooltip("ButtScape")
				.icon(bufferedImage) // Use a small icon
				.panel(panel)
				.priority(5)
				.build();

		clientToolbar.addNavigation(navButton);
		log.info("ButtScape started!");
		//connectDevice();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("ButtScape stopped!");
		clientToolbar.removeNavigation(navButton);
	}
	@Inject private ConfigManager configManager;

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
			//Checking for changes based on gamestate is unnecessary.
		}

	}

	@Subscribe
	public void onStatChanged(StatChanged event) {
		Skill skill = event.getSkill();
		int skillIndex = skill.ordinal();
		int newXp = event.getXp();
		int oldXp = previousXp[skillIndex];
		//Determine gained experience when a stat changes
		int minExp = panel.getExpThreshold();
		if (oldXp > 0) { // Ensure we don’t report on first observation
			int xpGained = newXp - oldXp;
			if (xpGained >= minExp) {
				//String message = "You gained " + xpGained + " experience in " + skill.getName() + "!";
				//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
				//System.out.println(message);
                try {
                    sendVibrate();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
		}
		previousXp[skillIndex] = newXp;
	}

	@Provides
	ButtscapeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ButtscapeConfig.class);
	}
}
