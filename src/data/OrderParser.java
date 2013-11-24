package data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import entity.market.Price;
import event.TimeStamp;

public class OrderParser {

    void processNYSE(File inputFile) throws FileNotFoundException{
        Scanner scanner = new Scanner(inputFile);
        
        while(scanner.hasNextLine()){
            Scanner lineScanner = new Scanner(scanner.nextLine()).useDelimiter(",");

            char messageType = lineScanner.next().charAt(0);
            
            switch (messageType){
            case 'A': parseNYSEAddOrder(lineScanner);// store this in a structure
                break;
            case 'D':
                break;
            case 'M': 
                break;
            case 'I':
                break;
            default:
                break;
            }

        }
        
        
    }
    
    OrderDatum parseNYSEAddOrder(Scanner lineScanner){
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
    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        if (args.length < 2) {
            System.err.println("Usage: <filename> (nyse | itch)");
            return;
        }
        
        int i = 0;
        File inputFile = new File(args[i++]).getCanonicalFile();
        
        String type = args[i++];
        if (type.equals("nyse")){
            process(inputFile);
        }
        else{
            
        }

        
    }

}
