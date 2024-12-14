#!/bin/bash

cd ..

mkdir -p out

gcc -Wall -g -c linked_list.c
gcc -Wall -g -o out/linked_list linked_list.o