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
	server = "http://ec2-54-183-116-184.us-west-1.compute.amazonaws.com:8080"	
	# server = "http://127.0.0.1:8080"	
	head = {'Content-Type' : 'application/json'}	
	ext = ['4000', '4001', '4002', '4003']
	password = ['password1', 'password2', 'password3', 'password4']

	s = requests.Session()

	# Begin Tests
	
	# Get Request for Presence without Authentication	
	r = s.get(server, params={'extension': 'bobsyouruncle', 'password':'saywhatnow', 'presence':'yes'})	
	print r.text
	print "Test 1: %s" % ('passed' if r.text == 'Request Rejected' else 'failed')

	# Get Request for Presence with Authentication 		
	r = s.get(server, params={'extension': ext[0],'password':password[0], 'presence':'yes'})			
	print r.text
	print "Test 2: %s" % ('passed' if r.text == '0000' else 'failed')

	# Get Request for RLE without posting any RLE, on a fresh server boot (should be blank)	
	r = s.get(server, params={'extension': ext[0],'password':password[0], 'presence':'no'})			
	print r.text
	print "Test 3: %s" % ('passed' if r.text == 'w,%d' % N**2 else 'failed')

	# Put Request followed by immediate Get Request for Presence 		
	r = s.put(server, data=json.dumps({'extension': ext[0], 'password':password[0]}), headers=head)	
	r = s.get(server, params={'extension': ext[0],'password':password[0], 'presence':'yes'})		
	print r.text
	print "Test 4: %s" % ('passed' if r.text == '1000' else 'failed')

	sleep(2)

	# Put Request for a different user after a few seconds, followed by Presence Poll	
	r = s.put(server, data=json.dumps({'extension': ext[1], 'password':password[1]}), headers=head)	
	r = s.get(server, params={'extension': ext[1],'password':password[1], 'presence':'yes'})		
	print r.text
	print "Test 5: %s" % ('passed' if r.text == '1100' else 'failed')

	sleep (35)

	# Put Request for a third user after 30 seconds, followed by a Presence Poll	
	r = s.put(server, data=json.dumps({'extension': ext[2], 'password':password[2]}), headers=head)	
	r = s.get(server, params={'extension': ext[2],'password':password[2], 'presence':'yes'})		
	print r.text
	print "Test 6: %s" % ('passed' if r.text == '0010' else 'failed')

	# Post Request followed by Get RLE request	
	r = s.post(server, data=json.dumps({'extension': ext[2], 'password':password[2], 'run_length_encoding':rles[1]}), headers=head)		
	r = s.get(server, params={'extension': ext[0],'password':password[0], 'presence':'no'})		
	print r.text
	print "Test 7: %s" % ('passed' if r.text == rles[1] else 'failed')

	sys.exit(0)