echo -e "-------------------- Compiling For AdInsertion --------------------"

javac AudioInsertor.java
javac VideoInsertor.java
javac AdInsertion.java
echo -e "-------------------- Running AdInsertion --------------------"

java AdInsertion dataset3 
