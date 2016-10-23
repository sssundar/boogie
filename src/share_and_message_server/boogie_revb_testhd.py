# Testing Higher Resolution Screen Sharing - Boogie Rev B v0.1
# Last Revised by Sushant Sundaresh on 2016-10-18

import os
import cherrypy

local_dir = os.path.dirname(__file__)
abs_dir = os.path.join(os.getcwd(), local_dir)

SCREENSHARE_PATH = './public/images/hd_screenshare.png'
STATIC_PATH = '/static/images/hd_screenshare.png'

class BoogieBackend(object):		
	@cherrypy.expose
	def index(self):
		counter = """<img src=\"""" + STATIC_PATH + """\">""" if os.path.isfile(SCREENSHARE_PATH) else """<body> Nothing as yet... </body>"""
		index = """<html> <meta http-equiv="refresh" content="0.2">""" + counter + """ </html>"""
		return index

	@cherrypy.expose
	def upload(self, myFile):								
		with open(SCREENSHARE_PATH, 'wb') as f:
			f.write(myFile.file.read())										

if __name__ == '__main__':	
	conf = {
		'/': { 
			'tools.staticdir.root': os.path.abspath(os.getcwd())
		},
		'/static': {
			'tools.staticdir.on': True,
			'tools.staticdir.dir': './public'
		}
	}	

	cherrypy.config.update( {'server.socket_host': '0.0.0.0', 'server.socket_port': 9080} )   
	cherrypy.quickstart(BoogieBackend(), '/', conf)