/* HOW TO RUN
   1) Configure things in the Configuration class
   2) Compile: javac Bot.java
   3) Run in loop: while true; do java Bot; sleep 1; done
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class Configuration {
    String exchange_name;
    int exchange_port;
    /* 0 = prod-like
       1 = slow
       2 = empty
    */
    final Integer test_exchange_kind = 1;
    /* replace REPLACEME with your team name! */
    final String team_name = "PROSPECTAVENUE";

    Configuration(Boolean test_mode) {
        if (!test_mode) {
            /* If we pass in false, we're putting our bot into PRODUCTION */
            exchange_port = 20000;
            exchange_name = "production";
        } else {
            /* If we pass in true, we're TESTING */
            exchange_port = 20000 + test_exchange_kind;
            exchange_name = "test-exch-" + this.team_name;
        }
    }

    String exchange_name() {
        return exchange_name;
    }

    Integer port() {
        return exchange_port;
    }
}

public class Bot {

    public static int orderid = 0;
    public static int bonds = 0;
    public static open =true;

    List<Integer> BOND = new ArrayList<Integer>();
    List<Integer> VALBZ = new ArrayList<Integer>();
    List<Integer> VALE = new ArrayList<Integer>();
    List<Integer> GS = new ArrayList<Integer>();
    List<Integer> MS = new ArrayList<Integer>();
    List<Integer> WFC = new ArrayList<Integer>();
    List<Integer> XLF = new ArrayList<Integer>();

    public static void main(String[] args) {
        /* The boolean passed to the Configuration constructor dictates whether or not the
           bot is connecting to the prod or test exchange. Be careful with this switch! */
        Configuration config = new Configuration(true);
        try {
            Socket skt = new Socket(config.exchange_name(), config.port());
            BufferedReader from_exchange = new BufferedReader(new InputStreamReader(skt.getInputStream()));
            PrintWriter to_exchange = new PrintWriter(skt.getOutputStream(), true);

            /*
              A common mistake people make is to to_exchange.println() > 1
              time for every from_exchange.readLine() response.
              Since many write messages generate marketdata, this will cause an
              exponential explosion in pending messages. Please, don't do that!
            */
            to_exchange.println(("HELLO " + config.team_name).toUpperCase());
            String reply = from_exchange.readLine().trim();
            System.err.printf("The exchange replied: %s\n", reply);
            while (true) {
                if (bonds < 100) {
                    to_exchange.println("ADD " + orderid++ + " BOND BUY 999 " + (100 - bonds));
                    bonds += 100;
                } else if (bonds > -100) {
                    to_exchange.println("ADD " + orderid++ + " BOND SELL 1001 " + (bonds));
                    bonds -= 100;
                }
                String ans = from_exchange.readLine().trim();
                System.err.printf("The exchange replied: %s\n", ans);
                Thread.sleep(1);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    // get info
    public static void getInfo(BufferedReader read) {
        System.err.pintln("Getting market info.");
        int counter = 0;
        for (int i = 0; i < 500; i++) {
            String line = read.readLine().trim();
            System.err.printf("Info: %s\n", line);
            String[] info = line.split(" ");
            if (info[0].equals("CLOSE")) {
                open = false;
                System.err.println("Market is closed".);
                return;
            }
            if (line == null)
                continue;
            if (info[0].equals("TRADE")) {
                if (info[1].equals("BOND"))
                    VALBZ.add(Integer.parseInt(info[2]));
                else if (info[1].equals("VALBZ"))
                    VALBZ.add(Integer.parseInt(info[2]));
                else if (info[1].equals("VALE"))
                    VALBZ.add(Integer.parseInt(info[2]));
                else if (info[1].equals("GS"))
                    VALBZ.add(Integer.parseInt(info[2]));
                else if (info[1].equals("MS"))
                    VALBZ.add(Integer.parseInt(info[2]));
                else if (info[1].equals("WFC"))
                    VALBZ.add(Integer.parseInt(info[2]));
            }
            Thread.sleep(1);
        }
    }

    // calculate average
    public double avg(List<Integer> prices, int lastN) {
        int sum = 0;
        for (int i = prices.length - 1; i > prices.length - lastN - 1; i--) {
            sum += prices.get(i);
        }
        return sum / lastN;
    }


    // adr pair trading
    public static void tradeADR(PrintWriter write) {
        int SIZE = 10;
        if (VALE.size() < SIZE || VALBZ.size() < SIZE)
            return;
        double ADR = avg(VALE, SIZE);
        double REG = avg(VALBZ, SIZE);
        double diff = REG - ADR;
        if (diff >= 3) {
            System.err.println("Buying ADR / selling regular");
            write.println("ADD " + orderid++ + " VALE BUY " + (ADR + 1) + " " + SIZE);
            write.println("CONVERT " + orderid++ + " VALE SELL " + SIZE);
            write.println("ADD " + orderid++ + " VALBZ SELL " + (REG - 1) + " " + SIZE);
        } else if (diff <= -3) {
            System.err.println("Buying ADR / selling regular");
            write.println("ADD " + orderid++ + " VALBZ BUY " + (REG + 1) + " " + SIZE);
            write.println("CONVERT " + orderid++ + " VALE BUY " + SIZE);
            write.println("ADD " + orderid++ + " VALE SELL " + (SELL - 1) + " " + SIZE);
        }
    }

    // etf arbitrage

    public static void tradeETF(PrintWriter write) {
        int SIZE = 20;
        double xlfP = avg(XLF, SIZE);
        double bondP = avg(BOND, SIZE);
        double gsP = avg(GS, SIZE);
        double msP = avg(MS, SIZE);
        double wfcP = avg(WFC, SIZE);
        double diff = (10 * xlfP) - (3 * bondP + 2 * gsP + 3 * msP + 2 * wfcP);

        if (diff > 102) {
            write.println("ADD " + orderid++ + " BOND BUY " + (bondP + 1) + " " + 30);
            write.println("ADD " + orderid++ + " GS BUY " + (gsP + 1) + " " + 20);
            write.println("ADD " + orderid++ + " MS BUY " + (msP + 1) + " " + 30);
            write.println("ADD " + orderid++ + " WFC BUY " + (wfcP + 1) + " " + 20);
            write.println("CONVERT " + orderid++ + " XLF BUY " + 100);
            write.println("ADD " + orderid++ + " XLF SELL " + (xlfP - 1) + " " + 100);
        } else if (diff < -102) {
            write.println("ADD " + orderid++ + " XLF BUY " + (xlfP + 1) + " " + 100);
            write.println("CONVERT " + orderid++ + " XLF SELL " + 100);
            write.println("ADD " + orderid++ + " BOND SELL " + (bondP - 1) + " " + 30);
            write.println("ADD " + orderid++ + " GS SELL " + (gsP - 1) + " " + 20);
            write.println("ADD " + orderid++ + " MS SELL " + (msP - 1) + " " + 30);
            write.println("ADD " + orderid++ + " WFC SELL " + (wfcP - 1) + " " + 20);
        }

    }


}

