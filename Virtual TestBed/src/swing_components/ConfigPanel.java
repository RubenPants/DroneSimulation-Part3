package swing_components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import control.WorldManager;
import entities.Airport;
import entities.Drone;
import interfaces.AutopilotConfig;
import worldSimulation.ConfigGenerator;
import worldSimulation.DroneStartSettings;

public class ConfigPanel extends JPanel{

	private JCheckBox degreesBox;
	
	private JComboBox<Drone> droneList = new JComboBox<>();
	private JComboBox<Airport> airportList = new JComboBox<>();
	private WorldManager manager;
	
	private ValuePanel gravityPanel;
	private ValuePanel wingXPanel;
	private ValuePanel tailSizePanel;
	private ValuePanel engineMassPanel;
	private ValuePanel wingMassPanel;
	private ValuePanel tailMassPanel;
	private ValuePanel maxThrustPanel;
	private ValuePanel maxAOAPanel;
	private ValuePanel wingSlopePanel;
	private ValuePanel horStabSlopePanel;
	private ValuePanel verStabSlopePanel;
	private ValuePanel tyreRadiusPanel;
	private ValuePanel wheelYPanel;
	private ValuePanel frontZPanel;
	private ValuePanel rearZPanel;
	private ValuePanel rearXPanel;
	
	private JButton confirmButton;
	private JButton droneButton;
	
	private SettingsListener listener;
	
	public ConfigPanel() {
		
		droneList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Drone drone = (Drone)droneList.getSelectedItem();
				setInfo(drone);
				airportList.setSelectedItem(drone.getAirport());
				listener.setDroneForCamera(drone.getID());
			}
		});
		
		gravityPanel = new ValuePanel("Gravity (positive):", 9.81f);
		wingXPanel = new ValuePanel("WingX:", 4.2f);
		tailSizePanel = new ValuePanel("Tail Size:", 4.2f);
		engineMassPanel = new ValuePanel("Engine Mass:", 180f);
		wingMassPanel = new ValuePanel("Wing Mass:", 100f);
		tailMassPanel = new ValuePanel("Tail Mass:", 100f);
		maxThrustPanel = new ValuePanel("Max Thrust:", 2000);
		maxAOAPanel = new ValuePanel("Max AOA:", .261f);
		wingSlopePanel = new ValuePanel("Wing LiftSlope:", 10f);
		horStabSlopePanel = new ValuePanel("Horizontal Stab LiftSlope:", 5f);
		verStabSlopePanel = new ValuePanel("Vertical Stab LiftSlope:", 5f);
		tyreRadiusPanel = new ValuePanel("Tyre Radius:", 0.2f);
		wheelYPanel = new ValuePanel("Wheels Y:", -4.5f);
		frontZPanel = new ValuePanel("Front Wheel Z:", -8.2f);
		rearZPanel = new ValuePanel("Rear Wheel Z :", 2f);
		rearXPanel = new ValuePanel("Rear Wheel X:", 4.55f);
		
		degreesBox = new JCheckBox("Angles in Degrees");
		degreesBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(degreesBox.isSelected()){
					maxAOAPanel.setValue((float)Math.toDegrees(maxAOAPanel.getValue()));
				} else {
					maxAOAPanel.setValue((float)Math.toRadians(maxAOAPanel.getValue()));
				}
			}
		});
		
		droneButton = new JButton("Add drone");
		droneButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Drone drone = new Drone(getConfig());
				manager.addDrone(drone);
				droneList.addItem(drone);
			}
		});
		
		confirmButton = new JButton("Load configurations to testbed");
		confirmButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveDrone();
			}
		});
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints gc = new GridBagConstraints();
		
		gc.gridy = 0;
		gc.gridx = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.ipadx = 20;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weighty = 0;
		add(droneList,gc);
		
		gc.gridx = 1;
		add(droneButton,gc);
		
		gc.gridy++;
		gc.anchor = GridBagConstraints.BASELINE_TRAILING;
		add(degreesBox,gc);

		gc.gridwidth = 2;
		gc.gridx = 0;
		gc.gridy++;
		gc.anchor = GridBagConstraints.CENTER;
		add(gravityPanel,gc);
		
		gc.gridy++;
		add(wingXPanel,gc);
		
		gc.gridy++;		
		add(tailSizePanel,gc);
		
		gc.gridy++;		
		add(engineMassPanel,gc);
		
		gc.gridy++;		
		add(wingMassPanel,gc);
		
		gc.gridy++;		
		add(tailMassPanel,gc);
		
		gc.gridy++;		
		add(maxThrustPanel,gc);
		
		gc.gridy++;		
		add(maxAOAPanel,gc);
		
		gc.gridy++;		
		add(wingSlopePanel,gc);
		
		gc.gridy++;	
		add(horStabSlopePanel,gc);
		
		gc.gridy++;	
		add(verStabSlopePanel,gc);
		
		gc.gridy++;	
		add(tyreRadiusPanel,gc);
		
		gc.gridy++;	
		add(wheelYPanel,gc);
		
		gc.gridy++;	
		add(frontZPanel,gc);
		
		gc.gridy++;	
		add(rearZPanel,gc);
		
		gc.gridy++;	
		add(rearXPanel,gc);
		
		gc.gridy++;	
		add(airportList,gc);
		
		gc.gridy++;	
		add(confirmButton,gc);
	}
	
	public void addActionListener(ActionListener listener) {
		confirmButton.addActionListener(listener);
	}
	
	public float getGravity() {
		return gravityPanel.getValue();
	}
	
	public float getWingX() {
		return wingXPanel.getValue();
	}
	
	public float getTailSize() {
		return tailSizePanel.getValue();
	}
	
	public float getEngineMass() {
		return engineMassPanel.getValue();
	}
	
	public float getWingMass() {
		return wingMassPanel.getValue();
	}
	
	public float getTailMass() {
		return tailMassPanel.getValue();
	}
	
	public float getMaxThrust() {
		return maxThrustPanel.getValue();
	}
	
	public float getMaxAOA() {
		float value = maxAOAPanel.getValue();
		if(degreesBox.isSelected())
			return (float) Math.toRadians(value);
		else {
			return value;
		}
	}
	
	public float getWingSlope() {
		return wingSlopePanel.getValue();
	}
	
	public float getHorStabSlope() {
		return horStabSlopePanel.getValue();
	}
	
	public float getVerStabSlope() {
		return verStabSlopePanel.getValue();
	}
	
	public float getTyreRadius() {
		return tyreRadiusPanel.getValue();
	}
	
	public float getWheelY() {
		return wheelYPanel.getValue();
	}
	
	public float getFrontZ() {
		return frontZPanel.getValue();
	}
	
	public float getRearZ() {
		return rearZPanel.getValue();
	}
	
	public float getRearX() {
		return rearXPanel.getValue();
	}
	public AutopilotConfig getConfig() {
		float engineMass = ConfigPanel.this.getEngineMass();
		float gravity = ConfigPanel.this.getGravity();
		float horStab = ConfigPanel.this.getHorStabSlope();
		float verStab = ConfigPanel.this.getVerStabSlope();
		float maxAOA = ConfigPanel.this.getMaxAOA();
		float maxThrust = ConfigPanel.this.getMaxThrust();
		float tailMass = ConfigPanel.this.getTailMass();
		float tailSize = ConfigPanel.this.getTailSize();
		float wingLift = ConfigPanel.this.getWingSlope();
		float wingMass = ConfigPanel.this.getWingMass();
		float wingX = ConfigPanel.this.getWingX();
		float tyreRadius = ConfigPanel.this.getTyreRadius();
		float wheelY = ConfigPanel.this.getWheelY();
		float frontZ = ConfigPanel.this.getFrontZ();
		float rearZ = ConfigPanel.this.getRearZ();
		float rearX = ConfigPanel.this.getRearX();
		return ConfigGenerator.generate(wingX, wingMass, tailSize, tailMass, engineMass, maxThrust, wingLift,
				horStab, verStab, maxAOA, tyreRadius, wheelY, frontZ, rearZ, rearX, gravity);
	}
	
	public void saveDrone(){
		Drone drone = (Drone)droneList.getSelectedItem();
		drone.setConfiguration(getConfig());
		Airport selectedPort = (Airport)airportList.getSelectedItem();
		if(drone.getAirport()!=selectedPort) {
			manager.removeDroneFromAirport(drone);
			if(!selectedPort.isGateZeroOccupied()) {
				manager.addDroneToAirport(drone, selectedPort, 0);
			} else {
				manager.addDroneToAirport(drone, selectedPort, 1);
			}
		}
	}
	
	public void fireData(WorldManager manager){
		this.manager = manager;
		for(Drone drone: manager.getDrones()){
			droneList.addItem(drone);
		}
		airportList.removeAllItems();
		for(Airport airport: manager.getFreeAirports()){
			airportList.addItem(airport);
		}
		Drone drone = (Drone)droneList.getSelectedItem();
		Airport port = drone.getAirport();
		if(port!=null && port.isAirportOccupied()) {
			airportList.addItem(drone.getAirport());
		}
	}
	
	private void setInfo(Drone drone){
		gravityPanel.setValue(drone.getConfigs().getGravity());
		wingXPanel.setValue(drone.getConfigs().getWingX());
		tailSizePanel.setValue(drone.getConfigs().getTailSize());
		engineMassPanel.setValue(drone.getConfigs().getEngineMass());
		wingMassPanel.setValue(drone.getConfigs().getWingMass());
		tailMassPanel.setValue(drone.getConfigs().getTailMass());
		maxThrustPanel.setValue(drone.getConfigs().getMaxThrust());
		maxAOAPanel.setValue(drone.getConfigs().getMaxAOA());
		wingSlopePanel.setValue(drone.getConfigs().getWingLiftSlope());
		horStabSlopePanel.setValue(drone.getConfigs().getHorStabLiftSlope());
		verStabSlopePanel.setValue(drone.getConfigs().getVerStabLiftSlope());
		tyreRadiusPanel.setValue(drone.getConfigs().getTyreRadius());
		wheelYPanel.setValue(drone.getConfigs().getWheelY());
		frontZPanel.setValue(drone.getConfigs().getFrontWheelZ());
		rearZPanel.setValue(drone.getConfigs().getRearWheelZ());
		rearXPanel.setValue(drone.getConfigs().getRearWheelX());
		
		airportList.removeAllItems();
		
		for(Airport airport: manager.getFreeAirports()){
			airportList.addItem(airport);
		}
		Airport port = drone.getAirport();
		if(port!=null && port.isAirportOccupied()) {
			airportList.addItem(drone.getAirport());
		}
	}
	
	public void setSettingsListener(SettingsListener listener) {
		this.listener = listener;
	}
}
