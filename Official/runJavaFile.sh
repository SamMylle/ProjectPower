# To all the poor bastards who have no better option than to use this script, I feel your pain.

if [ "$#" -ne 1 ]; then
	echo "Enter an argument with a file (including its package(s)) that you want to run."
	echo "For example: client.DistUser"
else
	java -cp src:lib/avro-1.7.7.jar:lib/avro-ipc-1.7.7.jar:lib/hamcrest-core-1.3.jar:lib/jackson-core-asl-1.9.13.jar:lib/jackson-mapper-asl-1.9.13.jar:lib/junit-4.12.jar:lib/slf4j-api-1.7.7.jar:lib/slf4j-simple-1.7.7.jar "$1"
	# java -cp src:lib/avro-1.7.7.jar:lib/avro-ipc-1.7.7.jar:./lib/hamcrest-core-1.3.jar:./lib/jackson-core-asl-1.9.13.jar:./lib/jackson-mapper-asl-1.9.13.jar:./lib/junit-4.12.jar:./lib/slf4j-api-1.7.7.jar:./lib/slf4j-simple-1.7.7.jar "$1"
fi



