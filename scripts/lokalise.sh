#To run this file, please go to terminal under /<Project Folder>/scripts then run the following command.
# sh lokalise.sh <localise token, pls find this in 1password>
LOKALISE_TOKEN=$1

mkdir -p "../temp"

#Download localised value as per the Android supported format
# --format xml : download in xml formats
#--bundle-structure "values-%LANG_ISO%/strings.xml : Downloads folder as per Android format, for example: values-it-rIT
#--original-filenames=false : Get Localised strings in a single file "strings.xml"
#--filter-data translated : Get only translated strings, if it is not transalted then it will refer to the default English
#--dest ../temp : Destination folder
#--unzip-to ../temp/locales : Unzip to this folder
lokalise2 --token "$LOKALISE_TOKEN" --project-id  99670955616fccfa149649.92277036 file download --format xml --bundle-structure "values-%LANG_ISO%/strings.xml" --original-filenames=false --filter-data translated --dest ../temp --unzip-to ../temp/locales

#It will replace only the strings.xml in each directory
cp -Rv "../temp/locales/" "../app/src/main/res/"

rm -r ../temp