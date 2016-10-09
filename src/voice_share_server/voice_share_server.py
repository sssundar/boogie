import glob
import os

import cherrypy
from cherrypy.lib.static import serve_file

local_dir = os.path.dirname(__file__)
abs_dir = os.path.join(os.getcwd(), local_dir)
storagePath = ""

class HearMyVoice(object):
	
	latest_message_filename = ""

	@cherrypy.expose
	def index(self):
		return ""

		# """
		# <html>
		# <body>
		# 	<h2>Upload a message</h2>

		# 	<form action="upload" method="post" enctype="multipart/form-data">
			
		# 		filename: <input type="file" name="myFile" /> <br />
				
		# 		<input type="submit" />
				
		# 	</form>

		# 	<br\>

		# 	<a href="/download/?name=test.3gp"> test.3gp </a> <br />
		# </body>
		# </html>
		# """

	@cherrypy.expose
	def check_mailbox(self):
		return HearMyVoice.latest_message_filename

	@cherrypy.expose
	def upload(self, myFile):
		if myFile.filename.split(".")[-1] == "3gp":			
			HearMyVoice.latest_message_filename = myFile.filename
			with open(os.path.join(storagePath, myFile.filename), 'wb') as f:
				f.write(myFile.file.read())
			return "Success"
		return "Failure"

	@cherrypy.expose
	def download(self, name):			
		return serve_file(os.path.join(storagePath, name))


if __name__ == '__main__':
	# Setup Temporary Storage Directory
	storagePath = os.path.join(abs_dir, "boogie_voice_demo_messages")
	if not os.path.exists(storagePath):
		os.makedirs(storagePath)	

	# Locally, for testing
	# cherrypy.config.update( {'server.socket_host': '127.0.0.1', 'server.socket_port': 8080} )      	

	# Remotely, for release
	cherrypy.config.update( {'server.socket_host': '0.0.0.0', 'server.socket_port': 8080} )   
	
	cherrypy.quickstart(HearMyVoice())

