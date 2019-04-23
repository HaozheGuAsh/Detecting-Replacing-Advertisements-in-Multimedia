echo -e "-------------------- Compiling For Player --------------------"

echo -e "---------- Compiling For VideoPlayer ----------"

javac -d . videoplayer/VideoPlayer.java

echo -e "---------- Compiling For AudioPlayer ----------"

javac -d . audioplayer/AudioPlayer.java

echo -e "---------- Compiling For MediaPlayer ----------"

javac MediaPlayer.java

echo -e "-------------------- Running Player --------------------"

java MediaPlayer dataset
