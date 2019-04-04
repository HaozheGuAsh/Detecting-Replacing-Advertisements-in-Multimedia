#!/bin/bash

datafiles=(ReadMe.txt dataset.zip dataset2.zip)
datafileIDs=(1hvV7I2yCkJmJyLifSh-dpUadqZ4cV9P2 1KJe4eE8lhQAzh_5FODOpTPEtMsjcF_w0 1DJzPOHv4LQPy2vvPxCRgfMtRRvIZZ4-a)

for index in ${!datafiles[*]}; do
  bash gdl.sh ${datafileIDs[$index]} ${datafiles[$index]}
done

unzip dataset.zip
unzip dataset2.zip

for index in ${!datafiles[*]}; do
  rm -f ${datafiles[$index]}
done

rm -f cookie
