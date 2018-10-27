package control;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.util.vector.Vector3f;

import entities.Airport;
import entities.Camera;
import entities.Drone;
import entities.Package;
import entities.PackageKey;
import interfaces.AutopilotModule;
import interfaces.AutopilotOutputs;
import tools.Tools;
import worldSimulation.DroneStartSettings;
import worldSimulation.RandomDeliverRequestGenerator;

public class WorldManager {

	private ArrayList<Drone> drones = new ArrayList<>();
	private ArrayList<Thread> threads = new ArrayList<>();
	private AutopilotModule module;
	
	private ArrayList<Airport> airports = new ArrayList<>();
	private ArrayList<Airport> occupiedAirports = new ArrayList<>();
	private ArrayList<Airport> freeAirports = new ArrayList<>();

	File file = new File("lastRun.txt");
	
	private ArrayList<Package> packages = new ArrayList<>();
	
	/**
	 * Map registering packages which is waitig for it's deliver.
	 */
	public Map<PackageKey, Package> waitingPackages = new HashMap<PackageKey, Package>();
	
	/**
	 * Map registering packages which is currently delivered by drones.
	 * Read this map when you want to know about those packages
	 * 
	 * Q. Why is the key of this hashmap "droneID" and not "airport/gate number"?
	 * A. The restriction is "Er zijn nooit 2 pakketten tegelijk beschikbaar aan dezelfde vertrekgate."
	 *    So two packages can have the same PackageKey when delivered.
	 */
	public Map<Integer, Package> deliveringPackages = new HashMap<Integer, Package>();
	
	/**
	 * ArrayList registering packages which are delivered. (Can be used as record)
	 */
	public ArrayList<Package> deliveredPackages = new ArrayList<Package>();
	
	private CrashHandler handler;
	
	public WorldManager(CrashHandler handler) {
		this.handler = handler;
	}
	
	public void setAutopilotModule(AutopilotModule module){
		this.module = module;
	}
	
	public ArrayList<Drone> getDrones(){
		return drones;
	}
	
	public Drone getDrone(int index){
		return drones.get(index);
	}
	
	public void addDrone(Drone drone){
		drone.setID(drones.size());
		drones.add(drone);
	}
	
	public void resetDrone(int drone, DroneStartSettings settings){
		drones.get(drone).reset(settings);
	}
	
	public void defineDrones(){
		for(Drone drone: drones){
			module.defineDrone(drone.getAirport().getId(), drone.getGate(), (drone.getGate()+1)%2, drone.getConfigs());
			//module.defineDrone(0, 0, 0, drone.getConfigs());			
		}
	}
	
	public void allDronesTimePassed(float timePassed, float simulationTime, int iterations){
		/*
		Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
		    public void uncaughtException(Thread th, Throwable ex) {
		    	handler.handleCrash(ex.getMessage());
		    	ex.printStackTrace();
		    }
		};
		for(int i=0;i<drones.size();i++){
			Thread thread = (new Thread(new DroneThread(drones.get(i), module, timePassed, simulationTime, iterations)));
			threads.add(thread);
			thread.setUncaughtExceptionHandler(h);
			thread.start();
		}
		*/
		
		// MULTITHREADING UITSCHAKELEN DOOR UITCOMMENTARIEREN EN BOVENSTAANDE COMMENTARIEREN
		for(int i=0;i<drones.size();i++){
			Drone drone = drones.get(i);
			module.startTimeHasPassed(drone.getID(), Tools.getAutopilotInputs(drone, simulationTime, new byte[0]));
			AutopilotOutputs outputs = module.completeTimeHasPassed(drone.getID());
			drone.setInputs(outputs);
			for(int j = 0;j<iterations;j++){
				drone.timePassed(timePassed/iterations);
			}
			
		}
	}
	
	public void waitForThreads(){
		for(Thread thread: threads){
			try {
				thread.join();
			} catch (InterruptedException e) {
				System.out.println("Thread interrupted");
			}
		}
		threads.clear();
	}
	
	public void reset(DroneStartSettings settings){
		for(Drone drone: drones){
			drone.reset(settings);
		}
	}
	
	public Camera getDefaultCamera(){
		return new Camera(drones.get(0));
	}

	public ArrayList<Airport> getOccupiedAirports() {
		return occupiedAirports;
	}

	public ArrayList<Airport> getFreeAirports() {
		return freeAirports;
	}
	
	public void addAirport(Airport airport){
		airports.add(airport);
		freeAirports.add(airport);
	}
	
	public Airport getAirport(int i){
		return airports.get(i);
	}
	
	public ArrayList<Airport> getAirports() {
		return airports;
	}

	public void occupyPort(Airport airport){
		if(airport.isAirportOccupied()) {
			freeAirports.remove(airport);
			occupiedAirports.add(airport);
		}
	}
	
	public void unoccupyPort(Airport airport){
		if(!airport.isAirportOccupied()) {
			freeAirports.add(airport);
			occupiedAirports.remove(airport);
		}
	}
	
	public void addDroneToAirport(Drone drone, Airport port, int gate) {
		DroneStartSettings settings = new DroneStartSettings();
		if(gate==1) {
			port.setDroneGate1(drone);
			settings.setPosition(port.getPlaneSpawnGate1(-drone.getConfigs().getWheelY()+0.2f));
			settings.setHeading(port.getRotation()-(float)Math.PI/2);
		} else {
			port.setDroneGate0(drone);
			settings.setPosition(port.getPlaneSpawnGate0(-drone.getConfigs().getWheelY()+0.2f));
			settings.setHeading(port.getRotation()+(float)Math.PI/2);
		}
		drone.reset(settings);
		occupyPort(port);
	}
	
	public void clearAll() {
		drones.clear();
		airports.clear();
		freeAirports.clear();
		occupiedAirports.clear();
	}
	
	public void removeDroneFromAirport(Drone drone) {
		Airport port = drone.getAirport();
		if(port==null) return;
		int gate = drone.getGate();
		if(gate==1) {
			port.setDroneGate1(null);
		} else {
			port.setDroneGate0(null);
		}
		unoccupyPort(port);
	}
	
	public ArrayList<Package> getPackages(){
		return this.packages;
	}
	
	/**
	 * Adds package to packagManager.
	 * @param pack
	 */
	public void addNewPackage(Package pack){
		if(pack.fromA >= airports.size() || pack.toA >= airports.size()) {
			throw new IllegalArgumentException("One of the given airports doesn't exist");
		}
		if (checkNoDuplication(pack)){
			this.waitingPackages.put(new PackageKey(pack.fromA,pack.fromG), pack);
			this.packages.add(pack);
			module.deliverPackage(pack.fromA, pack.fromG, pack.toA, pack.toG);			
			
			//Output voor eventuele log, mag weg als niemand het nodig vindt.
	        if(file.exists()){
	        	try {
					FileWriter fw = new FileWriter(file);
					fw.write(pack.fromA + " " + pack.fromG + " " + pack.toA + " " + pack.toG + "\r\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
		}else
			throw new IllegalArgumentException("The start airport already has a waiting package");
	}
	
	/**
	 * takes a package out of waitingPackages and stores it to deliveringPackages.
	 * 
	 * @param airNum
	 * @param gateNum
	 * @param droneID
	 */
	public boolean startDeliveringPackage(int airNum, int gateNum, Drone drone){
		if(drone.isHasPackage()) return false;
		Package pack = this.waitingPackages.remove(new PackageKey(airNum,gateNum));
		if(pack == null) return false;
		pack.startCarry(drone);
		this.deliveringPackages.put(drone.getID(), pack);
		System.out.println("PICKED UP PACKAGE");
		return true;
	}
	
	/**
	 * Ends delivering if the package's destination is current airport (and gate).
	 * 
	 * @param airNum
	 * @param gateNum
	 * @param droneID
	 */
	public boolean endDeliver(int airNum, int gateNum, Drone drone){
		Package pack = this.deliveringPackages.get(drone.getID());
		if(pack == null) return false;
		if(pack.toA == airNum && pack.toG == gateNum){
			Package endedPackage = this.deliveringPackages.remove(drone.getID());
			this.deliveredPackages.add(endedPackage);
			System.out.println("PACKAGE DELIVERED");
			pack.deliver();
			return true;
		} else {
			return false;
			//System.out.println("delivered on false position, or a false call of endDeliver");
		}
	}
	
	/**
	 * Avoids the existence of two packages at the same airport, same gate.
	 * @param pack
	 * @return
	 */
	public boolean checkNoDuplication(Package pack){
		if (pack == null)
			return false;
		else if (!this.waitingPackages.containsKey(new PackageKey(pack.fromA,pack.fromG)))
			return true;
		else 
			return false;
	}

	
	//Extra's
	public void createRandomRequest(){
		//world.getNumberOfAirport (iets om aantal airports te halen)
		int amountOfAirports = airports.size();
		Package pack = null;
		while (!checkNoDuplication(pack))
			pack = RandomDeliverRequestGenerator.requestRandomPackage(amountOfAirports);
	}
	
	
	public boolean checkDronesForPackagePickup() {
		for(Drone drone: drones) {
			if(drone.getVelocity().length() < 1) {
				for(Airport port: getAirports()) {
					int gate = port.isInGate(drone);
					if(gate != -1) {
						boolean end = endDeliver(port.getId(), gate, drone);
						boolean start = startDeliveringPackage(port.getId(), gate, drone);
						if(end||start) return true;
					}
				}
			}
		}
		return false;
	}
}