#!/bin/bash
 
java -Dlog4j.debug -jar ./target/*-jar-with-dependencies.jar -f ./rhea.rdf -o out.txt -q query.txt

