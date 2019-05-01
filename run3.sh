#!/usr/bin/env bash

Green_font_prefix="\033[32m" && Red_font_prefix="\033[31m" && Green_background_prefix="\033[42;37m" && Red_background_prefix="\033[41;37m" && Font_color_suffix="\033[0m"
name=$(whoami)
Info="${Green_font_prefix}[INFO]${Font_color_suffix}"
Error="${Red_font_prefix}[ERROR]${Font_color_suffix}"
Tip="${Green_font_prefix}[ATTENTION]${Font_color_suffix}"

filepath=$(cd "$(dirname "$0")"; pwd)
echo -e "${Info} Project RunAll Script Start"
echo -e "${Info} Running AdRemoval to Remove Current Advertisement"

echo -e "${Green_font_prefix}--------------------------------------------------------------------------------${Font_color_suffix}"

cd Java/AdRemoval/src/
bash run3.sh
cd "${filepath}"

echo -e "${Green_font_prefix}--------------------------------------------------------------------------------${Font_color_suffix}"

echo -e "${Info} Running AdInsertion to Add Matched Advertisement"

echo -e "${Green_font_prefix}--------------------------------------------------------------------------------${Font_color_suffix}"

cd Java/AdInsertion/src/
bash run3.sh
cd "${filepath}"

echo -e "${Green_font_prefix}--------------------------------------------------------------------------------${Font_color_suffix}"

echo -e "${Info} Running MediaPlayer to Show Result"

echo -e "${Green_font_prefix}--------------------------------------------------------------------------------${Font_color_suffix}"

cd Java/MediaPlayer/src/
bash run3.sh
cd "${filepath}"

echo -e "${Green_font_prefix}--------------------------------------------------------------------------------${Font_color_suffix}"


echo -e "${Info} Finish Project Live Demo"