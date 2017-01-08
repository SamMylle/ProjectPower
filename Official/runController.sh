if [ "$#" -ne 2 ]; then
	echo "This script needs 2 arguments, namely the ip address and port for the controller."
	echo "For example: 192.168.1.10 5000"
	exit 1
else
	sh runJavaFile.sh controller.DistController "$1" "$1" "$2"
fi

exit 0

