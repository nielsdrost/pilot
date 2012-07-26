#!/bin/sh

# Rotates a image file.
# Takes between 0.1 and 2 seconds depending on image size.

export MAGICK_THREAD_LIMIT=1
convert $1 -rotate 90 $2
