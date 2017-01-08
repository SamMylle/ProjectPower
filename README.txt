Project Distributed Systems 3rd year bachelor
---------------------------------------------


1. Usage
--------

In order to run the controller or certain clients, you can load the project into eclipse and run the specific files (DistController, DistUser, DistSmartFridge, DistTemperatureSensor, DistLight, UserWindow).
The files need 3 vm arguments when ran, namely ip, clientip and controllerport. 
These have the following meaning:
	ip = the IP address on which the controller can be reached.
	clientip = the IP address on which the client should run (ignored when running the controller).
	controllerport = the Port on which the controller is running/should be ran.


Alternatively, there are a few shell scripts provided which will compile and run the controller/clients.
The arguments needed to run the scripts are the following:
 * sh runController 'ownIP' 'controllerPort' 
 * sh runUser 'ownIP' 'controllerIP' 'controllerPort'
 * sh runUserNoGUI 'ownIP' 'controllerIP' 'controllerPort'
 * sh runFridge 'ownIP' 'controllerIP' 'controllerPort'
 * sh runLight 'ownIP' 'controllerIP' 'controllerPort'
 * sh runTemperatureSensor 'ownIP' 'controllerIP' 'controllerPort'



2. Structure/Architecture
-------------------------

Everything is synchronous except for the following:
 * The Chang-Roberts algorithm.
 * The sending of backup data.
 * The notification the server sends to the user to tell them he is the new Controller.

Each component has a part that should work in a non-distributed environment. We split every component in a distributed part and a non-distributed part. So the controller has a class DistController (which works in a distributed environment) which inherits from Controller (which works in a non-distributed environment)



3. Final Remarks
----------------

* When disconnecting/closing a client, an IOException gets thrown somewhere within avro. We were unable to catch this exception anywhere, and have reported this to the assistant.
