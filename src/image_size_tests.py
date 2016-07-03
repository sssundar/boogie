# Image Size Tests in Python for Boogie Rev A
# Please run this script from ~/Documents/boogie/src
# Last Revised by Sushant Sundaresh on 2 July 2016

# References
# 1. http://stackoverflow.com/a/32106188
# 2. http://stackoverflow.com/a/11739289

from PIL import Image
from time import sleep
import pylab as pl
import matplotlib.cm as cm
import numpy as np
import random

if __name__ == "__main__":
	N = 200

	# For saving images
	# img = Image.new('1', (N, N))
	# pixels = img.load()

	# Random Image Define
	# data = [random.choice((0, 1)) for _ in range(N**2)]
	# data[:] = [data[i:i + N] for i in range(0, N**2, N)]

	# Test RLE of Handwriting
	data = Image.open('test_gimp_bw_handwriting_rle.png')	
	data = np.array(data.convert('L'))
	
	# Random Image Render
	# for i in range(img.size[0]):
	# 	for j in range(img.size[1]):
	# 		pixels[i, j] = data[i][j]
	# img.show()

	# # Snake Image Render
	# # Signal a termination when you're happy it looks like a snake 
	# # and that your computer at least can render this fast enough
	# # to give the illusion of motion
	# for i in range(img.size[0]):
	# 	for j in range(img.size[1]):
	# 		pixels[i, j] = 1
	# 		img.save('test.bmp')
	# 		sleep(0.2)

	# Run length encoding of Random Image as the worst case RLE size
	# Encode as bytes (0-128 + 0/1). But here, just simulate this with
	# an array of arrays [[len, color], ...] and get the actual byte size 
	# in post-processing
	RLE = []
	firstFlag = True
	for i in range(N):
		for j in range(N):
			if firstFlag:
				firstFlag = False
				color = data[i][j]
				count = 0

			if data[i][j] != color:				
				RLE.append([count,color])
				color = data[i][j]
				count = 0

			count += 1

	print len(RLE)