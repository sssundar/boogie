# Boogie
## Revision B.2

Overview --  
This is an application to help my grandfather communicate with our family via screen-shared doodles. To understand some of the design decisions, it's helpful to note that he cannot speak, has little experience with modern electronics, and has low bandwidth internet. When he's physically with us, he uses a product called a "Boogie Board" to write out what he's thinking, hence the name. 

Release Notes --  
This project started on 29 June 2016.  
On 29 July 2016, our first alpha build was canceled due to stability issues with background threads.  
On 7 October 2016, we released a stable alpha build privately to three clients.  
On 23 October 2016, we released the beta build vB.1 privately to three clients.  
On 30 October 2016, we released the beta build vB.2 privately to six clients.  

Acknowledgements --  
This project has involved at some point or another most of my family. My grandmother has, in particular, risen above and beyond to learn about modern cell phones so she can support my grandfather. Especially to my mother, father, and sister: thank you.

Hardware --  
The grandfather-facing end of this project required about $200 of equipment. We are using a Nexus 7 (2013) running Marshmallow, a pair of decent over-ear headphones (to accomodate hearing aids), and a few soft tipped capacitive styluses. My mother and I are running Marshmallow on Google and Huawei smartphones. notebook_rev_a/ has the details, but the specifics really don't matter.

What's happening inside --  
Grandfather doodles and receives voice messages, and we record voice messages and receive doodles
Credentials are baked in for everyone to keep UI dead simple  
Numeric indicator of internet connectivity & online users  
Two-button UI  
: First button type records audio (us) & saves-and-clears-screen (grandfather)  
: Second button type plays back screen shares and audio  
High Priority Thread  
: Smoothly screen shares at perfectly legible resolution between US and India on a 56kbps connection  
: My grandfather's build sends data only on new draw events  
: The receivers (us) have builds that poll at a continuous 1-2 Hz  
Low Priority Thread  
: Naive presence polling  
: Mailbox check, message downloading, and local message management  
UI Thread  
: Drawable doodle area  
: Synchronization framework for the drawable area between all three threads  
Backend CherryPy HTTP server on an Amazon EC2 instance  
: Handles simple authentication and message distribution  
: Handles guest access (view-only, no audio or message history) for Chrome and Safari browsers  

Limitations --  
: Currently only three mobile users are supported; this is hardcoded. Other users only have guest access.  
: Memory usage could be a lot lighter. I heavily prioritized stability, update frequency, and feature exploration over scalability or power management in our alpha and beta releases, as this was never intended to reach beyond my family.  
: The beta hits every feature target we set, and is bulletproof with respect to button mashing, pause/resume thread control, and so on. Unfortunately, people who want to branch off this repo are going to have to tweak the UI heavily for their own needs.  

Source --  
The most current source is in BoogieRevB/ (app) and src/share_and_message_server (backend). 

Documentation --  
For the beta, no technical documentation, but a very clear user guide. Stability was the real target; the basic functionality was well in hand by the alpha. My notes from the alpha release (which was not successful) include timing measurements from India to the US, and, if you're interested, various attempts at a SIP based VoIP feature using FreePBX and Asterix on the backend.

Feature Wish List -- 
Redesign the backend + app screen-share thread to use a socket.io like framework  
Redesign the app itself for modern authentication  
Auto-update via Google Play  (costs about $60/year + a $25 developer fee, one-time)  
Auto-play voice to mimic VoIP  
Auto-brightness  
Ignore multi-touch  
SIP VoIP  
Slowly updating low-res video  
Integrate with iOS at back-end and look into whether WhatsApp will support draw-text, too  
Setting up parallel servers for small releases to other families  

Project Status --   
vB.2 is feature complete and very usable. No further changes are planned for the next several months, until we see how the app performs when my grandfather is in India.
