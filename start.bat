@echo off
title MediVisit
echo ===============================================
echo  MediVisit - system rejestracji wizyt
echo  Po uruchomieniu otworz: http://localhost:8080
echo ===============================================
echo.

if exist medivisit-1.0.0.jar (
    java -jar medivisit-1.0.0.jar
) else if exist target\medivisit-1.0.0.jar (
    java -jar target\medivisit-1.0.0.jar
) else (
    echo Nie znaleziono pliku medivisit-1.0.0.jar.
    echo Zbuduj projekt poleceniem: mvn package
)

echo.
pause
