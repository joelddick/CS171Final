#include <sys/types.h>
#include <sys/stat.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <syslog.h>
#include <string.h>

#include <ctime>
#include <iostream>
#include <string>
#include <cstdio>
#include <iostream>
#include <fstream>
using namespace std;

// Exec function                                                                                                                                                                                     
std::string exec(const char* cmd) {
  FILE* pipe = popen(cmd, "r");
  if (!pipe) return "ERROR";
  char buffer[128];
  std::string result = "";
  while(!feof(pipe)) {
    if(fgets(buffer, 128, pipe) != NULL)
      result += buffer;
  }
  pclose(pipe);
  return result;
}

int main(void) {

  // init addrs                                                                                                                                                                                    
  static const string address[] = {
      "52.4.206.21",    // N virginia           YogiBear1
      "52.8.92.5",      // N California         YogiBear2
      "54.79.84.94",    // Sydney               YogiBear3
      "52.74.13.50",    // Singapore            YogiBear4
      "54.94.255.64"   // Sao Paulo            YogiBear5
  };

  // scp -r -i ~/Desktop/turtlebeards.pem ~/Dropbox/Current\ Documents/cs271/cs271_proj1_java/clientServer/dist/clientServer.jar ec2-user@54.174.167.183:/home/ec2-user/                           
  //for(int i = 0; i < 5; i++) {
  string cmd = "scp -r -i ~/Desktop/Pem_Keys/YogiBearKey_Virginia.pem /Users/olivertownsend/Desktop/FinalProj.jar ubuntu@"+address[0]+":/home/ubuntu/";
  cout.write(cmd.c_str(), strlen(cmd.c_str()));
  cout.put('\n');
  string result = exec(cmd.c_str());
  // cout << "> STARTING INSTANCE" << endl;
  // cmd = "ssh -i /Users/olivertownsend/Desktop/Pem_Keys/YogiBearKey_Virginia.pem ubuntu@"+address[0]+" java -jar Project3.jar";
  // cout.write(cmd.c_str(), strlen(cmd.c_str()));
  // cout.put('\n');
  // result = exec(cmd.c_str());
    
  cmd = "scp -r -i ~/Desktop/Pem_Keys/YogiBearKey_California.pem /Users/olivertownsend/Desktop/FinalProj.jar ubuntu@"+address[1]+":/home/ubuntu/";
  cout.write(cmd.c_str(), strlen(cmd.c_str()));
  cout.put('\n');
  result = exec(cmd.c_str());
  // cout << "> STARTING INSTANCE" << endl;
  // cmd = "ssh -i /Users/olivertownsend/Desktop/Pem_Keys/YogiBearKey_California.pem ubuntu@"+address[1]+" java -jar Project3.jar";
  // cout.write(cmd.c_str(), strlen(cmd.c_str()));
  // cout.put('\n');
  // result = exec(cmd.c_str());
    
  cmd = "scp -r -i ~/Desktop/Pem_Keys/YogiBearKey_Sydney.pem /Users/olivertownsend/Desktop/FinalProj.jar ubuntu@"+address[2]+":/home/ubuntu/";
  cout.write(cmd.c_str(), strlen(cmd.c_str()));
  cout.put('\n');
  result = exec(cmd.c_str());
  // cout << "> STARTING INSTANCE" << endl;
  // cmd = "ssh -i /Users/olivertownsend/Desktop/Pem_Keys/YogiBearKey_Sydney.pem ubuntu@"+address[2]+" java -jar Project3.jar";
  // cout.write(cmd.c_str(), strlen(cmd.c_str()));
  // cout.put('\n');
  // result = exec(cmd.c_str());
    
  cmd = "scp -r -i ~/Desktop/Pem_Keys/YogiBearKey_Singapore.pem /Users/olivertownsend/Desktop/FinalProj.jar ubuntu@"+address[3]+":/home/ubuntu/";
  cout.write(cmd.c_str(), strlen(cmd.c_str()));
  cout.put('\n');
  result = exec(cmd.c_str());
  // cout << "> STARTING INSTANCE" << endl;
  // cmd = "ssh -i /Users/olivertownsend/Desktop/Pem_Keys/YogiBearKey_Singapore.pem ubuntu@"+address[3]+" java -jar Project3.jar";
  // cout.write(cmd.c_str(), strlen(cmd.c_str()));
  // cout.put('\n');
  // result = exec(cmd.c_str());
    
  cmd = "scp -r -i ~/Desktop/Pem_Keys/YogiBearKey_SaoPaolo.pem /Users/olivertownsend/Desktop/FinalProj.jar ubuntu@"+address[4]+":/home/ubuntu/";
  cout.write(cmd.c_str(), strlen(cmd.c_str()));
  cout.put('\n');
  result = exec(cmd.c_str());
  // cout << "> STARTING INSTANCE" << endl;
  // cmd = "ssh -i /Users/olivertownsend/Desktop/Pem_Keys/YogiBearKey_SaoPaolo.pem ubuntu@"+address[4]+" java -jar Project3.jar";
  // cout.write(cmd.c_str(), strlen(cmd.c_str()));
  // cout.put('\n');
  // result = exec(cmd.c_str());

  // cmd = "scp -r -i ~/Desktop/Pem_Keys/YogiBearKey_Ireland.pem /Users/olivertownsend/Desktop/FinalProj.jar ubuntu@"+address[5]+":/home/ubuntu/";
  // cout.write(cmd.c_str(), strlen(cmd.c_str()));
  // cout.put('\n');
  // result = exec(cmd.c_str());
  // cout << "> STARTING INSTANCE" << endl;
  // cmd = "ssh -i /Users/olivertownsend/Desktop/Pem_Keys/YogiBearKey_Ireland.pem ubuntu@"+address[5]+" java -jar Project3.jar";
  // cout.write(cmd.c_str(), strlen(cmd.c_str()));
  // cout.put('\n');
  // result = exec(cmd.c_str());
      
      
  // sleep(1);                                                                                                                                                                                 
  //}
    

  exit(EXIT_SUCCESS);
}