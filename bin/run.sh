#!/bin/bash

if [ -z "$1" ] ; then
    echo "Usage: $0 libs-folder"
    echo "Where libs-folder: "
    ls -1 ast/libs/
    exit 1
fi
libs_folder="ast/libs/$1"

[ -d ast/classes ] || mkdir ast/classes
javac -cp "${libs_folder}/*" -d ast/classes ast/src/AstMain.java && \
java -cp "ast/classes:${libs_folder}/*" AstMain
