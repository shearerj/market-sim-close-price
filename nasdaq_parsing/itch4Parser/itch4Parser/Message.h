
//
//  Message.h
//  itch4Parser
//
//  Created by Dylan Hurd on 11/3/13.
//  Copyright (c) 2013 Dylan Hurd. All rights reserved.
//

#ifndef __itch4Parser__Message__
#define __itch4Parser__Message__

#include <iostream>
#include <cstdint>

using namespace std;

class TimeStamp {
protected:
  uint32_t seconds;
  
public:
  friend istream& operator>> (istream &input,  TimeStamp &ts);
  friend ostream& operator<< (ostream &output, TimeStamp &ts);
};

//
// Messages
//
class Message {
public:
  TimeStamp ts;
protected:
  uint32_t nanoseconds;
  char eventCode;
  
public:
  friend istream& operator>> (istream &input,  Message &ts);
  friend ostream& operator<< (ostream &output, Message &ts);
};

class StockDirectory {
public:
  TimeStamp ts;
protected:
  uint32_t nanoseconds;
  char ticker[8];
  char mktCategory;
  char finStatus;
  uint32_t roundLotSize;
  char roundLotStatus;
public:
  friend istream& operator>> (istream &input,  StockDirectory &ts);
  friend ostream& operator<< (ostream &output, StockDirectory &ts);
};

class StockTradingAction {
public:
  TimeStamp ts;
protected:
  uint32_t nanoseconds;
  char ticker[8];
  char tradingState;
  char reason[4];
public:
  friend istream& operator>> (istream &input,  StockTradingAction &ts);
  friend ostream& operator<< (ostream &output, StockTradingAction &ts);
};

class ShortSalePriceTest {
public:
  TimeStamp ts;
private:
  uint32_t nanoseconds;
  char ticker[8];
  char regSHOAction;
public:
  friend istream& operator>> (istream &input,  ShortSalePriceTest &ts);
  friend ostream& operator<< (ostream &output, ShortSalePriceTest &ts);
};

class MarketParticipantPosition {
public:
  TimeStamp ts;
protected:
  uint32_t nanoseconds;
  char mpid[4];
  char ticker[8];
  char mmStatus;
  char mmMode;
  char mpStatus;
public:
  friend istream& operator>> (istream &input,  MarketParticipantPosition &ts);
  friend ostream& operator<< (ostream &output, MarketParticipantPosition &ts);
};

class BrokenTrade {
protected:
  uint32_t nanoseconds;
  uint64_t matchNumber;
public:
  TimeStamp ts;
  friend istream& operator>> (istream &input,  BrokenTrade &ts);
  friend ostream& operator<< (ostream &output, BrokenTrade &ts);
};

class NetOrderImbalance {
protected:
  uint32_t nanoseconds;
  uint64_t pairedShares;
  uint64_t imbalanceShares;
  char direction;
  char ticker[8];
  uint32_t farPrice;
  uint32_t nearPrice;
  uint32_t currentPrice;
  char crossType;
  char priceVar;
public:
  TimeStamp ts;
  friend istream& operator>> (istream &input,  NetOrderImbalance &ts);
  friend ostream& operator<< (ostream &output, NetOrderImbalance &ts);
};

class RetailPriceImprovement {
protected:
  uint32_t nanoseconds;
  char ticker[8];
  char interest;
public:
  TimeStamp ts;
  friend istream& operator>> (istream &input,  RetailPriceImprovement &ts);
  friend ostream& operator<< (ostream &output, RetailPriceImprovement &ts);
};

//
// Orders
//
class Order {
protected:
  uint32_t nanoseconds; //nanoseconds since last timestamp
  uint64_t refNum; //unique reference number
public:
  TimeStamp ts; // last timestamp
};

class AddOrder : public Order {
protected:
  char buyStatus;
  uint32_t quantity;
  char ticker[8];
  uint32_t price;
  
public:
  friend istream& operator>> (istream &input,  AddOrder &order);
  friend ostream& operator<< (ostream &output, AddOrder &order);

};

class AddMPIDOrder : public AddOrder {
protected:
  char mpid[4];
public:
  friend istream& operator>> (istream &input,  AddMPIDOrder &order);
  friend ostream& operator<< (ostream &output, AddMPIDOrder &order);
};

class ExecutedOrder : public Order {
protected:
  uint32_t quantity;
  uint64_t matchNumber;
public:
  friend istream& operator>> (istream &input,  ExecutedOrder &order);
  friend ostream& operator<< (ostream &output, ExecutedOrder &order);
};

class ExecutedPriceOrder : public ExecutedOrder {
protected:
  char printable;
  uint32_t price;
public:
  friend istream& operator>> (istream &input,  ExecutedPriceOrder &order);
  friend ostream& operator<< (ostream &output, ExecutedPriceOrder &order);
};

class CancelOrder : public Order{
protected:
  uint32_t quantity;
public:
  friend istream& operator>> (istream &input,  CancelOrder &order);
  friend ostream& operator<< (ostream &output, CancelOrder &order);
};


class DeleteOrder : public Order {
public:
  friend istream& operator>> (istream &input,  DeleteOrder &order);
  friend ostream& operator<< (ostream &output, DeleteOrder &order);
};

class ReplaceOrder : public Order {
protected:
  uint64_t oldRefNum;
  uint32_t quantity;
  uint32_t price;
  
public:
  friend istream& operator>> (istream &input,  ReplaceOrder &order);
  friend ostream& operator<< (ostream &output, ReplaceOrder &order);
};

class TradeMessage : public AddOrder {
protected:
  uint64_t matchNumber;
public:
  friend istream& operator>> (istream &input,  TradeMessage &order);
  friend ostream& operator<< (ostream &output, TradeMessage &order);
};

class CrossTradeMessage : public TradeMessage {
protected:
  char crossType;
public:
  friend istream& operator>> (istream &input,  CrossTradeMessage &order);
  friend ostream& operator<< (ostream &output, CrossTradeMessage &order);
};


#endif /* defined(__itch4Parser__Message__) */













