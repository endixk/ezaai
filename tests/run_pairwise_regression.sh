#!/bin/sh
# Usage: tests/run_pairwise_regression.sh path/to/EzAAI-jar-with-dependencies.jar
set -eu
if [ "$#" -ne 1 ] || [ ! -f "$1" ]; then
    echo "Usage: $0 path/to/EzAAI-jar-with-dependencies.jar" >&2
    exit 2
fi
jar_path=$(cd "$(dirname "$1")" && pwd)/$(basename "$1")
test_dir=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
build_dir=$(mktemp -d "$test_dir/.pairwise-test.XXXXXXXX")
trap 'rm -rf "$build_dir"' EXIT HUP INT TERM
javac -cp "$jar_path" -d "$build_dir" "$test_dir/PairwiseArrayRegression.java"
java -cp "$jar_path:$build_dir" PairwiseArrayRegression
