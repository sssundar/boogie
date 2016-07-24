import cherrypy, string, time

# Hard Coded Image Size Required (pixel width)
N = 200 # pixels
MAX_PIXELS = N**2
MAX_TIME_TRACKED = 35 # seconds

# ==TODO== Always clear to silly data before committing to Github. 
# For Presence Logging, Authentication for Get/POST	
# Format is 'extension':[username, ACTIVE_USERS[character_index], password]
USERS = {	'4000':['sush',0,'password1'],
			'4001':['mani',1,'password2'], 
			'4002':['amma',2,'password3'], 
			'4003':['balu',3,'password4'] }

# Takes a text string intended for RLE decoding and sanity checks it.
# If the pixel size (init color, run, run, run, ...) adds up to N**2,
# saves the encoding as a string in a local variable and returns True
# Otherwise returns False and doesn't change the text string. 
def rle_decode (rle_string):
	if len(rle_string) > 0:
		pixel_count = sum([int(x) for x in rle_string.split(",")[1::]])
		if pixel_count != MAX_PIXELS:			
			return False					
		RLEDecoderWebService.latest_screen_rle = rle_string
		return True	
	return False	

def authenticate(extension, password):
	if extension in USERS.keys():
		if password == USERS[extension][2]:
			return True	
	return False						

def update_presence(ping_flag, index_to_clear):		
	time_now = time.time()			
	seconds_elapsed = int(time_now - RLEDecoderWebService.zero_time)
	RLEDecoderWebService.zero_time = time_now;

	RLEDecoderWebService.ACTIVE_USERS = [x + seconds_elapsed for x in RLEDecoderWebService.ACTIVE_USERS]
	RLEDecoderWebService.ACTIVE_USERS = [x if x <= MAX_TIME_TRACKED else MAX_TIME_TRACKED for x in RLEDecoderWebService.ACTIVE_USERS]
	
	if ping_flag:
		RLEDecoderWebService.ACTIVE_USERS[index_to_clear] = 0

class RLEDecoderWebService(object):	
	exposed = True		

	# Seconds since last presence ping
	# Initially no one is present (all MAX_TIME_TRACKED seconds past a ping)
	ACTIVE_USERS = [MAX_TIME_TRACKED, MAX_TIME_TRACKED, MAX_TIME_TRACKED, MAX_TIME_TRACKED] 
	zero_time = time.time()

	# Run Length Encoding of Latest Screen Share
	latest_screen_rle = "w,%d" % MAX_PIXELS # initially

	# Expects a JSON input with key 'extension', and key 'password'.
	# Expects a JSON input with key 'presence' 			
	@cherrypy.tools.json_in()
	def GET(self, extension="", password="", presence=""):		
		if authenticate(extension, password):					
			if presence == 'yes':
				# Return a 4 character string based on which users have been
				# active in the last 30 seconds
				update_presence(False, 0)
				return "".join(['1' if x <= 30 else '0' for x in RLEDecoderWebService.ACTIVE_USERS])
			else:
				return RLEDecoderWebService.latest_screen_rle
		else:
			return "Request Rejected"

	# Presence Logging - simply calling this function with no data or arguments
	# from a client will reset their presence counter
	# Expects a JSON input with key 'extension', and key 'password'.
	@cherrypy.tools.json_in()        
	def PUT(self):				
		extension = cherrypy.request.json['extension']
		password = cherrypy.request.json['password']		
		if authenticate(extension, password):		
			index = USERS[extension][1]	
			update_presence(True, index)			

	# Expects a JSON input with key 'run_length_encoding'	
	# Expects a JSON input with key 'extension', and key 'password'.	
	@cherrypy.tools.json_in()        
	def POST(self):			
		extension = cherrypy.request.json['extension']
		password = cherrypy.request.json['password']		
		if authenticate(extension, password):		
			if rle_decode(cherrypy.request.json['run_length_encoding']):
				return "Request Accepted"
		return "Request Rejected"

if __name__ == '__main__':
	conf = {
		'/': { 
			'request.dispatch': cherrypy.dispatch.MethodDispatcher(),
			'tools.response_headers.on': True,
            'tools.response_headers.headers': [('Content-Type', 'text/plain')]
		}
	}	

	# Locally, for testing
	# cherrypy.config.update( {'server.socket_host': '127.0.0.1', 'server.socket_port': 8080} )      	

	# Remotely, for release
	cherrypy.config.update( {'server.socket_host': '0.0.0.0', 'server.socket_port': 8080} )      		

	cherrypy.quickstart(RLEDecoderWebService(), '/', conf)

