from time import sleep
import sys
import requests, json

# Reference: 
# http://docs.python-requests.org/en/master/user/quickstart/
# http://docs.python-requests.org/en/master/api/?highlight=get#requests.get

if __name__ == "__main__":

	# A few valid RLEs
	N = 200
	rles = []
	rles.append("b,%d" % N**2)
	rles.append("w,%d,%d" % (N**2-10,10))

	# Server IP Address (localhost, remote)
	# server = "http://ec2-54-183-116-184.us-west-1.compute.amazonaws.com:8080"	
	server = "http://127.0.0.1:8080"	

	head = {'Content-Type' : 'application/json'}	
	ext = ['4000', '4001', '4002', '4003']
	password = ['password1', 'password2', 'password3', 'password4']

	s = requests.Session()

	# Begin Tests
	
	# Get Request for Presence without Authentication	
	r = s.get(server, params={'extension': 'bobsyouruncle', 'password':'saywhatnow', 'presence':'yes', 'callquality' : '0', 'with_extension':'4001'})	
	print r.text
	print "Test 1: %s" % ('passed' if r.text == 'Request Rejected' else 'failed')

	# Get Request for Presence with Authentication and suggest a fictitious call with myself has bad quality
	r = s.get(server, params={'extension': ext[0],'password':password[0], 'presence':'yes', 'callquality' : '1', 'with_extension':'4000'})			
	print r.text
	print "Test 2: %s" % ('passed' if r.text == '10001' else 'failed')

	# Get Request for RLE without posting any RLE, on a fresh server boot (should be blank)	
	r = s.get(server, params={'extension': ext[0],'password':password[0], 'presence':'no', 'callquality' : '0', 'with_extension' : 'XXXX'})
	print r.text
	print "Test 3: %s" % ('passed' if r.text == 'w,%d' % N**2 else 'failed')

	# Request/Presence without a call issue flagged
	r = s.get(server, params={'extension': ext[1],'password':password[1], 'presence':'yes', 'callquality':'0'})		
	print r.text
	print "Test 4: %s" % ('passed' if r.text == '11000' else 'failed')

	sleep(2)

	# Put Request for a different user after a few seconds, with bad call quality flagged for user 3
	r = s.get(server, params={'extension': ext[2],'password':password[2], 'presence':'yes', 'callquality':'1', 'with_extension':'4003'})		
	print r.text
	print "Test 5: %s" % ('passed' if r.text == '11100' else 'failed')

	sleep (35)

	# Put Request for a third user after 30 seconds, followed by a Presence Poll	
	r = s.get(server, params={'extension': ext[3],'password':password[3], 'presence':'yes','callquality':'0'})		
	print r.text
	print "Test 6: %s" % ('passed' if r.text == '00011' else 'failed')

	# Post Request followed by Get RLE request	
	r = s.post(server, data=json.dumps({'extension': ext[2], 'password':password[2], 'run_length_encoding':rles[1]}), headers=head)		
	r = s.get(server, params={'extension': ext[0],'password':password[0], 'presence':'no'})		
	print r.text
	print "Test 7: %s" % ('passed' if r.text == rles[1] else 'failed')

	sys.exit(0)