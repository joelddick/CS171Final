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

 
  string cmd = "scp -r -i ~/Downloads/Keys/YogiBearKey_Virginia.pem ~/Desktop/CollegeAssignments/CS171_2/CS171Final/config0.txt ubuntu@"+address[0]+":/home/ubuntu/";
  cout.write(cmd.c_str(), strlen(cmd.c_str()));
  cout.put('\n');
  string result = exec(cmd.c_str());
    
  cmd = "scp -r -i ~/Downloads/Keys/YogiBearKey_California.pem ~/Desktop/CollegeAssignments/CS171_2/CS171Final/config1.txt ubuntu@"+address[1]+":/home/ubuntu/";
  cout.write(cmd.c_str(), strlen(cmd.c_str()));
  cout.put('\n');
  result = exec(cmd.c_str());
    
  cmd = "scp -r -i ~/Downloads/Keys/YogiBearKey_Sydney.pem ~/Desktop/CollegeAssignments/CS171_2/CS171Final/config2.txt ubuntu@"+address[2]+":/home/ubuntu/";
  cout.write(cmd.c_str(), strlen(cmd.c_str()));
  cout.put('\n');
  result = exec(cmd.c_str());
    
  cmd = "scp -r -i ~/Downloads/Keys/YogiBearKey_Singapore.pem ~/Desktop/CollegeAssignments/CS171_2/CS171Final/config3.txt ubuntu@"+address[3]+":/home/ubuntu/";
  cout.write(cmd.c_str(), strlen(cmd.c_str()));
  cout.put('\n');
  result = exec(cmd.c_str());
    
  cmd = "scp -r -i ~/Downloads/Keys/YogiBearKey_SaoPaolo.pem ~/Desktop/CollegeAssignments/CS171_2/CS171Final/config4.txt ubuntu@"+address[4]+":/home/ubuntu/";
  cout.write(cmd.c_str(), strlen(cmd.c_str()));
  cout.put('\n');
  result = exec(cmd.c_str());
    

  exit(EXIT_SUCCESS);
}