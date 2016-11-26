if [ "$#" -ne 1 ]; then
	echo "Please give the .apvr file as an argument."
else
	cd lib
	echo "Searching for file in directory named apvr..."
	echo "Using the avro 1.7.7 tool in directory named lib..."
	java -jar ./avro-tools-1.7.7.jar compile protocol ../avpr/"$1" ../src
fi



