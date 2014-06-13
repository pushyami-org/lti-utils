This is README.txt for project lti-utils

[Purpose]
=========

This project provides utilities for LTI communication between TC (Tool
Consumer) and TP (Tool Provider), along with giving TP information it
can store for communicating with the browser.  It supplies utiltites
for communicating the inital LTI launch and also for communication
with the LTI roster and settings services.

[Security]
==========

Oauth is used to verify that launch requests to the LTI tool are authorized.

HTTP operations within these utilities do not verify SSL information.
The operating assumption is that SSL is implemented by the environment
running the LTI tool.  I.e. and a load balancer or a Apache http
server is managing SSL.
