
# DroneSimulation-Part3 - Overview

A simulation of a multiple drones which will fly in a three dimensional virtual testbed. The main object of the drones is to bring packages from one airport
to another as fast as possible. This (intermediary) project will demonstrate a __worst case example__ of the next project (_DroneSimulation-Part4_) of a 
series of projects.



# Run the Project

To run the project, go to 'Virtual Testbed' -> 'src' -> 'main' -> 'MainLoop' and run 'MainLoopStack' as a Java Application. To run the Stack-example within
the testbed do the following:  
* 'Run' -> 'Start Simulation' (the time will start running but all drones will stay steady since they do not have any packaging-requests yet)  
* Go to the 'Configure Path' menu  
* Press the 'Test Stack' button to auto-start the _stack example_

In this example it will be possible to go to the view of all the drones that are flying, this can be done using the _Configuration_ panel by selecting a
different drone (default drone is _Drone 0_). The camera view will be by default _Custom_ which means that you can change the view on the drone using your
mouse: scroll in and out to zoom, drag around in the screen to change viewing points on the drone, and press space-bar to release focus on the drone or
get the focus back to the drone.



# Virtual Testbed

The _virtual testbed_ will provide a clear representation of all the drones and their actions. Inside the GUI there are multiple windows which will add a 
corresponding functionality to the project. A quick overview of these windows are:  
* __File__ - Add a custom path to the testbed  
* __Settings__ - Change the drone or the testbed settings
* __Run__ - Manage the flow of the program (start, reset, run tests, ...)
* __Window__ - Change the drone-view
* __Inputs__ - Standard information and settings of the testbed and drone
* __Configuration__ - Change the drone you are currently watching and modify its configuration, it is possible to add new drones to the testbed
* __Configure Path__ - Configure a given or custom path to load in the testbed



# Autopilot

In this version (the third one) multiple drones will work together as a distributed system to deliver packages at the requested airport. The drones will
communicate with each other to make sure that they do not crash into each other. The drones will also communicate with the airports to know if they have
the permission to land. 



# Airport

The airports all have two gates where they can have a drone. It is not possible to have two drones at the same airport. The airports have the control to 
grant permissions to drones and distrubute drones that are within the airport to other airports to make place for other drones within this airport. You can
consider an airport as a part within the distributed system.



# Changing the AutoPilot

When changes are made within the _AutoPilot_, you have to export the whole _AutoPilot_ file as a jar and place this jar in the _Virtual Testbed_ file on the
following location: 'Virtual Testbed' -> 'lib' -> 'jar'. You __must__ name this jar 'autopilot.jar' otherwise the testbed will not recognize the jar. At the
moment there is no functionality to toggle between multiple jars, and thus it will not be possible to test or compare two or more autopilots at the same time.



# Usefullness of this Project

The only reason that this project was made was for testing and demonstration purposes. There are still some bugs in this version of the project, which will
not be fixed. For a (hopefully) bug-free project go to _DroneSimulation-Part4_ (https://github.com/RubenPants/DroneSimulation-Part4). This project will
demonstrate how the distributed system handels a worst case scenario as a whole. This _worst case scenario_ is the scenario where all the drones within the
virtual testbed try to deliver a package to one given (central placed) airport. This will create a __waiting stack__ at this airport. The airport will handle
the drones in a _First In First Out_ order.



# History of the Project

This project is the seccond part of a larger whole:
* __Part1__ - Fly in the testbed.  
Link: https://github.com/RubenPants/DroneSimulation-Part1  
* __Part2__ - Take off, fly, land and taxi in the testbed. Control drone with phone.  
Link: https://github.com/RubenPants/DroneSimulation-Part2  
* __Part3__ - Example of a worst case of the package-distributing-system of _Part4_ where all the drones try to land at the same airport.  
Link: https://github.com/RubenPants/DroneSimulation-Part3  
* __Part4__ - A package-distributing-system where multiple drones must work together to distributed packages in a virtual environment.  
Link: https://github.com/RubenPants/DroneSimulation-Part4  
