//
//  Message.cpp
//  itch4Parser
//
//  Created by Dylan Hurd on 11/3/13.
//  Copyright (c) 2013 Dylan Hurd. All rights reserved.
//
#include <ios>
#include <stdint.h>
#include "Message.h"


//Reading input - use to read input from the istream - WORKS ON BINARY
template <class T>
void read(istream &input, T &object){
  T *ptr = &object;
  input.read(reinterpret_cast<char*>(ptr), sizeof(T));
}

void readInt(istream &input, unsigned int &n){
  char c[4];
  for(int i=3; i >= 0; i--) {
    input.read(&c[i], 1);
  }
  unsigned int* ptr = reinterpret_cast<unsigned int*>(c);
  n = *ptr;
}

void readLong(istream &input, unsigned long &n){
  char c[8];
  for(int i=7; i >= 0; i--) {
    input.read(&c[i], 1);
  }
  unsigned long* ptr = reinterpret_cast<unsigned long*>(c);
  n = *ptr;
}

//
// Input
//

istream& operator>> (istream &input, TimeStamp &ts){
  readInt(input, ts.seconds);
  return input;
}

istream& operator>> (istream &input,  Message &o) {
  readInt(input,o.nanoseconds);
  read(input, o.eventCode);
  return input;
}

istream& operator>> (istream &input,  StockDirectory &o) {
  readInt(input,o.nanoseconds);
  input.get(o.ticker, 9);
  read(input, o.mktCategory);
  read(input, o.finStatus);
  readInt(input, o.roundLotSize);
  read(input, o.roundLotStatus);
  return input;
}

istream& operator>> (istream &input,  StockTradingAction &o) {
  readInt(input,o.nanoseconds);
  input.get(o.ticker, 9);
  read(input, o.tradingState);
  input.ignore();
  input.get(o.reason, 5);
  return input;
}

istream& operator>> (istream &input,  ShortSalePriceTest &o) {
  readInt(input,o.nanoseconds);
  input.get(o.ticker, 9);
  read(input, o.regSHOAction);
  return input;
}

istream& operator>> (istream &input,  MarketParticipantPosition &o) {
  readInt(input,o.nanoseconds);
  input.get(o.mpid, 5);
  input.get(o.ticker, 9);
  read(input, o.mmStatus);
  read(input, o.mmMode);
  read(input, o.mpStatus);
  return input;
}

istream& operator>> (istream &input,  BrokenTrade &o) {
  readInt(input,o.nanoseconds);
  readLong(input, o.matchNumber);
  return input;
}

istream& operator>> (istream &input,  NetOrderImbalance &o) {
  readInt(input,o.nanoseconds);
  readLong(input, o.pairedShares);
  readLong(input, o.imbalanceShares);
  read(input, o.direction);
  input.get(o.ticker, 9);
  readInt(input, o.farPrice);
  readInt(input, o.nearPrice);
  readInt(input, o.currentPrice);
  read(input, o.crossType);
  read(input, o.priceVar);
  return input;
}

istream& operator>> (istream &input,  RetailPriceImprovement &o) {
  readInt(input,o.nanoseconds);
  input.get(o.ticker, 9);
  read(input, o.interest);
  return input;
}

istream& operator>> (istream &input,  AddOrder &o) {
  readInt(input,o.nanoseconds);
  readLong(input, o.refNum);
  read(input, o.buyStatus);
  readInt(input, o.quantity);
  input.get(o.ticker, 9);
  readInt(input, o.price);
  return input;
}

istream& operator>> (istream &input,  AddMPIDOrder &o) {
  readInt(input,o.nanoseconds);
  readLong(input, o.refNum);
  read(input, o.buyStatus);
  readInt(input, o.quantity);
  input.get(o.ticker, 9);
  readInt(input, o.price);
  input.get(o.mpid, 5);
  return input;
}

istream& operator>> (istream &input,  ExecutedOrder &o) {
  readInt(input,o.nanoseconds);
  readLong(input, o.refNum);
  readInt(input, o.quantity);
  readInt(input, o.matchNumber);
  return input;
}

istream& operator>> (istream &input,  ExecutedPriceOrder &o) {
  readInt(input,o.nanoseconds);
  readLong(input, o.refNum);
  readInt(input, o.quantity);
  readLong(input, o.matchNumber);
  read(input, o.printable);
  readInt(input, o.price);
  return input;
}

istream& operator>> (istream &input,  CancelOrder &o){
  readInt(input,o.nanoseconds);
  readLong(input, o.refNum);
  readInt(input, o.quantity);
  return input;
}


istream& operator>> (istream &input,  DeleteOrder &o){
  readInt(input,o.nanoseconds);
  readLong(input, o.refNum);
  return input;
}

istream& operator>> (istream &input, ReplaceOrder &o){
  readInt(input,o.nanoseconds);
  readLong(input, o.oldRefNum);
  readLong(input, o.refNum);
  readInt(input, o.quantity);
  readInt(input, o.price);
  return input;
}

istream& operator>> (istream &input,  TradeMessage &o) {
  readInt(input,o.nanoseconds);
  readLong(input, o.refNum);
  read(input, o.buyStatus);
  readInt(input, o.quantity);
  input.get(o.ticker, 9);
  readInt(input, o.price);
  readLong(input, o.matchNumber);
  return input;
}

istream& operator>> (istream &input,  CrossTradeMessage &o) {
  readInt(input,o.nanoseconds);
  readInt(input, o.quantity);
  input.get(o.ticker, 9);
  readInt(input, o.price);
  readLong(input, o.matchNumber);
  read(input, o.crossType);
  return input;
}

//
// Output
//

ostream& operator<< (ostream &output, TimeStamp &ts){
  output << ts.seconds;
  return output;
}

ostream& operator<< (ostream &output, Message &o){
  output << "S," << o.ts << ',' << o.nanoseconds << ',' << o.eventCode << '\n';
  return output;
}

ostream& operator<< (ostream &output, StockDirectory &o){
  output << "R," << o.ts << ',' << o.nanoseconds << ',' << o.ticker << ',' << o.mktCategory << ',' << o.finStatus << ',' << o.roundLotSize << ',' << o.roundLotStatus << '\n';
  return output;
}

ostream& operator<< (ostream &output, StockTradingAction &o){
  output << "H," << o.ts << ',' << o.nanoseconds << ',' << o.ticker << ',' << o.tradingState << ',' << o.reason << '\n';
  return output;
}

ostream& operator<< (ostream &output, ShortSalePriceTest &o){
  output << "Y," << o.ts << ',' << o.nanoseconds << ',' << o.ticker << ',' << o.regSHOAction << '\n';
  return output;
}

ostream& operator<< (ostream &output, MarketParticipantPosition &o){
  output << "L," << o.ts << ',' << o.nanoseconds << ',' << o.mpid << ',' << o.ticker << ',' << o.mmStatus << ',' << o.mmMode << ',' << o.mpStatus << '\n';
  return output;
}

ostream& operator<< (ostream &output, BrokenTrade &o){
  output << "B," << o.ts << ',' << o.nanoseconds << ',' << o.matchNumber << '\n';
  return output;
}

ostream& operator<< (ostream &output, NetOrderImbalance &o){
  output << "I," << o.ts << ',' << o.nanoseconds << ',' << o.pairedShares << ',' << o.imbalanceShares << ',' << o.direction << ',' << o.ticker << ',' << o.farPrice << ',' << o.nearPrice << ',' << o.currentPrice << ',' << o.crossType << ',' << o.priceVar << '\n';
  return output;
}

ostream& operator<< (ostream &output, RetailPriceImprovement &o){
  output << "N," << o.ts << ',' << o.nanoseconds << ',' << o.ticker << ',' << o.interest << '\n';
  return output;
}


ostream& operator<< (ostream &output, AddOrder &o) {
  output << "A," << o.ts << ',' << o.nanoseconds << ',' << o.refNum << ',' << o.buyStatus << ',' << o.quantity << ',' << o.ticker << ',' << o.price << '\n';
  return output;
}

ostream& operator<< (ostream &output, AddMPIDOrder &o) {
  output << "F," << o.ts << ',' << o.nanoseconds << ',' << o.refNum << ',' << o.buyStatus << ',' << o.quantity << ',' << o.ticker << ',' << o.price << ',' << o.mpid << '\n';
  return output;
}

ostream& operator<< (ostream &output, ExecutedOrder &o){
  output << "E," << o.ts << ',' << o.nanoseconds << ',' << o.refNum << ',' << o.quantity << ',' << o.matchNumber << '\n';
  return output;
}

ostream& operator<< (ostream &output, ExecutedPriceOrder &o){
  output << "C," << o.ts << ',' << o.nanoseconds << ',' << o.refNum << ',' << o.quantity << ',' << o.matchNumber << ',' << o.printable << ',' << o.price << '\n';
  return output;
}

ostream& operator<< (ostream &output, CancelOrder &o){
  output << "X," << o.ts << ',' << o.nanoseconds << ',' << o.refNum << ',' << o.quantity << '\n';
  return output;
}

ostream& operator<< (ostream &output, DeleteOrder &o){
  output << "D," << o.ts << ',' << o.nanoseconds << ',' << o.refNum <<'\n';
  return output;
}

ostream& operator<< (ostream &output, ReplaceOrder &o){
  output << "U," << o.ts << ',' << o.nanoseconds << ',' << o.oldRefNum << ',' << ',' << o.refNum << ',' << o.quantity << ',' << o.price << '\n';
  return output;
}

ostream& operator<< (ostream &output, TradeMessage &o){
  output << "P," << o.ts << ',' << o.nanoseconds << ',' << o.refNum << ',' << o.buyStatus << ',' << o.quantity << ',' << o.ticker << ',' << o.price << ',' << o.matchNumber << '\n';
  return output;
}

ostream& operator<< (ostream &output, CrossTradeMessage &o){
  output << "Q," << o.ts << ',' << o.nanoseconds << ',' << o.quantity << ',' << o.ticker << ',' << o.price << ',' << o.matchNumber << ',' << o.crossType << '\n';
  return output;
}







