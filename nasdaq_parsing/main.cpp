#include <iostream>
#include <fstream>


int main(int argc, char* argv[]){
  std::ios_base::sync_with_stdio (false);
  if(argc != 3) {
    std::cerr << "incorrect arguments";
    return 1;
  }

  std::ifstream input(argv[1], std::ios::in | std::ios::binary);
  if(!input.is_open()) {
    std::cerr << "Error opening input file";
    return 1;
  }
  
  std::ofstream output(argv[2], std::ios::out | std::ios::trunc);
  if(!output.is_open()){
    std::cerr << "Error opening output file";
    return 1;
  }
  
  output << std::hex << input.rdbuf();
}