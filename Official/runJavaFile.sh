if [ "$#" -ne 4 ]; then
	echo "Enter an argument with a file (including its package(s)) that you want to run, aswell as the client IP address, the server IP address and the server port."
	echo "For example: client.DistUser 192.168.1.10 192.168.1.11 5000"
	exit 1
else
	stringClass="$1"
	stringPath=$(echo $stringClass | sed 's/\./\//g')
	echo "Compiling the file..."
	javac -d bin -sourcepath src -cp bin:lib/avro-1.7.7.jar:lib/avro-ipc-1.7.7.jar:lib/hamcrest-core-1.3.jar:lib/jackson-core-asl-1.9.13.jar:lib/jackson-mapper-asl-1.9.13.jar:lib/junit-4.12.jar:lib/slf4j-api-1.7.7.jar:lib/slf4j-simple-1.7.7.jar "src/$stringPath.java" > /dev/null 2>&1
	
	if [ "$?" -ne 0 ]; then
		echo ""
		echo "There are errors in the file you are trying to compile."
		echo "Cannot run the executable."
		exit 1
	fi

	echo "Running the file..."
	echo ""

	java -Dcontrollerport="$4" -Dip="$3" -Dclientip="$2" -cp bin:lib/avro-1.7.7.jar:lib/avro-ipc-1.7.7.jar:lib/hamcrest-core-1.3.jar:lib/jackson-core-asl-1.9.13.jar:lib/jackson-mapper-asl-1.9.13.jar:lib/junit-4.12.jar:lib/slf4j-api-1.7.7.jar:lib/slf4j-nop-1.7.7.jar "$1"
fi

exit 0

