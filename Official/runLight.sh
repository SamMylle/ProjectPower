if [ "$#" -ne 3 ]; then
	echo "This script needs 3 arguments, namely your own ip address, as well as the ip address and port of the controller."
	echo "For example: 192.168.1.10 192.168.1.10 5000"
	exit 1
else
	sh runJavaFile.sh client.DistLight "$1" "$2" "$3"
fi

exit 0

