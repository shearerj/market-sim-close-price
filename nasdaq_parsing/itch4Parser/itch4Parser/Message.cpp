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

//
// Data Types - DO NOT TOUCH
//

//istream& operator>> (istream &input, uint32_t &n){
//  input.read(reinterpret_cast<char*>(&n), sizeof(uint32_t));
//  return input;
//}

//istream& operator>> (istream &input, uint64_t &n){
//  input.read(reinterpret_cast<char*>(&n), sizeof(uint64_t));
//  return input;
//}

istream& operator>> (istream &input, char &c){
  input.read(reinterpret_cast<char*>(&c), sizeof(unsigned char));
  return input;
}

//
// Input
//

istream& operator>> (istream &input, TimeStamp &ts){
  input  >>  ts.seconds;
  cerr << "After timestamp: " << input.good() << '\n';
  return input;
}

istream& operator>> (istream &input,  Message &o) {
  input >> o.nanoseconds;
  input >> o.eventCode;
  return input;
}

istream& operator>> (istream &input,  StockDirectory &o) {
  input >> o.nanoseconds;
  input.get(o.ticker, 9);
  input >> o.mktCategory;
  input >> o.finStatus;
  input >> o.roundLotSize;
  input >> o.roundLotStatus;
  return input;
}

istream& operator>> (istream &input,  StockTradingAction &o) {
  input >> o.nanoseconds;
  input.get(o.ticker, 9);
  input >> o.tradingState;
  input.ignore();
  input.get(o.reason, 5);
  return input;
}

istream& operator>> (istream &input,  ShortSalePriceTest &o) {
  input >> o.nanoseconds;
  input.get(o.ticker, 9);
  input >> o.regSHOAction;
  return input;
}

istream& operator>> (istream &input,  MarketParticipantPosition &o) {
  input >> o.nanoseconds;
  input.get(o.mpid, 5);
  input.get(o.ticker, 9);
  input >> o.mmStatus;
  input >> o.mmMode;
  input >> o.mpStatus;
  return input;
}

istream& operator>> (istream &input,  BrokenTrade &o) {
  input >> o.nanoseconds;
  input >> o.matchNumber;
  return input;
}

istream& operator>> (istream &input,  NetOrderImbalance &o) {
  input >> o.nanoseconds;
  input >> o.pairedShares;
  input >> o.imbalanceShares;
  input >> o.direction;
  input.get(o.ticker, 9);
  input >> o.farPrice;
  input >> o.nearPrice;
  input >> o.currentPrice;
  input >> o.crossType;
  input >> o.priceVar;
  return input;
}

istream& operator>> (istream &input,  RetailPriceImprovement &o) {
  input >> o.nanoseconds;
  input.get(o.ticker, 9);
  input >> o.interest;
  return input;
}

istream& operator>> (istream &input,  AddOrder &o) {
  input >> o.nanoseconds;
  input >> o.refNum;
  input >> o.buyStatus;
  input >> o.quantity;
  // setting the ticker
  input.get(o.ticker, 9);
  input >> o.price;
  return input;
}

istream& operator>> (istream &input,  AddMPIDOrder &o) {
  input >> o.nanoseconds;
  input >> o.refNum;
  input >> o.buyStatus;
  input >> o.quantity;
  // setting the ticker
  input.get(o.ticker, 9);
  input >> o.price;
  input.get(o.mpid, 5);
  return input;
}

istream& operator>> (istream &input,  ExecutedOrder &o) {
  input >> o.nanoseconds;
  input >> o.refNum;
  input >> o.quantity;
  input >> o.matchNumber;
  return input;
}

istream& operator>> (istream &input,  ExecutedPriceOrder &o) {
  input >> o.nanoseconds;
  input >> o.refNum;
  input >> o.quantity;
  input >> o.matchNumber;
  input >> o.printable;
  input >> o.price;
  return input;
}

istream& operator>> (istream &input,  CancelOrder &order){
  input >> order.nanoseconds;
  input >> order.refNum;
  input >> order.quantity;
  return input;
}


istream& operator>> (istream &input,  DeleteOrder &o){
  input >> o.nanoseconds;
  input >> o.refNum;
  return input;
}

istream& operator>> (istream &input, ReplaceOrder &o){
  input >> o.nanoseconds;
  input >> o.oldRefNum;
  input >> o.refNum;
  input >> o.quantity;
  input >> o.price;
  return input;
}

istream& operator>> (istream &input,  TradeMessage &o) {
  input >> o.nanoseconds;
  input >> o.refNum;
  input >> o.buyStatus;
  input >> o.quantity;
  input.get(o.ticker, 9);
  input >> o.price;
  input >> o.matchNumber;
  return input;
}

istream& operator>> (istream &input,  CrossTradeMessage &o) {
  input >> o.nanoseconds;
  input >> o.quantity;
  input.get(o.ticker, 9);
  input >> o.price;
  input >> o.matchNumber;
  input >> o.crossType;
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







