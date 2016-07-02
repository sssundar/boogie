import cherrypy

class HelloWorld(object):
    @cherrypy.expose
    def index(self):
        return "Hello world!"

if __name__ == '__main__':
	
	# Must be priviledged to bind port 80 (HTTP)
	cherrypy.config.update( {'server.socket_host': '0.0.0.0', 'server.socket_port': 80} )      	

   	cherrypy.quickstart(HelloWorld())