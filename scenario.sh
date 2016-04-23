#!/bin/bash
java Node 0 1 "message from 0" 50 &
java Node 1 1 &
java Node 2 2 &
java Node 3 2 "message from 3" 50 &
java Node 4 4 &
java Node 6 6 &
java Node 7 7 &
java Node 8 8 &
java Node 9 2 "message from 9" 25 &
java Controller &