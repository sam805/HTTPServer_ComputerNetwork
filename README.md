# HTTPServer_ComputerNetwork
In this project I designed a multi-thread Http server which can handle up to 50 simultaneous connections from different clients. Http Server receives its dedicated port number from input argument and tries to open and to listen on a TCP port. 
If it receives a HTTP request, first it verifies whether it has valid http/1.1 required syntax and formatting, then it checks if the required method is implemented or not (get and head methods are implemented and options, post, put, delete, trace, and connect are not) and if the request is associated with a file (either html, jpeg, gif, or pdf), it checks whether the file exists or not. Eventually if the request is “Get” and the file exists, server will send the file specified in bytes format. Here we have used 1024 byte buffer for sending data. 
