#README

To run the program:
 1. type "make" to compile the program
 2. open three terminal windows, one for the sender, one for the receiver, and another for the network emulator
	- on the emulator window: type "./nEmulator-linux386 port#1 host2 port#2 port#3 host3 port#4 1 0.2 0"
	- on the receiver window: type "java receiver host1 port#3 port#2 <output file>" 
	- on the sender window: type "java sender host1 port#1 port#4 <input file>" 


Undergrad machine the program was built and tested on:
	- ubuntu1604-008


Compilers I am using:
	- javac10.0.2  
	