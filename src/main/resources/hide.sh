#!/bin/bash

if [ $1 == "help" ]; then
	echo "Use \"hide file_to_hide(absolute or relative path) file_to_hide_inside_of(absolute or relative path)"
	echo "final file will be saved in same folder as file_to_hide"
	exit
fi

file=$(grealpath $1)
parentDir=$(dirname "$file")
pattern=$(basename "$file")
hideIntoFile=$(grealpath $2)

for f in $(ls "$parentDir" | egrep "$pattern")
do
	fullFilePath="$parentDir/$f"
	output=$(java -Djava.awt.headless=true -jar ~/Documents/steganographer_v1.jar encode "$hideIntoFile" "$fullFilePath" "$parentDir/$(date +%s%N).png" 2>&1)
	if [ $(echo "$output" | egrep -c "^success$") -eq 0 ]; 
	then
		echo "failed : $fullFilePath" 
		echo "command : $(echo java -Djava.awt.headless=true -jar ~/Documents/steganographer_v1.jar encode \"$hideIntoFile\" \"$fullFilePath\" \"$parentDir/$(date +%s%N).png\")"
		echo "output : $output"
	else
		echo "success : $fullFilePath"
	fi
done
