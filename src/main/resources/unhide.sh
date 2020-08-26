#!/bin/bash

if [ $1 == "help" ]; then
        echo "Arguements are : file_to_unhide directory_path_to_extract_unhidden_file"
        exit
fi
file=$(grealpath $1)
parentDir=$(dirname "$file")
pattern=$(basename "$file")
targetDir=$(grealpath $2)
        
for f in $(ls "$parentDir" | egrep "$pattern")
do
	output=$(java -Djava.awt.headless=true -jar ~/Documents/steganographer_v1.jar decode "$f" "$targetDir" 2>&1)
	if [ $(echo "$output" | egrep -c "^success$") -eq 0 ];      
	then
		echo "failed : $f"
		echo "command : $(echo java -Djava.awt.headless=true -jar ~/Documents/steganographer_v1.jar decode \"$f\" \"$targetDir\")"
		echo "output : $output"
	else
		echo "success : $f"
	fi
done
