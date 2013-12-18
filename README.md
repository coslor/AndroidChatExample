AndroidChatExample
==================
AndroidChatExample is a simple implementation of a mobile-to-mobile chat system with a central directory server. It was quickly written and the code is in sore need of refactoring, but it works, and it demonstrates some basic network communication techniques, as well as a basic Android GUI.

Build Instructions
=============
Both AndroidChat and ChatServer are Eclipse projects. The "executables" have been included in the distribution, but if you want to rebuild, you should follow the following steps:
1) Rebuild ChatServer
2) Regenerate the ChatServer.jar file in AndroidChat/lib by executing the makeJar.desc target in the ChatServer project. This gives AndroidChat a copy of the latest shared objects.
3) Rebuild and deploy AndroidChat on your devices (2 is a good number)

Execution Instructions
=================
Do the following:
1) Start ChatServer as a (desktop) Java application, with com.example.chatserver.ChatServer as the main class. Note its IP address in the console.
2) Start AndroidChat on your devices. You'll need to have Wifi enabled, and you'll need to be on the same Wifi network as the server. Enter the ChatServer IP or hostname when prompted.
3) Wait for the client to connect to the server and to choose a friend to chat to. (Note: this can take a significant amount of time right now; this is a known issue)
4) Type away!

Architecture
=========
Each application instance is represented by a "Chatter" which has a nickname and connection information. Chatters register their information with a central server, which then provides a list of all of the other Chatters that have registered with that server. The local Chatter may then choose another Chatter on the list as a "friend" to chat with; this is currently the first Chatter in the list, but one could easily imagine allowing the application user to choose from the list. Once a friend is chosen, the user can enter single lines of text, which are sent to the friend's network address, and the friend user in turn may send text to be displayed on the local device.

Implementation
============
The mobile app is a regular Android app, and uses plain old Sockets to connect to the server, and to other mobile instances. The server is a rather simple desktop Java application that essentially just implements a very simple name/directory server. While I wouldn't recommend this "reinventing the wheel" approach for, well, basically any real-world application, it was a way to show some server-side coding and to fit the project in the time I had.

Enhancement Opportunities
=====================
Lots of them--it's an example project done in a short amount of time. 

Here are a few:
1) Replace the server and its protocol with something that runs in a real environment--a RESTful service, maybe. Deploy it somewhere other than on a desktop.
2) Better yet, use some kind of existing directory or messaging service--LDAP, Google Cloud Messaging, maybe even just DNS
3) Or, get rid of the name server altogether and let the clients discover each other. There are some nifty Adhoc Wifi and Network Discovery packages in Android. I actually went some distance down that path until I realized that there's already a peer-to-peer Adhoc Wifi chat client example in the Android API samples. That didn't seem kosher, to I started over.
4) Refactor AndroidChatActivity to not be such a jumbled mess
5) Allow the users to enter server addresses, nicknames, and other preferences from within settings views in the app, like civilized people.
6) Refactor where possible to allow for mocking out of some of the network and system services, and thus allow for more test case coverage; there's almost none now.
7) Make the chat listener in AndroidChatActivity a service that communicates via Intents to the main activity, so that the app will come up whenever it receives a message from a friend
8) This has been tested with exactly *two* Android devices. Your Mileage May Vary.
