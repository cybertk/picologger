#!/bin/sh

./sendlog.py localhost 20504 1 1 2 12 tag0 content0
./sendlog.py localhost 20504 1 1 2 12 tag0 content1
./sendlog.py localhost 20504 1 1 2 12 tag0 content2
./sendlog.py localhost 20504 1 1 2 12 tag1 content0
./sendlog.py localhost 20504 1 1 2 12 tag2 content0
./sendlog.py localhost 20504 1 1 2 12 tag3 content0
