# Screen Sharing & Drawing/Voice Messaging CherryPy HTTP Server for Boogie-RevB
# Last Revised by Sushant Sundaresh on 2016-10-23

import glob, os, string, time, shutil
import cherrypy
from cherrypy.lib.static import serve_file

MAX_TIME_TRACKED = 2 # seconds

local_dir = os.path.dirname(__file__)
abs_dir = os.path.join(os.getcwd(), local_dir)
temp_storage = os.path.join(abs_dir, "temp_storage")	
ss_filename = "sharescreen.png"
ss_filepath = os.path.join(temp_storage, ss_filename)

# ==TODO== Always clear to silly data before committing to Github. 
# For Presence Logging, Authentication for GET/POST
# Format is 'username':[ACTIVE_USERS[character_index], password, storagePath, messageArray]
USERS = {	'sushant':[0,'password0',"",[]],
			'sudha':[1,'password1',"",[]],
			'mani':[2,'password2',"",[]] }

# Files in here must have .png or .3gp extensions
# Files in here must be named systemtimeinms_usersource.extension
def setup_storage_paths ():
	global temp_storage, ss_filename, ss_filepath
	# Setup User Message Remote Storage Directories
	for user in USERS.keys():
		storagePath = os.path.join(abs_dir, "%s_messages" % user)
		if not os.path.exists(storagePath):
			os.makedirs(storagePath)	
		USERS[user][2] = storagePath
	# Setup Temporary Write Storage	
	if not os.path.exists(temp_storage):
		os.makedirs(temp_storage)	

def setup_message_state ():
	# Check each user's message history (remote) and add it to their messageArray 
	# in order of timestamp (furthest from present = smallest index)	
	for user in USERS.keys():		
		files = []
		for f in os.listdir(USERS[user][2]):
			temp = f.split("_")			
			files.append([int(temp[0]), f]) # time in ms, timestamp_user.extension
		USERS[user][3] = [x[1] for x in sorted(files,key=lambda x: x[0])]

def authenticate(user, password):
	if user in USERS.keys():
		if password == USERS[user][1]:
			return True	
	return False		

def authenticate_by_filename (name):
	tokens = name.split("_")
	pwd = tokens[0]
	user = tokens[1].split(".")[0]
	return authenticate(user,pwd)	 

def validate_extensions (name):
	extension = name.split(".")[-1]
	return extension in ["3gp", "png"]

def update_presence(ping_flag, index_to_clear):		
	time_now = time.time()			
	seconds_elapsed = float(time_now - BoogieBackend.zero_time)
	BoogieBackend.zero_time = time_now;

	BoogieBackend.ACTIVE_USERS = [x + seconds_elapsed for x in BoogieBackend.ACTIVE_USERS]
	BoogieBackend.ACTIVE_USERS = [x if x <= MAX_TIME_TRACKED else MAX_TIME_TRACKED for x in BoogieBackend.ACTIVE_USERS]
	
	if ping_flag:
		BoogieBackend.ACTIVE_USERS[index_to_clear] = 0

class BoogieBackend(object):
		
	# Seconds since last presence ping
	# Initially no one is present (all MAX_TIME_TRACKED seconds past a ping)	
	ACTIVE_USERS = [MAX_TIME_TRACKED, MAX_TIME_TRACKED, MAX_TIME_TRACKED] 
	zero_time = time.time()

	@cherrypy.expose
	def index(self):
		return "Boogie Rev B Server. Please, don't try to break this. It is a backend server for an app that is helping my grandfather, who can't speak, draw messages to us."

	@cherrypy.expose
	def check_mailbox(self, user="", password=""):
		if authenticate(user,password):
			if len(USERS[user][3]) > 0:
				return USERS[user][3][0] # first unread message in user mailbox		
			else:
				return "empty"

	@cherrypy.expose
	def presence(self, user="", password=""):
		if authenticate(user,password):			
			update_presence(True, USERS[user][0])
			return "%d" % sum([x < MAX_TIME_TRACKED for x in BoogieBackend.ACTIVE_USERS])
		return "0"

	@cherrypy.expose
	def upload(self, myFile):		
		if authenticate_by_filename(myFile.filename) and validate_extensions(myFile.filename):						
			temp_filename = "%d_%s" % (int(time.time() * 1000),myFile.filename.split("_")[1])
			temp_filepath = os.path.join(temp_storage, temp_filename)
			with open(temp_filepath, 'wb') as f:
				f.write(myFile.file.read())					
			for user in USERS.keys():
				shutil.copy(temp_filepath, os.path.join(USERS[user][2], temp_filename))				
				USERS[user][3].append(temp_filename)
			os.unlink(temp_filepath)

	def cleanup_served_file(self):
		os.unlink(cherrypy.request.cleanuppath)				
		return

	@cherrypy.expose
	def download(self, user="", password="", fname=""):			
		if authenticate(user,password):
			if len(USERS[user][3]) > 0:
				if fname == USERS[user][3][0]:	
					cherrypy.request.cleanuppath = os.path.join(USERS[user][2], fname)
					cherrypy.request.hooks.attach('on_end_request', self.cleanup_served_file)
					USERS[user][3].pop(0)
					return serve_file(os.path.join(USERS[user][2], fname))
	
	@cherrypy.expose()       
	def sharescreen(self, myFile):			
		if authenticate_by_filename(myFile.filename):			
			with open(ss_filepath, 'wb') as f:
				f.write(myFile.file.read())			

	@cherrypy.expose()
	def grabscreen(self, user="", password=""):
		if authenticate(user,password):
			return serve_file(ss_filepath)

if __name__ == '__main__':
	setup_storage_paths()
	setup_message_state()

	# Locally, for testing
	# cherrypy.config.update( {'server.socket_host': '127.0.0.1', 'server.socket_port': 8080} )      	

	# Remotely, for release
	cherrypy.config.update( {'server.socket_host': '0.0.0.0', 'server.socket_port': 8080} )   
	
	cherrypy.quickstart(BoogieBackend())

