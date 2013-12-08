package data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import entity.market.Price;
import event.TimeStamp;

public class OrderParser {

    static void processNYSE(File inputFile) throws FileNotFoundException{
        Scanner scanner = new Scanner(inputFile);
        
        while(scanner.hasNextLine()){
            Scanner lineScanner = new Scanner(scanner.nextLine()).useDelimiter(",");

            char messageType = lineScanner.next().charAt(0);
            
            switch (messageType){
            case 'A': parseNYSEAddOrder(lineScanner);// store this in a structure
                break;
            case 'D': parseNYSEDeleteOrder(lineScanner);
                break;
            case 'M': parseNYSEModifyOrder(lineScanner);
                break;
            case 'I': parseNYSEImbalanceOrder(lineScanner);
                break;
            default:
                break;
            }

        }
        
        
    }
    
    static OrderDatum parseNYSEAddOrder(Scanner lineScanner){
        String sequenceNum = lineScanner.next();
        String orderReferenceNum = lineScanner.next();
        char exchangeCode = lineScanner.next().charAt(0);
        boolean isBuy = (lineScanner.next().charAt(0) == 'B') ? true: false;
        int quantity = lineScanner.nextInt();
        String stockSymbol = lineScanner.next();
        Price price = new Price(lineScanner.nextDouble());
        TimeStamp timestamp = new TimeStamp(lineScanner.nextInt()*1000 + lineScanner.nextInt());
        char systemCode = lineScanner.next().charAt(0);
        String quoteId = lineScanner.next();
            
        OrderDatum orderData = new OrderDatum('A',
                                              sequenceNum,
                                              orderReferenceNum,
                                              exchangeCode,
                                              stockSymbol, 
                                              timestamp,
                                              systemCode,
                                              quoteId,
                                              price,
                                              quantity,
                                              isBuy);
        
        return orderData;
    }
    
    static OrderDatum parseNYSEDeleteOrder(Scanner lineScanner){
    	 String sequenceNum = lineScanner.next();
         String orderReferenceNum = lineScanner.next();
         TimeStamp timestamp = new TimeStamp(lineScanner.nextInt()*1000 + lineScanner.nextInt());
         String stockSymbol = lineScanner.next();
         char exchangeCode = lineScanner.next().charAt(0);
         char systemCode = lineScanner.next().charAt(0);
         String quoteId = lineScanner.next();
         boolean isBuy = (lineScanner.next().charAt(0) == 'B') ? true: false;
             
         OrderDatum orderData = new OrderDatum('D',
                                               sequenceNum,
                                               orderReferenceNum,
                                               exchangeCode,
                                               stockSymbol, 
                                               timestamp,
                                               systemCode,
                                               quoteId,
                                               new Price(0), // price doesnt matter since delete
                                               0, //quantity as well
                                               isBuy);
         
         return orderData;
    
    }
    
    static OrderDatum parseNYSEModifyOrder(Scanner lineScanner){
    	 String sequenceNum = lineScanner.next();
         String orderReferenceNum = lineScanner.next();
         int quantity = lineScanner.nextInt();
         Price price = new Price(lineScanner.nextDouble());
         TimeStamp timestamp = new TimeStamp(lineScanner.nextInt()*1000 + lineScanner.nextInt());
         String stockSymbol = lineScanner.next();
         char exchangeCode = lineScanner.next().charAt(0);
         char systemCode = lineScanner.next().charAt(0);
         String quoteId = lineScanner.next();
         boolean isBuy = (lineScanner.next().charAt(0) == 'B') ? true: false;
             
         OrderDatum orderData = new OrderDatum('M',
                                               sequenceNum,
                                               orderReferenceNum,
                                               exchangeCode,
                                               stockSymbol, 
                                               timestamp,
                                               systemCode,
                                               quoteId,
                                               price,
                                               quantity,
                                               isBuy);
         
         return orderData;
   
   }
    
    static OrderDatum parseNYSEImbalanceOrder(Scanner lineScanner){
    	String sequenceNum = lineScanner.next();
        String stockSymbol = lineScanner.next();
        Price price = new Price(lineScanner.nextDouble());
        int quantity = lineScanner.nextInt();
        int totalImbalance = lineScanner.nextInt();
        TimeStamp timestamp = new TimeStamp(lineScanner.nextInt()*1000 + lineScanner.nextInt());
        int marketImbalance = lineScanner.nextInt();
        char auctionType = lineScanner.next().charAt(0);
        int auctionTime = lineScanner.nextInt();
        char exchangeCode = lineScanner.next().charAt(0);
        char systemCode = lineScanner.next().charAt(0);
        String quoteId="" , orderReferenceNum = "";
        boolean isBuy = false;
        OrderDatum orderData = new OrderDatum('I',
                                              sequenceNum,
                                              orderReferenceNum,
                                              exchangeCode,
                                              stockSymbol, 
                                              timestamp,
                                              systemCode,
                                              quoteId,
                                              price,
                                              quantity,
                                              isBuy);
        orderData.setAuctionTime(auctionTime);
        orderData.setTotalImbalance(totalImbalance);
        orderData.setAuctionType(auctionType);
        orderData.setMarketImbalance(marketImbalance);
        return orderData;
  
  }

    static void processNasdaq(File inputFile) throws FileNotFoundException{
        Scanner scanner = new Scanner(inputFile);
        
        while(scanner.hasNextLine()){
            Scanner lineScanner = new Scanner(scanner.nextLine()).useDelimiter(",");

            char messageType = lineScanner.next().charAt(0);

            switch (messageType){
            case 'A': //add order - no MPID attribution msg
            case 'F': //add order with MPID attribution message TODO figure out what an MPID attribution message is
                parseNasdaqAddOrder(lineScanner); 
                break;
            case 'D': 
                parseNasdaqDeleteOrder(lineScanner);
                break;
            case 'E': //modify order executed message
                //parseNasdaqModifyOrderExecuted(lineScanner); 
                break;
            case 'C': //modify order executed with price message
                //parseNasdaqModifyOrderExecutedPrice(lineScanner); 
                break;
            case 'X': //order cancel message
                //parseNasdaqModifyOrderCancel(lineScanner); 
                break;
            case 'U': //order replace message
                parseNasdaqModifyOrder(lineScanner); 
                break;
            /*case 'I': 
                parseNasdaqImbalanceOrder(lineScanner);
                break;*/
            case 'T': //standalone timestamp
            case 'S': //systems event message
            case 'R': //stock directory message
            case 'H': //stock trading action message
            case 'Y': //Reg SHO short sale price test restricted indicator
            case 'L': //market participant position message
            case 'P': //trade message
            case 'Q': //cross trade message
            case 'B': //broken trade message
            case 'N': //retail interest message
            default:
                break;
            }

        }
        
        
    }
    
    static OrderDatum parseNasdaqAddOrder(Scanner lineScanner){
        TimeStamp timestamp = new TimeStamp(lineScanner.nextInt()*1000 + lineScanner.nextInt());
        String orderReferenceNum = lineScanner.next();
        boolean isBuy = (lineScanner.next().charAt(0) == 'B') ? true: false;
        int quantity = lineScanner.nextInt();
        String stockSymbol = lineScanner.next().trim();
        Price price = new Price(lineScanner.nextDouble());

        //These values are not available for nasdaq
        //no such thing as a sequence number in nasdaq, set it to 0 for now
        //TODO figure out what we want to do with this discrepency
        String sequenceNum = "";
        char exchangeCode = ' ';
        char systemCode = ' ';
        String quoteId = "";
            
        OrderDatum orderData = new OrderDatum('A',
                                              sequenceNum,
                                              orderReferenceNum,
                                              exchangeCode,
                                              stockSymbol, 
                                              timestamp,
                                              systemCode,
                                              quoteId,
                                              price,
                                              quantity,
                                              isBuy);
        
        return orderData;
    }
    
    static OrderDatum parseNasdaqDeleteOrder(Scanner lineScanner){
         TimeStamp timestamp = new TimeStamp(lineScanner.nextInt()*1000 + lineScanner.nextInt());
         String orderReferenceNum = lineScanner.next();

         //these values aren't used
         String sequenceNum = "";
         String stockSymbol = "";
         char exchangeCode = ' ';
         char systemCode = ' ';
         String quoteId = "";
         boolean isBuy = false;
             
         OrderDatum orderData = new OrderDatum('D',
                                               sequenceNum,
                                               orderReferenceNum,
                                               exchangeCode,
                                               stockSymbol, 
                                               timestamp,
                                               systemCode,
                                               quoteId,
                                               new Price(0), // price doesnt matter since delete
                                               0, //quantity as well
                                               isBuy);
         
         return orderData;
    }
    
    static OrderDatum parseNasdaqModifyOrder(Scanner lineScanner){
        TimeStamp timestamp = new TimeStamp(lineScanner.nextInt()*1000 + lineScanner.nextInt());
        String originalOrderReferenceNum = lineScanner.next();
        String newOrderReferenceNum = lineScanner.next();
        int quantity = lineScanner.nextInt();
        Price price = new Price(lineScanner.nextDouble());

    	String sequenceNum = "";
        char exchangeCode = ' ';
        String stockSymbol = "";
        char systemCode = ' ';
        String quoteId = "";
        boolean isBuy = false;

        OrderDatum orderData = new OrderDatum('M',
                                           sequenceNum,
                                           originalOrderReferenceNum,
                                           exchangeCode,
                                           stockSymbol, 
                                           timestamp,
                                           systemCode,
                                           quoteId,
                                           price,
                                           quantity,
                                           isBuy);

        return orderData;
   }
    
    static OrderDatum parseNasdaqImbalanceOrder(Scanner lineScanner){
    	String sequenceNum = lineScanner.next();
        String stockSymbol = lineScanner.next();
        Price price = new Price(lineScanner.nextDouble());
        int quantity = lineScanner.nextInt();
        int totalImbalance = lineScanner.nextInt();
        TimeStamp timestamp = new TimeStamp(lineScanner.nextInt()*1000 + lineScanner.nextInt());
        int marketImbalance = lineScanner.nextInt();
        char auctionType = lineScanner.next().charAt(0);
        int auctionTime = lineScanner.nextInt();
        char exchangeCode = lineScanner.next().charAt(0);
        char systemCode = lineScanner.next().charAt(0);
        String quoteId="" , orderReferenceNum = "";
        boolean isBuy = false;
        OrderDatum orderData = new OrderDatum('I',
                                              sequenceNum,
                                              orderReferenceNum,
                                              exchangeCode,
                                              stockSymbol, 
                                              timestamp,
                                              systemCode,
                                              quoteId,
                                              price,
                                              quantity,
                                              isBuy);
        orderData.setAuctionTime(auctionTime);
        orderData.setTotalImbalance(totalImbalance);
        orderData.setAuctionType(auctionType);
        orderData.setMarketImbalance(marketImbalance);
        return orderData;
  
  }


    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        if (args.length < 2) {
            System.err.println("Usage: <filename> (nyse | nasdaq)");
            return;
        }
        
        int i = 0;
        File inputFile = new File(args[i++]).getCanonicalFile();
        
        String type = args[i++];
        if (type.equals("nyse")){
            processNYSE(inputFile);
        }
        else if (type.equals("nasdaq")){
            processNasdaq(inputFile);            
        }

        
    }

}
