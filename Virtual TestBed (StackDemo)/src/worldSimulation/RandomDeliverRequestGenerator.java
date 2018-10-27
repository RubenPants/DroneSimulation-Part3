package worldSimulation;

import java.util.Random;
import entities.Package;

public class RandomDeliverRequestGenerator {

	/**
	 * creates integer array with four elements, information needed for creation of new deliver request.
	 * CALL THIS FROM AirportAndPManager!
	 * @param airportArray 
	 * parameter containing all existing airport numbers (integer)
	 * @return int[] result
	 * result[0] = fromAirport
	 * result[1] = fromGate
	 * result[2] = toAirport
	 * result[3] = toGate
	 */
	public static Package requestRandomPackage(int amountOfAirport) {
		
		int departAirport = new Random().nextInt(amountOfAirport);
	    int departGate = new Random().nextInt(2);
	    
	    int arriveAirport = new Random().nextInt(amountOfAirport);
	    int arriveGate = new Random().nextInt(2);
	    
		int[] result = new int[4];
		result[0] = departAirport;
		result[1] = departGate;
		result[2] = arriveAirport;
		result[3] = arriveGate;
		
		Package packet = new Package(result[0],result[1],result[2],result[3]);
		
		return packet;
		
	}

}