import os, os.path
import string
import cherrypy
from PIL import Image

# Hard Coded Image Size Required (pixel width)
N = 200
MAX_PIXELS = N**2
PATH = './public/images/counter.png'
STATIC_PATH = '/static/images/counter.png'

# Takes a text string intended for RLE decoding and santizes it
# Interprets the data as a character array of [0-128 [0,1]] for a BW image in 
# run length encoding. If the pixel size adds up to N**2, returns True
# and saves the encoding as a png locally in ./public/images/counter.png.
# Otherwise returns false and doesn't change counter.png
def rle_decode (rle_string):
	if len(rle_string) > 0:
		rle_data = rle_string.split(",")
		starting_color = rle_data[0]
		rle_data = [int(x) for x in rle_data[1::]]
		
		img = Image.new('1', (N, N))
		pixels = img.load()
		
		pixel_count = 0
		row = 0
		col = 0
		color = 0 if (starting_color == 'b') else 1

		for run in rle_data:
			
			pixel_count += run

			if pixel_count > MAX_PIXELS:
				return False

			# Expand out the run
			for m in xrange(run):
				# Image appears transposed otherwise
				pixels[col, row] = color 

				col += 1
				if col >= N:
					col = 0
					row += 1

			# Toggle Color
			color += 1
			color = color % 2			

		if pixel_count < MAX_PIXELS:
			return FALSE

		img.save(PATH, "PNG")

	else:
		return False

	return True

class RLEDecoderWebService(object):
	exposed = True

	def GET(self):		
		counter = """<img src=\"""" + STATIC_PATH + """\">""" if os.path.isfile(PATH) else """<body> Nothing as yet... </body>"""
		index = """<html> <meta http-equiv="refresh" content="0.2">""" + counter + """ </html>"""
		return index

	# Expects a JSON input with key 'run_length_encoding'
	@cherrypy.tools.json_in()    
	def POST(self):
		if rle_decode(cherrypy.request.json['run_length_encoding']):
			return "Data accepted"
		return "Data rejected"		

if __name__ == '__main__':
	conf = {
		'/': { 
			'request.dispatch': cherrypy.dispatch.MethodDispatcher(),
			'tools.staticdir.root': os.path.abspath(os.getcwd())
		},
		'/static': {
			'tools.staticdir.on': True,
			'tools.staticdir.dir': './public'
		}
	}	
	
	# Locally: 
	#cherrypy.config.update( {'server.socket_host': '127.0.0.1', 'server.socket_port': 8080} )      	

	# Remotely: 
	# Must be priviledged to bind port 80
	cherrypy.config.update( {'server.socket_host': '0.0.0.0', 'server.socket_port': 80} )      	

	cherrypy.quickstart(RLEDecoderWebService(), '/', conf)

