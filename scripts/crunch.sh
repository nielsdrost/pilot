#!/bin/sh

# Blurs an image file.
# Takes anywhere from 3 to 90 sec. depending on image size.

FILENAME=`basename $1`

mkdir -p $2

export MAGICK_THREAD_LIMIT=1
convert $1 -adaptive-blur 10% $2/out-$FILENAME
