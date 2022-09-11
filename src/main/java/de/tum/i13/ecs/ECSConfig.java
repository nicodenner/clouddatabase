package de.tum.i13.ecs;

import de.tum.i13.shared.InetSocketAddressTypeConverter;
import picocli.CommandLine;
import java.net.InetSocketAddress;
import java.nio.file.Path;

public class ECSConfig {
    @CommandLine.Option(names = "-a", description = "Address of the ECS, used as bootstrap", defaultValue = "127.0.0.1")
    public String address;

    @CommandLine.Option(names = "-p", description = "Port of the ECS, used as bootstrap", defaultValue = "4200")
    public int port;

    @CommandLine.Option(names = "-ll", description = "Log level", defaultValue = "ALL")
    public String loglevel;

    @CommandLine.Option(names = "-l", description = "Logfile", defaultValue = "logs/ecs-server.log")
    public Path logfile;

    public static ECSConfig parseCommandlineArgs(String[] args) {
        ECSConfig cfg = new ECSConfig();
        CommandLine.ParseResult parseResult = new CommandLine(cfg).registerConverter(InetSocketAddress.class, new InetSocketAddressTypeConverter()).parseArgs(args);

        if(!parseResult.errors().isEmpty()) {
            for(Exception ex : parseResult.errors()) {
                ex.printStackTrace();
            }

            CommandLine.usage(new ECSConfig(), System.out);
            System.exit(-1);
        }

        return cfg;
    }

    @Override
    public String toString() {
        return "Config{" +
                "address='" + address + '\'' +
                ", port=" + port +
                ", logfile=" + logfile +
                ", loglevel='" + loglevel + '\'' +
                '}';
    }
}
