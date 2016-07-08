from PIL import Image
from time import sleep
import numpy as np
import sys
import urllib2, json
import time, datetime

# Samples images must be NxN to match what is hardcoded into barebones.py
N = 200

if __name__ == "__main__":
	# Generate RLE for the GIMP 'handwriting' samples of 0-5	
	# These images must be threshold to 0, 255 so they can be treated as binarized
	rles = []
	for countFile in ['./public/images/0.png', './public/images/1.png', './public/images/2.png', './public/images/3.png', './public/images/4.png', './public/images/5.png']:
		
		data = Image.open(countFile)			
		data = np.array(data.convert('L'))
		
		# Run length binning of Random Image as the worst case RLE size		
		rle = []
		firstFlag = True
		veryFirstColor = None
		pixels = 0
		for i in range(N):
			for j in range(N):
				if firstFlag:
					firstFlag = False
					veryFirstColor = 'b' if (data[i][j] == 0) else 'w'
					raw_color = data[i][j]
					count = 0

				if data[i][j] != raw_color:				
					rle.append(count)															
					raw_color = data[i][j]
					pixels += count
					count = 0

				count += 1

		rle.append(count)					
		pixels += count

		if pixels != N**2:			
			print "Error: pixel count incorrect."
			sys.exit(1)

		# Encode rle as firstColor,run,run,run,... as a string
		encoded = veryFirstColor + "," + ",".join([str(x) for x in rle])
		rles.append(encoded)		

	# for rle in rles:
	# 	print len(rle)	

	# Repeatedly send the rles strings to the server to test RLE decoding	
	
	# Port number is 80 by default with http://
	dns = "http://ec2-54-67-127-196.us-west-1.compute.amazonaws.com/"		
	# dns = "http://127.0.0.1:8080"

	count = 100
	while (count > 0):
		for rle in rles:
			j = json.dumps({'run_length_encoding': rle})			
			start_time = time.time()			
			req = urllib2.Request(dns, j, headers={'Content-Type': 'application/json'})
			RESULT = urllib2.urlopen(req).read()
			# print (RESULT)
			print "HTTP Post Delay: %0.3f s" % (time.time()-start_time)

		count -= 1

