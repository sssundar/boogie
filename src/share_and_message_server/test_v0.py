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
	shareserver = "http://127.0.0.1:8080/sharescreen"	
	grabserver = "http://127.0.0.1:8080/grabscreen"	
	head = {'Content-Type' : 'application/json'}
	user = 'sushant'
	password = 'password0'
	s = requests.Session()

	# Begin Tests	

	# Post Request followed by Get RLE request	
	r = s.post(shareserver, data=json.dumps({'user': user, 'password':password, 'run_length_encoding':rles[1]}), headers=head)		
	r = s.get(grabserver, params={'user': user,'password':password})		
	print r.text
	print "Test 7: %s" % ('passed' if r.text == rles[1] else 'failed')

	sys.exit(0)