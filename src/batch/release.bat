cd target
pkzip25 -add %1 *.exe
pkzip25 -add %1 ..\src\main\resources\*.cfg
copy %1-jar-with-dependencies.jar %1.jar
pkzip25 -add %1 %1.jar
cd ..
