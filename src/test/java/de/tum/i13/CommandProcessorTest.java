// package de.tum.i13;

// import de.tum.i13.ecs.ECSCommandProcessor;
// import de.tum.i13.shared.CommandProcessor;

// import java.net.InetAddress;
// import java.net.InetSocketAddress;
// import java.util.logging.Logger;

// public class CommandProcessorTest implements CommandProcessor {

//     @Override
//     public String process(String command) {
//         String[] commandArr = command.trim().split("\\s+");
//         String cmd = commandArr[0];
//         switch(cmd){
//             case "join_success":
//                 System.out.println("Server echo: " + command);
//                 return command;
//             case "enable_write_lock":
//                 System.out.println("Server echo: " + command);
//                 return command;
//             case "disable_write_lock":
//                 System.out.println("Server echo: " + command);
//                 return command;
//             case "remove_success":
//                 System.out.println("Server echo: " + command);
//                 return command;
//             case "metadata_update":
//                 System.out.println("Server echo: " + command);
//                 return command;
//             case "heartbeat":
//                 System.out.println("Server echo: " + command);
//                 return "still_alive";
//             default:
//                 System.out.println("Server echo (all): " + command);
//                 return command;
//         }
//     }

//     @Override
//     public String connectionAccepted(InetSocketAddress address, InetSocketAddress remoteAddress) {
//         return "Connection to the Storage Server is accepted at address: " + address.toString() +"\r\n";
//     }

//     @Override
//     public void connectionClosed(InetAddress address) {
//     }
// }
