# Boogie
## Revision B.1

Overview --
This is an application to help my grandfather communicate with me and my mother via screen-shared doodles. To understand some of the design decisions, it's helpful to note that he cannot speak, has little experience with modern electronics, and has low bandwidth internet. When he's physically with us, he uses a product called a "Boogie Board" to write out what he's thinking, hence the name. 

Release Notes --
On 23 October 2016, we released a stable beta build privately to three clients.

Acknowledgements --
This project has involved at some point or another most of my family. My grandmother has, in particular, risen above and beyond to learn about modern cell phones so she can support my grandfather. Especially to my mother, father, and sister: thank you.

Hardware --
The grandfather-facing end of this project required about $200 of equipment. We are using a Nexus 7 (2013) running Marshmallow, a pair of decent over-ear headphones (to accomodate hearing aids), and a few soft tipped capacitive styluses. His  My mother and I are running Marshmallow on Google and Huawei smartphones. notebook_rev_a/ has the details, but the specifics really don't matter.

What's happening inside --
: Grandfather doodles, we record voice messages
: Credentials baked in for everyone to keep UI dead simple
: Numeric indicator of internet connectivity & online users
: Two-button UI
:: First button is to record audio (us) & save-screen (grandfather)
:: Second button is to playback screen shares and audio
: High Priority Thread
:: Smoothly screen shares at perfectly legible resolution between US and India on a 56kbps connection, updating stably between 1-2 Hz 
: Low Priority Thread
:: Naive presence polling
:: Mailbox check, message downloading, and local message management
: UI Thread
:: Drawable doodle area
:: Synchronization framework for the drawable area between all three threads
: Backend CherryPy HTTP server on an Amazon EC2 instance handles simple authentication and message distribution

Limitations --
Currently only three mobile users are supported; this is hardcoded. Memory usage could be a lot lighter. I heavily prioritized stability, update frequency, and feature exploration over scalability or power management in our alpha and beta releases, as this was never intended to reach beyond my family. The beta hits every feature target we set, and is bulletproof with respect to button mashing, pause/resume thread control, and so on. Unfortunately, people who want to branch off this repo are going to have to tweak the UI heavily for their own needs. 

Source --
The most current source is in BoogieRevB/ (app) and src/share_and_message_server (backend). 

Documentation --
For the beta... not so much. Stability was the real target; the basic functionality was well in hand by the alpha. My notes from the alpha release (which was not successful) include timing measurements from India to the US, and, if you're interested, various attempts at a SIP based VoIP feature using FreePBX and Asterix on the backend.

Feature Wish List --
Auto-brightness
Auto-update via Google Play
Auto-play voice to mimic VoIP
Guest access via a simple DNS lookup, with simple auth, for screen-sharing ONLY
Clean up the AWS instance, and play with containers to minimize resources 
Ignore multi-touch
SIP VoIP 
Slowly updating low-res video
Integrate with iOS at back-end (draw-text)
Setting up parallel servers for small releases to other family members, like Jana's dad

Project Status --
Complete

Alpha - tested. unstable. unusable. unsuccessful.
Beta - tested. stable. meets feature spec. very responsive and usable. successful.
