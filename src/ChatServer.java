import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executors;

public class ChatServer {

    //a hash set to keep names so we can check duplicates and prevent them
    private static Set<String> names = new HashSet<>();

    //set of all printwriters
    private static Set<PrintWriter> writers = new HashSet<>();

    public static void main(String[] args) throws Exception {
        System.out.println("Chat server is running...");
        var pool = Executors.newFixedThreadPool(500);
        try (var listener = new ServerSocket(59001)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));
            }
        }
    }
    // client handler
    private static class Handler implements Runnable{
        private String name;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;

        public Handler(Socket socket){
            this.socket = socket;
        }

        public void run(){
            try{
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true){
                    out.println("SUBMITNAME");
                    name = in.nextLine();
                    if (name == null){
                        return;
                    }
                    synchronized (names){
                        if(!name.isBlank() && !names.contains(name)){
                            names.add(name);
                            break;
                        }
                    }
                }
                out.println("NAMEACCEPTED" + name);
                for (PrintWriter writer : writers){
                    writer.println("MESSAGE" + name + " has joined");
                }
                writers.add(out);

                // broadcasting this client's message
                while (true){
                 String input = in.nextLine();
                    if (input.toLowerCase().startsWith("/quit")){
                        return;
                    }
                    for (PrintWriter writer : writers){
                        writer.println("MESSAGE " + name + " : " + input );
                    }
                }
            }catch (Exception e){
                System.out.println(e);
            }finally {
                if (out != null){
                    writers.remove(out);
                }
                if (name != null){
                    names.remove(name);
                    for (PrintWriter writer : writers){
                        writer.println("MESSAGE" + name + " has left");
                    }
                }
                try {
                    socket.close();
                }catch (IOException e){}
            }
        }
    }
}
