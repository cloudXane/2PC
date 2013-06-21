#!/bin/sh

echo "build util..."
javac util/*.java

echo "build client..."
javac client/*.java

echo "build server..."
javac server/*.java

echo "build dbmgr..."
javac dbmgr/*.java

